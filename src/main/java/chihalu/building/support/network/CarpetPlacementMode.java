package chihalu.building.support.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * サーバー側で Alt 状態を保持するヘルパー。
 */
public final class CarpetPlacementMode {
	private static final Map<UUID, Boolean> ACTIVE_MAP = new ConcurrentHashMap<>();

	private CarpetPlacementMode() {
	}

	public static void initServer() {
		CarpetAltStatePayload.registerType();
		ServerPlayNetworking.registerGlobalReceiver(CarpetAltStatePayload.ID, (payload, context) -> {
			ACTIVE_MAP.put(context.player().getUuid(), payload.active());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.player;
			if (player != null) {
				ACTIVE_MAP.remove(player.getUuid());
			}
		});
	}

	public static boolean isAltActive(ServerPlayerEntity player) {
		return ACTIVE_MAP.getOrDefault(player.getUuid(), false);
	}
}
