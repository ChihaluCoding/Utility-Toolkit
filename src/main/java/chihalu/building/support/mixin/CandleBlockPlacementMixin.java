package chihalu.building.support.mixin;

import chihalu.building.support.BuildingSupport;
import chihalu.building.support.config.BuildingSupportConfig;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CandleBlock;
import net.minecraft.item.ItemPlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CandleBlock.class)
public abstract class CandleBlockPlacementMixin {
	@Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
	private void utility_toolkit$autoLightPlacement(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
		if (!BuildingSupportConfig.getInstance().isAutoLightCandlesEnabled()) {
			return;
		}

		BlockState state = cir.getReturnValue();
		if (state == null || !state.contains(AbstractCandleBlock.LIT) || state.get(AbstractCandleBlock.LIT)) {
			return;
		}

		if (BuildingSupport.isAutoLightVanillaRestricted() && !BuildingSupport.isVanillaBlock(state.getBlock())) {
			return;
		}

		cir.setReturnValue(state.with(AbstractCandleBlock.LIT, true));
	}
}
