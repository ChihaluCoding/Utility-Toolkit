package chihalu.building.support.network;

import chihalu.building.support.BuildingSupport;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.RegistryByteBuf;

/**
 * Altキー状態をサーバーへ送るための簡単なペイロード。
 */
public record CarpetAltStatePayload(boolean active) implements CustomPayload {
	public static final CustomPayload.Id<CarpetAltStatePayload> ID = new CustomPayload.Id<>(BuildingSupport.id("carpet_alt_state"));
	public static final PacketCodec<RegistryByteBuf, CarpetAltStatePayload> CODEC = PacketCodec.of(CarpetAltStatePayload::write, CarpetAltStatePayload::decode);

	public static void registerType() {
		PayloadTypeRegistry.playC2S().register(ID, CODEC);
	}

	private static CarpetAltStatePayload decode(RegistryByteBuf buf) {
		return new CarpetAltStatePayload(buf.readBoolean());
	}

	private void write(RegistryByteBuf buf) {
		buf.writeBoolean(active);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
