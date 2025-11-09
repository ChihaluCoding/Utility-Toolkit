package chihalu.building.support.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import chihalu.building.support.BuildingSupport;
import chihalu.building.support.BuildingSupportStorage;

public final class MemoManager {
	private static final MemoManager INSTANCE = new MemoManager();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path configPath = BuildingSupportStorage.resolve("memos.json");

	private final Map<String, MemoEntry> memos = new LinkedHashMap<>();

	private MemoManager() {
	}

	public static MemoManager getInstance() {
		return INSTANCE;
	}

	public synchronized void reload() {
		memos.clear();
		if (!Files.exists(configPath)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
			MemoFile file = GSON.fromJson(reader, MemoFile.class);
			if (file == null || file.memos == null) {
				return;
			}
			for (MemoEntry entry : file.memos) {
				if (entry == null || entry.command == null || entry.note == null) {
					continue;
				}
				String sanitizedCommand = sanitizeCommand(entry.command);
				String note = normalizeNote(entry.note);
				if (sanitizedCommand.isBlank() || note.isBlank()) {
					continue;
				}
				memos.put(noteKey(note), new MemoEntry(sanitizedCommand, note));
			}
		} catch (IOException | JsonSyntaxException exception) {
			BuildingSupport.LOGGER.error("メモファイルの読み込みに失敗しました: {}", configPath, exception);
		}
	}

	public synchronized void save() {
		try {
			Files.createDirectories(configPath.getParent());
			MemoFile file = new MemoFile(new ArrayList<>(memos.values()));
			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				GSON.toJson(file, writer);
			}
		} catch (IOException exception) {
			BuildingSupport.LOGGER.error("メモファイルの保存に失敗しました: {}", configPath, exception);
		}
	}

	public synchronized Text addMemo(String command, String note) {
		String sanitizedCommand = sanitizeCommand(command);
		String normalizedNote = normalizeNote(note);
		if (sanitizedCommand.isBlank()) {
			return Text.translatable("command.utility-toolkit.memo.invalid_command").formatted(Formatting.RED);
		}
		if (normalizedNote.isBlank()) {
			return Text.translatable("command.utility-toolkit.memo.invalid_note").formatted(Formatting.RED);
		}
		memos.put(noteKey(normalizedNote), new MemoEntry(sanitizedCommand, normalizedNote));
		save();
		return Text.translatable("command.utility-toolkit.memo.added", normalizedNote).formatted(Formatting.GREEN);
	}

	public synchronized Text removeMemo(String note) {
		String normalizedNote = normalizeNote(note);
		if (normalizedNote.isBlank()) {
			return Text.translatable("command.utility-toolkit.memo.invalid_note").formatted(Formatting.RED);
		}
		MemoEntry removed = memos.remove(noteKey(normalizedNote));
		if (removed == null) {
			return Text.translatable("command.utility-toolkit.memo.not_found", normalizedNote).formatted(Formatting.RED);
		}
		save();
		return Text.translatable("command.utility-toolkit.memo.removed", normalizedNote).formatted(Formatting.YELLOW);
	}

	public synchronized List<MemoEntry> getAllMemos() {
		return Collections.unmodifiableList(new ArrayList<>(memos.values()));
	}

	public synchronized MemoEntry getMemo(String note) {
		String normalizedNote = normalizeNote(note);
		if (normalizedNote.isBlank()) {
			return null;
		}
		return memos.get(noteKey(normalizedNote));
	}

	public synchronized MemoEditResult editMemo(String note, String newCommand, String newNote) {
		String normalizedNote = normalizeNote(note);
		if (normalizedNote.isBlank()) {
			return MemoEditResult.failure(Text.translatable("command.utility-toolkit.memo.invalid_note").formatted(Formatting.RED));
		}
		MemoEntry existing = memos.get(noteKey(normalizedNote));
		if (existing == null) {
			return MemoEditResult.failure(Text.translatable("command.utility-toolkit.memo.not_found", normalizedNote).formatted(Formatting.RED));
		}
		String commandToSave = newCommand == null || newCommand.isBlank() ? existing.getCommand() : sanitizeCommand(newCommand);
		String noteToSave = newNote == null || newNote.isBlank() ? existing.getNote() : normalizeNote(newNote);
		if (commandToSave.isBlank()) {
			return MemoEditResult.failure(Text.translatable("command.utility-toolkit.memo.invalid_command").formatted(Formatting.RED));
		}
		if (noteToSave.isBlank()) {
			return MemoEditResult.failure(Text.translatable("command.utility-toolkit.memo.invalid_note").formatted(Formatting.RED));
		}
		if (!noteKey(existing.getNote()).equals(noteKey(noteToSave))) {
			memos.remove(noteKey(existing.getNote()));
		}
		MemoEntry updated = new MemoEntry(commandToSave, noteToSave);
		memos.put(noteKey(noteToSave), updated);
		save();
		return MemoEditResult.success(Text.translatable("command.utility-toolkit.memo.edited", noteToSave).formatted(Formatting.GREEN), existing, updated);
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

	private static String normalizeNote(String note) {
		return note == null ? "" : note.trim();
	}

	private static String noteKey(String note) {
		return note.toLowerCase(Locale.ROOT);
	}

	private static final class MemoFile {
		private List<MemoEntry> memos;

		private MemoFile(List<MemoEntry> memos) {
			this.memos = memos;
		}
	}

	public static final class MemoEntry {
		private final String command;
		private final String note;

		public MemoEntry(String command, String note) {
			this.command = command;
			this.note = note;
		}

		public String getCommand() {
			return command;
		}

		public String getNote() {
			return note;
		}
	}

	public static final class MemoEditResult {
		private final Text feedback;
		private final MemoEntry before;
		private final MemoEntry after;

		private MemoEditResult(Text feedback, MemoEntry before, MemoEntry after) {
			this.feedback = feedback;
			this.before = before;
			this.after = after;
		}

		public static MemoEditResult success(Text feedback, MemoEntry before, MemoEntry after) {
			return new MemoEditResult(feedback, before, after);
		}

		public static MemoEditResult failure(Text feedback) {
			return new MemoEditResult(feedback, null, null);
		}

		public Text feedback() {
			return feedback;
		}

		public MemoEntry before() {
			return before;
		}

		public MemoEntry after() {
			return after;
		}
	}
}
