package chihalu.building.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CommandPresetManager {
	private static final CommandPresetManager INSTANCE = new CommandPresetManager();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path configPath = FabricLoader.getInstance()
		.getConfigDir()
		.resolve(BuildingSupport.MOD_ID + "-commands.json");

	private final Map<Integer, PresetEntry> presets = new LinkedHashMap<>();

	private CommandPresetManager() {
	}

	public static CommandPresetManager getInstance() {
		return INSTANCE;
	}

	public synchronized void reload() {
		presets.clear();

		if (!Files.exists(configPath)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
			PresetFile file = GSON.fromJson(reader, PresetFile.class);

			if (file == null || file.presets == null) {
				return;
			}

			for (PresetEntry entry : file.presets) {
				if (entry == null || entry.command == null || entry.command.isBlank()) {
					continue;
				}

				int slot = entry.slot;
				if (slot <= 0) {
					continue;
				}

				presets.put(slot, entry.normalized());
			}
		} catch (IOException | JsonSyntaxException exception) {
			BuildingSupport.LOGGER.error("コマンドプリセットの読み込みに失敗しました: {}", configPath, exception);
		}
	}

	public synchronized void save() {
		try {
			Files.createDirectories(configPath.getParent());
			PresetFile file = new PresetFile(new ArrayList<>(presets.values()));
			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				GSON.toJson(file, writer);
			}
		} catch (IOException exception) {
			BuildingSupport.LOGGER.error("コマンドプリセットの保存に失敗しました: {}", configPath, exception);
		}
	}

	public synchronized Text addPreset(int slot, String command, String description) {
		if (slot <= 0) {
			return Text.translatable("command.building-support.preset.invalid_slot");
		}

		String sanitizedCommand = sanitizeCommand(command);
		if (sanitizedCommand.isBlank()) {
			return Text.translatable("command.building-support.preset.invalid_command");
		}

		PresetEntry entry = new PresetEntry(slot, sanitizedCommand, normalizeDescription(description));
		presets.put(slot, entry);
		save();
		return Text.translatable("command.building-support.preset.added", slot, entry.preview());
	}

	public synchronized Text removePreset(int slot) {
		PresetEntry removed = presets.remove(slot);
		if (removed == null) {
			return Text.translatable("command.building-support.preset.not_found", slot);
		}

		save();
		return Text.translatable("command.building-support.preset.removed", slot);
	}

	public synchronized PresetEntry getPreset(int slot) {
		return presets.get(slot);
	}

	public synchronized List<PresetEntry> getAllPresets() {
		List<PresetEntry> list = new ArrayList<>(presets.values());
		list.sort(Comparator.comparingInt(PresetEntry::getSlot));
		return Collections.unmodifiableList(list);
	}

	private String normalizeDescription(String description) {
		if (description == null) {
			return "";
		}
		return description.trim();
	}

	private static String sanitizeCommand(String command) {
		if (command == null) {
			return "";
		}

		String sanitized = command.trim();
		if (sanitized.startsWith("/")) {
			sanitized = sanitized.substring(1).trim();
		}
		return sanitized;
	}

	private static final class PresetFile {
		private List<PresetEntry> presets;

		private PresetFile(List<PresetEntry> presets) {
			this.presets = presets;
		}
	}

	public static final class PresetEntry {
		private int slot;
		private String command;
		private String description;

		public PresetEntry(int slot, String command, String description) {
			this.slot = slot;
			this.command = command;
			this.description = description;
		}

		public int getSlot() {
			return slot;
		}

		public String getCommand() {
			return command;
		}

		public String getDescription() {
			return description;
		}

		private PresetEntry normalized() {
			return new PresetEntry(slot, CommandPresetManager.sanitizeCommand(command), description == null ? "" : description.trim());
		}

		private String preview() {
			return description == null || description.isBlank() ? command : description;
		}
	}
}
