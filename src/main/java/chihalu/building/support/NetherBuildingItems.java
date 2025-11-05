package chihalu.building.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * ネザーで入手できる建材をまとめたユーティリティ。
 */
public final class NetherBuildingItems {
	private static final List<ItemStack> NETHER_BLOCKS = buildNetherBlockList();

	private NetherBuildingItems() {
	}

	public static void populate(ItemGroup.Entries entries) {
		for (ItemStack stack : NETHER_BLOCKS) {
			entries.add(stack.copy(), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	public static ItemStack getIconStack() {
		return new ItemStack(Items.NETHER_BRICKS);
	}

	private static List<ItemStack> buildNetherBlockList() {
		List<ItemStack> stacks = new ArrayList<>();

		addSet(stacks, Items.NETHER_BRICKS, Items.CRACKED_NETHER_BRICKS, Items.CHISELED_NETHER_BRICKS,
			Items.NETHER_BRICK_STAIRS, Items.NETHER_BRICK_SLAB, Items.NETHER_BRICK_WALL);
		addSet(stacks, Items.RED_NETHER_BRICKS,
			Items.RED_NETHER_BRICK_STAIRS, Items.RED_NETHER_BRICK_SLAB, Items.RED_NETHER_BRICK_WALL);
		addSet(stacks, Items.NETHER_WART_BLOCK, Items.WARPED_WART_BLOCK, Items.SHROOMLIGHT);
		addSet(stacks, Items.SOUL_SAND, Items.SOUL_SOIL, Items.MAGMA_BLOCK);
		addSet(stacks, Items.BASALT, Items.SMOOTH_BASALT, Items.POLISHED_BASALT);
		addSet(stacks, Items.BLACKSTONE, Items.POLISHED_BLACKSTONE, Items.POLISHED_BLACKSTONE_BRICKS,
			Items.BLACKSTONE_STAIRS, Items.BLACKSTONE_SLAB, Items.BLACKSTONE_WALL,
			Items.POLISHED_BLACKSTONE_STAIRS, Items.POLISHED_BLACKSTONE_SLAB, Items.POLISHED_BLACKSTONE_WALL,
			Items.POLISHED_BLACKSTONE_BRICK_STAIRS, Items.POLISHED_BLACKSTONE_BRICK_SLAB, Items.POLISHED_BLACKSTONE_BRICK_WALL,
			Items.GILDED_BLACKSTONE);
		addSet(stacks, Items.QUARTZ_BLOCK, Items.SMOOTH_QUARTZ, Items.CHISELED_QUARTZ_BLOCK, Items.QUARTZ_PILLAR,
			Items.QUARTZ_STAIRS, Items.QUARTZ_SLAB, Items.SMOOTH_QUARTZ_STAIRS, Items.SMOOTH_QUARTZ_SLAB);
		addSet(stacks, Items.NETHER_QUARTZ_ORE, Items.ANCIENT_DEBRIS, Items.GLOWSTONE);
		addSet(stacks, Items.OBSIDIAN, Items.CRYING_OBSIDIAN);

		return Collections.unmodifiableList(stacks);
	}

	private static void addSet(List<ItemStack> stacks, net.minecraft.item.Item... items) {
		for (net.minecraft.item.Item item : items) {
			stacks.add(new ItemStack(item));
		}
	}
}
