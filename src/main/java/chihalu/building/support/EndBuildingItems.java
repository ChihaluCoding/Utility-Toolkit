package chihalu.building.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * ジ・エンド関連の建築ブロックをまとめたユーティリティ。
 */
public final class EndBuildingItems {
	private static final List<ItemStack> END_BLOCKS = buildEndBlockList();

	private EndBuildingItems() {
	}

	public static void populate(ItemGroup.Entries entries) {
		for (ItemStack stack : END_BLOCKS) {
			entries.add(stack.copy(), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	public static ItemStack getIconStack() {
		return new ItemStack(Items.END_STONE_BRICKS);
	}

	private static List<ItemStack> buildEndBlockList() {
		List<ItemStack> stacks = new ArrayList<>();

		addSet(stacks, Items.END_STONE, Items.END_STONE_BRICKS, Items.END_STONE_BRICK_STAIRS, Items.END_STONE_BRICK_SLAB, Items.END_STONE_BRICK_WALL);
		addSet(stacks, Items.PURPUR_BLOCK, Items.PURPUR_PILLAR, Items.PURPUR_STAIRS, Items.PURPUR_SLAB);
		addSet(stacks, Items.CHORUS_PLANT, Items.CHORUS_FLOWER, Items.POPPED_CHORUS_FRUIT);
		addSet(stacks, Items.END_ROD, Items.SHULKER_BOX);

		return Collections.unmodifiableList(stacks);
	}

	private static void addSet(List<ItemStack> stacks, net.minecraft.item.Item... items) {
		for (net.minecraft.item.Item item : items) {
			stacks.add(new ItemStack(item));
		}
	}
}
