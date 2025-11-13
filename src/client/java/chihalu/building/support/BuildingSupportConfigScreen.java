package chihalu.building.support;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import chihalu.building.support.config.BuildingSupportConfig;
import chihalu.building.support.config.BuildingSupportConfig.WeatherMode;
import chihalu.building.support.customtabs.CustomTabsManager;

@Environment(EnvType.CLIENT)
public class BuildingSupportConfigScreen extends Screen {
	private final Screen parent;
	private Category currentCategory = Category.ROOT;

	private static final int BUTTON_WIDTH = 160;
	private static final int BUTTON_HEIGHT = 20;
	private static final int COLUMN_SPACING = 12;
	private static final int ROW_SPACING = 24;
	private static final int TAB_LIST_ROW_HEIGHT = BUTTON_HEIGHT + 18; // タブリストは標準ボタンより縦にゆとりを持たせる
	private TabToggleListWidget tabToggleList;
	private int tabToggleBreadcrumbY = 0;
	private Text tabToggleBreadcrumbText = Text.empty();
	private TextFieldWidget customTabNameField;
	private TextFieldWidget customTabIconField;

	private enum Category {
		ROOT,
		ENVIRONMENT,
		AUTOMATION,
		PICK_BLOCK_CONTROL,
		SPAWN,
		INVENTORY_CONTROL,
		WORLD_SETTINGS,
		CUSTOM_TABS,
		OPTIMIZATION,
		INVENTORY_TAB_TOGGLE
	}

