package chihalu.building.support;

import java.util.List;
import java.nio.file.Path;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.ActionResult;
import net.minecraft.util.WorldSavePath;
import org.lwjgl.glfw.GLFW;

import chihalu.building.support.mixin.client.CreativeInventoryScreenInvoker;
import chihalu.building.support.mixin.client.HandledScreenAccessor;

public class BuildingSupportClient implements ClientModInitializer {
	private static final KeyBinding.Category FAVORITE_CATEGORY = KeyBinding.Category.create(BuildingSupport.id("favorites"));
	private KeyBinding toggleFavoriteKey;
	private static String currentWorldKey = null;

	@Override
	public void onInitializeClient() {
		toggleFavoriteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.building-support.add_favorite",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			FAVORITE_CATEGORY
		));

		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			ScreenKeyboardEvents.beforeKeyPress(screen).register(new ScreenKeyboardEvents.BeforeKeyPress() {
				@Override
				public void beforeKeyPress(net.minecraft.client.gui.screen.Screen currentScreen, KeyInput input) {
					if (!toggleFavoriteKey.matchesKey(input)) {
						return;
					}

					if (!(currentScreen instanceof CreativeInventoryScreen creativeScreen)) {
						if (client.player != null) {
							client.player.sendMessage(Text.translatable("message.building-support.favorite.require_creative"), false);
						}
						return;
					}

					handleToggleFavorite(client, creativeScreen);
				}
			});

			ScreenMouseEvents.afterMouseClick(screen).register((currentScreen, click, consumed) -> {
				handleHistoryClick(currentScreen, click);
				return consumed;
			});
		});

		registerUsageEvents();

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			client.execute(() -> {
				currentWorldKey = resolveWorldKey(client, handler);
				HistoryManager.getInstance().setActiveWorldKey(currentWorldKey);
				List<ItemStack> historyStacks = updateHistoryGroupStacks();
				if (client.currentScreen instanceof CreativeInventoryScreen creativeScreen) {
					refreshHistoryTabIfSelected(creativeScreen, historyStacks);
				}
			});
		});
	}

	private void handleToggleFavorite(MinecraftClient client, CreativeInventoryScreen screen) {
		Slot slot = ((HandledScreenAccessor) screen).building_support$getFocusedSlot();
		if (slot == null || !slot.hasStack()) {
			client.player.sendMessage(Text.translatable("message.building-support.favorite.no_item"), false);
			return;
		}

		ItemStack stack = slot.getStack();
		Identifier id = Registries.ITEM.getId(stack.getItem());
		FavoritesManager manager = FavoritesManager.getInstance();
		boolean added = manager.toggleFavorite(id);
		Text itemText = stack.toHoverableText();

		if (added) {
			client.player.sendMessage(Text.translatable("message.building-support.favorite.added", itemText).formatted(Formatting.GREEN), false);
		} else {
			client.player.sendMessage(Text.translatable("message.building-support.favorite.removed", itemText).formatted(Formatting.YELLOW), false);
		}

		ItemGroup favoritesGroup = Registries.ITEM_GROUP.get(BuildingSupport.FAVORITES_ITEM_GROUP_KEY);
		var favoritesStacks = manager.getDisplayStacksForTab();

		replaceGroupStacks(favoritesGroup, favoritesStacks);

		ItemGroup currentTab = CreativeInventoryScreenInvoker.building_support$getSelectedTab();

		if (currentTab == favoritesGroup) {
			((CreativeInventoryScreenInvoker) screen).building_support$refreshSelectedTab(favoritesStacks);
		}
	}

	private void handleHistoryClick(net.minecraft.client.gui.screen.Screen currentScreen, Click click) {
		if (!(currentScreen instanceof CreativeInventoryScreen creativeScreen)) {
			return;
		}

		Slot slot = ((HandledScreenAccessor) creativeScreen).building_support$getFocusedSlot();
		if (slot == null || !slot.hasStack()) {
			return;
		}

		ItemStack stack = slot.getStack();
		if (stack.isEmpty()) {
			return;
		}

		recordHistoryUsage(stack);
	}

	private void registerUsageEvents() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClient() && player != null) {
				recordHistoryUsage(player.getStackInHand(hand));
			}
			return ActionResult.PASS;
		});

		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (world.isClient() && player != null) {
				recordHistoryUsage(player.getStackInHand(hand));
			}
			return ActionResult.PASS;
		});
	}

	public static void recordHistoryUsage(ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		Identifier id = Registries.ITEM.getId(stack.getItem());
		HistoryManager.getInstance().recordUsage(id);

		List<ItemStack> historyStacks = updateHistoryGroupStacks();

		if (MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen creativeScreen) {
			refreshHistoryTabIfSelected(creativeScreen, historyStacks);
		}
	}

	private static List<ItemStack> updateHistoryGroupStacks() {
		ItemGroup historyGroup = Registries.ITEM_GROUP.get(BuildingSupport.HISTORY_ITEM_GROUP_KEY);
		List<ItemStack> historyStacks = HistoryManager.getInstance().getDisplayStacksForTab();
		replaceGroupStacks(historyGroup, historyStacks);
		return historyStacks;
	}

	private static void refreshHistoryTabIfSelected(CreativeInventoryScreen screen, List<ItemStack> historyStacks) {
		ItemGroup historyGroup = Registries.ITEM_GROUP.get(BuildingSupport.HISTORY_ITEM_GROUP_KEY);
		ItemGroup currentTab = CreativeInventoryScreenInvoker.building_support$getSelectedTab();
		if (currentTab == historyGroup) {
			((CreativeInventoryScreenInvoker) screen).building_support$refreshSelectedTab(historyStacks);
		}
	}

	private static void replaceGroupStacks(ItemGroup group, List<ItemStack> newStacks) {
		var displayStacks = group.getDisplayStacks();
		displayStacks.clear();
		for (ItemStack stack : newStacks) {
			displayStacks.add(stack.copy());
		}

		var searchStacks = group.getSearchTabStacks();
		searchStacks.clear();
		for (ItemStack stack : newStacks) {
			searchStacks.add(stack.copy());
		}
	}

	public static void onHistoryModeChanged() {
		MinecraftClient client = MinecraftClient.getInstance();
		HistoryManager.getInstance().reloadActive();
		List<ItemStack> historyStacks = updateHistoryGroupStacks();
		if (client.currentScreen instanceof CreativeInventoryScreen creativeScreen) {
			refreshHistoryTabIfSelected(creativeScreen, historyStacks);
		}
	}

	private static String resolveWorldKey(MinecraftClient client, ClientPlayNetworkHandler handler) {
		if (client.getServer() != null) {
			String levelName = client.getServer().getSaveProperties().getLevelName();
			String absolutePath = "";
			try {
				Path path = client.getServer().getSavePath(WorldSavePath.ROOT);
				if (path != null) {
					Path dir = path.getFileName();
					if (dir != null && !dir.toString().isBlank() && !dir.toString().equals(".")) {
						absolutePath = dir.toString();
					} else {
						absolutePath = path.toAbsolutePath().normalize().toString();
					}
				}
			} catch (Exception ignored) {
			}
			return "singleplayer:" + levelName + ":" + absolutePath;
		}

		if (client.getCurrentServerEntry() != null) {
			String address = client.getCurrentServerEntry().address == null ? "unknown" : client.getCurrentServerEntry().address;
			String name = client.getCurrentServerEntry().name == null ? "unnamed" : client.getCurrentServerEntry().name;
			return "multiplayer:" + address + ":" + name;
		}

		if (handler != null && handler.getConnection().getAddress() != null) {
			return "direct:" + handler.getConnection().getAddress().toString();
		}

		if (handler != null && handler.getWorld() != null) {
			return "world:" + handler.getWorld().getRegistryKey().getValue().toString();
		}

		return "unknown_world";
	}
}
