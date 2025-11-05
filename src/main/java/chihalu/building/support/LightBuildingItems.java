package chihalu.building.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * 光源系ブロックの一覧を提供するユーティリティ。
 */
public final class LightBuildingItems {
	private static final List<ItemStack> LIGHT_BLOCKS = buildLightBlockList();

	private LightBuildingItems() {
	}

	public static void populate(ItemGroup.Entries entries) {
		for (ItemStack stack : LIGHT_BLOCKS) {
			entries.add(stack.copy(), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	public static ItemStack getIconStack() {
		return new ItemStack(Items.SEA_LANTERN);
	}

	private static List<ItemStack> buildLightBlockList() {
		List<ItemStack> stacks = new ArrayList<>();

		addSet(stacks, Items.TORCH, Items.SOUL_TORCH, Items.REDSTONE_TORCH);
		addSet(stacks, Items.LANTERN, Items.SOUL_LANTERN);
		addSet(stacks, Items.CANDLE, Items.WHITE_CANDLE, Items.ORANGE_CANDLE, Items.MAGENTA_CANDLE, Items.LIGHT_BLUE_CANDLE,
			Items.YELLOW_CANDLE, Items.LIME_CANDLE, Items.PINK_CANDLE, Items.GRAY_CANDLE, Items.LIGHT_GRAY_CANDLE,
			Items.CYAN_CANDLE, Items.PURPLE_CANDLE, Items.BLUE_CANDLE, Items.BROWN_CANDLE, Items.GREEN_CANDLE,
			Items.RED_CANDLE, Items.BLACK_CANDLE);
		addSet(stacks, Items.GLOWSTONE, Items.SEA_LANTERN, Items.SHROOMLIGHT, Items.GLOW_LICHEN);
		addSet(stacks, Items.END_ROD, Items.BEACON, Items.CONDUIT);
		addSet(stacks, Items.JACK_O_LANTERN, Items.REDSTONE_LAMP, Items.OCHRE_FROGLIGHT, Items.VERDANT_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT);
		addSet(stacks, Items.CAMPFIRE, Items.SOUL_CAMPFIRE);
		addSet(stacks, Items.MAGMA_BLOCK, Items.LIGHT);

		return Collections.unmodifiableList(stacks);
	}

	private static void addSet(List<ItemStack> stacks, net.minecraft.item.Item... items) {
		for (net.minecraft.item.Item item : items) {
			stacks.add(new ItemStack(item));
		}
	}
}
