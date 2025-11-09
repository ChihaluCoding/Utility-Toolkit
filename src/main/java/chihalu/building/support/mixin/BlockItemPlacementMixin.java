package chihalu.building.support.mixin;

import chihalu.building.support.BuildingSupport;
import chihalu.building.support.config.BuildingSupportConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CandleCakeBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemPlacementMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void utility_toolkit$autoLightCandleCake(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
		if (!BuildingSupportConfig.getInstance().isAutoLightCandlesEnabled()) {
			return;
		}

		ActionResult result = cir.getReturnValue();
		if (!result.isAccepted()) {
			return;
		}

		World world = context.getWorld();
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}

		BlockPos placementPos = context.getBlockPos();
		BlockState state = serverWorld.getBlockState(placementPos);
		if (BuildingSupport.isAutoLightVanillaRestricted() && !BuildingSupport.isVanillaBlock(state.getBlock())) {
			return;
		}

		if (state.getBlock() instanceof CandleCakeBlock && state.contains(CandleCakeBlock.LIT) && !state.get(CandleCakeBlock.LIT)) {
			serverWorld.setBlockState(placementPos, state.with(CandleCakeBlock.LIT, true), Block.NOTIFY_ALL);
		}
	}
}
