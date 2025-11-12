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
import java.util.LinkedHashMap;
import java.util.List;

import chihalu.building.support.BuildingSupport;
import chihalu.building.support.BuildingSupportStorage;
import chihalu.building.support.config.BuildingSupportConfig;
import chihalu.building.support.client.accessor.ItemGroupIconAccessor;
import chihalu.building.support.storage.SavedStack;

/**
 * カスタムタブに登録されたアイテムを管理するクラス。
 */
public final class CustomTabsManager {
	public static final CustomTabsManager INSTANCE = new CustomTabsManager();

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path configPath = BuildingSupportStorage.resolve("custom_tabs.json");

	private final LinkedHashMap<String, SavedStack> items = new LinkedHashMap<>();
	private ItemGroup registeredGroup;

	private CustomTabsManager() {
	}

	public static CustomTabsManager getInstance() {
		return INSTANCE;
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
				// JSONに保存されたスタック情報をそのまま読み込み、欠落があれば後で再保存して補完する
				boolean needsRewrite = loadSerializedEntries(data.entries);
				if (needsRewrite) {
					save();
				}
				return;
			}
			if (data.items == null || data.items.isEmpty()) {
				return;
			}
			// 旧フォーマット(items配列のみ)の場合はIDだけを頼りに現在の表現へ移行する
			if (migrateLegacyItems(data.items)) {
				save();
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
		boolean added = putSnapshotIfAbsent(saved.get());
		if (added) {
			save();
		}
		return added;
	}

	public synchronized boolean removeItem(Identifier id) {
		boolean removed = removeFirstMatching(id);
		if (removed) {
			save();
		}
		return removed;
	}

	public synchronized boolean toggleItem(Identifier id) {
		if (!isValidItem(id)) {
			return false;
		}
		String existingKey = findKeyByIdentifier(id);
		if (existingKey != null) {
			items.remove(existingKey);
			save();
			return false;
		}
		var saved = SavedStack.fromId(id);
		if (saved.isEmpty()) {
			return false;
		}
		items.put(saved.get().uniqueKey(), saved.get());
		save();
		return true;
	}

	// Shift + B から渡されたスタックを装飾込みでカスタムタブに保持する
	public synchronized boolean toggleItem(ItemStack stack) {
		var saved = SavedStack.capture(stack);
		if (saved.isEmpty()) {
			return false;
		}
		boolean added = toggleSnapshot(saved.get());
		save();
		return added;
	}

	public synchronized void clear() {
		if (items.isEmpty()) {
			return;
		}
		items.clear();
		save();
	}

	public synchronized List<Identifier> getItems() {
		return items.values().stream()
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
		for (SavedStack saved : items.values()) {
			ItemStack stack = saved.toItemStack();
			if (!stack.isEmpty()) {
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
		// entries一覧を順序通りに復元しつつ、スタック情報が欠けていないか確認する
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
			SavedStack.fromSerialized(entry).ifPresent(saved -> items.put(saved.uniqueKey(), saved));
		}
		return needsRewrite;
	}

	private boolean migrateLegacyItems(List<String> legacyItems) {
		// 旧データのID群を SavedStack に変換して現在の形式へ差し替える
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
			if (SavedStack.fromId(id).map(saved -> {
				items.put(saved.uniqueKey(), saved);
				return true;
			}).orElse(false)) {
				migrated = true;
			}
		}
		return migrated;
	}

	// 既に登録済みかどうかを確認しつつスナップショットを保持する
	// ID単位での登録状態を維持するヘルパー
	private boolean putSnapshotIfAbsent(SavedStack snapshot) {
		String key = snapshot.uniqueKey();
		if (items.containsKey(key)) {
			return false;
		}
		items.put(key, snapshot);
		return true;
	}

	// 実際の見た目単位でトグルするヘルパー
	private boolean toggleSnapshot(SavedStack snapshot) {
		String key = snapshot.uniqueKey();
		if (items.containsKey(key)) {
			items.remove(key);
			return false;
		}
		items.put(key, snapshot);
		return true;
	}

	private String findKeyByIdentifier(Identifier id) {
		if (id == null) {
			return null;
		}
		for (var entry : items.entrySet()) {
			if (entry.getValue().id().equals(id)) {
				return entry.getKey();
			}
		}
		return null;
	}

	private boolean removeFirstMatching(Identifier id) {
		String key = findKeyByIdentifier(id);
		if (key == null) {
			return false;
		}
		items.remove(key);
		return true;
	}

	private synchronized void save() {
		List<SavedStack.Serialized> serialized = items.values().stream()
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

