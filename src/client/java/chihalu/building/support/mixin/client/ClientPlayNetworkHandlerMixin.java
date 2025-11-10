package chihalu.building.support.mixin.client;

import chihalu.building.support.config.BuildingSupportConfig;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.SignEditorOpenS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	@Inject(method = "onSignEditorOpen", at = @At("HEAD"), cancellable = true)
	private void utility_toolkit$maybeSkipSignEditor(SignEditorOpenS2CPacket packet, CallbackInfo ci) {
		if (BuildingSupportConfig.getInstance().isSignEditScreenDisabled()) {
			ci.cancel();
		}
	}
}
