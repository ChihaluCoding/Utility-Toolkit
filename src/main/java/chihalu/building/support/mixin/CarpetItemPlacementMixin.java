package chihalu.building.support.mixin;

import chihalu.building.support.BuildingSupport;
import chihalu.building.support.config.BuildingSupportConfig;
import chihalu.building.support.network.CarpetPlacementMode;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleCakeBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * カーペットAlt設置時の糸上げと、ケーキ付きキャンドルの自動点灯をBlockItem.placeで処理するMixin。
 */
@Mixin(BlockItem.class)
public abstract class CarpetItemPlacementMixin {
	@Unique
	private BlockPos utility_toolkit$carpetPlacementPos;
	@Unique
	private boolean utility_toolkit$carpetPlacementTracked;
	@Unique
	private boolean utility_toolkit$carpetPlacementWasAir;

	@Inject(method = "place", at = @At("HEAD"))
	private void utility_toolkit$prepareCarpetSupport(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
		utility_toolkit$carpetPlacementTracked = false;
		utility_toolkit$carpetPlacementPos = null;
		utility_toolkit$carpetPlacementWasAir = false;

		World world = context.getWorld();
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}

		if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}
		if (!CarpetPlacementMode.isAltActive(serverPlayer)) {
			return;
		}

		Block block = ((BlockItem) (Object) this).getBlock();
		BuildingSupportConfig config = BuildingSupportConfig.getInstance();
		if (config.isAutoCarpetStringEnabled() && block.getDefaultState().isIn(BlockTags.WOOL_CARPETS)) {
			BlockPos placementPos = context.getBlockPos();
			BlockState beforeState = serverWorld.getBlockState(placementPos);
			utility_toolkit$carpetPlacementTracked = true;
			utility_toolkit$carpetPlacementPos = placementPos.toImmutable();
			utility_toolkit$carpetPlacementWasAir = beforeState.isAir();
		}
	}

	@Inject(method = "place", at = @At("RETURN"))
	private void utility_toolkit$handlePlacementResult(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
		ActionResult result = cir.getReturnValue();
		World world = context.getWorld();
		ServerWorld serverWorld = world instanceof ServerWorld ? (ServerWorld) world : null;
		BuildingSupportConfig config = BuildingSupportConfig.getInstance();
		BlockPos placementPos = context.getBlockPos();

		if (serverWorld != null && result.isAccepted() && config.isAutoLightCandlesEnabled()) {
			BlockState state = serverWorld.getBlockState(placementPos);
			if ((!BuildingSupport.isAutoLightVanillaRestricted() || BuildingSupport.isVanillaBlock(state.getBlock()))
				&& state.getBlock() instanceof CandleCakeBlock
				&& state.contains(CandleCakeBlock.LIT)
				&& !state.get(CandleCakeBlock.LIT)) {
				serverWorld.setBlockState(placementPos, state.with(CandleCakeBlock.LIT, true), Block.NOTIFY_ALL);
			}
		}

		if (serverWorld != null) {
			handleCarpetElevation(serverWorld, result.isAccepted());
		}
	}

	@Unique
	private void handleCarpetElevation(ServerWorld world, boolean placementSucceeded) {
		if (!utility_toolkit$carpetPlacementTracked) {
			return;
		}
		try {
			if (!placementSucceeded || !utility_toolkit$carpetPlacementWasAir) {
				return;
			}
			BlockPos carpetPos = utility_toolkit$carpetPlacementPos;
			BlockState state = world.getBlockState(carpetPos);
			if (!state.isIn(BlockTags.WOOL_CARPETS)) {
				return;
			}
			BlockPos newCarpetPos = carpetPos.up();
			if (world.isOutOfHeightLimit(newCarpetPos) || !world.getBlockState(newCarpetPos).isAir()) {
				return;
			}
			world.setBlockState(carpetPos, Blocks.TRIPWIRE.getDefaultState(), Block.NOTIFY_ALL);
			world.setBlockState(newCarpetPos, state, Block.NOTIFY_ALL);
		} finally {
			utility_toolkit$carpetPlacementTracked = false;
			utility_toolkit$carpetPlacementPos = null;
			utility_toolkit$carpetPlacementWasAir = false;
		}
	}
}
