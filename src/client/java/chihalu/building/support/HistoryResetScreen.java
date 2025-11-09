package chihalu.building.support;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import chihalu.building.support.history.HistoryManager;

@Environment(EnvType.CLIENT)
public class HistoryResetScreen extends Screen {
	private static final int BUTTON_WIDTH = 160;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int COLUMN_SPACING = 12;

	private final Screen parent;
	private ResetTarget resetTarget;
	private CyclingButtonWidget<ResetTarget> targetSelector;
	private ButtonWidget resetButton;
	private Text worldStatusText;
	private Text resultMessage;

	public HistoryResetScreen(Screen parent) {
		super(Text.translatable("config.utility-toolkit.history_reset.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		resultMessage = null;
		resetTarget = BuildingSupportClient.hasActiveWorld() ? ResetTarget.CURRENT_WORLD : ResetTarget.ALL_WORLD;
		rebuildControls();
	}

	private void rebuildControls() {
		clearChildren();
		int infoY = height / 2 - 10;
		int leftX = getLeftColumnX();
		int rightX = getRightColumnX();

		targetSelector = CyclingButtonWidget.<ResetTarget>builder(target -> Text.translatable(target.translationKey()))
			.values(ResetTarget.values())
			.initially(resetTarget)
			.build(leftX, infoY, BUTTON_WIDTH, BUTTON_HEIGHT,
				Text.translatable("config.utility-toolkit.history_reset.target"),
				(button, value) -> {
					resetTarget = value;
					updateWorldStatusText();
					updateResetButtonState();
				});
		targetSelector.setFocused(false);
		addDrawableChild(targetSelector);

		updateWorldStatusText();

		resetButton = ButtonWidget.builder(Text.translatable("config.utility-toolkit.history_reset.reset_button"), button -> performReset())
			.dimensions(rightX, infoY, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build();
		addDrawableChild(resetButton);

		addDrawableChild(ButtonWidget.builder(Text.translatable("config.utility-toolkit.back_to_categories"), button -> returnToParent())
			.dimensions(getCenterButtonX(), getBottomButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		updateResetButtonState();
	}

	private void updateWorldStatusText() {
		if (resetTarget == ResetTarget.CURRENT_WORLD) {
			if (BuildingSupportClient.hasActiveWorld()) {
				worldStatusText = Text.translatable("config.utility-toolkit.history_reset.current_world",
					BuildingSupportClient.getCurrentWorldKey());
			} else {
				worldStatusText = Text.translatable("config.utility-toolkit.history_reset.not_in_world");
			}
		} else {
			worldStatusText = null;
		}
	}

	private void updateResetButtonState() {
		if (resetButton != null) {
			if (resetTarget == ResetTarget.ALL_WORLD) {
				resetButton.active = true;
			} else {
				resetButton.active = BuildingSupportClient.hasActiveWorld();
			}
		}
	}

	private void performReset() {
		boolean success;
		if (resetTarget == ResetTarget.CURRENT_WORLD) {
			if (!BuildingSupportClient.hasActiveWorld()) {
				resultMessage = Text.translatable("config.utility-toolkit.history_reset.result.not_in_world");
				return;
			}
			success = HistoryManager.getInstance().resetActiveWorldHistory();
			resultMessage = success
				? Text.translatable("config.utility-toolkit.history_reset.result.world_success")
				: Text.translatable("config.utility-toolkit.history_reset.result.failure");
		} else {
			success = HistoryManager.getInstance().resetGlobalHistory();
			resultMessage = success
				? Text.translatable("config.utility-toolkit.history_reset.result.global_success")
				: Text.translatable("config.utility-toolkit.history_reset.result.failure");
		}

		if (success) {
			BuildingSupportClient.onHistoryModeChanged();
		}
		updateWorldStatusText();
		updateResetButtonState();
	}

	private void returnToParent() {
		if (client != null) {
			client.setScreen(parent);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0xB0000000);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);

		if (worldStatusText != null) {
			context.drawCenteredTextWithShadow(textRenderer, worldStatusText, width / 2, height / 2 - ROW_SPACING * 2 - 10, 0xFFFFFF);
		}

		if (resultMessage != null) {
			context.drawCenteredTextWithShadow(textRenderer, resultMessage, width / 2, height - 30, 0xFFFFFF);
		}

		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void close() {
		returnToParent();
	}

	private int getCenterButtonX() {
		return width / 2 - BUTTON_WIDTH / 2;
	}

	private int getBottomButtonY() {
		return height - 27;
	}

	private int getLeftColumnX() {
		return width / 2 - BUTTON_WIDTH - COLUMN_SPACING / 2;
	}

	private int getRightColumnX() {
		return width / 2 + COLUMN_SPACING / 2;
	}

	private enum ResetTarget {
		CURRENT_WORLD("config.utility-toolkit.history_reset.target.current"),
		ALL_WORLD("config.utility-toolkit.history_reset.target.all");

		private final String translationKey;

		ResetTarget(String translationKey) {
			this.translationKey = translationKey;
		}

		public String translationKey() {
			return translationKey;
		}
	}
}
