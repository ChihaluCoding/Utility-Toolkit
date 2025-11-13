package chihalu.building.support.client;

import chihalu.building.support.network.CarpetAltStatePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * クライアント側で Alt キーの押下状態を監視し、サーバーへ同期する。
 */
public final class CarpetPlacementModeClient {
	private static boolean lastState = false;

	private CarpetPlacementModeClient() {
	}

	public static void init() {
		CarpetAltStatePayload.registerType();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.getNetworkHandler() == null) {
				if (lastState) {
					lastState = false;
					sendAltState(false);
				}
				return;
			}
			boolean altDown = isAltDown(client);
			if (altDown != lastState) {
				lastState = altDown;
				sendAltState(altDown);
			}
		});
	}

	private static void sendAltState(boolean active) {
		ClientPlayNetworking.send(new CarpetAltStatePayload(active));
	}

	private static boolean isAltDown(MinecraftClient client) {
		Window window = client.getWindow();
		if (window == null) {
			return false;
		}
		return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT)
			|| InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT);
	}
}
