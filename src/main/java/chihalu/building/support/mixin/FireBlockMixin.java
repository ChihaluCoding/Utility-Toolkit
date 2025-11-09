package chihalu.building.support.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import chihalu.building.support.config.BuildingSupportConfig;
import net.minecraft.block.FireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

@Mixin(FireBlock.class)
public class FireBlockMixin {
	/**
	 * 延焼防止が有効な場合はFireBlockの拡散処理そのものを中断する。
	 */
	@Inject(
		method = "trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void utility_toolkit$cancelHazardSpread(World world, BlockPos pos, int chance, Random random, int age, CallbackInfo ci) {
		if (BuildingSupportConfig.getInstance().isHazardFireProtectionEnabled()) {
			ci.cancel();
		}
	}

	/**
	 * 燃焼確率を0にしてブロックが着火対象として扱われないようにする。
	 */
	@Inject(
		method = "getBurnChance(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;)I",
		at = @At("HEAD"),
		cancellable = true
	)
	private void utility_toolkit$zeroBurnChance(WorldView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (BuildingSupportConfig.getInstance().isHazardFireProtectionEnabled()) {
			cir.setReturnValue(0);
		}
	}
}
