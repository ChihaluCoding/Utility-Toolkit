package chihalu.building.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
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
import java.util.LinkedHashSet;
import java.util.List;

public final class FavoritesManager {
	private static final FavoritesManager INSTANCE = new FavoritesManager();

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path configPath = FabricLoader.getInstance()
		.getConfigDir()
		.resolve(BuildingSupport.MOD_ID + "-favorites.json");
	private final LinkedHashSet<Identifier> favorites = new LinkedHashSet<>();

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

			if (data == null || data.favorites == null) {
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
					favorites.add(id);
				} else {
					BuildingSupport.LOGGER.warn("存在しないアイテムIDを無視しました: {}", entry);
				}
			}
		} catch (IOException | JsonSyntaxException exception) {
			BuildingSupport.LOGGER.error("お気に入り設定の読み込みに失敗しました: {}", configPath, exception);
		}
	}

	public synchronized boolean addFavorite(Identifier id) {
		boolean added = favorites.add(id);
		if (added) {
			save();
		}
		return added;
	}

	public synchronized boolean removeFavorite(Identifier id) {
		boolean removed = favorites.remove(id);
		if (removed) {
			save();
		}
		return removed;
	}

	public synchronized boolean toggleFavorite(Identifier id) {
		if (favorites.contains(id)) {
			favorites.remove(id);
			save();
			return false;
		}

		favorites.add(id);
		save();
		return true;
	}

	public synchronized void clearFavorites() {
		if (favorites.isEmpty()) {
			return;
		}

		favorites.clear();
		save();
	}

	public synchronized boolean isFavorite(Identifier id) {
		return favorites.contains(id);
	}

	public synchronized List<Identifier> getFavoriteIds() {
		return List.copyOf(favorites);
	}

	public synchronized ItemStack getIconStack() {
		for (Identifier id : favorites) {
			ItemStack stack = createStack(id);
			if (!stack.isEmpty()) {
				return stack;
			}
		}

		return new ItemStack(Items.NETHER_STAR);
	}

	public synchronized List<ItemStack> getFavoriteStacks() {
		List<ItemStack> stacks = new ArrayList<>();
		for (Identifier id : favorites) {
			ItemStack stack = createStack(id);
			if (!stack.isEmpty()) {
				stacks.add(stack);
			}
		}
		return stacks;
	}

	public synchronized List<ItemStack> getDisplayStacksForTab() {
		List<ItemStack> stacks = getFavoriteStacks();
		if (stacks.isEmpty()) {
			stacks.add(new ItemStack(Items.NETHER_STAR));
		}
		return stacks;
	}

	public synchronized void populate(ItemGroup.Entries entries) {
		List<ItemStack> stacks = getDisplayStacksForTab();

		for (ItemStack stack : stacks) {
			entries.add(stack.copy(), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	private ItemStack createStack(Identifier id) {
		if (!Registries.ITEM.containsId(id)) {
			return ItemStack.EMPTY;
		}

		return new ItemStack(Registries.ITEM.get(id));
	}

	private synchronized void save() {
		try {
			Files.createDirectories(configPath.getParent());
			SerializableData data = new SerializableData(favorites.stream().map(Identifier::toString).toList());

			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
			}
		} catch (IOException exception) {
			BuildingSupport.LOGGER.error("お気に入り設定の保存に失敗しました: {}", configPath, exception);
		}
	}

	private static final class SerializableData {
		private List<String> favorites;

		private SerializableData() {
		}

		private SerializableData(List<String> favorites) {
			this.favorites = favorites;
		}
	}
}
