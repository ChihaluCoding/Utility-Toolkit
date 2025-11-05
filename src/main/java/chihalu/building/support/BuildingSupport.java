package chihalu.building.support;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildingSupport implements ModInitializer {
	public static final String MOD_ID = "building-support";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final RegistryKey<ItemGroup> FAVORITES_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("a_favorites"));
	public static final RegistryKey<ItemGroup> WOOD_BUILDING_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("b_wood_building"));
	public static final RegistryKey<ItemGroup> STONE_BUILDING_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("c_stone_building"));
	public static final RegistryKey<ItemGroup> COPPER_BUILDING_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("d_copper_building"));
	public static final RegistryKey<ItemGroup> LIGHT_BUILDING_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("e_light_building"));
	public static final RegistryKey<ItemGroup> NETHER_BUILDING_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("f_nether_building"));
	public static final RegistryKey<ItemGroup> END_BUILDING_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("g_end_building"));
	public static final RegistryKey<ItemGroup> HISTORY_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("h_history_building"));

	@Override
	public void onInitialize() {
		BuildingSupportConfig.getInstance().reload();
		FavoritesManager favoritesManager = FavoritesManager.getInstance();
		favoritesManager.reload();
		CommandPresetManager presetManager = CommandPresetManager.getInstance();
		presetManager.reload();
		HistoryManager.getInstance().reload();
		registerItemGroups(favoritesManager);
		registerCommands(favoritesManager, presetManager);
		LOGGER.info("Building Support mod initialized");
	}

	private void registerItemGroups(FavoritesManager manager) {
		Registry.register(Registries.ITEM_GROUP, FAVORITES_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.building-support.favorites"))
				.icon(manager::getIconStack)
				.entries((displayContext, entries) -> manager.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, WOOD_BUILDING_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.building-support.wood_building"))
				.icon(() -> WoodBuildingItems.getIconStack())
				.entries((displayContext, entries) -> WoodBuildingItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, STONE_BUILDING_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.building-support.stone_building"))
				.icon(() -> StoneBuildingItems.getIconStack())
				.entries((displayContext, entries) -> StoneBuildingItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, COPPER_BUILDING_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.building-support.copper_building"))
				.icon(() -> CopperBuildingItems.getIconStack())
				.entries((displayContext, entries) -> CopperBuildingItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, NETHER_BUILDING_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.building-support.nether_building"))
				.icon(() -> NetherBuildingItems.getIconStack())
				.entries((displayContext, entries) -> NetherBuildingItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, LIGHT_BUILDING_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.building-support.light_building"))
				.icon(() -> LightBuildingItems.getIconStack())
				.entries((displayContext, entries) -> LightBuildingItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, END_BUILDING_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.building-support.end_building"))
				.icon(() -> EndBuildingItems.getIconStack())
				.entries((displayContext, entries) -> EndBuildingItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, HISTORY_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.building-support.history_building"))
				.icon(() -> HistoryManager.getInstance().getIconStack())
				.entries((displayContext, entries) -> HistoryManager.getInstance().populate(entries))
				.build());
	}

	private void registerCommands(FavoritesManager favoritesManager, CommandPresetManager presetManager) {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			FavoritesCommand.register(dispatcher, registryAccess, favoritesManager));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			PresetCommand.register(dispatcher, presetManager));
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
