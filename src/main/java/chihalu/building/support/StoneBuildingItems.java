package chihalu.building.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * 石材系建築ブロックの一覧を管理するユーティリティ。
 */
public final class StoneBuildingItems {
	private static final List<ItemStack> STONE_BLOCKS = buildStoneBlockList();

	private StoneBuildingItems() {
	}

	public static void populate(ItemGroup.Entries entries) {
		for (ItemStack stack : STONE_BLOCKS) {
			entries.add(stack.copy(), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	public static ItemStack getIconStack() {
		return new ItemStack(Items.STONE_BRICKS);
	}

	private static List<ItemStack> buildStoneBlockList() {
		List<ItemStack> stacks = new ArrayList<>();

		addSet(stacks, Items.STONE, Items.SMOOTH_STONE, Items.STONE_SLAB, Items.SMOOTH_STONE_SLAB, Items.STONE_STAIRS);
		addSet(stacks, Items.STONE_BRICKS, Items.CRACKED_STONE_BRICKS, Items.CHISELED_STONE_BRICKS, Items.STONE_BRICK_SLAB, Items.STONE_BRICK_STAIRS, Items.STONE_BRICK_WALL, Items.INFESTED_STONE, Items.INFESTED_STONE_BRICKS);
		addSet(stacks, Items.COBBLESTONE, Items.MOSSY_COBBLESTONE, Items.COBBLESTONE_STAIRS, Items.COBBLESTONE_SLAB, Items.COBBLESTONE_WALL, Items.MOSSY_COBBLESTONE_STAIRS, Items.MOSSY_COBBLESTONE_SLAB, Items.MOSSY_COBBLESTONE_WALL);
		addSet(stacks, Items.ANDESITE, Items.POLISHED_ANDESITE, Items.ANDESITE_SLAB, Items.ANDESITE_STAIRS, Items.ANDESITE_WALL, Items.POLISHED_ANDESITE_SLAB, Items.POLISHED_ANDESITE_STAIRS);
		addSet(stacks, Items.DIORITE, Items.POLISHED_DIORITE, Items.DIORITE_SLAB, Items.DIORITE_STAIRS, Items.DIORITE_WALL, Items.POLISHED_DIORITE_SLAB, Items.POLISHED_DIORITE_STAIRS);
		addSet(stacks, Items.GRANITE, Items.POLISHED_GRANITE, Items.GRANITE_SLAB, Items.GRANITE_STAIRS, Items.GRANITE_WALL, Items.POLISHED_GRANITE_SLAB, Items.POLISHED_GRANITE_STAIRS);
		addSet(stacks, Items.BRICKS, Items.BRICK_STAIRS, Items.BRICK_SLAB, Items.BRICK_WALL);
		addSet(stacks, Items.MUD_BRICKS, Items.MUD_BRICK_STAIRS, Items.MUD_BRICK_SLAB, Items.MUD_BRICK_WALL, Items.PACKED_MUD);
		addSet(stacks, Items.DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.DEEPSLATE_TILES, Items.POLISHED_DEEPSLATE, Items.DEEPSLATE_BRICKS, Items.CHISELED_DEEPSLATE,
			Items.DEEPSLATE_TILE_SLAB, Items.DEEPSLATE_TILE_STAIRS, Items.DEEPSLATE_TILE_WALL,
			Items.DEEPSLATE_BRICK_SLAB, Items.DEEPSLATE_BRICK_STAIRS, Items.DEEPSLATE_BRICK_WALL,
			Items.COBBLED_DEEPSLATE_SLAB, Items.COBBLED_DEEPSLATE_STAIRS, Items.COBBLED_DEEPSLATE_WALL,
			Items.POLISHED_DEEPSLATE_SLAB, Items.POLISHED_DEEPSLATE_STAIRS, Items.POLISHED_DEEPSLATE_WALL);
		addSet(stacks, Items.BLACKSTONE, Items.POLISHED_BLACKSTONE, Items.POLISHED_BLACKSTONE_BRICKS, Items.CHISELED_POLISHED_BLACKSTONE,
			Items.BLACKSTONE_SLAB, Items.BLACKSTONE_STAIRS, Items.BLACKSTONE_WALL,
			Items.POLISHED_BLACKSTONE_SLAB, Items.POLISHED_BLACKSTONE_STAIRS, Items.POLISHED_BLACKSTONE_WALL,
			Items.POLISHED_BLACKSTONE_BRICK_SLAB, Items.POLISHED_BLACKSTONE_BRICK_STAIRS, Items.POLISHED_BLACKSTONE_BRICK_WALL);
		addSet(stacks, Items.SANDSTONE, Items.SMOOTH_SANDSTONE, Items.CUT_SANDSTONE, Items.CHISELED_SANDSTONE,
			Items.SANDSTONE_STAIRS, Items.SANDSTONE_SLAB, Items.SANDSTONE_WALL,
			Items.SMOOTH_SANDSTONE_STAIRS, Items.SMOOTH_SANDSTONE_SLAB,
			Items.CUT_SANDSTONE_SLAB);
		addSet(stacks, Items.RED_SANDSTONE, Items.SMOOTH_RED_SANDSTONE, Items.CUT_RED_SANDSTONE, Items.CHISELED_RED_SANDSTONE,
			Items.RED_SANDSTONE_STAIRS, Items.RED_SANDSTONE_SLAB, Items.RED_SANDSTONE_WALL,
			Items.SMOOTH_RED_SANDSTONE_STAIRS, Items.SMOOTH_RED_SANDSTONE_SLAB,
			Items.CUT_RED_SANDSTONE_SLAB);
		addSet(stacks, Items.BASALT, Items.SMOOTH_BASALT, Items.POLISHED_BASALT);
		addSet(stacks, Items.PRISMARINE, Items.PRISMARINE_BRICKS, Items.DARK_PRISMARINE,
			Items.PRISMARINE_SLAB, Items.PRISMARINE_STAIRS, Items.PRISMARINE_WALL,
			Items.PRISMARINE_BRICK_SLAB, Items.PRISMARINE_BRICK_STAIRS,
			Items.DARK_PRISMARINE_SLAB, Items.DARK_PRISMARINE_STAIRS);
		addSet(stacks, Items.QUARTZ_BLOCK, Items.SMOOTH_QUARTZ, Items.CHISELED_QUARTZ_BLOCK, Items.QUARTZ_PILLAR,
			Items.QUARTZ_STAIRS, Items.QUARTZ_SLAB, Items.SMOOTH_QUARTZ_STAIRS, Items.SMOOTH_QUARTZ_SLAB);
		addSet(stacks, Items.CALCITE, Items.DRIPSTONE_BLOCK, Items.AMETHYST_BLOCK, Items.BUDDING_AMETHYST);
		addSet(stacks, Items.TUFF, Items.POLISHED_TUFF, Items.TUFF_STAIRS, Items.TUFF_SLAB, Items.TUFF_WALL,
			Items.POLISHED_TUFF_STAIRS, Items.POLISHED_TUFF_SLAB, Items.POLISHED_TUFF_WALL,
			Items.TUFF_BRICKS, Items.TUFF_BRICK_STAIRS, Items.TUFF_BRICK_SLAB, Items.TUFF_BRICK_WALL);

		return Collections.unmodifiableList(stacks);
	}

	private static void addSet(List<ItemStack> stacks, net.minecraft.item.Item... items) {
		for (net.minecraft.item.Item item : items) {
			stacks.add(new ItemStack(item));
		}
	}
}
