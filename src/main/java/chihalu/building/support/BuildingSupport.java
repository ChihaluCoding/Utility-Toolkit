package chihalu.building.support;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CandleCakeBlock;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chihalu.building.support.command.CommandPresetManager;
import chihalu.building.support.command.ExtinguishCommand;
import chihalu.building.support.command.MemoCommand;
import chihalu.building.support.command.MemoManager;
import chihalu.building.support.command.PresetCommand;
import chihalu.building.support.command.VillageCommand;
import chihalu.building.support.config.BuildingSupportConfig;
import chihalu.building.support.customtabs.CustomTabsManager;
import chihalu.building.support.favorites.FavoritesManager;
import chihalu.building.support.history.HistoryManager;
import chihalu.building.support.itemgroup.CopperBuildingItems;
import chihalu.building.support.itemgroup.EndBuildingItems;
import chihalu.building.support.itemgroup.LightBuildingItems;
import chihalu.building.support.itemgroup.NetherBuildingItems;
import chihalu.building.support.itemgroup.SignShelfItems;
import chihalu.building.support.itemgroup.StoneBuildingItems;
import chihalu.building.support.itemgroup.TrimmedArmorItems;
import chihalu.building.support.itemgroup.WoodBuildingItems;
import chihalu.building.support.village.VillageSpawnManager;

