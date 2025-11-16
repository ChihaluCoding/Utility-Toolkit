package chihalu.building.support.customtabs;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import chihalu.building.support.BuildingSupport;
import chihalu.building.support.BuildingSupportStorage;
import chihalu.building.support.client.accessor.ItemGroupIconAccessor;
import chihalu.building.support.config.BuildingSupportConfig;
import chihalu.building.support.storage.SavedStack;

/**
 * カスタムタブに登録されたスタックを管理するクラス。
 */
public final class CustomTabsManager {
	public static final CustomTabsManager INSTANCE = new CustomTabsManager();
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				INSTANCE.shutdown();
			} catch (Exception ignored) {
			}
		}, "UtilityToolkit-CustomTabsShutdown"));
	}

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path configPath = BuildingSupportStorage.resolve("custom_tabs.json");
	private final List<SavedStack> items = new ArrayList<>();
	private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "UtilityToolkit-CustomTabsIO");
		thread.setDaemon(true);
		return thread;
	});
	private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
	private ItemGroup registeredGroup;

	private CustomTabsManager() {
	}

	public static CustomTabsManager getInstance() {
		return INSTANCE;
	}

	public void shutdown() {
		if (shuttingDown.compareAndSet(false, true)) {
			ioExecutor.shutdown();
		}
	}

	public synchronized void reload() {
		items.clear();
		if (!Files.exists(configPath)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
			SerializableData data = gson.fromJson(reader, SerializableData.class);
			if (data == null) {
				return;
			}
			if (data.entries != null && !data.entries.isEmpty()) {
				boolean needsRewrite = loadSerializedEntries(data.entries);
				if (needsRewrite) {
					saveAsync();
				}
				return;
			}
			if (data.items == null || data.items.isEmpty()) {
				return;
			}
			if (migrateLegacyItems(data.items)) {
				saveAsync();
			}
		} catch (IOException | JsonSyntaxException exception) {
			BuildingSupport.LOGGER.error("custom_tabs.json の読み込みに失敗しました: {}", configPath, exception);
		}
	}

	public synchronized void registerGroupInstance(ItemGroup group) {
		this.registeredGroup = group;
	}

	public synchronized boolean isCustomTabGroup(ItemGroup group) {
		return registeredGroup == group;
	}

	public synchronized boolean addItem(Identifier id) {
		if (!isValidItem(id)) {
			return false;
		}
		var saved = SavedStack.fromId(id);
		if (saved.isEmpty()) {
			return false;
		}
		boolean added = addSnapshotIfAbsent(saved.get());
		if (added) {
			saveAsync();
		}
		return added;
	}

	public synchronized boolean removeItem(Identifier id) {
		boolean removed = removeFirstMatching(id);
		if (removed) {
			saveAsync();
		}
		return removed;
	}

	public synchronized boolean toggleItem(Identifier id) {
		if (!isValidItem(id)) {
			return false;
		}
		int index = findIndexByIdentifier(id);
		if (index >= 0) {
			items.remove(index);
			saveAsync();
			return false;
		}
		var saved = SavedStack.fromId(id);
		if (saved.isEmpty()) {
			return false;
		}
		items.add(saved.get());
		saveAsync();
		return true;
	}

	// Shift + B で選択中のスタックをトグル登録する
	public synchronized boolean toggleItem(ItemStack stack) {
		var saved = SavedStack.capture(stack);
		if (saved.isEmpty()) {
			return false;
		}
		boolean added = toggleSnapshot(saved.get());
		saveAsync();
		return added;
	}

	public synchronized void clear() {
		if (items.isEmpty()) {
			return;
		}
		items.clear();
		saveAsync();
	}

	public synchronized List<Identifier> getItems() {
		return items.stream()
			.map(SavedStack::id)
			.toList();
	}

	public synchronized ItemStack getIconStack() {
		ItemStack configured = BuildingSupportConfig.getInstance().getCustomTabIconStack();
		if (!configured.isEmpty()) {
			return configured.copy();
		}
		List<ItemStack> stacks = getDisplayStacks();
		return stacks.isEmpty() ? new ItemStack(Items.PAPER) : stacks.get(0);
	}

	public synchronized List<ItemStack> getDisplayStacksForTab() {
		return getDisplayStacks();
	}

	private synchronized List<ItemStack> getDisplayStacks() {
		List<ItemStack> stacks = new ArrayList<>();
		for (SavedStack saved : items) {
			ItemStack stack = saved.toItemStack();
			if (stack.isEmpty()) {
				continue;
			}
			boolean duplicate = stacks.stream().anyMatch(existing -> ItemStack.areEqual(existing, stack));
			if (!duplicate) {
				stacks.add(stack);
			}
		}
		if (stacks.isEmpty()) {
			stacks.add(new ItemStack(Items.PAPER));
		}
		return List.copyOf(stacks);
	}

	public synchronized void populateEntries(ItemGroup.Entries entries) {
		for (ItemStack stack : getDisplayStacks()) {
			entries.add(stack.copy(), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	public synchronized void refreshGroupIcon() {
		if (registeredGroup instanceof ItemGroupIconAccessor accessor) {
			accessor.utility_toolkit$resetIconCache();
			registeredGroup.getIcon();
		}
	}

	private boolean isValidItem(Identifier id) {
		return id != null && Registries.ITEM.containsId(id);
	}

	private boolean loadSerializedEntries(List<SavedStack.Serialized> entries) {
		boolean needsRewrite = false;
		for (SavedStack.Serialized entry : entries) {
			if (entry == null) {
				continue;
			}
			boolean hasSerializedStack = (entry.stack != null && !entry.stack.isJsonNull())
				|| (entry.nbt != null && !entry.nbt.isBlank());
			if (!hasSerializedStack) {
				needsRewrite = true;
			}
			if (SavedStack.fromSerialized(entry).map(items::add).orElse(false)) {
				continue;
			}
			needsRewrite = true;
		}
		return needsRewrite;
	}

	private boolean migrateLegacyItems(List<String> legacyItems) {
		boolean migrated = false;
		for (String rawId : legacyItems) {
			if (rawId == null || rawId.isBlank()) {
				continue;
			}
			Identifier id = Identifier.tryParse(rawId.trim());
			if (!isValidItem(id)) {
				BuildingSupport.LOGGER.warn("カスタムタブに追加できないIDを検出しました: {}", rawId);
				continue;
			}
			if (SavedStack.fromId(id).map(items::add).orElse(false)) {
				migrated = true;
			}
		}
		return migrated;
	}

	private boolean addSnapshotIfAbsent(SavedStack snapshot) {
		for (SavedStack existing : items) {
			if (existing.isSameStack(snapshot)) {
				return false;
			}
		}
		items.add(snapshot);
		return true;
	}

	private boolean toggleSnapshot(SavedStack snapshot) {
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).isSameStack(snapshot)) {
				items.remove(i);
				return false;
			}
		}
		items.add(snapshot);
		return true;
	}

	private int findIndexByIdentifier(Identifier id) {
		if (id == null) {
			return -1;
		}
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).id().equals(id)) {
				return i;
			}
		}
		return -1;
	}

	private boolean removeFirstMatching(Identifier id) {
		Iterator<SavedStack> iterator = items.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().id().equals(id)) {
				iterator.remove();
				return true;
			}
		}
		return false;
	}

	private void saveAsync() {
		if (shuttingDown.get()) {
			return;
		}
		List<SavedStack> snapshot = List.copyOf(items);
		ioExecutor.execute(() -> writeSnapshot(snapshot));
	}

	private void writeSnapshot(List<SavedStack> snapshot) {
		List<SavedStack.Serialized> serialized = snapshot.stream()
			.map(SavedStack::toSerialized)
			.toList();
		SerializableData data = new SerializableData(serialized);
		try {
			Files.createDirectories(configPath.getParent());
			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
			}
		} catch (IOException exception) {
			BuildingSupport.LOGGER.error("custom_tabs.json の保存に失敗しました: {}", configPath, exception);
		}
	}

	private static final class SerializableData {
		private List<String> items;
		private List<SavedStack.Serialized> entries;

		private SerializableData() {
		}

		private SerializableData(List<SavedStack.Serialized> entries) {
			this.entries = entries;
		}
	}
}

