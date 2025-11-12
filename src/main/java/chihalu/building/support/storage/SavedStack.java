package chihalu.building.support.storage;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
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
	private static volatile RegistryWrapper.WrapperLookup CURRENT_LOOKUP = BuiltinRegistries.createWrapperLookup();
	private static volatile RegistryOps<NbtElement> NBT_OPS = RegistryOps.of(NbtOps.INSTANCE, CURRENT_LOOKUP);
	private static volatile RegistryOps<JsonElement> JSON_OPS = RegistryOps.of(JsonOps.INSTANCE, CURRENT_LOOKUP);

	private final Identifier id;
	private final ItemStack stack;
	private final String uniqueKey;

	private SavedStack(Identifier id, ItemStack stack) {
		this.id = id;
		this.stack = stack.copy();
		this.uniqueKey = buildUniqueKey(this.id, this.stack);
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
		if (form.nbt != null && !form.nbt.isBlank()) {
			ItemStack decoded = decodeStack(form.nbt);
			if (!decoded.isEmpty()) {
				return Optional.of(new SavedStack(identifier, decoded));
			}
		}
		if (form.stack != null && !form.stack.isJsonNull()) {
			ItemStack decoded = decodeStack(form.stack);
			if (!decoded.isEmpty()) {
				return Optional.of(new SavedStack(identifier, decoded));
			}
		}
		return Optional.of(new SavedStack(identifier, new ItemStack(Registries.ITEM.get(identifier))));
	}

	private static String buildUniqueKey(Identifier id, ItemStack stack) {
		String nbtString = encodeStackNbt(stack);
		if (nbtString != null && !nbtString.isBlank()) {
			return id + "#" + nbtString;
		}
		JsonElement json = encodeStack(stack);
		String jsonString = json.isJsonNull() ? "" : json.toString();
		if (!jsonString.isBlank()) {
			return id + "#" + jsonString;
		}
		return id.toString();
	}


	/**
	 * Gson 経由でシリアライズ可能なフォームへ変換する。
	 */
	public Serialized toSerialized() {
		Serialized serialized = new Serialized();
		serialized.id = id.toString();
		// すべての保存データをJSONとSNBTの両方で維持し、どの環境でも装飾情報を安全に再現する
		serialized.stack = encodeStack(stack);
		serialized.nbt = encodeStackNbt(stack);
		return serialized;
	}

	/**
	 * UI などに表示するための ItemStack コピーを返す。
	 */
	public ItemStack toItemStack() {
		return stack.copy();
	}

	/**
	 * ���ݒ��Ƀ}�l�[�W���̈ꗗ���A�t�H�g�o�[�g���𐧌䂷�邽�߂ɒ������l�B
	 */
	public String uniqueKey() {
		return uniqueKey;
	}

	/**
	 * スナップショットが保持している Item の Identifier を返す。
	 */
	public Identifier id() {
		return id;
	}

	public static synchronized void updateLookup(RegistryWrapper.WrapperLookup lookup) {
		if (lookup == null) {
			return;
		}
		CURRENT_LOOKUP = lookup;
		NBT_OPS = RegistryOps.of(NbtOps.INSTANCE, CURRENT_LOOKUP);
		JSON_OPS = RegistryOps.of(JsonOps.INSTANCE, CURRENT_LOOKUP);
		BuildingSupport.LOGGER.debug("SavedStack registry lookup updated: {}", lookup);
	}

	public static synchronized void resetLookup() {
		updateLookup(BuiltinRegistries.createWrapperLookup());
	}

	private static JsonElement encodeStack(ItemStack stack) {
		DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(JSON_OPS, stack.copy());
		if (result.result().isEmpty()) {
			String error = result.error().map(partial -> partial.message()).orElse("unknown");
			BuildingSupport.LOGGER.warn("ItemStack JSON のエンコードに失敗しました: {} / {}", stack, error);
		}
		return result.result().orElse(JsonNull.INSTANCE);
	}

	private static String encodeStackNbt(ItemStack stack) {
		DataResult<NbtElement> result = ItemStack.CODEC.encodeStart(NBT_OPS, stack.copy());
		if (result.result().isEmpty()) {
			String error = result.error().map(partial -> partial.message()).orElse("unknown");
			BuildingSupport.LOGGER.warn("Failed to encode ItemStack NBT for {}: {}", stack, error);
		}
		return result.result().map(NbtElement::toString).orElse("");
	}

	private static ItemStack decodeStack(JsonElement element) {
		DataResult<ItemStack> result = ItemStack.CODEC.parse(JSON_OPS, element);
		if (result.result().isEmpty()) {
			String error = result.error().map(partial -> partial.message()).orElse("unknown");
			BuildingSupport.LOGGER.warn("Failed to decode ItemStack from JSON: {}", error);
		}
		return result.result().map(ItemStack::copy).orElse(ItemStack.EMPTY);
	}

	private static ItemStack decodeStack(String nbtString) {
		if (nbtString == null || nbtString.isBlank()) {
			return ItemStack.EMPTY;
		}
		try {
			NbtElement element = StringNbtReader.fromOps(NbtOps.INSTANCE).read(nbtString);
			DataResult<ItemStack> result = ItemStack.CODEC.parse(NBT_OPS, element);
			return result.result().map(ItemStack::copy).orElse(ItemStack.EMPTY);
		} catch (Exception exception) {
			BuildingSupport.LOGGER.warn("ItemStack の復元処理で例外が発生したため、空のスタックを返します: {}", nbtString, exception);
			return ItemStack.EMPTY;
		}
	}

	/**
	 * Gson により自動でマッピングされるシリアライズ済みデータ構造。
	 */
	public static final class Serialized {
		public String id;
		public JsonElement stack;
		public String nbt;
	}

}
