package chihalu.building.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * 銅系建築ブロックの一覧を管理するユーティリティ。
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

		stacks.add(new ItemStack(Items.RAW_COPPER));
		stacks.add(new ItemStack(Items.COPPER_INGOT));
		stacks.add(new ItemStack(Items.COPPER_ORE));
		stacks.add(new ItemStack(Items.DEEPSLATE_COPPER_ORE));
		stacks.add(new ItemStack(Items.RAW_COPPER_BLOCK));

		// 酸化段階ごとの銅ブロックと派生ブロック
		addCopperTier(stacks,
			Items.COPPER_BLOCK, Items.CUT_COPPER, Items.CUT_COPPER_SLAB, Items.CUT_COPPER_STAIRS,
			Items.COPPER_DOOR, Items.COPPER_TRAPDOOR, Items.COPPER_GRATE, Items.COPPER_BULB);
		addCopperTier(stacks,
			Items.EXPOSED_COPPER, Items.EXPOSED_CUT_COPPER, Items.EXPOSED_CUT_COPPER_SLAB, Items.EXPOSED_CUT_COPPER_STAIRS,
			Items.EXPOSED_COPPER_DOOR, Items.EXPOSED_COPPER_TRAPDOOR, Items.EXPOSED_COPPER_GRATE, Items.EXPOSED_COPPER_BULB);
		addCopperTier(stacks,
			Items.WEATHERED_COPPER, Items.WEATHERED_CUT_COPPER, Items.WEATHERED_CUT_COPPER_SLAB, Items.WEATHERED_CUT_COPPER_STAIRS,
			Items.WEATHERED_COPPER_DOOR, Items.WEATHERED_COPPER_TRAPDOOR, Items.WEATHERED_COPPER_GRATE, Items.WEATHERED_COPPER_BULB);
		addCopperTier(stacks,
			Items.OXIDIZED_COPPER, Items.OXIDIZED_CUT_COPPER, Items.OXIDIZED_CUT_COPPER_SLAB, Items.OXIDIZED_CUT_COPPER_STAIRS,
			Items.OXIDIZED_COPPER_DOOR, Items.OXIDIZED_COPPER_TRAPDOOR, Items.OXIDIZED_COPPER_GRATE, Items.OXIDIZED_COPPER_BULB);

		// ワックス済みバリエーション
		addCopperTier(stacks,
			Items.WAXED_COPPER_BLOCK, Items.WAXED_CUT_COPPER, Items.WAXED_CUT_COPPER_SLAB, Items.WAXED_CUT_COPPER_STAIRS,
			Items.WAXED_COPPER_DOOR, Items.WAXED_COPPER_TRAPDOOR, Items.WAXED_COPPER_GRATE, Items.WAXED_COPPER_BULB);
		addCopperTier(stacks,
			Items.WAXED_EXPOSED_COPPER, Items.WAXED_EXPOSED_CUT_COPPER, Items.WAXED_EXPOSED_CUT_COPPER_SLAB, Items.WAXED_EXPOSED_CUT_COPPER_STAIRS,
			Items.WAXED_EXPOSED_COPPER_DOOR, Items.WAXED_EXPOSED_COPPER_TRAPDOOR, Items.WAXED_EXPOSED_COPPER_GRATE, Items.WAXED_EXPOSED_COPPER_BULB);
		addCopperTier(stacks,
			Items.WAXED_WEATHERED_COPPER, Items.WAXED_WEATHERED_CUT_COPPER, Items.WAXED_WEATHERED_CUT_COPPER_SLAB, Items.WAXED_WEATHERED_CUT_COPPER_STAIRS,
			Items.WAXED_WEATHERED_COPPER_DOOR, Items.WAXED_WEATHERED_COPPER_TRAPDOOR, Items.WAXED_WEATHERED_COPPER_GRATE, Items.WAXED_WEATHERED_COPPER_BULB);
		addCopperTier(stacks,
			Items.WAXED_OXIDIZED_COPPER, Items.WAXED_OXIDIZED_CUT_COPPER, Items.WAXED_OXIDIZED_CUT_COPPER_SLAB, Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS,
			Items.WAXED_OXIDIZED_COPPER_DOOR, Items.WAXED_OXIDIZED_COPPER_TRAPDOOR, Items.WAXED_OXIDIZED_COPPER_GRATE, Items.WAXED_OXIDIZED_COPPER_BULB);

		// その他銅系建材
		stacks.add(new ItemStack(Items.LIGHTNING_ROD));
		stacks.add(new ItemStack(Items.CHISELED_COPPER));
		stacks.add(new ItemStack(Items.EXPOSED_CHISELED_COPPER));
		stacks.add(new ItemStack(Items.WEATHERED_CHISELED_COPPER));
		stacks.add(new ItemStack(Items.OXIDIZED_CHISELED_COPPER));
		stacks.add(new ItemStack(Items.WAXED_CHISELED_COPPER));
		stacks.add(new ItemStack(Items.WAXED_EXPOSED_CHISELED_COPPER));
		stacks.add(new ItemStack(Items.WAXED_WEATHERED_CHISELED_COPPER));
		stacks.add(new ItemStack(Items.WAXED_OXIDIZED_CHISELED_COPPER));

		return Collections.unmodifiableList(stacks);
	}

	private static void addCopperTier(List<ItemStack> stacks,
									  net.minecraft.item.Item block,
									  net.minecraft.item.Item cut,
									  net.minecraft.item.Item slab,
									  net.minecraft.item.Item stairs,
									  net.minecraft.item.Item door,
									  net.minecraft.item.Item trapdoor,
									  net.minecraft.item.Item grate,
									  net.minecraft.item.Item bulb) {
		stacks.add(new ItemStack(block));
		stacks.add(new ItemStack(cut));
		stacks.add(new ItemStack(slab));
		stacks.add(new ItemStack(stairs));
		stacks.add(new ItemStack(door));
		stacks.add(new ItemStack(trapdoor));
		stacks.add(new ItemStack(grate));
		stacks.add(new ItemStack(bulb));
	}
}
