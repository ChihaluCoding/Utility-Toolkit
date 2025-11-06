package chihalu.building.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mod全体の設定を管理するクラス。
 */
public final class BuildingSupportConfig {
	private static final BuildingSupportConfig INSTANCE = new BuildingSupportConfig();

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path configPath = FabricLoader.getInstance()
		.getConfigDir()
		.resolve(BuildingSupport.MOD_ID + "-config.json");

	private boolean preventIceMelting = false;
	private boolean autoLightCandles = false;
	private boolean villageSpawnEnabled = false;
	private HistoryDisplayMode historyDisplayMode = HistoryDisplayMode.PER_WORLD;
	private VillageSpawnType villageSpawnType = VillageSpawnType.PLAINS;

	private BuildingSupportConfig() {
	}

	public static BuildingSupportConfig getInstance() {
		return INSTANCE;
	}

	public synchronized void reload() {
		if (!Files.exists(configPath)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
			SerializableData data = gson.fromJson(reader, SerializableData.class);
			if (data != null) {
				this.preventIceMelting = data.preventIceMelting;
				this.autoLightCandles = data.autoLightCandles;
				this.villageSpawnEnabled = data.villageSpawnEnabled;
				this.historyDisplayMode = data.historyDisplayMode == null ? HistoryDisplayMode.PER_WORLD : data.historyDisplayMode;
				this.villageSpawnType = VillageSpawnType.byId(data.villageSpawnType);
			}
		} catch (IOException | JsonSyntaxException exception) {
			getLogger().error("設定ファイルの読み込みに失敗しました: {}", configPath, exception);
		}
	}

	public synchronized void save() {
		try {
			Files.createDirectories(configPath.getParent());
			SerializableData data = new SerializableData(preventIceMelting, autoLightCandles, villageSpawnEnabled, historyDisplayMode, villageSpawnType);
			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
			}
		} catch (IOException exception) {
			getLogger().error("設定ファイルの保存に失敗しました: {}", configPath, exception);
		}
	}

	public synchronized boolean isPreventIceMeltingEnabled() {
		return preventIceMelting;
	}

	public synchronized void setPreventIceMeltingEnabled(boolean enabled) {
		if (this.preventIceMelting != enabled) {
			this.preventIceMelting = enabled;
			save();
		}
	}

	public synchronized boolean isAutoLightCandlesEnabled() {
		return autoLightCandles;
	}

	public synchronized void setAutoLightCandlesEnabled(boolean enabled) {
		if (this.autoLightCandles != enabled) {
			this.autoLightCandles = enabled;
			save();
		}
	}

	public synchronized boolean isVillageSpawnEnabled() {
		return villageSpawnEnabled;
	}

	public synchronized void setVillageSpawnEnabled(boolean enabled) {
		if (this.villageSpawnEnabled != enabled) {
			this.villageSpawnEnabled = enabled;
			save();
		}
	}

	public synchronized HistoryDisplayMode getHistoryDisplayMode() {
		return historyDisplayMode;
	}

	public synchronized void setHistoryDisplayMode(HistoryDisplayMode mode) {
		if (mode == null) {
			return;
		}
		if (this.historyDisplayMode != mode) {
			this.historyDisplayMode = mode;
			save();
		}
	}

	public synchronized VillageSpawnType getVillageSpawnType() {
		return villageSpawnType;
	}

	public synchronized void setVillageSpawnType(VillageSpawnType type) {
		if (type == null) {
			return;
		}
		if (this.villageSpawnType != type) {
			this.villageSpawnType = type;
			save();
		}
	}

	private Logger getLogger() {
		return BuildingSupport.LOGGER;
	}

	private static final class SerializableData {
		private boolean preventIceMelting;
		private boolean autoLightCandles;
		private HistoryDisplayMode historyDisplayMode;
		private boolean villageSpawnEnabled;
		private String villageSpawnType = VillageSpawnType.PLAINS.id();

		private SerializableData(boolean preventIceMelting, boolean autoLightCandles, boolean villageSpawnEnabled, HistoryDisplayMode historyDisplayMode, VillageSpawnType villageSpawnType) {
			this.preventIceMelting = preventIceMelting;
			this.autoLightCandles = autoLightCandles;
			this.historyDisplayMode = historyDisplayMode;
			this.villageSpawnEnabled = villageSpawnEnabled;
			this.villageSpawnType = villageSpawnType == null ? VillageSpawnType.PLAINS.id() : villageSpawnType.id();
		}
	}

	public enum HistoryDisplayMode {
		PER_WORLD("per_world"),
		ALL_WORLD("all_world");

		private final String suffix;

		HistoryDisplayMode(String suffix) {
			this.suffix = suffix;
		}

		public String translationKey() {
			return "config.building-support.history_display_mode." + suffix;
		}
	}

	public enum VillageSpawnType {
		PLAINS("plains", "village_plains"),
		DESERT("desert", "village_desert"),
		SAVANNA("savanna", "village_savanna"),
		TAIGA("taiga", "village_taiga"),
		SNOWY("snowy", "village_snowy");

		private final String id;
		private final String structurePath;

		VillageSpawnType(String id, String structurePath) {
			this.id = id;
			this.structurePath = structurePath;
		}

		public String id() {
			return id;
		}

		public String translationKey() {
			return "config.building-support.spawn.village_biome." + id;
		}

		public Identifier structureId() {
			return Identifier.of("minecraft", structurePath);
		}

		public static VillageSpawnType byId(String id) {
			if (id == null || id.isBlank()) {
				return PLAINS;
			}
			for (VillageSpawnType type : values()) {
				if (type.id.equalsIgnoreCase(id.trim())) {
					return type;
				}
			}
			return PLAINS;
		}
	}
}
