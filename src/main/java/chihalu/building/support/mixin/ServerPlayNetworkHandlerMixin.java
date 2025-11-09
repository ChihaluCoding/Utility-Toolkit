package chihalu.building.support.mixin;

import chihalu.building.support.config.BuildingSupportConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PickItemFromBlockC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
	private static final double FLOWER_HEIGHT_THRESHOLD = 0.4375D;

	@Shadow
	public ServerPlayerEntity player;

	@Shadow
	protected abstract void onPickItem(ItemStack stack);

	@Inject(method = "onPickItemFromBlock", at = @At("HEAD"), cancellable = true)
	private void utility_toolkit$adjustPottedPlantPick(PickItemFromBlockC2SPacket packet, CallbackInfo ci) {
		if (!BuildingSupportConfig.getInstance().isPottedPlantPickPrefersPot()) {
			return;
		}

		ServerWorld serverWorld = this.player.getEntityWorld();
		if (serverWorld == null) {
			return;
		}

		BlockPos blockPos = packet.pos();
		if (!this.player.canInteractWithBlockAt(blockPos, 1.0) || !serverWorld.isPosLoaded(blockPos)) {
			return;
		}

		BlockState blockState = serverWorld.getBlockState(blockPos);
		Block block = blockState.getBlock();
		if (!(block instanceof FlowerPotBlock potBlock) || potBlock.getContent() == Blocks.AIR) {
			return;
		}

		boolean preferFlower = this.player.isSneaking() || isTargetingFlower(blockPos);
		ItemStack override = createPottedPlantStack(potBlock, preferFlower);
		if (override.isEmpty()) {
			return;
		}

		this.onPickItem(override);
		ci.cancel();
	}

	private ItemStack createPottedPlantStack(FlowerPotBlock potBlock, boolean preferFlower) {
		if (preferFlower) {
			Block content = potBlock.getContent();
			ItemStack flowerStack = new ItemStack(content);
			if (!flowerStack.isEmpty()) {
				return flowerStack;
			}
		}
		return new ItemStack(Items.FLOWER_POT);
	}

	private boolean isTargetingFlower(BlockPos blockPos) {
		double reach = this.player.getBlockInteractionRange();
		HitResult hitResult = this.player.raycast(reach, 0.0F, false);
		if (!(hitResult instanceof BlockHitResult blockHitResult)) {
			return false;
		}

		if (!blockHitResult.getBlockPos().equals(blockPos)) {
			return false;
		}

		Vec3d hitPos = blockHitResult.getPos();
		double relativeY = hitPos.y - blockPos.getY();
		if (relativeY > FLOWER_HEIGHT_THRESHOLD) {
			return true;
		}

		return relativeY > 0.375D && blockHitResult.getSide() == Direction.UP;
	}

}
