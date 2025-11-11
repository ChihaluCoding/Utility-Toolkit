package chihalu.building.support.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import chihalu.building.support.config.BuildingSupportConfig;
import chihalu.building.support.village.VillageSpawnManager;
import chihalu.building.support.village.VillageVisitTracker;

public final class VillageCommand {
	private static final int DEFAULT_NEW_VILLAGE_DISTANCE = 640;
	private static final int MIN_NEW_VILLAGE_DISTANCE = 128;
	private static final int MAX_NEW_VILLAGE_DISTANCE = 16000;
	private final VillageSpawnManager villageSpawnManager;

	private VillageCommand(VillageSpawnManager villageSpawnManager) {
		this.villageSpawnManager = villageSpawnManager;
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, VillageSpawnManager manager) {
		VillageCommand handler = new VillageCommand(manager);
		// /village new コマンドだけを提供し、旧 /village コマンドは無効化する
		dispatcher.register(CommandManager.literal("village")
			.requires(source -> source.hasPermissionLevel(2))
			.then(CommandManager.literal("new")
				.executes(context -> handler.teleportToNewVillage(context.getSource(), DEFAULT_NEW_VILLAGE_DISTANCE))
				.then(CommandManager.argument("distance", IntegerArgumentType.integer(1))
					.executes(context -> handler.teleportToNewVillage(context.getSource(), IntegerArgumentType.getInteger(context, "distance"))))));
	}

	// まだ訪れていない村へプレイヤーを送るメイン処理
	private int teleportToNewVillage(ServerCommandSource source, int distance) throws CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayer();
		ServerWorld world = source.getWorld();
		if (!isOverworld(world, source)) {
			return 0;
		}

		BuildingSupportConfig.VillageSpawnType type = BuildingSupportConfig.getInstance().getVillageSpawnType();
		VillageVisitTracker tracker = VillageVisitTracker.get(world);
		int cappedDistance = clampDistance(distance);
		Optional<VillageSpawnManager.VillageLocation> location = villageSpawnManager.findNearestVillageMatching(
			world,
			player.getBlockPos(),
			type,
			cappedDistance,
			candidate -> !tracker.isVisited(player.getUuid(), world, type, candidate)
		);
		if (location.isPresent()) {
			return performTeleport(source, player, world, type, location.get(), tracker);
		}

		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.village.no_new"), false);
		return 0;
	}

	private boolean isOverworld(ServerWorld world, ServerCommandSource source) {
		if (!world.getRegistryKey().equals(World.OVERWORLD)) {
			source.sendFeedback(() -> Text.translatable("command.utility-toolkit.village.overworld_only"), false);
			return false;
		}
		return true;
	}

	private int clampDistance(int distance) {
		return Math.max(MIN_NEW_VILLAGE_DISTANCE, Math.min(distance, MAX_NEW_VILLAGE_DISTANCE));
	}

	private int performTeleport(ServerCommandSource source, ServerPlayerEntity player, ServerWorld world, BuildingSupportConfig.VillageSpawnType type, VillageSpawnManager.VillageLocation location, VillageVisitTracker tracker) {
		BlockPos pos = location.spawnPos();
		world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
		Set<PositionFlag> flags = EnumSet.noneOf(PositionFlag.class);
		player.teleport(
			world,
			pos.getX() + 0.5,
			pos.getY(),
			pos.getZ() + 0.5,
			flags,
			player.getYaw(),
			player.getPitch(),
			false
		);
		tracker.markVisited(player.getUuid(), world, type, location);

		source.sendFeedback(() -> Text.translatable(
			"command.utility-toolkit.village.teleported",
			Text.translatable(type.translationKey()),
			pos.getX(),
			pos.getY(),
			pos.getZ()
		), false);

		return 1;
	}
}
