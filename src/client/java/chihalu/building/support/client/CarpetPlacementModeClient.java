package chihalu.building.support.client;

import chihalu.building.support.network.CarpetAltStatePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;

/**
 * クライアント側で Alt キーの押下状態を監視し、サーバーへ同期する。
 */
public final class CarpetPlacementModeClient {
	private static boolean lastState = false;
	private static KeyBinding carpetAltKey;

	private CarpetPlacementModeClient() {
	}

	public static void init(KeyBinding altKey) {
		carpetAltKey = altKey;
		CarpetAltStatePayload.registerType();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.getNetworkHandler() == null) {
				if (lastState) {
					lastState = false;
					sendAltState(false);
				}
				return;
			}
			boolean altDown = isAltBindingDown();
			if (altDown != lastState) {
				lastState = altDown;
				sendAltState(altDown);
			}
		});
	}

	private static void sendAltState(boolean active) {
		ClientPlayNetworking.send(new CarpetAltStatePayload(active));
	}

	private static boolean isAltBindingDown() {
		return carpetAltKey != null && carpetAltKey.isPressed();
	}
}
