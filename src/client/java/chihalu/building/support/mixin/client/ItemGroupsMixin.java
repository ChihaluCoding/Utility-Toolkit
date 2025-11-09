package chihalu.building.support.mixin.client;

import java.util.List;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import chihalu.building.support.InventoryTabVisibilityController;

/**
 * クリエイティブタブ一覧を描画する直前で Utility Toolkit 側のフィルタを適用する。
 */
@Mixin(ItemGroups.class)
public class ItemGroupsMixin {
	@Inject(method = "getGroupsToDisplay", at = @At("RETURN"), cancellable = true)
	private static void utility_toolkit$filterGroups(CallbackInfoReturnable<List<ItemGroup>> cir) {
		List<ItemGroup> filtered = InventoryTabVisibilityController.filterGroups(cir.getReturnValue());
		cir.setReturnValue(filtered);
	}
}
