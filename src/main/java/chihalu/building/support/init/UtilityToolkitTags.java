package chihalu.building.support.init;

import chihalu.building.support.BuildingSupport;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

/**
 * 互換性向上のためにブロックに付与する独自タグ郡。
 * 他Modやデータパックがタグを書き換えることで、保護対象から外すことができる。
 */
public final class UtilityToolkitTags {
	private UtilityToolkitTags() {
	}

	public static final TagKey<Block> FIRE_PROTECTION_TARGETS = TagKey.of(RegistryKeys.BLOCK, BuildingSupport.id("fire_protection_targets"));
	public static final TagKey<Block> ICE_PROTECTION_TARGETS = TagKey.of(RegistryKeys.BLOCK, BuildingSupport.id("ice_protection_targets"));
}
