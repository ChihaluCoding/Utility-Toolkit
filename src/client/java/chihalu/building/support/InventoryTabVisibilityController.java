package chihalu.building.support;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;

import chihalu.building.support.config.BuildingSupportConfig;
import chihalu.building.support.config.BuildingSupportConfig.ItemGroupOption;
import chihalu.building.support.mixin.client.CreativeInventoryScreenInvoker;

/**
 * 設定変更を即座に反映し、クリエイティブタブの表示状態を切り替えるためのコントローラ。
 */
@Environment(EnvType.CLIENT)
public final class InventoryTabVisibilityController {
	/**
	 * 制御対象となるタブと設定オプションの対応表。ここで対象を限定して安全に処理する。
	 */
	private static final Map<RegistryKey<ItemGroup>, ItemGroupOption> OPTION_BY_KEY = Map.ofEntries(
		Map.entry(BuildingSupport.FAVORITES_ITEM_GROUP_KEY, ItemGroupOption.FAVORITES),
		Map.entry(BuildingSupport.HISTORY_ITEM_GROUP_KEY, ItemGroupOption.HISTORY),
		Map.entry(BuildingSupport.WOOD_BUILDING_ITEM_GROUP_KEY, ItemGroupOption.WOOD_BUILDING),
		Map.entry(BuildingSupport.STONE_BUILDING_ITEM_GROUP_KEY, ItemGroupOption.STONE_BUILDING),
		Map.entry(BuildingSupport.COPPER_BUILDING_ITEM_GROUP_KEY, ItemGroupOption.COPPER_BUILDING),
		Map.entry(BuildingSupport.LIGHT_BUILDING_ITEM_GROUP_KEY, ItemGroupOption.LIGHT_BUILDING),
		Map.entry(BuildingSupport.NETHER_BUILDING_ITEM_GROUP_KEY, ItemGroupOption.NETHER_BUILDING),
		Map.entry(BuildingSupport.END_BUILDING_ITEM_GROUP_KEY, ItemGroupOption.END_BUILDING),
		Map.entry(BuildingSupport.SIGN_SHELF_ITEM_GROUP_KEY, ItemGroupOption.SIGN_SHELF),
		Map.entry(BuildingSupport.LEATHER_EQUIPMENT_ITEM_GROUP_KEY, ItemGroupOption.ARMOR_LEATHER),
		Map.entry(BuildingSupport.CHAIN_EQUIPMENT_ITEM_GROUP_KEY, ItemGroupOption.ARMOR_CHAIN),
		Map.entry(BuildingSupport.IRON_EQUIPMENT_ITEM_GROUP_KEY, ItemGroupOption.ARMOR_IRON),
		Map.entry(BuildingSupport.GOLD_EQUIPMENT_ITEM_GROUP_KEY, ItemGroupOption.ARMOR_GOLD),
		Map.entry(BuildingSupport.DIAMOND_EQUIPMENT_ITEM_GROUP_KEY, ItemGroupOption.ARMOR_DIAMOND),
		Map.entry(BuildingSupport.NETHERITE_EQUIPMENT_ITEM_GROUP_KEY, ItemGroupOption.ARMOR_NETHERITE)
	);

	/**
	 * 現在非表示扱いになっているタブ集合。IdentityHashMap 由来のセットで高速に判定できる。
	 */
	private static final Set<ItemGroup> HIDDEN_GROUPS =
		java.util.Collections.newSetFromMap(new IdentityHashMap<>());

	private InventoryTabVisibilityController() {
	}

	/**
	 * 設定値を再読み込みして非表示リストを更新し、必要に応じて画面をリフレッシュする。
	 */
	public static void reloadFromConfig() {
		BuildingSupportConfig config = BuildingSupportConfig.getInstance();
		HIDDEN_GROUPS.clear();
		OPTION_BY_KEY.forEach((key, option) -> {
			ItemGroup group = Registries.ITEM_GROUP.get(key);
			if (group != null && !config.isItemGroupEnabled(option)) {
				HIDDEN_GROUPS.add(group);
			}
		});
		refreshCreativeScreen();
	}

	/**
	 * ItemGroups#getGroupsToDisplay() の結果をフィルタして UI に渡す。
	 */
	public static List<ItemGroup> filterGroups(List<ItemGroup> original) {
		if (HIDDEN_GROUPS.isEmpty() || original.isEmpty()) {
			return original;
		}
		List<ItemGroup> filtered = new ArrayList<>(original.size());
		for (ItemGroup group : original) {
			if (!HIDDEN_GROUPS.contains(group)) {
				filtered.add(group);
			}
		}
		if (filtered.size() == original.size()) {
			return original;
		}
		return List.copyOf(filtered);
	}

	/**
	 * クリエイティブ画面を開いている場合、表示禁止タブが選択されていないか確認して差し替える。
	 */
	private static void refreshCreativeScreen() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!(client.currentScreen instanceof CreativeInventoryScreen screen)) {
			return;
		}
		ItemGroup selected = CreativeInventoryScreenInvoker.utility_toolkit$getSelectedTab();
		CreativeInventoryScreenInvoker invoker = (CreativeInventoryScreenInvoker) screen;
		if (selected == null || HIDDEN_GROUPS.contains(selected)) {
			invoker.utility_toolkit$setSelectedTab(ItemGroups.getDefaultTab());
		} else {
			invoker.utility_toolkit$setSelectedTab(selected);
		}
	}
}
