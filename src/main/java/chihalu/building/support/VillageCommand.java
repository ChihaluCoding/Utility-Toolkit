package chihalu.building.support;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraft.network.packet.s2c.play.PositionFlag;

import java.util.EnumSet;
import java.util.Set;
import java.util.Optional;

public final class VillageCommand {
	private final VillageSpawnManager villageSpawnManager;

	private VillageCommand(VillageSpawnManager villageSpawnManager) {
		this.villageSpawnManager = villageSpawnManager;
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, VillageSpawnManager manager) {
		VillageCommand handler = new VillageCommand(manager);
		dispatcher.register(CommandManager.literal("village")
			.requires(source -> source.hasPermissionLevel(2))
			.executes(context -> handler.teleportToSecondVillage(context.getSource())));
	}

	private int teleportToSecondVillage(ServerCommandSource source) throws CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayer();
		ServerWorld world = source.getWorld();
		if (!world.getRegistryKey().equals(World.OVERWORLD)) {
			source.sendFeedback(() -> Text.translatable("command.building-support.village.overworld_only"), false);
			return 0;
		}

		BuildingSupportConfig config = BuildingSupportConfig.getInstance();
		BuildingSupportConfig.VillageSpawnType type = config.getVillageSpawnType();
		Optional<BlockPos> target = villageSpawnManager.findNthNearestVillage(world, player.getBlockPos(), type, 1);
		if (target.isEmpty()) {
			source.sendFeedback(() -> Text.translatable("command.building-support.village.not_found"), false);
			return 0;
		}

		BlockPos pos = target.get();
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

		source.sendFeedback(() -> Text.translatable(
			"command.building-support.village.teleported",
			Text.translatable(type.translationKey()),
			pos.getX(),
			pos.getY(),
			pos.getZ()
		), false);

		return 1;
	}
}
