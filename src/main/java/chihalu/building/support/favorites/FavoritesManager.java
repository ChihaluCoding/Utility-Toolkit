package chihalu.building.support.favorites;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
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
import chihalu.building.support.client.ClientNotificationBridge;
import chihalu.building.support.storage.SavedStack;

/**
 * Favorites tab manager for saved items.
 */
public final class FavoritesManager {
	private static final FavoritesManager INSTANCE = new FavoritesManager();
	private static final int MAX_SAVE_ATTEMPTS = 3;
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				INSTANCE.shutdown();
			} catch (Exception ignored) {
			}
		}, "UtilityToolkit-FavoritesShutdown"));
	}

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path configPath = BuildingSupportStorage.resolve("favorites.json");
	private final List<SavedStack> favorites = new ArrayList<>();
	private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "UtilityToolkit-FavoritesIO");
		thread.setDaemon(true);
		return thread;
	});
	private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

	private FavoritesManager() {
	}

	public static FavoritesManager getInstance() {
		return INSTANCE;
	}

	public void shutdown() {
		if (shuttingDown.compareAndSet(false, true)) {
			ioExecutor.shutdown();
		}
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
					SavedStack.fromSerialized(entry).ifPresent(favorites::add);
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
					BuildingSupport.LOGGER.warn("Ignored malformed item ID: {}", entry);
					continue;
				}
				if (Registries.ITEM.containsId(id)) {
					SavedStack.fromId(id).ifPresent(favorites::add);
				} else {
					BuildingSupport.LOGGER.warn("Ignored missing item ID: {}", entry);
				}
			}
		} catch (IOException | JsonSyntaxException exception) {
			BuildingSupport.LOGGER.error("Failed to load favorites data: {}", configPath, exception);
			notifyLoadFailure();
		}
	}

	public synchronized boolean addFavorite(Identifier id) {
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

	public synchronized boolean removeFavorite(Identifier id) {
		boolean removed = removeFirstMatching(id);
		if (removed) {
			saveAsync();
		}
		return removed;
	}

	public synchronized boolean toggleFavorite(Identifier id) {
		Iterator<SavedStack> iterator = favorites.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().id().equals(id)) {
				iterator.remove();
				saveAsync();
				return false;
			}
		}
		var saved = SavedStack.fromId(id);
		if (saved.isEmpty()) {
			return false;
		}
		favorites.add(saved.get());
		saveAsync();
		return true;
	}

	// Shift+B で現在のスロットをお気に入りに登録/解除する
	public synchronized boolean toggleFavorite(ItemStack stack) {
		var saved = SavedStack.capture(stack);
		if (saved.isEmpty()) {
			return false;
		}
		boolean added = toggleSnapshot(saved.get());
		saveAsync();
		return added;
	}

	public synchronized void clearFavorites() {
		if (favorites.isEmpty()) {
			return;
		}
		favorites.clear();
		saveAsync();
	}

	public synchronized boolean isFavorite(Identifier id) {
		return favorites.stream().anyMatch(saved -> saved.id().equals(id));
	}

	public synchronized List<Identifier> getFavoriteIds() {
		return favorites.stream()
			.map(SavedStack::id)
			.toList();
	}

	public synchronized ItemStack getIconStack() {
		for (SavedStack saved : favorites) {
			ItemStack stack = saved.toItemStack();
			if (!stack.isEmpty()) {
				return stack;
			}
		}
		return new ItemStack(Blocks.AMETHYST_CLUSTER);
	}

	public synchronized List<ItemStack> getFavoriteStacks() {
		List<ItemStack> stacks = new ArrayList<>();
		for (SavedStack saved : favorites) {
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
		for (ItemStack stack : getDisplayStacksForTab()) {
			entries.add(stack.copy(), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	private boolean addSnapshotIfAbsent(SavedStack snapshot) {
		for (SavedStack existing : favorites) {
			if (existing.isSameStack(snapshot)) {
				return false;
			}
		}
		favorites.add(snapshot);
		return true;
	}

	private boolean toggleSnapshot(SavedStack snapshot) {
		for (int i = 0; i < favorites.size(); i++) {
			if (favorites.get(i).isSameStack(snapshot)) {
				favorites.remove(i);
				return false;
			}
		}
		favorites.add(snapshot);
		return true;
	}

	private boolean removeFirstMatching(Identifier id) {
		Iterator<SavedStack> iterator = favorites.iterator();
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
		List<SavedStack> snapshot = List.copyOf(favorites);
		ioExecutor.execute(() -> writeSnapshot(snapshot, 0));
	}

	private void writeSnapshot(List<SavedStack> snapshot, int attempt) {
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
			BuildingSupport.LOGGER.error("Failed to save favorites data: {}", configPath, exception);
			if (attempt < MAX_SAVE_ATTEMPTS - 1 && !shuttingDown.get()) {
				notifySaveFailure("message.utility-toolkit.favorites.save_failed.retry");
				ioExecutor.execute(() -> {
					try {
						Thread.sleep((attempt + 1) * 1000L);
					} catch (InterruptedException interruptedException) {
						Thread.currentThread().interrupt();
						return;
					}
					writeSnapshot(snapshot, attempt + 1);
				});
			} else {
				notifySaveFailure("message.utility-toolkit.favorites.save_failed.final");
			}
		}
	}

	private void notifySaveFailure(String translationKey) {
		ClientNotificationBridge.notify(translationKey);
	}

	private void notifyLoadFailure() {
		ClientNotificationBridge.notify("message.utility-toolkit.favorites.load_failed");
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
