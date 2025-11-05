package chihalu.building.support.mixin;

import chihalu.building.support.BuildingSupportConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.IceBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public abstract class IceBlockMixin {
	@Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
	private void building_support$preventRandomTickMelting(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
		if (BuildingSupportConfig.getInstance().isPreventIceMeltingEnabled()) {
			ci.cancel();
		}
	}

	@Inject(method = "melt", at = @At("HEAD"), cancellable = true)
	private void building_support$preventMelt(BlockState state, World world, BlockPos pos, CallbackInfo ci) {
		if (BuildingSupportConfig.getInstance().isPreventIceMeltingEnabled()) {
			ci.cancel();
		}
	}

	@Redirect(method = "afterBreak", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
	private boolean building_support$skipWaterOnBreak(World world, BlockPos pos, BlockState state) {
		if (BuildingSupportConfig.getInstance().isPreventIceMeltingEnabled()) {
			return false;
		}
		return world.setBlockState(pos, state);
	}
}
