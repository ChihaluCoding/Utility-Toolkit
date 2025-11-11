package chihalu.building.support.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class ExtinguishCommand {
	private static final int DEFAULT_RADIUS = 32;
	private static final int MIN_RADIUS = 1;
	private static final int MAX_RADIUS = 128;

	private ExtinguishCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
		dispatcher.register(
			CommandManager.literal("extinguishing")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.literal("fire")
					.executes(context -> execute(context, DEFAULT_RADIUS))
					.then(CommandManager.argument("radius", IntegerArgumentType.integer(MIN_RADIUS, MAX_RADIUS))
						.executes(context -> execute(context, IntegerArgumentType.getInteger(context, "radius")))))
		);
	}

	private static int execute(CommandContext<ServerCommandSource> context, int radius) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();
		ServerWorld world = source.getWorld();
		BlockPos origin = player != null ? player.getBlockPos() : BlockPos.ofFloored(source.getPosition());
		int clamped = Math.max(MIN_RADIUS, Math.min(radius, MAX_RADIUS));
		int removed = extinguishWithin(world, origin, clamped);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.extinguish.fire.success", removed, clamped), true);
		return removed;
	}

	private static int extinguishWithin(ServerWorld world, BlockPos origin, int radius) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int radiusSq = radius * radius;
		int minY = Math.max(world.getBottomY(), origin.getY() - radius);
		int worldTop = world.getBottomY() + world.getHeight() - 1;
		int maxY = Math.min(worldTop, origin.getY() + radius);
		int removed = 0;

		for (int dx = -radius; dx <= radius; dx++) {
			int x = origin.getX() + dx;
			for (int dy = minY; dy <= maxY; dy++) {
				int dyOffset = dy - origin.getY();
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx * dx + dyOffset * dyOffset + dz * dz > radiusSq) {
						continue;
					}
					int z = origin.getZ() + dz;
					mutable.set(x, dy, z);
					BlockState state = world.getBlockState(mutable);
					if (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE)) {
						world.setBlockState(mutable, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
						removed++;
					}
				}
			}
		}
		return removed;
	}
}
