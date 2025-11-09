package chihalu.building.support.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import chihalu.building.support.BuildingSupport;
import chihalu.building.support.BuildingSupportStorage;

/**
 * Mod蜈ｨ菴薙・險ｭ螳壹ｒ邂｡逅・☆繧九け繝ｩ繧ｹ縲・ */
public final class BuildingSupportConfig {
	private static final BuildingSupportConfig INSTANCE = new BuildingSupportConfig();

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path configPath = BuildingSupportStorage.resolve("config.json");

	private boolean preventIceMelting = false;
	private boolean preventHazardFireSpread = false;
	private boolean autoLightCandles = false;
	private boolean villageSpawnEnabled = false;
	private HistoryDisplayMode historyDisplayMode = HistoryDisplayMode.PER_WORLD;
	private VillageSpawnType villageSpawnType = VillageSpawnType.PLAINS;
	private boolean pottedPlantPickPrefersPot = true;
	private final EnumMap<ItemGroupOption, Boolean> itemGroupVisibility = new EnumMap<>(ItemGroupOption.class);

	private BuildingSupportConfig() {
		resetItemGroupVisibility();
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
				this.preventHazardFireSpread = data.preventHazardFireSpread;
				this.autoLightCandles = data.autoLightCandles;
				this.villageSpawnEnabled = data.villageSpawnEnabled;
				this.historyDisplayMode = data.historyDisplayMode == null ? HistoryDisplayMode.PER_WORLD : data.historyDisplayMode;
				this.villageSpawnType = VillageSpawnType.byId(data.villageSpawnType);
				this.pottedPlantPickPrefersPot = data.pottedPlantPickPrefersPot;
				applyItemGroupVisibility(data.itemGroupVisibility);
			}
		} catch (IOException | JsonSyntaxException exception) {
			getLogger().error("險ｭ螳壹ヵ繧｡繧､繝ｫ縺ｮ隱ｭ縺ｿ霎ｼ縺ｿ縺ｫ螟ｱ謨励＠縺ｾ縺励◆: {}", configPath, exception);
		}
	}

	public synchronized void save() {
		try {
			Files.createDirectories(configPath.getParent());
			SerializableData data = new SerializableData(
				preventIceMelting,
				preventHazardFireSpread,
				autoLightCandles,
				villageSpawnEnabled,
				historyDisplayMode,
				villageSpawnType,
				pottedPlantPickPrefersPot,
				createItemGroupVisibilityData()
			);
			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
			}
		} catch (IOException exception) {
			getLogger().error("險ｭ螳壹ヵ繧｡繧､繝ｫ縺ｮ菫晏ｭ倥↓螟ｱ謨励＠縺ｾ縺励◆: {}", configPath, exception);
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

	/**
	 * 焚火やマグマ由来の延焼を抑止するかどうかを取得する。
	 */
	public synchronized boolean isHazardFireProtectionEnabled() {
		return preventHazardFireSpread;
	}

	/**
	 * 焚火やマグマ由来の延焼抑止フラグを更新する。
	 */
	public synchronized void setHazardFireProtectionEnabled(boolean enabled) {
		if (this.preventHazardFireSpread != enabled) {
			this.preventHazardFireSpread = enabled;
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

	public synchronized boolean isPottedPlantPickPrefersPot() {
		return pottedPlantPickPrefersPot;
	}

	public synchronized void setPottedPlantPickPrefersPot(boolean enabled) {
		if (this.pottedPlantPickPrefersPot != enabled) {
			this.pottedPlantPickPrefersPot = enabled;
			save();
		}
	}

	public synchronized boolean isItemGroupEnabled(ItemGroupOption option) {
		return itemGroupVisibility.getOrDefault(option, true);
	}

	public synchronized void setItemGroupEnabled(ItemGroupOption option, boolean enabled) {
		Boolean current = itemGroupVisibility.get(option);
		if (current == null || current != enabled) {
			itemGroupVisibility.put(option, enabled);
			save();
		}
	}

	private Logger getLogger() {
		return BuildingSupport.LOGGER;
	}

	private static final class SerializableData {
		private boolean preventIceMelting;
		private boolean preventHazardFireSpread;
		private boolean autoLightCandles;
		private boolean villageSpawnEnabled;
		private HistoryDisplayMode historyDisplayMode;
		private String villageSpawnType = VillageSpawnType.PLAINS.id();
		private boolean pottedPlantPickPrefersPot = true;
		private Map<String, Boolean> itemGroupVisibility = new HashMap<>();

		private SerializableData(
			boolean preventIceMelting,
			boolean preventHazardFireSpread,
			boolean autoLightCandles,
			boolean villageSpawnEnabled,
			HistoryDisplayMode historyDisplayMode,
			VillageSpawnType villageSpawnType,
			boolean pottedPlantPickPrefersPot,
			Map<String, Boolean> itemGroupVisibility
		) {
			this.preventIceMelting = preventIceMelting;
			this.preventHazardFireSpread = preventHazardFireSpread;
			this.autoLightCandles = autoLightCandles;
			this.villageSpawnEnabled = villageSpawnEnabled;
			this.historyDisplayMode = historyDisplayMode;
			this.villageSpawnType = villageSpawnType == null ? VillageSpawnType.PLAINS.id() : villageSpawnType.id();
			this.pottedPlantPickPrefersPot = pottedPlantPickPrefersPot;
			if (itemGroupVisibility != null) {
				this.itemGroupVisibility.putAll(itemGroupVisibility);
			}
		}
	}

	private void resetItemGroupVisibility() {
		for (ItemGroupOption option : ItemGroupOption.values()) {
			itemGroupVisibility.put(option, true);
		}
	}

	private void applyItemGroupVisibility(Map<String, Boolean> source) {
		resetItemGroupVisibility();
		if (source == null) {
			return;
		}
		for (Map.Entry<String, Boolean> entry : source.entrySet()) {
			ItemGroupOption option = ItemGroupOption.fromId(entry.getKey());
			if (option != null && entry.getValue() != null) {
				itemGroupVisibility.put(option, entry.getValue());
			}
		}
	}

	private Map<String, Boolean> createItemGroupVisibilityData() {
		Map<String, Boolean> map = new HashMap<>();
		for (ItemGroupOption option : ItemGroupOption.values()) {
			map.put(option.id(), itemGroupVisibility.getOrDefault(option, true));
		}
		return map;
	}

	public enum HistoryDisplayMode {
		PER_WORLD("per_world"),
		ALL_WORLD("all_world");

		private final String suffix;

		HistoryDisplayMode(String suffix) {
			this.suffix = suffix;
		}

		public String translationKey() {
			return "config.utility-toolkit.history_display_mode." + suffix;
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
			return "config.utility-toolkit.spawn.village_biome." + id;
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

	public enum ItemGroupOption {
		FAVORITES("favorites_tab", "config.utility-toolkit.inventory_tab.favorites"),
		HISTORY("history_tab", "config.utility-toolkit.inventory_tab.history"),
		WOOD_BUILDING("wood_building_tab", "config.utility-toolkit.inventory_tab.wood_building"),
		STONE_BUILDING("stone_building_tab", "config.utility-toolkit.inventory_tab.stone_building"),
		COPPER_BUILDING("copper_building_tab", "config.utility-toolkit.inventory_tab.copper_building"),
		LIGHT_BUILDING("light_building_tab", "config.utility-toolkit.inventory_tab.light_building"),
		NETHER_BUILDING("nether_building_tab", "config.utility-toolkit.inventory_tab.nether_building"),
		END_BUILDING("end_building_tab", "config.utility-toolkit.inventory_tab.end_building"),
		SIGN_SHELF("sign_shelf_tab", "config.utility-toolkit.inventory_tab.sign_shelf"),
		ARMOR_LEATHER("armor_leather_tab", "config.utility-toolkit.inventory_tab.armor_leather"),
		ARMOR_CHAIN("armor_chain_tab", "config.utility-toolkit.inventory_tab.armor_chain"),
		ARMOR_IRON("armor_iron_tab", "config.utility-toolkit.inventory_tab.armor_iron"),
		ARMOR_GOLD("armor_gold_tab", "config.utility-toolkit.inventory_tab.armor_gold"),
		ARMOR_DIAMOND("armor_diamond_tab", "config.utility-toolkit.inventory_tab.armor_diamond"),
		ARMOR_NETHERITE("armor_netherite_tab", "config.utility-toolkit.inventory_tab.armor_netherite");

		private final String id;
		private final String translationKey;
		private final String tooltipKey;

		ItemGroupOption(String id, String translationKey) {
			this.id = id;
			this.translationKey = translationKey;
			this.tooltipKey = translationKey + ".tooltip";
		}

		public String id() {
			return id;
		}

		public String translationKey() {
			return translationKey;
		}

		public String tooltipKey() {
			return tooltipKey;
		}

		public static ItemGroupOption fromId(String id) {
			if (id == null || id.isBlank()) {
				return null;
			}
			for (ItemGroupOption option : values()) {
				if (option.id.equalsIgnoreCase(id)) {
					return option;
				}
			}
			return null;
		}
	}
}
