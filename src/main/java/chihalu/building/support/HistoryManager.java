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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class HistoryManager {
	private static final HistoryManager INSTANCE = new HistoryManager();
	private static final int MAX_HISTORY = 64;

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path configPath = FabricLoader.getInstance()
		.getConfigDir()
		.resolve(BuildingSupport.MOD_ID + "-history.json");

	private final Deque<Identifier> recentItems = new ArrayDeque<>();

	private HistoryManager() {
	}

	public static HistoryManager getInstance() {
		return INSTANCE;
	}

	public synchronized void reload() {
		recentItems.clear();

		if (!Files.exists(configPath)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
			SerializableData data = gson.fromJson(reader, SerializableData.class);
			if (data == null || data.items == null) {
				return;
			}

			for (String idString : data.items) {
				if (idString == null || idString.isBlank()) {
					continue;
				}

				Identifier id = Identifier.tryParse(idString.trim());
				if (id != null && Registries.ITEM.containsId(id)) {
					pushWithoutSave(id);
				}
			}
		} catch (IOException | JsonSyntaxException exception) {
			BuildingSupport.LOGGER.error("履歴データの読み込みに失敗しました: {}", configPath, exception);
		}
	}

	public synchronized void recordUsage(Identifier id) {
		if (id == null || !Registries.ITEM.containsId(id)) {
			return;
		}

		recentItems.remove(id);
		recentItems.addFirst(id);

		while (recentItems.size() > MAX_HISTORY) {
			recentItems.removeLast();
		}

		save();
	}

	public synchronized List<ItemStack> getHistoryStacks() {
		List<ItemStack> stacks = new ArrayList<>();
		for (Identifier id : recentItems) {
			ItemStack stack = createStack(id);
			if (!stack.isEmpty()) {
				stacks.add(stack);
			}
		}
		return stacks;
	}

	public synchronized List<ItemStack> getDisplayStacksForTab() {
		List<ItemStack> stacks = getHistoryStacks();
		if (stacks.isEmpty()) {
			stacks.add(new ItemStack(Items.GLOWSTONE));
		}
		return stacks;
	}

	public synchronized ItemStack getIconStack() {
		for (Identifier id : recentItems) {
			ItemStack stack = createStack(id);
			if (!stack.isEmpty()) {
				return stack;
			}
		}
		return new ItemStack(Items.GLOWSTONE);
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

	private void pushWithoutSave(Identifier id) {
		recentItems.remove(id);
		recentItems.addFirst(id);
		while (recentItems.size() > MAX_HISTORY) {
			recentItems.removeLast();
		}
	}

	private synchronized void save() {
		try {
			Files.createDirectories(configPath.getParent());
			SerializableData data = new SerializableData(recentItems.stream().map(Identifier::toString).toList());
			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
			}
		} catch (IOException exception) {
			BuildingSupport.LOGGER.error("履歴データの保存に失敗しました: {}", configPath, exception);
		}
	}

	private static final class SerializableData {
		private List<String> items;

		private SerializableData(List<String> items) {
			this.items = items;
		}
	}
}
