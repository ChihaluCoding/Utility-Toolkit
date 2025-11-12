package chihalu.building.support.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * 装飾済みの防具を立体表示するプレビュー画面。
 */
public class DecoratedArmorPreviewScreen extends Screen {
	private final Screen parent;
	private final ItemStack previewStack;
	private ArmorStandEntity mannequin;
	private float rotationOffset = 180.0f;
	private boolean dragging = false;
	private double lastDragX;

	public DecoratedArmorPreviewScreen(Screen parent, ItemStack stack) {
		super(Text.translatable("screen.utility-toolkit.preview.title"));
		this.parent = parent;
		this.previewStack = stack.copy();
	}

	@Override
	protected void init() {
		super.init();
		createPreviewEntity(); // プレイヤーから切り離した試着用マネキンを生成する
		int buttonWidth = 120;
		int buttonHeight = 20;
		addDrawableChild(ButtonWidget.builder(
			Text.translatable("screen.utility-toolkit.preview.close"),
			button -> close())
			.dimensions(width / 2 - buttonWidth / 2, height - 40, buttonWidth, buttonHeight)
			.build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fillGradient(0, 0, width, height, 0xD0000000, 0xD0000000); // ブラーを多重適用しないよう半透明グラデで暗転
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(textRenderer, previewStack.getName(), width / 2, 20, 0xFFFFFF); // 装備名をヘッダーに描画
		context.drawCenteredTextWithShadow(
			textRenderer,
			Text.translatable("screen.utility-toolkit.preview.hint"),
			width / 2,
			40,
			0xAAAAAA);

		if (mannequin == null && client.world != null) {
			createPreviewEntity();
		}

		if (mannequin != null) {
			renderMannequin(context, mannequin);
		} else {
			context.drawItem(previewStack, width / 2 - 8, height / 2 - 8);
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			dragging = true; // 左クリックでドラッグ回転モードに移行
			lastDragX = click.x();
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && dragging) {
			double diff = click.x() - lastDragX;
			lastDragX = click.x();
			rotationOffset -= (float) diff * 0.8f; // ドラッグ方向と回転方向を合わせるため符号を反転
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			dragging = false;
		}
		return super.mouseReleased(click);
	}

	@Override
	public void close() {
		MinecraftClient.getInstance().setScreen(parent);
	}

	// 実際のワールドとは独立した試着用マネキンを生成して装備を着せる
	private void createPreviewEntity() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			mannequin = null;
			return;
		}
		ArmorStandEntity entity = new ArmorStandEntity(client.world, 0.0, 0.0, 0.0);
		entity.setNoGravity(true);
		entity.setSilent(true);
		entity.setHideBasePlate(true);
		entity.setShowArms(true); // 盾を持たせるケースに備えて腕を表示
		equipPreviewStack(entity, previewStack);
		this.mannequin = entity;
	}

	// 対象アイテムの装備スロットに応じてマネキンへコピーを装着する
	private void equipPreviewStack(ArmorStandEntity entity, ItemStack stack) {
		EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
		EquipmentSlot slot = equippable != null ? equippable.slot() : EquipmentSlot.OFFHAND;
		entity.equipStack(slot, stack.copy());
	}

	private void renderMannequin(DrawContext context, ArmorStandEntity entity) {
		float prevBodyYaw = entity.bodyYaw;
		float prevYaw = entity.getYaw();
		float prevHeadYaw = entity.headYaw;
		float prevPitch = entity.getPitch();
		float prevLastHeadYaw = entity.lastHeadYaw;

		entity.bodyYaw = rotationOffset;
		entity.setYaw(rotationOffset);
		entity.setPitch(0.0F);
		entity.headYaw = rotationOffset;
		entity.lastHeadYaw = rotationOffset;

		int left = width / 2 - 70;
		int top = height / 2 - 110;
		int right = width / 2 + 70;
		int bottom = height / 2 + 70;
		float entityScale = entity.getScale();
		float renderScale = 60.0F / entityScale;
		Vector3f translation = new Vector3f(0.0F, entity.getHeight() / 2.0F + 0.0625F * entityScale, 0.0F);
		Quaternionf modelRotation = new Quaternionf().rotateZ((float)Math.PI);
		Quaternionf cameraRotation = new Quaternionf().rotateX(0.0F);
		modelRotation.mul(cameraRotation);

		InventoryScreen.drawEntity(context, left, top, right, bottom, renderScale, translation, modelRotation, null, entity);

		entity.bodyYaw = prevBodyYaw;
		entity.setYaw(prevYaw);
		entity.setPitch(prevPitch);
		entity.headYaw = prevHeadYaw;
		entity.lastHeadYaw = prevLastHeadYaw;
	}

	// 装備がトリムや染色など視覚的な差分を持っているか判定する
	public static boolean isDecoratedArmor(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
		EquipmentSlot equippableSlot = equippable != null ? equippable.slot() : null;
		boolean armorLike = equippableSlot != null && equippableSlot.isArmorSlot() || stack.isOf(Items.SHIELD);
		if (!armorLike) {
			return false;
		}
		boolean trimmed = stack.get(DataComponentTypes.TRIM) != null;
		boolean dyed = stack.get(DataComponentTypes.DYED_COLOR) != null;
		boolean patterned = stack.get(DataComponentTypes.BANNER_PATTERNS) != null;
		return trimmed || dyed || patterned;
	}
}
