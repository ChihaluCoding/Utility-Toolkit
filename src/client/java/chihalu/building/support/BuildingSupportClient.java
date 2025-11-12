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

import chihalu.building.support.client.screen.DecoratedArmorPreviewScreen;
import chihalu.building.support.config.BuildingSupportConfig;
import chihalu.building.support.config.BuildingSupportConfig.ItemGroupOption;
import chihalu.building.support.customtabs.CustomTabsManager;
import chihalu.building.support.favorites.FavoritesManager;
import chihalu.building.support.history.HistoryManager;
import chihalu.building.support.mixin.client.CreativeInventoryScreenInvoker;
import chihalu.building.support.mixin.client.HandledScreenAccessor;
import chihalu.building.support.storage.SavedStack;

public class BuildingSupportClient implements ClientModInitializer {
	private static final KeyBinding.Category FAVORITE_CATEGORY = KeyBinding.Category.create(BuildingSupport.id("favorites"));
	private KeyBinding toggleFavoriteKey;
	private static String currentWorldKey = null;
	public static boolean hasActiveWorld() {
		return currentWorldKey != null;
	}

	public static String getCurrentWorldKey() {
		return currentWorldKey == null ? "" : currentWorldKey;
	}

	@Override
	public void onInitializeClient() {
		toggleFavoriteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.utility-toolkit.add_favorite",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			FAVORITE_CATEGORY
		));
		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			ScreenKeyboardEvents.beforeKeyPress(screen).register(new ScreenKeyboardEvents.BeforeKeyPress() {
				@Override
				public void beforeKeyPress(net.minecraft.client.gui.screen.Screen currentScreen, KeyInput input) {
					if (currentScreen instanceof CreativeInventoryScreen creativeScreen && input.isCopy()) {
						handleCopyItemIdShortcut(client, creativeScreen);
						return;
					}

					if (!toggleFavoriteKey.matchesKey(input)) {
						return;
					}

					boolean shiftPressed = isShiftDown();
					if (!(currentScreen instanceof CreativeInventoryScreen creativeScreen)) {
						if (client.player != null) {
							String key = shiftPressed
								? "message.utility-toolkit.custom_tab.require_creative"
								: "message.utility-toolkit.favorite.require_creative";
							client.player.sendMessage(Text.translatable(key), false);
						}
						return;
					}

					if (shiftPressed) {
						handleToggleCustomTab(client, creativeScreen);
					} else {
						handleToggleFavorite(client, creativeScreen);
					}
				}
			});
			ScreenMouseEvents.allowMouseClick(screen).register((currentScreen, click) -> {
				if (handleDecoratedPreviewClick(client, currentScreen, click)) {
					return false;
				}
				return true;
			});
		});

		registerUsageEvents();

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			client.execute(() -> {
				if (handler != null) {
					SavedStack.updateLookup(handler.getRegistryManager());
				}
				currentWorldKey = resolveWorldKey(client, handler);
				HistoryManager.getInstance().setActiveWorldKey(currentWorldKey);
				List<ItemStack> historyStacks = updateHistoryGroupStacks();
				if (client.currentScreen instanceof CreativeInventoryScreen creativeScreen) {
					refreshHistoryTabIfSelected(creativeScreen, historyStacks);
				}
			});
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			client.execute(() -> {
				SavedStack.resetLookup();
				currentWorldKey = null;
				HistoryManager.getInstance().setActiveWorldKey(null);
				BuildingSupportClient.onHistoryModeChanged();
			});
		});

		InventoryTabVisibilityController.reloadFromConfig();

	}

	private void handleCopyItemIdShortcut(MinecraftClient client, CreativeInventoryScreen screen) {
		// Ctrl + C でコピー要求が来た際に対象スロットを確認する
		Slot slot = ((HandledScreenAccessor) screen).utility_toolkit$getFocusedSlot();
		if (slot == null || !slot.hasStack()) {
			if (client.player != null) {
				client.player.sendMessage(Text.translatable("message.utility-toolkit.copy_item_id.no_item"), false);
			}
			return;
		}

		// 実際の ItemStack から登録名を解決してクリップボードへ書き込む
		ItemStack stack = slot.getStack();
		Identifier id = Registries.ITEM.getId(stack.getItem());
		if (client.keyboard != null) {
			client.keyboard.setClipboard(id.toString());
		}

		// メッセージ上ではID部分を紫色にして視認性を向上させる
		Text idText = Text.literal("(" + id.toString() + ")").formatted(Formatting.LIGHT_PURPLE);
		if (client.player != null) {
			client.player.sendMessage(
				Text.translatable("message.utility-toolkit.copy_item_id.success", idText)
					.formatted(Formatting.GREEN),
				false
			);
		}
	}

	private void handleToggleFavorite(MinecraftClient client, CreativeInventoryScreen screen) {
		BuildingSupportConfig config = BuildingSupportConfig.getInstance();
		if (!config.isItemGroupEnabled(ItemGroupOption.FAVORITES)) {
			if (client.player != null) {
				client.player.sendMessage(Text.translatable("message.utility-toolkit.favorite.tab_disabled"), false);
			}
			return;
		}

		Slot slot = ((HandledScreenAccessor) screen).utility_toolkit$getFocusedSlot();
		if (slot == null || !slot.hasStack()) {
			client.player.sendMessage(Text.translatable("message.utility-toolkit.favorite.no_item"), false);
			return;
		}

		ItemStack stack = slot.getStack();
		FavoritesManager manager = FavoritesManager.getInstance();
		boolean added = manager.toggleFavorite(stack); // 装飾を失わずにお気に入りへ登録する
		Text itemText = stack.toHoverableText();
		Text tabText = Text.translatable("itemGroup.utility-toolkit.favorites").formatted(Formatting.LIGHT_PURPLE);

		if (added) {
			client.player.sendMessage(Text.translatable("message.utility-toolkit.favorite.added", itemText, tabText).formatted(Formatting.GREEN), false);
		} else {
			client.player.sendMessage(Text.translatable("message.utility-toolkit.favorite.removed", itemText, tabText).formatted(Formatting.YELLOW), false);
		}

		ItemGroup favoritesGroup = Registries.ITEM_GROUP.get(BuildingSupport.FAVORITES_ITEM_GROUP_KEY);
		var favoritesStacks = manager.getDisplayStacksForTab();
		replaceGroupStacks(favoritesGroup, favoritesStacks);

		ItemGroup currentTab = CreativeInventoryScreenInvoker.utility_toolkit$getSelectedTab();
		if (currentTab == favoritesGroup) {
			((CreativeInventoryScreenInvoker) screen).utility_toolkit$refreshSelectedTab(favoritesStacks);
		}
	}

	private void handleToggleCustomTab(MinecraftClient client, CreativeInventoryScreen screen) {
		Slot slot = ((HandledScreenAccessor) screen).utility_toolkit$getFocusedSlot();
		if (slot == null || !slot.hasStack()) {
			if (client.player != null) {
				client.player.sendMessage(Text.translatable("message.utility-toolkit.custom_tab.no_item"), false);
			}
			return;
		}

		ItemStack stack = slot.getStack();
		CustomTabsManager manager = CustomTabsManager.getInstance();
		boolean added = manager.toggleItem(stack); // 実際に表示されている見た目をそのまま記録する
		Text itemText = stack.toHoverableText();
		String tabName = BuildingSupportConfig.getInstance().getCustomTabName();
		Text tabNameText = Text.literal(tabName).formatted(Formatting.LIGHT_PURPLE);

		if (client.player != null) {
			String messageKey = added ? "message.utility-toolkit.custom_tab.added" : "message.utility-toolkit.custom_tab.removed";
			Formatting color = added ? Formatting.GREEN : Formatting.YELLOW;
			client.player.sendMessage(Text.translatable(messageKey, itemText, tabNameText).formatted(color), false);
		}

		ItemGroup customGroup = Registries.ITEM_GROUP.get(BuildingSupport.CUSTOM_TAB_ITEM_GROUP_KEY);
		List<ItemStack> customStacks = manager.getDisplayStacksForTab();
		replaceGroupStacks(customGroup, customStacks);

		ItemGroup currentTab = CreativeInventoryScreenInvoker.utility_toolkit$getSelectedTab();
		if (currentTab == customGroup) {
			((CreativeInventoryScreenInvoker) screen).utility_toolkit$refreshSelectedTab(customStacks);
		}
	}

	// Ctrl+右クリック時に装飾済み装備のプレビュー画面を開く処理
	private boolean handleDecoratedPreviewClick(MinecraftClient client, net.minecraft.client.gui.screen.Screen currentScreen, Click click) {
		if (click.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT || !isControlDown()) {
			return false;
		}
		if (!(currentScreen instanceof CreativeInventoryScreen creativeScreen)) {
			return false;
		}
		Slot slot = ((HandledScreenAccessor) creativeScreen).utility_toolkit$getFocusedSlot();
		if (slot == null || !slot.hasStack()) {
			return false;
		}
		ItemStack stack = slot.getStack();
		if (!DecoratedArmorPreviewScreen.isDecoratedArmor(stack)) {
			return false;
		}
		client.setScreen(new DecoratedArmorPreviewScreen(currentScreen, stack));
		return true;
	}

	private static boolean isShiftDown() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getWindow() == null) {
			return false;
		}
		return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
			|| InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	// Ctrlキーの押下状態をまとめて確認するヘルパー
	private static boolean isControlDown() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getWindow() == null) {
			return false;
		}
		return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
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

		HistoryManager.getInstance().recordUsage(stack); // 使用履歴にも装飾込みで反映する

		List<ItemStack> historyStacks = updateHistoryGroupStacks();

		if (MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen creativeScreen) {
			refreshHistoryTabIfSelected(creativeScreen, historyStacks);
		}
	}

	private static List<ItemStack> updateHistoryGroupStacks() {
		BuildingSupportConfig config = BuildingSupportConfig.getInstance();
		if (!config.isItemGroupEnabled(ItemGroupOption.HISTORY)) {
			return List.of();
		}
		ItemGroup historyGroup = Registries.ITEM_GROUP.get(BuildingSupport.HISTORY_ITEM_GROUP_KEY);
		List<ItemStack> historyStacks = HistoryManager.getInstance().getDisplayStacksForTab();
		replaceGroupStacks(historyGroup, historyStacks);
		return historyStacks;
	}

	private static void refreshHistoryTabIfSelected(CreativeInventoryScreen screen, List<ItemStack> historyStacks) {
		if (!BuildingSupportConfig.getInstance().isItemGroupEnabled(ItemGroupOption.HISTORY)) {
			return;
		}
		ItemGroup historyGroup = Registries.ITEM_GROUP.get(BuildingSupport.HISTORY_ITEM_GROUP_KEY);
		ItemGroup currentTab = CreativeInventoryScreenInvoker.utility_toolkit$getSelectedTab();
		if (currentTab == historyGroup) {
			((CreativeInventoryScreenInvoker) screen).utility_toolkit$refreshSelectedTab(historyStacks);
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
