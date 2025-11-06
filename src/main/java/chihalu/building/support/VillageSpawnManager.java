package chihalu.building.support;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class VillageSpawnManager {
	private static final VillageSpawnManager INSTANCE = new VillageSpawnManager();
	private static final int[] SPAWN_SEARCH_RADII = {64, 128, 256};
	private static final int[] EXTENDED_SEARCH_RADII = {64, 96, 128, 160, 192, 224, 256, 320};
	private static final int SEARCH_CENTER_STEP = 512;
	private static final BlockPos[] SEARCH_CENTER_OFFSETS = {
		BlockPos.ORIGIN,
		new BlockPos(SEARCH_CENTER_STEP, 0, 0),
		new BlockPos(-SEARCH_CENTER_STEP, 0, 0),
		new BlockPos(0, 0, SEARCH_CENTER_STEP),
		new BlockPos(0, 0, -SEARCH_CENTER_STEP),
		new BlockPos(SEARCH_CENTER_STEP, 0, SEARCH_CENTER_STEP),
		new BlockPos(SEARCH_CENTER_STEP, 0, -SEARCH_CENTER_STEP),
		new BlockPos(-SEARCH_CENTER_STEP, 0, SEARCH_CENTER_STEP),
		new BlockPos(-SEARCH_CENTER_STEP, 0, -SEARCH_CENTER_STEP)
	};

	private VillageSpawnManager() {
	}

	public static VillageSpawnManager getInstance() {
		return INSTANCE;
	}

	public void initialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
	}

	private void onServerStarted(MinecraftServer server) {
		ServerWorld overworld = server.getOverworld();
		if (overworld == null) {
			return;
		}

		BuildingSupportConfig config = BuildingSupportConfig.getInstance();
		if (!config.isVillageSpawnEnabled()) {
			return;
		}

		BuildingSupportConfig.VillageSpawnType desiredType = config.getVillageSpawnType();
		Optional<BlockPos> spawnPos = findNthNearestVillage(overworld, BlockPos.ORIGIN, desiredType, 0);
		if (spawnPos.isEmpty()) {
			BuildingSupport.LOGGER.warn("���X�|�[����{}�ŒT�����܂������A�����ɍ�������������܂���ł����B�����̃X�|�[���n�_���ێ����܂��B", desiredType.id());
			return;
		}

		setWorldSpawn(server, overworld, spawnPos.get());
		BuildingSupport.LOGGER.info("�X�|�[���n�_��{}�̑� ({}) �ɐݒ肵�܂����B", desiredType.id(), spawnPos.get());
	}

	public Optional<BlockPos> findNthNearestVillage(ServerWorld world, BlockPos origin, BuildingSupportConfig.VillageSpawnType type, int index) {
		if (index < 0) {
			return Optional.empty();
		}

		Optional<RegistryEntry<Structure>> structureEntry = resolveStructureEntry(world, type);
		if (structureEntry.isEmpty()) {
			return Optional.empty();
		}

		RegistryEntryList<Structure> structures = RegistryEntryList.of(structureEntry.get());
		ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
		Set<Long> visited = new HashSet<>();
		List<Pair<BlockPos, BlockPos>> candidates = new ArrayList<>();
		int requiredCount = Math.max(index + 3, 3);
		BlockPos[] centers = createSearchCenters(origin);

		search:
		for (BlockPos center : centers) {
			int[] radii = isOrigin(center) ? SPAWN_SEARCH_RADII : EXTENDED_SEARCH_RADII;
			for (int radius : radii) {
				Pair<BlockPos, RegistryEntry<Structure>> located = generator.locateStructure(world, structures, center, radius, false);
				if (located == null) {
					continue;
				}

				BlockPos structurePos = located.getFirst();
				ChunkPos chunkPos = new ChunkPos(structurePos);
				if (!visited.add(chunkPos.toLong())) {
					continue;
				}

				world.getChunk(chunkPos.x, chunkPos.z);
				BlockPos spawnPos = adjustSpawnPosition(world, structurePos);
				candidates.add(Pair.of(structurePos, spawnPos));

				if (candidates.size() >= requiredCount) {
					break search;
				}
			}
		}

		if (candidates.size() <= index) {
			return Optional.empty();
		}

		candidates.sort(Comparator.comparingDouble(candidate -> candidate.getFirst().getSquaredDistance(origin)));
		return Optional.of(candidates.get(index).getSecond());
	}

	private Optional<RegistryEntry<Structure>> resolveStructureEntry(ServerWorld world, BuildingSupportConfig.VillageSpawnType type) {
		Optional<Registry<Structure>> registryOptional = world.getRegistryManager().getOptional(RegistryKeys.STRUCTURE);
		if (registryOptional.isEmpty()) {
			return Optional.empty();
		}
		Registry<Structure> registry = registryOptional.get();
		RegistryKey<Structure> structureKey = RegistryKey.of(RegistryKeys.STRUCTURE, type.structureId());
		return registry.getEntry(structureKey.getValue()).map(entry -> (RegistryEntry<Structure>) entry);
	}

	private BlockPos[] createSearchCenters(BlockPos origin) {
		BlockPos[] centers = new BlockPos[SEARCH_CENTER_OFFSETS.length];
		for (int i = 0; i < SEARCH_CENTER_OFFSETS.length; i++) {
			BlockPos offset = SEARCH_CENTER_OFFSETS[i];
			centers[i] = origin.add(offset.getX(), offset.getY(), offset.getZ());
		}
		return centers;
	}

	private boolean isOrigin(BlockPos pos) {
		return pos.getX() == 0 && pos.getY() == 0 && pos.getZ() == 0;
	}

	private BlockPos adjustSpawnPosition(ServerWorld world, BlockPos structurePos) {
		BlockPos surface = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, structurePos);
		return surface.up();
	}

	private void setWorldSpawn(MinecraftServer server, ServerWorld world, BlockPos pos) {
		world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
		String command = String.format("setworldspawn %d %d %d", pos.getX(), pos.getY(), pos.getZ());
		server.getCommandManager().executeWithPrefix(server.getCommandSource().withLevel(2), command);
	}
}
