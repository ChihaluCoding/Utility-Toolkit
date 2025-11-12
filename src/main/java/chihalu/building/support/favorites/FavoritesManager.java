package chihalu.building.support.favorites;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Blocks;
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
import chihalu.building.support.storage.SavedStack;

public final class FavoritesManager {
	private static final FavoritesManager INSTANCE = new FavoritesManager();

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path configPath = BuildingSupportStorage.resolve("favorites.json");
	private final LinkedHashMap<String, SavedStack> favorites = new LinkedHashMap<>();

	private FavoritesManager() {
	}

	public static FavoritesManager getInstance() {
		return INSTANCE;
	}

	public synchronized void reload() {
		favorites.clear();

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
					SavedStack.fromSerialized(entry).ifPresent(saved -> favorites.put(saved.uniqueKey(), saved));
				}
				return;
			}

			if (data.favorites == null) {
				return;
			}

			for (String entry : data.favorites) {
				if (entry == null || entry.isBlank()) {
					continue;
				}

				Identifier id = Identifier.tryParse(entry.trim());
				if (id == null) {
					BuildingSupport.LOGGER.warn("無効なアイテムIDを無視しました: {}", entry);
					continue;
				}

				if (Registries.ITEM.containsId(id)) {
					SavedStack.fromId(id).ifPresent(saved -> favorites.put(saved.uniqueKey(), saved));
				} else {
					BuildingSupport.LOGGER.warn("存在しないアイテムIDを無視しました: {}", entry);
				}
			}
		} catch (IOException | JsonSyntaxException exception) {
			BuildingSupport.LOGGER.error("お気に入り設定の読み込みに失敗しました: {}", configPath, exception);
		}
	}

	public synchronized boolean addFavorite(Identifier id) {
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

	public synchronized boolean removeFavorite(Identifier id) {
		boolean removed = removeFirstMatching(id);
		if (removed) {
			save();
		}
		return removed;
	}

	public synchronized boolean toggleFavorite(Identifier id) {
		String existingKey = findKeyByIdentifier(id);
		if (existingKey != null) {
			favorites.remove(existingKey);
			save();
			return false;
		}
		var saved = SavedStack.fromId(id);
		if (saved.isEmpty()) {
			return false;
		}
		favorites.put(saved.get().uniqueKey(), saved.get());
		save();
		return true;
	}

	// 装飾を含めたスタックをそのままお気に入りへトグル登録する
	public synchronized boolean toggleFavorite(ItemStack stack) {
		var saved = SavedStack.capture(stack);
		if (saved.isEmpty()) {
			return false;
		}
		boolean added = toggleSnapshot(saved.get());
		save();
		return added;
	}

	public synchronized void clearFavorites() {
		if (favorites.isEmpty()) {
			return;
		}

		favorites.clear();
		save();
	}

	public synchronized boolean isFavorite(Identifier id) {
		return favorites.values().stream().anyMatch(saved -> saved.id().equals(id));
	}

	public synchronized List<Identifier> getFavoriteIds() {
		return favorites.values().stream()
			.map(SavedStack::id)
			.toList();
	}

	public synchronized ItemStack getIconStack() {
		for (SavedStack saved : favorites.values()) {
			ItemStack stack = saved.toItemStack();
			if (!stack.isEmpty()) {
				return stack;
			}
		}
		return new ItemStack(Blocks.AMETHYST_CLUSTER);
	}

	public synchronized List<ItemStack> getFavoriteStacks() {
		List<ItemStack> stacks = new ArrayList<>();
		for (SavedStack saved : favorites.values()) {
			ItemStack stack = saved.toItemStack();
			if (!stack.isEmpty()) {
				stacks.add(stack);
			}
		}
		return stacks;
	}

	public synchronized List<ItemStack> getDisplayStacksForTab() {
		List<ItemStack> stacks = getFavoriteStacks();
		if (stacks.isEmpty()) {
			stacks.add(new ItemStack(Blocks.AMETHYST_CLUSTER));
		}
		return stacks;
	}

	public synchronized void populate(ItemGroup.Entries entries) {
		List<ItemStack> stacks = getDisplayStacksForTab();

		for (ItemStack stack : stacks) {
			entries.add(stack.copy(), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	// 同じIDが二重登録されないように確認しつつ追加するためのヘルパー
	private boolean putSnapshotIfAbsent(SavedStack snapshot) {
		String key = snapshot.uniqueKey();
		if (favorites.containsKey(key)) {
			return false;
		}
		favorites.put(key, snapshot);
		return true;
	}

	// ���� ID �ɑ΂��镶���Ԃ��߂̃w���p�[
	private boolean toggleSnapshot(SavedStack snapshot) {
		String key = snapshot.uniqueKey();
		if (favorites.containsKey(key)) {
			favorites.remove(key);
			return false;
		}
		favorites.put(key, snapshot);
		return true;
	}

	private String findKeyByIdentifier(Identifier id) {
		if (id == null) {
			return null;
		}
		for (var entry : favorites.entrySet()) {
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
		favorites.remove(key);
		return true;
	}

	private synchronized void save() {
		try {
			Files.createDirectories(configPath.getParent());
			List<SavedStack.Serialized> serialized = favorites.values().stream()
				.map(SavedStack::toSerialized)
				.toList();
			SerializableData data = new SerializableData(serialized);

			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
			}
		} catch (IOException exception) {
			BuildingSupport.LOGGER.error("お気に入り設定の保存に失敗しました: {}", configPath, exception);
		}
	}

	private static final class SerializableData {
		private List<String> favorites;
		private List<SavedStack.Serialized> entries;

		private SerializableData() {
		}

		private SerializableData(List<SavedStack.Serialized> entries) {
			this.entries = entries;
		}
	}
}


