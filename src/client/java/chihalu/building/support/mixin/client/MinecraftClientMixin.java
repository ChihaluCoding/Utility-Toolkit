package chihalu.building.support.mixin.client;

import chihalu.building.support.BuildingSupportClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "doItemPick", at = @At("TAIL"))
	private void building_support$recordPickBlock(CallbackInfo ci) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		if (client.player == null) {
			return;
		}

		ItemStack stack = client.player.getMainHandStack();
		BuildingSupportClient.recordHistoryUsage(stack);
	}
}
