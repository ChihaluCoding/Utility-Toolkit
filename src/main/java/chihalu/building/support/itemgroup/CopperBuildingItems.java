package chihalu.building.support.itemgroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * 銅関連ブロックのリストを管理するユーティリティ。
 */
public final class CopperBuildingItems {
	private static final List<ItemStack> COPPER_BLOCKS = buildCopperBlockList();

	private CopperBuildingItems() {
	}

	public static void populate(ItemGroup.Entries entries) {
		for (ItemStack stack : COPPER_BLOCKS) {
			entries.add(stack.copy(), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	public static ItemStack getIconStack() {
		return new ItemStack(Items.COPPER_BLOCK);
	}

	private static List<ItemStack> buildCopperBlockList() {
		List<ItemStack> stacks = new ArrayList<>();

		stacks.add(new ItemStack(Items.COPPER_ORE));
		stacks.add(new ItemStack(Items.DEEPSLATE_COPPER_ORE));
		stacks.add(new ItemStack(Items.RAW_COPPER_BLOCK));

		addWeatheringFamily(stacks,
			Items.COPPER_BLOCK, Items.EXPOSED_COPPER, Items.WEATHERED_COPPER, Items.OXIDIZED_COPPER,
			Items.WAXED_COPPER_BLOCK, Items.WAXED_EXPOSED_COPPER, Items.WAXED_WEATHERED_COPPER, Items.WAXED_OXIDIZED_COPPER);
		addWeatheringFamily(stacks,
			Items.CUT_COPPER, Items.EXPOSED_CUT_COPPER, Items.WEATHERED_CUT_COPPER, Items.OXIDIZED_CUT_COPPER,
			Items.WAXED_CUT_COPPER, Items.WAXED_EXPOSED_CUT_COPPER, Items.WAXED_WEATHERED_CUT_COPPER, Items.WAXED_OXIDIZED_CUT_COPPER);
		addWeatheringFamily(stacks,
			Items.CUT_COPPER_SLAB, Items.EXPOSED_CUT_COPPER_SLAB, Items.WEATHERED_CUT_COPPER_SLAB, Items.OXIDIZED_CUT_COPPER_SLAB,
			Items.WAXED_CUT_COPPER_SLAB, Items.WAXED_EXPOSED_CUT_COPPER_SLAB, Items.WAXED_WEATHERED_CUT_COPPER_SLAB, Items.WAXED_OXIDIZED_CUT_COPPER_SLAB);
		addWeatheringFamily(stacks,
			Items.CUT_COPPER_STAIRS, Items.EXPOSED_CUT_COPPER_STAIRS, Items.WEATHERED_CUT_COPPER_STAIRS, Items.OXIDIZED_CUT_COPPER_STAIRS,
			Items.WAXED_CUT_COPPER_STAIRS, Items.WAXED_EXPOSED_CUT_COPPER_STAIRS, Items.WAXED_WEATHERED_CUT_COPPER_STAIRS, Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS);
		addWeatheringFamily(stacks,
			Items.COPPER_DOOR, Items.EXPOSED_COPPER_DOOR, Items.WEATHERED_COPPER_DOOR, Items.OXIDIZED_COPPER_DOOR,
			Items.WAXED_COPPER_DOOR, Items.WAXED_EXPOSED_COPPER_DOOR, Items.WAXED_WEATHERED_COPPER_DOOR, Items.WAXED_OXIDIZED_COPPER_DOOR);
		addWeatheringFamily(stacks,
			Items.COPPER_TRAPDOOR, Items.EXPOSED_COPPER_TRAPDOOR, Items.WEATHERED_COPPER_TRAPDOOR, Items.OXIDIZED_COPPER_TRAPDOOR,
			Items.WAXED_COPPER_TRAPDOOR, Items.WAXED_EXPOSED_COPPER_TRAPDOOR, Items.WAXED_WEATHERED_COPPER_TRAPDOOR, Items.WAXED_OXIDIZED_COPPER_TRAPDOOR);
		addWeatheringFamily(stacks,
			Items.COPPER_GRATE, Items.EXPOSED_COPPER_GRATE, Items.WEATHERED_COPPER_GRATE, Items.OXIDIZED_COPPER_GRATE,
			Items.WAXED_COPPER_GRATE, Items.WAXED_EXPOSED_COPPER_GRATE, Items.WAXED_WEATHERED_COPPER_GRATE, Items.WAXED_OXIDIZED_COPPER_GRATE);
		addWeatheringFamily(stacks,
			Items.COPPER_BULB, Items.EXPOSED_COPPER_BULB, Items.WEATHERED_COPPER_BULB, Items.OXIDIZED_COPPER_BULB,
			Items.WAXED_COPPER_BULB, Items.WAXED_EXPOSED_COPPER_BULB, Items.WAXED_WEATHERED_COPPER_BULB, Items.WAXED_OXIDIZED_COPPER_BULB);

		addWeatheringFamily(stacks, "lightning_rod");
		addWeatheringFamily(stacks, "chiseled_copper");

		addItemIfPresent(stacks, "chain");

		addWeatheringFamily(stacks, "copper_chain");
		addWeatheringFamily(stacks, "copper_lantern");
		addWeatheringFamily(stacks, "copper_bars");
		addWeatheringFamily(stacks, "copper_torch");
		addWeatheringFamily(stacks, "copper_chest");

		return Collections.unmodifiableList(stacks);
	}

	private static void addWeatheringFamily(List<ItemStack> stacks, Item... variants) {
		for (Item item : variants) {
			if (item != null) {
				stacks.add(new ItemStack(item));
			}
		}
	}

	private static void addWeatheringFamily(List<ItemStack> stacks, String baseId) {
		addVariant(stacks, baseId);
		addVariant(stacks, "exposed_" + baseId);
		addVariant(stacks, "weathered_" + baseId);
		addVariant(stacks, "oxidized_" + baseId);
		addVariant(stacks, "waxed_" + baseId);
		addVariant(stacks, "waxed_exposed_" + baseId);
		addVariant(stacks, "waxed_weathered_" + baseId);
		addVariant(stacks, "waxed_oxidized_" + baseId);
	}

	private static void addVariant(List<ItemStack> stacks, String id) {
		addItemIfPresent(stacks, id);
	}

	private static void addItemIfPresent(List<ItemStack> stacks, String id) {
		Identifier identifier = Identifier.ofVanilla(id);
		if (!Registries.ITEM.containsId(identifier)) {
			return;
		}
		Item item = Registries.ITEM.get(identifier);
		if (item != Items.AIR) {
			stacks.add(new ItemStack(item));
		}
	}
}
