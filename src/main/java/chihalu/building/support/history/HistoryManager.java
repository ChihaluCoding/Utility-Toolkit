package chihalu.building.support.history;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import chihalu.building.support.BuildingSupport;
import chihalu.building.support.config.BuildingSupportConfig;
import chihalu.building.support.BuildingSupportStorage;
import chihalu.building.support.storage.SavedStack;

public final class HistoryManager {
	private static final HistoryManager INSTANCE = new HistoryManager();
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				INSTANCE.shutdown();
			} catch (Exception ignored) {
			}
		}, "UtilityToolkit-HistoryShutdown"));
	}
	private static final int MAX_HISTORY = 64;
	private static final String DEFAULT_WORLD_KEY = "unknown_world";
	private static final String ALL_WORLD_FILE_NAME = "all_world.json";
	private static final String GLOBAL_HISTORY_WORLD_KEY = "global_history";

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path historyDir = BuildingSupportStorage.resolve("history");

	private final Deque<SavedStack> recentItems = new ArrayDeque<>();
	private Path activeHistoryPath = getWorldHistoryPath(DEFAULT_WORLD_KEY);
	private String activeWorldKey = DEFAULT_WORLD_KEY;
	// 履歴ファイルの読み書きをメインスレッドから切り離すための専用I/Oスレッド
	private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "UtilityToolkit-HistoryIO");
		thread.setDaemon(true);
		return thread;
	});
	private final AtomicBoolean executorShutdown = new AtomicBoolean(false);
	// 全ワールド履歴をキャッシュして繰り返しのディスクI/Oを避ける
	private final Object globalCacheLock = new Object();
	private Deque<SavedStack> globalHistoryCache = new ArrayDeque<>();
	private FileTime globalCacheTimestamp = FileTime.fromMillis(0L);
	private boolean globalCacheInitialized = false;

	private HistoryManager() {
	}

	public static HistoryManager getInstance() {
		return INSTANCE;
	}

	public void shutdown() {
		if (executorShutdown.compareAndSet(false, true)) {
			ioExecutor.shutdown();
		}
	}

	public synchronized void initialize() {
		try {
			Files.createDirectories(historyDir);
		} catch (IOException exception) {
			BuildingSupport.LOGGER.error("Failed to prepare history directory: {}", historyDir, exception);
		}
		setActiveWorldKey(null);
	}

	public synchronized void setActiveWorldKey(String worldKey) {
		String sanitized = sanitize(worldKey);
		activeHistoryPath = getWorldHistoryPath(sanitized);
		this.activeWorldKey = worldKey == null || worldKey.isBlank() ? DEFAULT_WORLD_KEY : worldKey;
		reloadActive();
	}

	public synchronized void reloadActive() {
		recentItems.clear();
		recentItems.addAll(loadHistory(activeHistoryPath));
	}

	// 装飾を含む最新の ItemStack を履歴へ記録する
	public synchronized void recordUsage(ItemStack stack) {
		SavedStack.capture(stack).ifPresent(this::recordSnapshot);
	}

	// 旧API互換: ID 指定のみで履歴へ登録する
	public synchronized void recordUsage(Identifier id) {
		SavedStack.fromId(id).ifPresent(this::recordSnapshot);
	}

	public synchronized List<ItemStack> getDisplayStacksForTab() {
		List<ItemStack> stacks = new ArrayList<>();
		for (SavedStack saved : getHistoryEntriesForDisplay()) {
			if (saved == null) {
				continue;
			}
			ItemStack stack = saved.toItemStack();
			if (stack.isEmpty()) {
				continue;
			}
			stack.setCount(1);
			if (containsStack(stacks, stack)) {
				continue;
			}
			stacks.add(stack);
		}
		if (stacks.isEmpty()) {
			stacks.add(new ItemStack(Items.BOOK));
		}
		return stacks;
	}

	public synchronized ItemStack getIconStack() {
		List<ItemStack> displayStacks = getDisplayStacksForTab();
		return displayStacks.get(0).copy();
	}

	public synchronized void populate(ItemGroup.Entries entries) {
		for (ItemStack stack : getDisplayStacksForTab()) {
			entries.add(stack.copy(), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	// スナップショット化した履歴をメモリとディスクへ反映する共通処理
	private void recordSnapshot(SavedStack snapshot) {
		updateDeque(recentItems, snapshot);
		Path historyPath = activeHistoryPath;
		String worldKeySnapshot = activeWorldKey;
		saveHistoryAsync(historyPath, recentItems, worldKeySnapshot);
		updateGlobalHistory(snapshot);
	}

	private Deque<SavedStack> getHistoryEntriesForDisplay() {
		BuildingSupportConfig.HistoryDisplayMode mode = BuildingSupportConfig.getInstance().getHistoryDisplayMode();
		if (mode == BuildingSupportConfig.HistoryDisplayMode.ALL_WORLD) {
			return loadHistory(getGlobalHistoryPath());
		}
		return new ArrayDeque<>(recentItems);
	}

	private static boolean containsStack(List<ItemStack> stacks, ItemStack candidate) {
		for (ItemStack existing : stacks) {
			if (ItemStack.areEqual(existing, candidate)) {
				return true;
			}
		}
		return false;
	}

	private void updateGlobalHistory(SavedStack snapshot) {
		Path globalPath = getGlobalHistoryPath();
		Deque<SavedStack> global = loadHistory(globalPath);
		updateDeque(global, snapshot);
		saveHistoryAsync(globalPath, global, GLOBAL_HISTORY_WORLD_KEY);
	}

	public synchronized boolean resetHistory(Path historyPath) {
		boolean deleted = deleteHistoryFile(historyPath);
		if (deleted && historyPath.equals(activeHistoryPath)) {
			reloadActive();
		}
		return deleted;
	}

	public synchronized boolean resetActiveWorldHistory() {
		boolean deleted = deleteHistoryFile(activeHistoryPath);
		if (deleted) {
			reloadActive();
		}
		return deleted;
	}

	public synchronized boolean resetGlobalHistory() {
		// 共有履歴ファイルとワールド別履歴ファイルを両方削除してリセットとみなす
		boolean deletedGlobal = deleteHistoryFile(getGlobalHistoryPath());
		boolean deletedWorlds = deleteWorldHistoryFiles();
		if (deletedGlobal) {
			invalidateGlobalCache();
		}
		return deletedGlobal || deletedWorlds;
	}

	// 履歴ディレクトリ内からワールド別履歴ファイルを列挙し、すべて削除する
	private boolean deleteWorldHistoryFiles() {
		if (!Files.exists(historyDir)) {
			return false;
		}

		boolean deletedAny = false;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(historyDir, "*.json")) {
			for (Path path : stream) {
				if (ALL_WORLD_FILE_NAME.equals(path.getFileName().toString())) {
					continue;
				}
				if (deleteHistoryFile(path)) {
					deletedAny = true;
				}
			}
		} catch (IOException exception) {
			BuildingSupport.LOGGER.error("Failed to delete world-specific history files: {}", historyDir, exception);
		}
		return deletedAny;
	}

	private void invalidateGlobalCache() {
		synchronized (globalCacheLock) {
			globalHistoryCache = new ArrayDeque<>();
			globalCacheTimestamp = FileTime.fromMillis(0L);
			globalCacheInitialized = false;
		}
	}

	private boolean deleteHistoryFile(Path path) {
		try {
			boolean deleted = Files.deleteIfExists(path);
			if (deleted && path.equals(getGlobalHistoryPath())) {
				invalidateGlobalCache();
			}
			return deleted;
		} catch (IOException exception) {
			BuildingSupport.LOGGER.error("Failed to delete history data: {}", path, exception);
			return false;
		}
	}

	public synchronized List<WorldHistoryInfo> listWorldHistories() {
		List<WorldHistoryInfo> entries = new ArrayList<>();
		if (!Files.exists(historyDir)) {
			return entries;
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(historyDir, "*.json")) {
			for (Path path : stream) {
				if (Files.isDirectory(path)) {
					continue;
				}
				String fileName = path.getFileName().toString();
				if (ALL_WORLD_FILE_NAME.equals(fileName)) {
					continue;
				}
				String fallback = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
				String displayName = readSerializableData(path)
					.map(data -> data.worldKey)
					.filter(key -> key != null && !key.isBlank())
					.orElse(fallback);
				entries.add(new WorldHistoryInfo(path, displayName, fallback));
			}
		} catch (IOException exception) {
			BuildingSupport.LOGGER.error("Failed to scan history directory: {}", historyDir, exception);
		}

		entries.sort(Comparator.comparing(WorldHistoryInfo::displayName, String.CASE_INSENSITIVE_ORDER));
		return entries;
	}

	private Deque<SavedStack> loadHistory(Path path) {
		if (path.equals(getGlobalHistoryPath())) {
			return loadGlobalHistoryWithCache();
		}
		return readHistoryFromDisk(path);
	}

	private Deque<SavedStack> loadGlobalHistoryWithCache() {
		Path globalPath = getGlobalHistoryPath();
		FileTime lastModified = getFileTimestamp(globalPath);
		synchronized (globalCacheLock) {
			if (!globalCacheInitialized || !Objects.equals(lastModified, globalCacheTimestamp)) {
				Deque<SavedStack> loaded = readHistoryFromDisk(globalPath);
				globalHistoryCache = new ArrayDeque<>(loaded);
				globalCacheTimestamp = lastModified;
				globalCacheInitialized = true;
				return new ArrayDeque<>(loaded);
			}
			return new ArrayDeque<>(globalHistoryCache);
		}
	}

	private FileTime getFileTimestamp(Path path) {
		try {
			return Files.exists(path) ? Files.getLastModifiedTime(path) : FileTime.fromMillis(0L);
		} catch (IOException exception) {
			return FileTime.fromMillis(0L);
		}
	}

	private Deque<SavedStack> readHistoryFromDisk(Path path) {
		Deque<SavedStack> deque = new ArrayDeque<>();
		Optional<SerializableData> data = readSerializableData(path);
		if (data.isEmpty()) {
			return deque;
		}
		SerializableData serializableData = data.get();
		String targetWorldKey = resolveWorldKeyForSave(serializableData.worldKey, path);

		if (serializableData.entries != null && !serializableData.entries.isEmpty()) {
			// entries が存在する場合は保存されている ItemStack をそのまま復元し、欠損があれば後で上書き保存する
			boolean needsRewrite = appendSerializedEntries(serializableData.entries, deque);
			if (needsRewrite) {
				saveHistoryAsync(path, deque, targetWorldKey);
			}
			return deque;
		}
		if (serializableData.items == null || serializableData.items.isEmpty()) {
			return deque;
		}
		// 旧フォーマット(items配列)から読み取った場合は現在の形式へ書き戻す
		if (appendLegacyItems(serializableData.items, deque)) {
			saveHistoryAsync(path, deque, targetWorldKey);
		}
		return deque;
	}

	/**
	 * メインスレッドで収集した履歴内容を即座にスナップショットし、I/O専用スレッドで非同期保存する。
	 */
	private void saveHistoryAsync(Path path, Deque<SavedStack> deque, String worldKey) {
		if (executorShutdown.get()) {
			return;
		}
		Deque<SavedStack> snapshot = new ArrayDeque<>(deque);
		ioExecutor.execute(() -> {
			try {
				writeHistorySnapshot(path, snapshot, worldKey);
			} catch (Exception exception) {
				BuildingSupport.LOGGER.error("履歴データの保存中にエラーが発生しました: {}", path, exception);
			}
		});
	}

	private void writeHistorySnapshot(Path path, Deque<SavedStack> snapshot, String worldKey) {
		try {
			Files.createDirectories(historyDir);
			List<SavedStack.Serialized> serialized = snapshot.stream()
				.map(SavedStack::toSerialized)
				.toList();
			SerializableData data = new SerializableData(serialized, worldKey);
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
			}
			if (path.equals(getGlobalHistoryPath())) {
				updateGlobalCacheFromSnapshot(snapshot, path);
			}
		} catch (IOException exception) {
			BuildingSupport.LOGGER.error("Failed to save history data: {}", path, exception);
		}
	}

	private void updateGlobalCacheFromSnapshot(Deque<SavedStack> snapshot, Path path) {
		FileTime timestamp = getFileTimestamp(path);
		synchronized (globalCacheLock) {
			globalHistoryCache = new ArrayDeque<>(snapshot);
			globalCacheTimestamp = timestamp;
			globalCacheInitialized = true;
		}
	}

	private Optional<SerializableData> readSerializableData(Path path) {
		if (!Files.exists(path)) {
			return Optional.empty();
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			SerializableData data = gson.fromJson(reader, SerializableData.class);
			return Optional.ofNullable(data);
		} catch (IOException | JsonSyntaxException exception) {
			BuildingSupport.LOGGER.error("Failed to read history data: {}", path, exception);
		}
		return Optional.empty();
	}

	private boolean appendSerializedEntries(List<SavedStack.Serialized> entries, Deque<SavedStack> deque) {
		// 保存済みエントリを新しいデータ構造へ流し込みつつ、不足や破損を検知する
		boolean needsRewrite = false;
		for (int i = entries.size() - 1; i >= 0; i--) {
			SavedStack.Serialized entry = entries.get(i);
			if (entry == null) {
				needsRewrite = true;
				continue;
			}
			boolean hasSerializedStack = (entry.stack != null && !entry.stack.isJsonNull())
				|| (entry.nbt != null && !entry.nbt.isBlank());
			if (!hasSerializedStack) {
				needsRewrite = true;
			}
			boolean added = SavedStack.fromSerialized(entry).map(saved -> {
				updateDeque(deque, saved);
				return true;
			}).orElse(false);
			if (!added) {
				needsRewrite = true;
			}
		}
		return needsRewrite;
	}

	private boolean appendLegacyItems(List<String> items, Deque<SavedStack> deque) {
		// 旧データのID一覧を ItemStack に変換して履歴へ加える
		boolean migrated = false;
		for (int i = items.size() - 1; i >= 0; i--) {
			String idString = items.get(i);
			if (idString == null || idString.isBlank()) {
				continue;
			}
			Identifier id = Identifier.tryParse(idString.trim());
			if (id == null || !Registries.ITEM.containsId(id)) {
				BuildingSupport.LOGGER.warn("履歴に復元できないアイテムIDを検出しました: {}", idString);
				continue;
			}
			if (SavedStack.fromId(id).map(saved -> {
				updateDeque(deque, saved);
				return true;
			}).orElse(false)) {
				migrated = true;
			}
		}
		return migrated;
	}

	private String resolveWorldKeyForSave(String storedKey, Path path) {
		// worldKey が空の場合はファイル情報から識別子を推測して保存時に利用する
		if (storedKey != null && !storedKey.isBlank()) {
			return storedKey;
		}
		if (path.equals(getGlobalHistoryPath())) {
			return GLOBAL_HISTORY_WORLD_KEY;
		}
		String fileName = path.getFileName().toString();
		if (fileName.endsWith(".json")) {
			fileName = fileName.substring(0, fileName.length() - 5);
		}
		return fileName.isBlank() ? DEFAULT_WORLD_KEY : fileName;
	}

	private static void updateDeque(Deque<SavedStack> deque, SavedStack snapshot) {
		deque.removeIf(existing -> existing.isSameStack(snapshot));
		deque.addFirst(snapshot);
		while (deque.size() > MAX_HISTORY) {
			deque.removeLast();
		}
	}

	private Path getWorldHistoryPath(String sanitizedKey) {
		return historyDir.resolve(sanitizedKey + ".json");
	}

	private Path getGlobalHistoryPath() {
		return historyDir.resolve(ALL_WORLD_FILE_NAME);
	}

	private static String sanitize(String key) {
		if (key == null || key.isBlank()) {
			return DEFAULT_WORLD_KEY;
		}
		String sanitized = key.replaceAll("[^a-zA-Z0-9._-]", "_");
		if (sanitized.isBlank()) {
			return DEFAULT_WORLD_KEY;
		}
		if (sanitized.length() > 80) {
			sanitized = sanitized.substring(0, 80);
		}
		String hash = Integer.toUnsignedString(key.hashCode());
		return sanitized + "_" + hash;
	}

	public static final class WorldHistoryInfo {
		private final Path path;
		private final String displayName;
		private final String fileName;

		private WorldHistoryInfo(Path path, String displayName, String fileName) {
			this.path = path;
			this.displayName = displayName;
			this.fileName = fileName;
		}

		public Path path() {
			return path;
		}

		public String displayName() {
			return displayName;
		}

		public String fileName() {
			return fileName;
		}
	}

	private static final class SerializableData {
		private List<String> items;
		private List<SavedStack.Serialized> entries;
		private String worldKey;

		private SerializableData() {
		}

		private SerializableData(List<SavedStack.Serialized> entries, String worldKey) {
			this.entries = entries;
			this.worldKey = worldKey;
		}
	}
}