public class BuildingSupport implements ModInitializer {
	public static final String MOD_ID = "utility-toolkit";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final RegistryKey<ItemGroup> FAVORITES_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("a_zfavorites"));
	public static final RegistryKey<ItemGroup> WOOD_BUILDING_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("b_wood_building"));
	public static final RegistryKey<ItemGroup> STONE_BUILDING_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("c_stone_building"));
	public static final RegistryKey<ItemGroup> COPPER_BUILDING_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("d_copper_building"));
	public static final RegistryKey<ItemGroup> LIGHT_BUILDING_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("e_light_building"));
	public static final RegistryKey<ItemGroup> NETHER_BUILDING_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("f_nether_building"));
	public static final RegistryKey<ItemGroup> END_BUILDING_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("g_end_building"));
	public static final RegistryKey<ItemGroup> SIGN_SHELF_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("ga_sign_shelf"));
	public static final RegistryKey<ItemGroup> CUSTOM_TAB_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("ab_custom_tab"));
	private static final boolean AUTO_LIGHT_VANILLA_ONLY = true;
	private static final boolean AUTO_LIGHT_NEIGHBORS = true;
	public static final RegistryKey<ItemGroup> HISTORY_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("a_history_building"));
	public static final RegistryKey<ItemGroup> LEATHER_EQUIPMENT_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("h_leather_equipment"));
	public static final RegistryKey<ItemGroup> CHAIN_EQUIPMENT_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("i_chain_equipment"));
	public static final RegistryKey<ItemGroup> IRON_EQUIPMENT_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("j_iron_equipment"));
	public static final RegistryKey<ItemGroup> GOLD_EQUIPMENT_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("k_gold_equipment"));
	public static final RegistryKey<ItemGroup> DIAMOND_EQUIPMENT_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("l_diamond_equipment"));
	public static final RegistryKey<ItemGroup> NETHERITE_EQUIPMENT_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("m_netherite_equipment"));

	@Override
	public void onInitialize() {
		BuildingSupportConfig config = BuildingSupportConfig.getInstance();
		config.reload();
		FavoritesManager favoritesManager = FavoritesManager.getInstance();
		favoritesManager.reload();
		CustomTabsManager customTabsManager = CustomTabsManager.getInstance();
		customTabsManager.reload();
		CommandPresetManager presetManager = CommandPresetManager.getInstance();
		presetManager.reload();
		MemoManager.getInstance().reload();
		HistoryManager.getInstance().initialize();
		VillageSpawnManager.getInstance().initialize();
		registerItemGroups(favoritesManager, customTabsManager);
		registerCommands(presetManager);
		registerEvents();
		LOGGER.info("Utility Toolkit mod initialized");
	}

	private void registerItemGroups(FavoritesManager favoritesManager, CustomTabsManager customTabsManager) {
		HistoryManager historyManager = HistoryManager.getInstance();
		Registry.register(Registries.ITEM_GROUP, HISTORY_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.history_building"))
				.icon(() -> new ItemStack(Items.BOOK))
				.entries((displayContext, entries) -> historyManager.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, FAVORITES_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.favorites"))
				.icon(() -> new ItemStack(Blocks.AMETHYST_CLUSTER))
				.entries((displayContext, entries) -> favoritesManager.populate(entries))
				.build());

		ItemGroup customGroup = FabricItemGroup.builder()
			.displayName(Text.translatable("itemGroup.utility-toolkit.custom_tab"))
			.icon(customTabsManager::getIconStack)
			.entries((displayContext, entries) -> customTabsManager.populateEntries(entries))
			.build();
		Registry.register(Registries.ITEM_GROUP, CUSTOM_TAB_ITEM_GROUP_KEY, customGroup);
		customTabsManager.registerGroupInstance(customGroup);

		Registry.register(Registries.ITEM_GROUP, WOOD_BUILDING_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.wood_building"))
				.icon(WoodBuildingItems::getIconStack)
				.entries((displayContext, entries) -> WoodBuildingItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, STONE_BUILDING_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.stone_building"))
				.icon(StoneBuildingItems::getIconStack)
				.entries((displayContext, entries) -> StoneBuildingItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, COPPER_BUILDING_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.copper_building"))
				.icon(CopperBuildingItems::getIconStack)
				.entries((displayContext, entries) -> CopperBuildingItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, NETHER_BUILDING_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.nether_building"))
				.icon(NetherBuildingItems::getIconStack)
				.entries((displayContext, entries) -> NetherBuildingItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, LIGHT_BUILDING_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.light_building"))
				.icon(LightBuildingItems::getIconStack)
				.entries((displayContext, entries) -> LightBuildingItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, END_BUILDING_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.end_building"))
				.icon(EndBuildingItems::getIconStack)
				.entries((displayContext, entries) -> EndBuildingItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, SIGN_SHELF_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.sign_shelf"))
				.icon(SignShelfItems::getIconStack)
				.entries((displayContext, entries) -> SignShelfItems.populate(entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, LEATHER_EQUIPMENT_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.equipment.leather"))
				.icon(() -> TrimmedArmorItems.createIcon(Items.LEATHER_CHESTPLATE))
				.entries((displayContext, entries) -> TrimmedArmorItems.populateLeather(displayContext, entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, CHAIN_EQUIPMENT_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.equipment.chain"))
				.icon(() -> TrimmedArmorItems.createIcon(Items.CHAINMAIL_CHESTPLATE))
				.entries((displayContext, entries) -> TrimmedArmorItems.populateChainmail(displayContext, entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, IRON_EQUIPMENT_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.equipment.iron"))
				.icon(() -> TrimmedArmorItems.createIcon(Items.IRON_CHESTPLATE))
				.entries((displayContext, entries) -> TrimmedArmorItems.populateIron(displayContext, entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, GOLD_EQUIPMENT_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.equipment.gold"))
				.icon(() -> TrimmedArmorItems.createIcon(Items.GOLDEN_CHESTPLATE))
				.entries((displayContext, entries) -> TrimmedArmorItems.populateGold(displayContext, entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, DIAMOND_EQUIPMENT_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.equipment.diamond"))
				.icon(() -> TrimmedArmorItems.createIcon(Items.DIAMOND_CHESTPLATE))
				.entries((displayContext, entries) -> TrimmedArmorItems.populateDiamond(displayContext, entries))
				.build());

		Registry.register(Registries.ITEM_GROUP, NETHERITE_EQUIPMENT_ITEM_GROUP_KEY,
			FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup.utility-toolkit.equipment.netherite"))
				.icon(() -> TrimmedArmorItems.createIcon(Items.NETHERITE_CHESTPLATE))
				.entries((displayContext, entries) -> TrimmedArmorItems.populateNetherite(displayContext, entries))
				.build());
	}

	private void registerCommands(CommandPresetManager presetManager) {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			PresetCommand.register(dispatcher, presetManager));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			MemoCommand.register(dispatcher, MemoManager.getInstance()));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			VillageCommand.register(dispatcher, registryAccess, VillageSpawnManager.getInstance()));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			ExtinguishCommand.register(dispatcher, registryAccess));
	}

	private void registerEvents() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!(world instanceof ServerWorld serverWorld)) {
				return ActionResult.PASS;
			}

			BuildingSupportConfig config = BuildingSupportConfig.getInstance();

			if (!config.isAutoLightCandlesEnabled()) {
				return ActionResult.PASS;
			}

			ItemStack stack = player.getStackInHand(hand);
			if (!stack.isIn(ItemTags.CANDLES)) {
				return ActionResult.PASS;
			}

			BlockPos targetPos = hitResult.getBlockPos();
			BlockPos offsetPos = targetPos.offset(hitResult.getSide());
			boolean restrictVanilla = isAutoLightVanillaRestricted();
			boolean lightNeighbors = isAutoLightNeighborEnabled();

			serverWorld.getServer().execute(() -> {
				autoLightCandles(serverWorld, targetPos, restrictVanilla);
				if (lightNeighbors) {
					autoLightCandles(serverWorld, offsetPos, restrictVanilla);
				}
			});

			return ActionResult.PASS;
		});

	}

	private static void autoLightCandles(ServerWorld world, BlockPos pos, boolean restrictVanilla) {
		if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
			return;
		}

		BlockState state = world.getBlockState(pos);
		if (state.isAir()) {
			return;
		}

		if (restrictVanilla && !isVanillaBlock(state.getBlock())) {
			return;
		}

		if (state.getBlock() instanceof AbstractCandleBlock && state.contains(AbstractCandleBlock.LIT) && !state.get(AbstractCandleBlock.LIT)) {
			world.setBlockState(pos, state.with(AbstractCandleBlock.LIT, true), Block.NOTIFY_ALL);
			return;
		}

		if (state.getBlock() instanceof CandleCakeBlock && state.contains(CandleCakeBlock.LIT) && !state.get(CandleCakeBlock.LIT)) {
			world.setBlockState(pos, state.with(CandleCakeBlock.LIT, true), Block.NOTIFY_ALL);
		}
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	/**
	 * バニラ（minecraft 名前空間）のブロックかどうかを判定するヘルパー。
	 */
	public static boolean isVanillaBlock(Block block) {
		if (block == null) {
			return false;
		}
		Identifier identifier = Registries.BLOCK.getId(block);
		return identifier != null && "minecraft".equals(identifier.getNamespace());
	}

	public static boolean isAutoLightVanillaRestricted() {
		return AUTO_LIGHT_VANILLA_ONLY;
	}

	public static boolean isAutoLightNeighborEnabled() {
		return AUTO_LIGHT_NEIGHBORS;
	}
}
