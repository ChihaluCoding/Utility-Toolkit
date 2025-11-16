package chihalu.building.support.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import chihalu.building.support.config.BuildingSupportConfig;
import chihalu.building.support.init.UtilityToolkitTags;
import net.minecraft.block.FireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

@Mixin(FireBlock.class)
public class FireBlockMixin {
	/**
	 * 危険建材保護設定が有効な場合は炎が広がる処理そのものを打ち切る。
	 */
	@Inject(
		method = "trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void utility_toolkit$cancelHazardSpread(World world, BlockPos pos, int chance, Random random, int age, CallbackInfo ci) {
		if (!BuildingSupportConfig.getInstance().isHazardFireProtectionEnabled()) {
			return;
		}
		BlockState fireState = world.getBlockState(pos);
		if (!fireState.isIn(UtilityToolkitTags.FIRE_PROTECTION_TARGETS)) {
			return;
		}
		ci.cancel();
	}

	/**
	 * 同じ設定がオンのときは燃焼確率を常に0へ固定し、可燃ブロック扱いを防ぐ。
	 */
	@Inject(
		method = "getBurnChance(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;)I",
		at = @At("HEAD"),
		cancellable = true
	)
	private void utility_toolkit$zeroBurnChance(WorldView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (!BuildingSupportConfig.getInstance().isHazardFireProtectionEnabled()) {
			return;
		}
		if (!world.getBlockState(pos).isIn(UtilityToolkitTags.FIRE_PROTECTION_TARGETS)) {
			return;
		}
		cir.setReturnValue(0);
	}
}
