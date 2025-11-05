package chihalu.building.support;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class BuildingSupportConfigScreen extends Screen {
	private final Screen parent;
	private CyclingButtonWidget<Boolean> preventIceMeltingButton;

	public BuildingSupportConfigScreen(Screen parent) {
		super(Text.translatable("config.building-support.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		var config = BuildingSupportConfig.getInstance();

		preventIceMeltingButton = CyclingButtonWidget.onOffBuilder(config.isPreventIceMeltingEnabled())
			.build(width / 2 - 100, height / 2 - 24, 200, 20,
				Text.translatable("config.building-support.prevent_ice_melting"),
				(button, value) -> BuildingSupportConfig.getInstance().setPreventIceMeltingEnabled(value));
		preventIceMeltingButton.setTooltip(Tooltip.of(Text.translatable("config.building-support.prevent_ice_melting.tooltip")));
		addDrawableChild(preventIceMeltingButton);

		addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
			.dimensions(width / 2 - 100, height / 2 + 2, 200, 20)
			.build());
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
		super.render(context, mouseX, mouseY, delta);
	}
}
