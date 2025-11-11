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

	private final LinkedHashMap<Identifier, SavedStack> items = new LinkedHashMap<>();
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
				for (SavedStack.Serialized entry : data.entries) {
					SavedStack.fromSerialized(entry).ifPresent(saved -> items.put(saved.id(), saved));
				}
				return;
			}
			if (data.items == null) {
				return;
			}
			for (String rawId : data.items) {
				if (rawId == null || rawId.isBlank()) {
					continue;
				}
				Identifier id = Identifier.tryParse(rawId.trim());
				if (!isValidItem(id)) {
					BuildingSupport.LOGGER.warn("無効なカスタムタブ用IDを検出しました: {}", rawId);
					continue;
				}
				SavedStack.fromId(id).ifPresent(saved -> items.put(saved.id(), saved));
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
		boolean removed = items.remove(id) != null;
		if (removed) {
			save();
		}
		return removed;
	}

	public synchronized boolean toggleItem(Identifier id) {
		if (!isValidItem(id)) {
			return false;
		}
		if (items.containsKey(id)) {
			items.remove(id);
			save();
			return false;
		}
		var saved = SavedStack.fromId(id);
		if (saved.isEmpty()) {
			return false;
		}
		items.put(id, saved.get());
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
		return List.copyOf(items.keySet());
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

	// 既に登録済みかどうかを確認しつつスナップショットを保持する
	private boolean putSnapshotIfAbsent(SavedStack snapshot) {
		if (items.containsKey(snapshot.id())) {
			return false;
		}
		items.put(snapshot.id(), snapshot);
		return true;
	}

	// トグル操作時に使用するヘルパー。存在すれば削除、無ければ追加する。
	private boolean toggleSnapshot(SavedStack snapshot) {
		Identifier key = snapshot.id();
		if (items.containsKey(key)) {
			items.remove(key);
			return false;
		}
		items.put(key, snapshot);
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