	public BuildingSupportConfigScreen(Screen parent) {
		super(Text.translatable("config.utility-toolkit.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		openRoot();
	}


	private void openRoot() {
		currentCategory = Category.ROOT;
		clearScreen();
		int leftX = getLeftColumnX();
		int rightX = getRightColumnX();
		int startY = height / 2 - ROW_SPACING * 2;

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.category.environment"),
			button -> openEnvironment())
			.dimensions(leftX, startY, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.category.automation"),
			button -> openAutomation())
			.dimensions(rightX, startY, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.category.pick_block_control"),
			button -> openPickBlockControl())
			.dimensions(leftX, startY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.category.spawn"),
			button -> openSpawn())
			.dimensions(rightX, startY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.category.inventory_control"),
			button -> openInventoryControl())
			.dimensions(leftX, startY + ROW_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.category.optimization"),
			button -> openOptimization())
			.dimensions(rightX, startY + ROW_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.category.world_settings"),
			button -> openWorldSettings())
			.dimensions(leftX, startY + ROW_SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
			.dimensions(getCenterButtonX(), getBottomButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openPickBlockControl() {
		currentCategory = Category.PICK_BLOCK_CONTROL;
		clearScreen();
		int leftX = getLeftColumnX();
		int startY = height / 2 - 10;
		var config = BuildingSupportConfig.getInstance();

		CyclingButtonWidget<Boolean> potToggle = CyclingButtonWidget.onOffBuilder(config.isPottedPlantPickPrefersPot())
			.build(leftX, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.utility-toolkit.potted_pick_prefers_pot"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setPottedPlantPickPrefersPot(value);
					button.setFocused(false);
					setFocused(null);
				});
		potToggle.setFocused(false);
		addDrawableChild(potToggle);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.back_to_categories"),
			button -> {
				setFocused(null);
				openRoot();
			})
			.dimensions(getCenterButtonX(), getBottomButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openEnvironment() {
		currentCategory = Category.ENVIRONMENT;
		clearScreen();
		int leftX = getLeftColumnX();
		int rightX = getRightColumnX();
		int startY = height / 2 - 10;
		var config = BuildingSupportConfig.getInstance();

		CyclingButtonWidget<Boolean> iceToggle = CyclingButtonWidget.onOffBuilder(config.isPreventIceMeltingEnabled())
			.build(leftX, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.utility-toolkit.prevent_ice_melting"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setPreventIceMeltingEnabled(value);
					button.setFocused(false);
					setFocused(null);
				});
		iceToggle.setFocused(false);
		addDrawableChild(iceToggle);

		// 右列には火災拡大防止のトグルをまとめて配置する
		CyclingButtonWidget<Boolean> fireSpreadToggle = CyclingButtonWidget.onOffBuilder(config.isHazardFireProtectionEnabled())
			.build(rightX, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.utility-toolkit.prevent_hazard_fire_spread"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setHazardFireProtectionEnabled(value);
					button.setFocused(false);
					setFocused(null);
				});
		fireSpreadToggle.setFocused(false);
		addDrawableChild(fireSpreadToggle);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.back_to_categories"),
			button -> {
				setFocused(null);
				openRoot();
			})
			.dimensions(getCenterButtonX(), getBottomButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openOptimization() {
		currentCategory = Category.OPTIMIZATION;
		clearScreen();
		int leftX = getLeftColumnX();
		int rightX = getRightColumnX();
		int startY = height / 2 - 10;
		var config = BuildingSupportConfig.getInstance();

		CyclingButtonWidget<Boolean> disableSignInput = CyclingButtonWidget.onOffBuilder(config.isSignEditScreenDisabled())
			.build(leftX, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.utility-toolkit.disable_sign_edit_screen"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setSignEditScreenDisabled(value);
					button.setFocused(false);
					setFocused(null);
				});
		disableSignInput.setTooltip(Tooltip.of(Text.translatable("config.utility-toolkit.disable_sign_edit_screen.tooltip")));
		addDrawableChild(disableSignInput);

		CyclingButtonWidget<Boolean> carpetStringToggle = CyclingButtonWidget.onOffBuilder(config.isAutoCarpetStringEnabled())
			.build(rightX, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.utility-toolkit.auto_carpet_string"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setAutoCarpetStringEnabled(value);
					button.setFocused(false);
					setFocused(null);
				});
		carpetStringToggle.setTooltip(Tooltip.of(Text.translatable("config.utility-toolkit.auto_carpet_string.tooltip")));
		addDrawableChild(carpetStringToggle);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.back_to_categories"),
			button -> {
				setFocused(null);
				openRoot();
			})
			.dimensions(getCenterButtonX(), getBottomButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openWorldSettings() {
		currentCategory = Category.WORLD_SETTINGS;
		clearScreen();
		var config = BuildingSupportConfig.getInstance();
		int leftX = getLeftColumnX();
		int rightX = getRightColumnX();
		int baseY = Math.max(60, height / 2 - ROW_SPACING * 2);
		int labelOffset = textRenderer.fontHeight + 4;

		// 時間固定の有効・無効を切り替えるトグル
		CyclingButtonWidget<Boolean> fixedTimeToggle = CyclingButtonWidget.onOffBuilder(config.isFixedTimeEnabled())
			.build(leftX, baseY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.utility-toolkit.world.fixed_time.toggle"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setFixedTimeEnabled(value);
					button.setFocused(false);
					setFocused(null);
				});
		addDrawableChild(fixedTimeToggle);

		// 時間数値を直接入力するフィールド
		int timeFieldY = baseY + ROW_SPACING;
		addDrawableChild(new LabelWidget(leftX, timeFieldY - labelOffset, Text.translatable("config.utility-toolkit.world.fixed_time.value_label")));
		TextFieldWidget timeField = new TextFieldWidget(textRenderer, leftX, timeFieldY, BUTTON_WIDTH, BUTTON_HEIGHT, Text.empty());
		timeField.setMaxLength(6);
		timeField.setText(String.valueOf(config.getFixedTimeValue()));
		timeField.setChangedListener(value -> timeField.setEditableColor(0xFFFFFFFF));
		addDrawableChild(timeField);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.world.fixed_time.apply"),
			button -> {
				if (applyFixedTimeValue(timeField)) {
					button.setFocused(false);
					setFocused(null);
				}
			})
			.dimensions(leftX, timeFieldY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		// 天候固定のトグル
		CyclingButtonWidget<Boolean> fixedWeatherToggle = CyclingButtonWidget.onOffBuilder(config.isFixedWeatherEnabled())
			.build(rightX, baseY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.utility-toolkit.world.fixed_weather.toggle"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setFixedWeatherEnabled(value);
					button.setFocused(false);
					setFocused(null);
				});
		addDrawableChild(fixedWeatherToggle);

		// 天候モードを選択するボタン
		CyclingButtonWidget<WeatherMode> weatherModeButton = CyclingButtonWidget.<WeatherMode>builder(mode -> Text.translatable(mode.translationKey()))
			.values(WeatherMode.values())
			.initially(config.getFixedWeatherMode())
			.build(rightX, baseY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.utility-toolkit.world.fixed_weather.mode"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setFixedWeatherMode(value);
					button.setFocused(false);
					setFocused(null);
				});
		addDrawableChild(weatherModeButton);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.back_to_categories"),
			button -> {
				setFocused(null);
				openRoot();
			})
			.dimensions(getCenterButtonX(), getBottomButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openAutomation() {
		currentCategory = Category.AUTOMATION;
		clearScreen();
		int leftX = getLeftColumnX();
		int startY = height / 2 - 10;
		var config = BuildingSupportConfig.getInstance();

		CyclingButtonWidget<Boolean> toggle = CyclingButtonWidget.onOffBuilder(config.isAutoLightCandlesEnabled())
			.build(leftX, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.utility-toolkit.auto_light_candles"),
				(button, value) -> {
					BuildingSupportConfig.getInstance().setAutoLightCandlesEnabled(value);
					button.setFocused(false);
					setFocused(null);
				});
		toggle.setFocused(false);
		addDrawableChild(toggle);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.back_to_categories"),
			button -> {
				setFocused(null);
				openRoot();
			})
			.dimensions(getCenterButtonX(), getBottomButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openSpawn() {
		currentCategory = Category.SPAWN;
		clearScreen();
		var config = BuildingSupportConfig.getInstance();
		@SuppressWarnings("unchecked")
		CyclingButtonWidget<BuildingSupportConfig.VillageSpawnType>[] typeButtonHolder = new CyclingButtonWidget[1];
		List<ClickableWidget> spawnOptionButtons = new ArrayList<>();

		CyclingButtonWidget<Boolean> toggle = CyclingButtonWidget.onOffBuilder(config.isVillageSpawnEnabled())
			.build(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.utility-toolkit.spawn.force_village"),
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
		spawnOptionButtons.add(toggle);

		CyclingButtonWidget<BuildingSupportConfig.VillageSpawnType> typeButton =
			CyclingButtonWidget.<BuildingSupportConfig.VillageSpawnType>builder(type -> Text.translatable(type.translationKey()))
				.values(BuildingSupportConfig.VillageSpawnType.values())
				.initially(config.getVillageSpawnType())
				.build(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
					Text.translatable("config.utility-toolkit.spawn.village_biome"),
					(button, value) -> {
						BuildingSupportConfig.getInstance().setVillageSpawnType(value);
						button.setFocused(false);
						setFocused(null);
					});
		typeButton.active = config.isVillageSpawnEnabled();
		typeButton.setFocused(false);
		spawnOptionButtons.add(typeButton);
		typeButtonHolder[0] = typeButton;

		layoutWidgetsCentered(spawnOptionButtons, 2); // スポーン設定のボタン群を2列中央配置で揃える
		spawnOptionButtons.forEach(this::addDrawableChild);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.back_to_categories"),
			button -> {
				setFocused(null);
				openRoot();
			})
			.dimensions(getCenterButtonX(), getBottomButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openInventoryControl() {
		currentCategory = Category.INVENTORY_CONTROL;
		clearScreen();
		int leftX = getLeftColumnX();
		int rightX = getRightColumnX();
		int startY = Math.max(60, height / 2 - ROW_SPACING * 2);
		var config = BuildingSupportConfig.getInstance();

		CyclingButtonWidget<BuildingSupportConfig.HistoryDisplayMode> modeButton =
			CyclingButtonWidget.<BuildingSupportConfig.HistoryDisplayMode>builder(mode -> Text.translatable(mode.translationKey()))
				.values(BuildingSupportConfig.HistoryDisplayMode.values())
				.initially(config.getHistoryDisplayMode())
				.build(getLeftColumnX(), startY, BUTTON_WIDTH, BUTTON_HEIGHT,
					Text.translatable("config.utility-toolkit.history_display_mode"),
					(button, value) -> {
						BuildingSupportConfig.getInstance().setHistoryDisplayMode(value);
						BuildingSupportClient.onHistoryModeChanged();
						button.setFocused(false);
						setFocused(null);
					});
		modeButton.setFocused(false);
		addDrawableChild(modeButton);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.history_reset.button"),
			button -> {
				if (client != null) {
					client.setScreen(new HistoryResetScreen(this));
				}
			})
			.dimensions(getRightColumnX(), startY, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.inventory_control.open_tab_list"),
			button -> {
				setFocused(null);
				openInventoryTabToggleList();
			})
			.dimensions(leftX, startY + ROW_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.inventory_control.custom_tabs"),
			button -> {
				setFocused(null);
				openCustomTabsScreen();
			})
			.dimensions(rightX, startY + ROW_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.back_to_categories"),
			button -> {
				setFocused(null);
				openRoot();
			})
			.dimensions(getCenterButtonX(), getBottomButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openInventoryTabToggleList() {
		currentCategory = Category.INVENTORY_TAB_TOGGLE;
		clearScreen();
		tabToggleBreadcrumbText = Text.translatable("config.utility-toolkit.inventory_control.tab_path");

		List<CyclingButtonWidget<Boolean>> toggleWidgets = new ArrayList<>();
		var config = BuildingSupportConfig.getInstance();
		for (BuildingSupportConfig.ItemGroupOption option : BuildingSupportConfig.ItemGroupOption.values()) {
			CyclingButtonWidget<Boolean> tabToggle = CyclingButtonWidget.onOffBuilder(config.isItemGroupEnabled(option))
				.build(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
					Text.translatable(option.translationKey()),
					(button, value) -> {
						BuildingSupportConfig.getInstance().setItemGroupEnabled(option, value);
						InventoryTabVisibilityController.reloadFromConfig();
						button.setFocused(false);
						setFocused(null);
					});
			tabToggle.setFocused(false);
			toggleWidgets.add(tabToggle);
		}

		if (client != null) {
			int listWidth = BUTTON_WIDTH * 2 + COLUMN_SPACING;
			int totalRowHeight = TAB_LIST_ROW_HEIGHT * Math.max(1, (toggleWidgets.size() + 1) / 2);
			int maxHeight = getBottomButtonY() - ROW_SPACING * 3;
			int listHeight = Math.min(totalRowHeight, Math.max(TAB_LIST_ROW_HEIGHT * 4, maxHeight));
			int listTop = Math.max(ROW_SPACING * 2, (height - listHeight) / 2);
			tabToggleBreadcrumbY = listTop - ROW_SPACING;
			tabToggleList = new TabToggleListWidget(client, listWidth, listHeight, listTop, TAB_LIST_ROW_HEIGHT);
			tabToggleList.setX(width / 2 - listWidth / 2);
			tabToggleList.populate(toggleWidgets);
			addDrawableChild(tabToggleList);

			int controlButtonY = Math.max(ROW_SPACING, listTop - ROW_SPACING * 2);
			addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.inventory_control.toggle_all_on"),
				button -> {
					setAllTabVisibility(true);
					openInventoryTabToggleList();
				})
				.dimensions(width / 2 - BUTTON_WIDTH - COLUMN_SPACING / 2, controlButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());

			addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.inventory_control.toggle_all_off"),
				button -> {
					setAllTabVisibility(false);
					openInventoryTabToggleList();
				})
				.dimensions(width / 2 + COLUMN_SPACING / 2, controlButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		}

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.inventory_control.back_to_inventory"),
			button -> {
				setFocused(null);
				openInventoryControl();
			})
			.dimensions(getCenterButtonX(), getBottomButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void openCustomTabsScreen() {
		currentCategory = Category.CUSTOM_TABS;
		clearScreen();
		int fieldWidth = BUTTON_WIDTH * 2 + COLUMN_SPACING;
		int fieldX = width / 2 - fieldWidth / 2;
		int baseY = Math.max(60, height / 2 - ROW_SPACING * 2);
		var config = BuildingSupportConfig.getInstance();

		// 常時表示されるカスタムタブの名前を直接編集できるフィールド
		int labelOffset = textRenderer.fontHeight + 4;
		addDrawableChild(new LabelWidget(fieldX, baseY - labelOffset, Text.translatable("config.utility-toolkit.custom_tabs.name_label")));
		customTabNameField = new TextFieldWidget(textRenderer, fieldX, baseY, fieldWidth, BUTTON_HEIGHT, Text.empty());
		customTabNameField.setMaxLength(32);
		customTabNameField.setText(config.getCustomTabName());
		addDrawableChild(customTabNameField);

		int iconFieldY = baseY + ROW_SPACING * 2;
		addDrawableChild(new LabelWidget(fieldX, iconFieldY - labelOffset, Text.translatable("config.utility-toolkit.custom_tabs.icon_label")));
		customTabIconField = new TextFieldWidget(textRenderer, fieldX, iconFieldY, fieldWidth, BUTTON_HEIGHT, Text.empty());
		customTabIconField.setMaxLength(128);
		customTabIconField.setText(config.getCustomTabIconId());
		customTabIconField.setSuggestion("minecraft:paper");
		customTabIconField.setTooltip(Tooltip.of(Text.translatable("config.utility-toolkit.custom_tabs.icon_hint")));
		addDrawableChild(customTabIconField);

		int applyButtonY = iconFieldY + ROW_SPACING;
		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.custom_tabs.rename_button"),
			button -> {
				config.setCustomTabName(customTabNameField.getText());
				boolean iconChanged = config.setCustomTabIconId(customTabIconField.getText());
				if (iconChanged) {
					CustomTabsManager.getInstance().refreshGroupIcon();
				}
				button.setFocused(false);
				setFocused(null);
			})
			.dimensions(getCenterButtonX(), applyButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.inventory_control.back_to_inventory"),
			button -> {
				setFocused(null);
				openInventoryControl();
			})
			.dimensions(getCenterButtonX(), getBottomButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		setFocused(null);
	}

	private void clearScreen() {
		clearChildren();
		tabToggleList = null;
		tabToggleBreadcrumbText = Text.empty();
		customTabNameField = null;
		customTabIconField = null;
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
		super.close();
		if (client != null) {
			client.setScreen(parent);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0xB0000000);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);

		if (currentCategory == Category.ENVIRONMENT) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.utility-toolkit.category.environment"), width / 2, 50, 0xFFFFFF);
		} else if (currentCategory == Category.AUTOMATION) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.utility-toolkit.category.automation"), width / 2, 50, 0xFFFFFF);
		} else if (currentCategory == Category.PICK_BLOCK_CONTROL) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.utility-toolkit.category.pick_block_control"), width / 2, 50, 0xFFFFFF);
		} else if (currentCategory == Category.SPAWN) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.utility-toolkit.category.spawn"), width / 2, 50, 0xFFFFFF);
		} else if (currentCategory == Category.INVENTORY_CONTROL) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.utility-toolkit.category.inventory_control"), width / 2, 50, 0xFFFFFF);
		} else if (currentCategory == Category.INVENTORY_TAB_TOGGLE) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.utility-toolkit.inventory_control.tab_category"), width / 2, 50, 0xFFFFFF);
			if (tabToggleList != null && !tabToggleBreadcrumbText.getString().isBlank()) {
				context.drawCenteredTextWithShadow(textRenderer, tabToggleBreadcrumbText, width / 2, tabToggleBreadcrumbY, 0xFFFFFF);
			}
		} else if (currentCategory == Category.CUSTOM_TABS) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.utility-toolkit.inventory_control.custom_tabs"), width / 2, 50, 0xFFFFFF);
		} else if (currentCategory == Category.WORLD_SETTINGS) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.utility-toolkit.category.world_settings"), width / 2, 50, 0xFFFFFF);
		}

		super.render(context, mouseX, mouseY, delta);
	}

	private int getLeftColumnX() {
		return width / 2 - BUTTON_WIDTH - COLUMN_SPACING / 2;
	}

	private int getRightColumnX() {
		return width / 2 + COLUMN_SPACING / 2;
	}

	private int layoutWidgetsCentered(List<? extends ClickableWidget> widgets, int columns) {
		if (widgets.isEmpty()) {
			return height / 2;
		}
		int totalRows = Math.max(1, (widgets.size() + columns - 1) / columns);
		int blockHeight = totalRows * BUTTON_HEIGHT + (totalRows - 1) * ROW_SPACING;
		int currentY = Math.max((height - blockHeight) / 2, 40); // 中央基準の開始位置（画面が狭い場合の余裕も確保）
		int widgetIndex = 0;
		int remaining = widgets.size();
		int lastRowTop = currentY;

		while (remaining > 0) {
			int countThisRow = Math.min(columns, remaining);
			int[] rowXs = getCenteredColumnXs(countThisRow);
			lastRowTop = currentY;

			for (int i = 0; i < countThisRow; i++) {
				ClickableWidget widget = widgets.get(widgetIndex++);
				widget.setPosition(rowXs[i], currentY);
			}

			currentY += ROW_SPACING;
			remaining -= countThisRow;
		}

		return lastRowTop + BUTTON_HEIGHT;
	}

	private int[] getCenteredColumnXs(int columnCount) {
		if (columnCount <= 0) {
			return new int[0];
		}
		int groupWidth = columnCount * BUTTON_WIDTH + (columnCount - 1) * COLUMN_SPACING;
		int startX = Math.max((width - groupWidth) / 2, 0); // 画面幅より広い場合でも左端からはみ出さないようにする
		int[] xs = new int[columnCount];
		for (int i = 0; i < columnCount; i++) {
			xs[i] = startX + i * (BUTTON_WIDTH + COLUMN_SPACING);
		}
		return xs;
	}

	private int getCenterButtonX() {
		return width / 2 - BUTTON_WIDTH / 2;
	}

	private int getBottomButtonY() {
		return height - 27;
	}

	private class TabToggleListWidget extends ElementListWidget<TabToggleListWidget.Row> {
		TabToggleListWidget(MinecraftClient client, int width, int height, int top, int itemHeight) {
			super(client, width, height, top, itemHeight);
			this.centerListVertically = false;
		}

		void populate(List<CyclingButtonWidget<Boolean>> widgets) {
			this.clearEntries();
			List<CyclingButtonWidget<Boolean>> buffer = new ArrayList<>(2);
			for (CyclingButtonWidget<Boolean> widget : widgets) {
				buffer.add(widget);
				if (buffer.size() == 2) {
					this.addEntry(new Row(new ArrayList<>(buffer)));
					buffer.clear();
				}
			}
			if (!buffer.isEmpty()) {
				this.addEntry(new Row(new ArrayList<>(buffer)));
			}
		}

		@Override
		public int getRowWidth() {
			return BUTTON_WIDTH * 2 + COLUMN_SPACING;
		}

		private class Row extends ElementListWidget.Entry<Row> {
			private final List<CyclingButtonWidget<Boolean>> widgets;

			private Row(List<CyclingButtonWidget<Boolean>> widgets) {
				this.widgets = widgets;
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float delta) {
				int baseX = getContentX();
				int contentHeight = getContentHeight();
				int verticalPadding = Math.max(0, (contentHeight - BUTTON_HEIGHT) / 2);
				int baseY = getContentY() + verticalPadding;
				for (int i = 0; i < widgets.size(); i++) {
					CyclingButtonWidget<Boolean> widget = widgets.get(i);
					int buttonX = baseX + i * (BUTTON_WIDTH + COLUMN_SPACING);
					widget.setPosition(buttonX, baseY);
					widget.render(context, mouseX, mouseY, delta);
				}
			}

			@Override
			public List<? extends Element> children() {
				return widgets;
			}

			@Override
			public List<? extends Selectable> selectableChildren() {
				return widgets;
			}
		}
	}

	private static void setAllTabVisibility(boolean enabled) {
		BuildingSupportConfig config = BuildingSupportConfig.getInstance();
		for (BuildingSupportConfig.ItemGroupOption option : BuildingSupportConfig.ItemGroupOption.values()) {
			config.setItemGroupEnabled(option, enabled);
		}
		InventoryTabVisibilityController.reloadFromConfig();
	}

	// 数値フィールドの内容を検証しつつ固定時間を反映するユーティリティ
	private boolean applyFixedTimeValue(TextFieldWidget field) {
		String raw = field.getText().trim();
		if (raw.isEmpty()) {
			field.setEditableColor(0xFFFF5555);
			return false;
		}
		try {
			int value = Integer.parseInt(raw);
			BuildingSupportConfig.getInstance().setFixedTimeValue(value);
			field.setEditableColor(0xFFFFFFFF);
			field.setText(String.valueOf(BuildingSupportConfig.getInstance().getFixedTimeValue()));
			return true;
		} catch (NumberFormatException exception) {
			field.setEditableColor(0xFFFF5555);
			return false;
		}
	}

	private class LabelWidget extends ClickableWidget {
		private final TextRenderer renderer;
		private final int color = 0xFFFFFF;

		LabelWidget(int x, int y, Text text) {
			super(x, y,
				BuildingSupportConfigScreen.this.textRenderer.getWidth(text),
				BuildingSupportConfigScreen.this.textRenderer.fontHeight,
				text);
			this.renderer = BuildingSupportConfigScreen.this.textRenderer;
			this.active = false;
		}

		@Override
		protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			context.drawTextWithShadow(renderer, getMessage(), getX(), getY(), color);
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			builder.put(NarrationPart.TITLE, getMessage());
		}
	}
}

