package chihalu.building.support.storage;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import chihalu.building.support.BuildingSupport;

/**
 * 装飾や染色などの追加データを含んだ ItemStack を安全に保存・復元するためのスナップショット。
 */
public final class SavedStack {
	private static final RegistryWrapper.WrapperLookup WRAPPER_LOOKUP = BuiltinRegistries.createWrapperLookup();
	private static final RegistryOps<JsonElement> JSON_OPS = RegistryOps.of(JsonOps.INSTANCE, WRAPPER_LOOKUP);

	private final Identifier id;
	private final ItemStack stack;

	private SavedStack(Identifier id, ItemStack stack) {
		this.id = id;
		this.stack = stack.copy();
	}

	/**
	 * 与えられた ItemStack から保存用スナップショットを生成する。
	 */
	public static Optional<SavedStack> capture(ItemStack original) {
		if (original == null || original.isEmpty()) {
			return Optional.empty();
		}
		Item item = original.getItem();
		Identifier identifier = Registries.ITEM.getId(item);
		if (!Registries.ITEM.containsId(identifier)) {
			return Optional.empty();
		}
		return Optional.of(new SavedStack(identifier, original));
	}

	/**
	 * 既存の Identifier のみからデフォルト状態のスタックを生成する。
	 */
	public static Optional<SavedStack> fromId(Identifier id) {
		if (id == null || !Registries.ITEM.containsId(id)) {
			return Optional.empty();
		}
		return Optional.of(new SavedStack(id, new ItemStack(Registries.ITEM.get(id))));
	}

	/**
	 * JSON へ保存していたスナップショットから ItemStack を復元する。
	 */
	public static Optional<SavedStack> fromSerialized(Serialized form) {
		if (form == null || form.id == null || form.id.isBlank()) {
			return Optional.empty();
		}
		Identifier identifier = Identifier.tryParse(form.id.trim());
		if (identifier == null || !Registries.ITEM.containsId(identifier)) {
			return Optional.empty();
		}
		if (form.stack != null && !form.stack.isJsonNull()) {
			ItemStack decoded = decodeStack(form.stack);
			if (!decoded.isEmpty()) {
				return Optional.of(new SavedStack(identifier, decoded));
			}
		}
		return Optional.of(new SavedStack(identifier, new ItemStack(Registries.ITEM.get(identifier))));
	}

	/**
	 * Gson 経由でシリアライズ可能なフォームへ変換する。
	 */
	public Serialized toSerialized() {
		Serialized serialized = new Serialized();
		serialized.id = id.toString();
		serialized.stack = encodeStack(stack);
		return serialized;
	}

	/**
	 * UI などに表示するための ItemStack コピーを返す。
	 */
	public ItemStack toItemStack() {
		return stack.copy();
	}

	/**
	 * スナップショットが保持している Item の Identifier を返す。
	 */
	public Identifier id() {
		return id;
	}

	private static JsonElement encodeStack(ItemStack stack) {
		DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(JSON_OPS, stack.copy());
		return result.result().orElseGet(() -> {
			BuildingSupport.LOGGER.warn("ItemStack のシリアライズに失敗したため、ベースIDのみ保存します: {}", stack);
			return JsonNull.INSTANCE;
		});
	}

	private static ItemStack decodeStack(JsonElement element) {
		DataResult<ItemStack> result = ItemStack.CODEC.parse(JSON_OPS, element);
		return result.result().map(ItemStack::copy).orElseGet(() -> {
			BuildingSupport.LOGGER.warn("ItemStack の復元に失敗したため、空のスタックを返します: {}", element);
			return ItemStack.EMPTY;
		});
	}

	/**
	 * Gson により自動でマッピングされるシリアライズ済みデータ構造。
	 */
	public static final class Serialized {
		public String id;
		public JsonElement stack;
	}

}
