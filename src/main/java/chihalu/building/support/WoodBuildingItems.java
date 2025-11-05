package chihalu.building.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * 木材系建築ブロックの一覧を管理するユーティリティ。
 */
public final class WoodBuildingItems {
	private static final List<ItemStack> WOOD_BLOCKS = buildWoodBlockList();

	private WoodBuildingItems() {
	}

	public static void populate(ItemGroup.Entries entries) {
		for (ItemStack stack : WOOD_BLOCKS) {
			entries.add(stack.copy(), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	public static ItemStack getIconStack() {
		return new ItemStack(Items.OAK_PLANKS);
	}

	private static List<ItemStack> buildWoodBlockList() {
		List<ItemStack> stacks = new ArrayList<>();

		// オーバーワールド木材
		addWoodSet(stacks, Items.OAK_LOG, Items.STRIPPED_OAK_LOG, Items.OAK_WOOD, Items.STRIPPED_OAK_WOOD, Items.OAK_PLANKS, Items.OAK_STAIRS, Items.OAK_SLAB, Items.OAK_FENCE, Items.OAK_FENCE_GATE, Items.OAK_DOOR, Items.OAK_TRAPDOOR, Items.OAK_PRESSURE_PLATE, Items.OAK_BUTTON);
		addWoodSet(stacks, Items.SPRUCE_LOG, Items.STRIPPED_SPRUCE_LOG, Items.SPRUCE_WOOD, Items.STRIPPED_SPRUCE_WOOD, Items.SPRUCE_PLANKS, Items.SPRUCE_STAIRS, Items.SPRUCE_SLAB, Items.SPRUCE_FENCE, Items.SPRUCE_FENCE_GATE, Items.SPRUCE_DOOR, Items.SPRUCE_TRAPDOOR, Items.SPRUCE_PRESSURE_PLATE, Items.SPRUCE_BUTTON);
		addWoodSet(stacks, Items.BIRCH_LOG, Items.STRIPPED_BIRCH_LOG, Items.BIRCH_WOOD, Items.STRIPPED_BIRCH_WOOD, Items.BIRCH_PLANKS, Items.BIRCH_STAIRS, Items.BIRCH_SLAB, Items.BIRCH_FENCE, Items.BIRCH_FENCE_GATE, Items.BIRCH_DOOR, Items.BIRCH_TRAPDOOR, Items.BIRCH_PRESSURE_PLATE, Items.BIRCH_BUTTON);
		addWoodSet(stacks, Items.JUNGLE_LOG, Items.STRIPPED_JUNGLE_LOG, Items.JUNGLE_WOOD, Items.STRIPPED_JUNGLE_WOOD, Items.JUNGLE_PLANKS, Items.JUNGLE_STAIRS, Items.JUNGLE_SLAB, Items.JUNGLE_FENCE, Items.JUNGLE_FENCE_GATE, Items.JUNGLE_DOOR, Items.JUNGLE_TRAPDOOR, Items.JUNGLE_PRESSURE_PLATE, Items.JUNGLE_BUTTON);
		addWoodSet(stacks, Items.ACACIA_LOG, Items.STRIPPED_ACACIA_LOG, Items.ACACIA_WOOD, Items.STRIPPED_ACACIA_WOOD, Items.ACACIA_PLANKS, Items.ACACIA_STAIRS, Items.ACACIA_SLAB, Items.ACACIA_FENCE, Items.ACACIA_FENCE_GATE, Items.ACACIA_DOOR, Items.ACACIA_TRAPDOOR, Items.ACACIA_PRESSURE_PLATE, Items.ACACIA_BUTTON);
		addWoodSet(stacks, Items.DARK_OAK_LOG, Items.STRIPPED_DARK_OAK_LOG, Items.DARK_OAK_WOOD, Items.STRIPPED_DARK_OAK_WOOD, Items.DARK_OAK_PLANKS, Items.DARK_OAK_STAIRS, Items.DARK_OAK_SLAB, Items.DARK_OAK_FENCE, Items.DARK_OAK_FENCE_GATE, Items.DARK_OAK_DOOR, Items.DARK_OAK_TRAPDOOR, Items.DARK_OAK_PRESSURE_PLATE, Items.DARK_OAK_BUTTON);
		addWoodSet(stacks, Items.MANGROVE_LOG, Items.STRIPPED_MANGROVE_LOG, Items.MANGROVE_WOOD, Items.STRIPPED_MANGROVE_WOOD, Items.MANGROVE_PLANKS, Items.MANGROVE_STAIRS, Items.MANGROVE_SLAB, Items.MANGROVE_FENCE, Items.MANGROVE_FENCE_GATE, Items.MANGROVE_DOOR, Items.MANGROVE_TRAPDOOR, Items.MANGROVE_PRESSURE_PLATE, Items.MANGROVE_BUTTON);
		addWoodSet(stacks, Items.CHERRY_LOG, Items.STRIPPED_CHERRY_LOG, Items.CHERRY_WOOD, Items.STRIPPED_CHERRY_WOOD, Items.CHERRY_PLANKS, Items.CHERRY_STAIRS, Items.CHERRY_SLAB, Items.CHERRY_FENCE, Items.CHERRY_FENCE_GATE, Items.CHERRY_DOOR, Items.CHERRY_TRAPDOOR, Items.CHERRY_PRESSURE_PLATE, Items.CHERRY_BUTTON);
		addWoodSet(stacks, Items.BAMBOO_BLOCK, Items.STRIPPED_BAMBOO_BLOCK, Items.BAMBOO_PLANKS, Items.BAMBOO_STAIRS, Items.BAMBOO_SLAB, Items.BAMBOO_FENCE, Items.BAMBOO_FENCE_GATE, Items.BAMBOO_DOOR, Items.BAMBOO_TRAPDOOR, Items.BAMBOO_PRESSURE_PLATE, Items.BAMBOO_BUTTON);

		// ネザー木材
		addWoodSet(stacks, Items.CRIMSON_STEM, Items.STRIPPED_CRIMSON_STEM, Items.CRIMSON_HYPHAE, Items.STRIPPED_CRIMSON_HYPHAE, Items.CRIMSON_PLANKS, Items.CRIMSON_STAIRS, Items.CRIMSON_SLAB, Items.CRIMSON_FENCE, Items.CRIMSON_FENCE_GATE, Items.CRIMSON_DOOR, Items.CRIMSON_TRAPDOOR, Items.CRIMSON_PRESSURE_PLATE, Items.CRIMSON_BUTTON);
		addWoodSet(stacks, Items.WARPED_STEM, Items.STRIPPED_WARPED_STEM, Items.WARPED_HYPHAE, Items.STRIPPED_WARPED_HYPHAE, Items.WARPED_PLANKS, Items.WARPED_STAIRS, Items.WARPED_SLAB, Items.WARPED_FENCE, Items.WARPED_FENCE_GATE, Items.WARPED_DOOR, Items.WARPED_TRAPDOOR, Items.WARPED_PRESSURE_PLATE, Items.WARPED_BUTTON);

		return Collections.unmodifiableList(stacks);
	}

	private static void addWoodSet(List<ItemStack> stacks, net.minecraft.item.Item... items) {
		for (net.minecraft.item.Item item : items) {
			stacks.add(new ItemStack(item));
		}
	}
}
