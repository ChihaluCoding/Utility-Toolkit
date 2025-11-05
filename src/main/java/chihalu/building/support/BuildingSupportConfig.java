package chihalu.building.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mod全体の設定を管理するクラス。
 */
public final class BuildingSupportConfig {
	private static final BuildingSupportConfig INSTANCE = new BuildingSupportConfig();

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path configPath = FabricLoader.getInstance()
		.getConfigDir()
		.resolve(BuildingSupport.MOD_ID + "-config.json");

	private boolean preventIceMelting = false;

	private BuildingSupportConfig() {
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
			}
		} catch (IOException | JsonSyntaxException exception) {
			getLogger().error("設定ファイルの読み込みに失敗しました: {}", configPath, exception);
		}
	}

	public synchronized void save() {
		try {
			Files.createDirectories(configPath.getParent());
			SerializableData data = new SerializableData(preventIceMelting);
			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
			}
		} catch (IOException exception) {
			getLogger().error("設定ファイルの保存に失敗しました: {}", configPath, exception);
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

	private Logger getLogger() {
		return BuildingSupport.LOGGER;
	}

	private static final class SerializableData {
		private boolean preventIceMelting;

		private SerializableData(boolean preventIceMelting) {
			this.preventIceMelting = preventIceMelting;
		}
	}
}
