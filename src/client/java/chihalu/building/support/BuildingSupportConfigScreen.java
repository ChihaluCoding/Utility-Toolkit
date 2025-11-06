package chihalu.building.support;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class BuildingSupportConfigScreen extends Screen {
	private final Screen parent;
	private Category currentCategory = Category.ROOT;

	private static final int BUTTON_WIDTH = 160;
	private static final int BUTTON_HEIGHT = 20;
	private static final int COLUMN_SPACING = 12;
	private static final int ROW_SPACING = 24;

	private enum Category {
		ROOT,
		ENVIRONMENT,
		AUTOMATION,
		PICK_BLOCK_CONTROL,
		SPAWN,
		INVENTORY_CONTROL
	}

	public BuildingSupportConfigScreen(Screen parent) {
		super(Text.translatable("config.building-support.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		openRoot();
	}

	private void openRoot() {
		currentCategory = Category.ROOT;
		clearScreen();
		int centerX = width / 2;
		int leftX = getLeftColumnX();
		int rightX = getRightColumnX();
		int startY = height / 2 - ROW_SPACING * 2;

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.building-support.category.environment"),
			button -> openEnvironment())
			.dimensions(leftX, startY, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.building-support.category.automation"),
			button -> openAutomation())
			.dimensions(rightX, startY, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.building-support.category.pick_block_control"),
			button -> openPickBlockControl())
			.dimensions(leftX, startY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.building-support.category.spawn"),
			button -> openSpawn())
			.dimensions(rightX, startY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.building-support.category.inventory_control"),
			button -> openInventoryControl())
			.dimensions(leftX, startY + ROW_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
			.dimensions(centerX - BUTTON_WIDTH / 2, startY + ROW_SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openPickBlockControl() {
		currentCategory = Category.PICK_BLOCK_CONTROL;
		clearScreen();
		int centerX = width / 2;
		int leftX = getLeftColumnX();
		int startY = height / 2 - 10;
		var config = BuildingSupportConfig.getInstance();

		CyclingButtonWidget<Boolean> potToggle = CyclingButtonWidget.onOffBuilder(config.isPottedPlantPickPrefersPot())
			.build(leftX, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.building-support.potted_pick_prefers_pot"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setPottedPlantPickPrefersPot(value);
					button.setFocused(false);
					setFocused(null);
				});
		potToggle.setFocused(false);
		addDrawableChild(potToggle);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.building-support.back_to_categories"),
			button -> {
				setFocused(null);
				openRoot();
			})
			.dimensions(centerX - BUTTON_WIDTH / 2, startY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openEnvironment() {
		currentCategory = Category.ENVIRONMENT;
		clearScreen();
		int centerX = width / 2;
		int leftX = getLeftColumnX();
		int startY = height / 2 - 10;
		var config = BuildingSupportConfig.getInstance();

		CyclingButtonWidget<Boolean> toggle = CyclingButtonWidget.onOffBuilder(config.isPreventIceMeltingEnabled())
			.build(leftX, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.building-support.prevent_ice_melting"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setPreventIceMeltingEnabled(value);
					button.setFocused(false);
					setFocused(null);
		});
		toggle.setFocused(false);
		addDrawableChild(toggle);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.building-support.back_to_categories"),
			button -> {
				setFocused(null);
				openRoot();
			})
			.dimensions(centerX - BUTTON_WIDTH / 2, startY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openAutomation() {
		currentCategory = Category.AUTOMATION;
		clearScreen();
		int centerX = width / 2;
		int leftX = getLeftColumnX();
		int startY = height / 2 - 10;
		var config = BuildingSupportConfig.getInstance();

		CyclingButtonWidget<Boolean> toggle = CyclingButtonWidget.onOffBuilder(config.isAutoLightCandlesEnabled())
			.build(leftX, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.building-support.auto_light_candles"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setAutoLightCandlesEnabled(value);
					button.setFocused(false);
					setFocused(null);
				});
		toggle.setFocused(false);
		addDrawableChild(toggle);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.building-support.back_to_categories"),
			button -> {
				setFocused(null);
				openRoot();
			})
			.dimensions(centerX - BUTTON_WIDTH / 2, startY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openSpawn() {
		currentCategory = Category.SPAWN;
		clearScreen();
		int centerX = width / 2;
		int leftX = getLeftColumnX();
		int rightX = getRightColumnX();
		int startY = height / 2 - 34;
		var config = BuildingSupportConfig.getInstance();
		@SuppressWarnings("unchecked")
		CyclingButtonWidget<BuildingSupportConfig.VillageSpawnType>[] typeButtonHolder = new CyclingButtonWidget[1];

		CyclingButtonWidget<Boolean> toggle = CyclingButtonWidget.onOffBuilder(config.isVillageSpawnEnabled())
			.build(leftX, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.building-support.spawn.force_village"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setVillageSpawnEnabled(value);
					CyclingButtonWidget<BuildingSupportConfig.VillageSpawnType> typeButton = typeButtonHolder[0];
					if (typeButton != null) {
						typeButton.active = value;
					}
					button.setFocused(false);
					setFocused(null);
				});
		toggle.setFocused(false);
		addDrawableChild(toggle);

		CyclingButtonWidget<BuildingSupportConfig.VillageSpawnType> typeButton =
			CyclingButtonWidget.<BuildingSupportConfig.VillageSpawnType>builder(type -> Text.translatable(type.translationKey()))
				.values(BuildingSupportConfig.VillageSpawnType.values())
				.initially(config.getVillageSpawnType())
				.build(rightX, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
					Text.translatable("config.building-support.spawn.village_biome"),
					(button, value) -> {
						BuildingSupportConfig.getInstance().setVillageSpawnType(value);
						button.setFocused(false);
						setFocused(null);
					});
		typeButton.active = config.isVillageSpawnEnabled();
		typeButton.setFocused(false);
		addDrawableChild(typeButton);
		typeButtonHolder[0] = typeButton;

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.building-support.back_to_categories"),
			button -> {
				setFocused(null);
				openRoot();
			})
			.dimensions(centerX - BUTTON_WIDTH / 2, startY + ROW_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openInventoryControl() {
		currentCategory = Category.INVENTORY_CONTROL;
		clearScreen();
		int centerX = width / 2;
		int leftX = getLeftColumnX();
		int startY = height / 2 - 10;
		var config = BuildingSupportConfig.getInstance();

		CyclingButtonWidget<BuildingSupportConfig.HistoryDisplayMode> modeButton =
			CyclingButtonWidget.<BuildingSupportConfig.HistoryDisplayMode>builder(mode -> Text.translatable(mode.translationKey()))
				.values(BuildingSupportConfig.HistoryDisplayMode.values())
				.initially(config.getHistoryDisplayMode())
				.build(leftX, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
					Text.translatable("config.building-support.history_display_mode"),
					(button, value) -> {
						BuildingSupportConfig.getInstance().setHistoryDisplayMode(value);
						BuildingSupportClient.onHistoryModeChanged();
						button.setFocused(false);
						setFocused(null);
					});
		modeButton.setFocused(false);
		addDrawableChild(modeButton);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.building-support.back_to_categories"),
			button -> {
				setFocused(null);
				openRoot();
			})
			.dimensions(centerX - BUTTON_WIDTH / 2, startY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void clearScreen() {
		clearChildren();
	}

	@Override
	public void tick() {
		super.tick();
		if (getFocused() instanceof CyclingButtonWidget<?>) {
			setFocused(null);
		}
	}

	@Override
	public void close() {
		if (client != null) {
			client.setScreen(parent);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0xB0000000);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);

		if (currentCategory == Category.ENVIRONMENT) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.building-support.category.environment"), width / 2, 50, 0xFFFFFF);
		} else if (currentCategory == Category.AUTOMATION) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.building-support.category.automation"), width / 2, 50, 0xFFFFFF);
		} else if (currentCategory == Category.PICK_BLOCK_CONTROL) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.building-support.category.pick_block_control"), width / 2, 50, 0xFFFFFF);
		} else if (currentCategory == Category.SPAWN) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.building-support.category.spawn"), width / 2, 50, 0xFFFFFF);
		} else if (currentCategory == Category.INVENTORY_CONTROL) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.building-support.category.inventory_control"), width / 2, 50, 0xFFFFFF);
		}

		super.render(context, mouseX, mouseY, delta);
	}

	private int getLeftColumnX() {
		return width / 2 - BUTTON_WIDTH - COLUMN_SPACING / 2;
	}

	private int getRightColumnX() {
		return width / 2 + COLUMN_SPACING / 2;
	}
}
