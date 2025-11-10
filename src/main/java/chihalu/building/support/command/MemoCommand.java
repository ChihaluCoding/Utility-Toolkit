package chihalu.building.support.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent.CopyToClipboard;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class MemoCommand {
	private static final String NO_CONTENT_TRANSLATION_KEY = "command.utility-toolkit.memo.no_content_label";
	private static final Map<UUID, String> EDIT_SESSIONS = new ConcurrentHashMap<>();
	private static boolean cleanupRegistered = false;

	private MemoCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, MemoManager memoManager) {
		ensureSessionCleanupHook();
		dispatcher.register(CommandManager.literal("memo")
			.requires(source -> source.hasPermissionLevel(0))
			.then(CommandManager.literal("add")
				.then(CommandManager.argument("command", StringArgumentType.greedyString())
					.suggests((context, builder) -> suggestCommands(dispatcher, context, builder))
					.executes(context -> addMemo(context, memoManager))))
			.then(CommandManager.literal("remove")
				.then(CommandManager.argument("note", StringArgumentType.greedyString())
					.suggests((context, builder) -> suggestNotes(memoManager, builder))
					.executes(context -> removeMemo(context, memoManager))))
			.then(CommandManager.literal("edit")
				.then(CommandManager.argument("payload", StringArgumentType.greedyString())
					.suggests((context, builder) -> suggestEditPayload(dispatcher, memoManager, context, builder))
					.executes(context -> editMemo(context, memoManager))))
			.then(CommandManager.literal("list")
				.executes(context -> listMemos(context.getSource(), memoManager, false))
				.then(CommandManager.literal("cmd")
					.executes(context -> listMemos(context.getSource(), memoManager, true))))
			.then(CommandManager.literal("style")
				.then(CommandManager.argument("value", IntegerArgumentType.integer(1, 3))
					.executes(context -> setStyle(
						context.getSource(),
						memoManager,
						IntegerArgumentType.getInteger(context, "value")
					)))));
	}

	/**
	 * プレイヤー切断時に編集セッションを確実に破棄してリークを防ぐ。
	 */
	private static void ensureSessionCleanupHook() {
		if (cleanupRegistered) {
			return;
		}
		cleanupRegistered = true;
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler.player != null) {
				EDIT_SESSIONS.remove(handler.player.getUuid());
			}
		});
	}

	private static int addMemo(CommandContext<ServerCommandSource> context, MemoManager manager) {
		String raw = StringArgumentType.getString(context, "command");
		String trimmed = raw.trim();
		if (trimmed.startsWith("\"")) {
			NoteMemoInput noteMemo = NoteMemoInput.parse(raw);
			if (noteMemo == null) {
				return sendFeedback(
					context.getSource(),
					Text.translatable("command.utility-toolkit.memo.add.invalid_format").formatted(Formatting.RED)
				);
			}
			Text result = manager.addMemo("", noteMemo.note(), noteMemo.body());
			return sendFeedback(context.getSource(), result);
		}

		ParsedInput input = ParsedInput.parse(raw);
		if (!input.hasNote()) {
			return sendFeedback(context.getSource(), Text.translatable("command.utility-toolkit.memo.invalid_note").formatted(Formatting.RED));
		}
		Text result = manager.addMemo(input.command(), input.note(), "");
		return sendFeedback(context.getSource(), result);
	}

	private static int removeMemo(CommandContext<ServerCommandSource> context, MemoManager manager) {
		String note = StringArgumentType.getString(context, "note");
		Text result = manager.removeMemo(note);
		return sendFeedback(context.getSource(), result);
	}

	private static int editMemo(CommandContext<ServerCommandSource> context, MemoManager manager) {
		String raw = StringArgumentType.getString(context, "payload");
		ParsedInput input = ParsedInput.parse(raw);
		if (!input.hasNote()) {
			return sendFeedback(context.getSource(), Text.translatable("command.utility-toolkit.memo.invalid_note").formatted(Formatting.RED));
		}
		if (input.command().isBlank()) {
			return beginEdit(context.getSource(), manager, input.note());
		}
		return applyEdit(context.getSource(), manager, input.note(), input.command());
	}

	private static int beginEdit(ServerCommandSource source, MemoManager manager, String note) {
		MemoManager.MemoEntry entry = manager.getMemo(note);
		if (entry == null) {
			return sendFeedback(source, Text.translatable("command.utility-toolkit.memo.not_found", note).formatted(Formatting.RED));
		}
		ServerPlayerEntity player = getPlayer(source);
		if (player == null) {
			return sendFeedback(source, Text.translatable("command.utility-toolkit.memo.edit.no_session").formatted(Formatting.RED));
		}
		EDIT_SESSIONS.put(player.getUuid(), entry.getNote());
		sendEditPrompt(source, entry);
		return 1;
	}

	private static int applyEdit(ServerCommandSource source, MemoManager manager, String note, String command) {
		ServerPlayerEntity player = getPlayer(source);
		if (player == null) {
			return sendFeedback(source, Text.translatable("command.utility-toolkit.memo.edit.no_session").formatted(Formatting.RED));
		}
		String sessionNote = EDIT_SESSIONS.get(player.getUuid());
		if (sessionNote == null) {
			return sendFeedback(source, Text.translatable("command.utility-toolkit.memo.edit.no_session").formatted(Formatting.RED));
		}
		String newNote = note;
		MemoManager.MemoEditResult result = manager.editMemo(sessionNote, command, newNote);
		sendFeedback(source, result.feedback());
		if (result.before() != null && result.after() != null) {
			Text before = Text.translatable(
				"command.utility-toolkit.memo.edit.before",
				describeMemoContent(result.before()),
				result.before().getNote()
			).formatted(Formatting.BLUE);
			Text after = Text.translatable(
				"command.utility-toolkit.memo.edit.after",
				describeMemoContent(result.after()),
				result.after().getNote()
			).formatted(Formatting.AQUA);
			source.sendFeedback(() -> before, false);
			source.sendFeedback(() -> after, false);
			EDIT_SESSIONS.remove(player.getUuid());
		} else {
			// 失敗時はセッションを保持し、ユーザーに再入力を促す
		}
		return 1;
	}

	private static int listMemos(ServerCommandSource source, MemoManager manager, boolean commandOnly) {
		List<MemoManager.MemoEntry> filtered = new ArrayList<>();
		for (MemoManager.MemoEntry entry : manager.getAllMemos()) {
			boolean isCommandMemo = !entry.getCommand().isBlank();
			if (commandOnly == isCommandMemo) {
				filtered.add(entry);
			}
		}
		int style = manager.getListStyle();
		String headerKey = commandOnly
			? "command.utility-toolkit.memo.list.header.command"
			: "command.utility-toolkit.memo.list.header";
		source.sendFeedback(() -> Text.empty(), false);
		source.sendFeedback(() -> Text.translatable(headerKey).formatted(Formatting.AQUA, Formatting.BOLD), false);
		if (filtered.isEmpty()) {
			String emptyKey = commandOnly
				? "command.utility-toolkit.memo.list.empty.command"
				: "command.utility-toolkit.memo.list.empty";
			source.sendFeedback(() -> Text.translatable(emptyKey).formatted(Formatting.GRAY), false);
			source.sendFeedback(() -> Text.empty(), false);
			return 0;
		}

		for (MemoManager.MemoEntry entry : filtered) {
			for (Text line : formatEntryLines(entry, style)) {
				source.sendFeedback(() -> line, false);
			}
			source.sendFeedback(() -> Text.empty(), false);
		}
		return filtered.size();
	}

	private static List<Text> formatEntryLines(MemoManager.MemoEntry entry, int style) {
		List<Text> lines = new ArrayList<>();
		boolean isCommandMemo = !entry.getCommand().isBlank();
		if (isCommandMemo) {
			MutableText content = createMemoContentDisplay(entry);
			switch (style) {
				case 2 -> lines.add(
					Text.literal("　").formatted(Formatting.GRAY)
						.append(Text.literal(entry.getNote()).formatted(Formatting.YELLOW))
						.append(Text.literal("：").formatted(Formatting.GRAY))
						.append(content)
				);
				case 3 -> {
					lines.add(Text.literal("　").formatted(Formatting.GRAY)
						.append(Text.literal(entry.getNote()).formatted(Formatting.YELLOW)));
					lines.add(Text.literal("　　").formatted(Formatting.GRAY).append(content));
				}
				default -> lines.add(
					Text.literal(" ・ ").formatted(Formatting.GRAY)
						.append(Text.literal(entry.getNote()).formatted(Formatting.YELLOW))
						.append(Text.literal("：").formatted(Formatting.GRAY))
						.append(content)
				);
			}
			return lines;
		}

		String[] detailLines = entry.getDetails().split("\\n", -1);
		if (detailLines.length == 0) {
			detailLines = new String[]{""};
		}
		boolean hasContent = Arrays.stream(detailLines).anyMatch(line -> !line.isEmpty());

		switch (style) {
			case 2 -> {
				boolean separate = detailLines.length > 1;
				MutableText header = Text.literal("　").formatted(Formatting.GRAY)
					.append(Text.literal("「" + entry.getNote() + "」").formatted(Formatting.YELLOW))
					.append(Text.literal("：").formatted(Formatting.GRAY));
				if (!separate) {
					header.append(formatDetailText(detailLines[0], !hasContent));
					lines.add(header);
				} else {
					lines.add(header);
					lines.add(Text.literal("　　").formatted(Formatting.GRAY)
						.append(formatDetailText(detailLines[0], !hasContent)));
					for (int i = 1; i < detailLines.length; i++) {
						lines.add(Text.literal("　　").formatted(Formatting.GRAY)
							.append(formatDetailText(detailLines[i], false)));
					}
				}
			}
			case 3 -> {
				lines.add(Text.literal("　「").formatted(Formatting.GRAY)
					.append(Text.literal(entry.getNote()).formatted(Formatting.YELLOW))
					.append(Text.literal("」").formatted(Formatting.GRAY)));
				for (int i = 0; i < detailLines.length; i++) {
					lines.add(Text.literal("　　").formatted(Formatting.GRAY)
						.append(formatDetailText(detailLines[i], !hasContent && i == 0)));
				}
			}
			default -> {
				boolean separate = detailLines.length > 1;
				MutableText header = Text.literal(" ・").formatted(Formatting.GRAY)
					.append(Text.literal(entry.getNote()).formatted(Formatting.YELLOW))
					.append(Text.literal("：").formatted(Formatting.GRAY));
				if (!separate) {
					header.append(formatDetailText(detailLines[0], !hasContent));
					lines.add(header);
				} else {
					lines.add(header);
					lines.add(Text.literal("　　").formatted(Formatting.GRAY)
						.append(formatDetailText(detailLines[0], !hasContent)));
					for (int i = 1; i < detailLines.length; i++) {
						lines.add(Text.literal("　　").formatted(Formatting.GRAY)
							.append(formatDetailText(detailLines[i], false)));
					}
				}
			}
		}
		return lines;
	}

	private static MutableText formatDetailText(String detail, boolean allowPlaceholder) {
		if (detail == null || detail.isEmpty()) {
			if (allowPlaceholder) {
				return Text.translatable(NO_CONTENT_TRANSLATION_KEY).formatted(Formatting.DARK_GRAY);
			}
			return Text.literal("");
		}
		return Text.literal(detail).formatted(Formatting.WHITE);
	}

	private static int setStyle(ServerCommandSource source, MemoManager manager, int style) {
		Text feedback = manager.setListStyle(style);
		return sendFeedback(source, feedback);
	}

	private static int sendFeedback(ServerCommandSource source, Text message) {
		source.sendFeedback(() -> message, false);
		return 1;
	}

	private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestNotes(MemoManager manager, SuggestionsBuilder builder) {
		for (MemoManager.MemoEntry entry : manager.getAllMemos()) {
			builder.suggest(entry.getNote(), createMemoContentDisplay(entry));
		}
		return builder.buildFuture();
	}

	private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestEditPayload(
		CommandDispatcher<ServerCommandSource> dispatcher,
		MemoManager manager,
		CommandContext<ServerCommandSource> context,
		SuggestionsBuilder builder
	) {
		// 編集セッション前はメモ名だけ提案し、セッション開始後にコマンド補完へ切り替える
		ServerPlayerEntity player = getPlayer(context.getSource());
		boolean hasSession = player != null && EDIT_SESSIONS.containsKey(player.getUuid());
		if (!hasSession) {
			return suggestEditTargets(manager, builder);
		}
		return suggestCommands(dispatcher, context, builder);
	}

	private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestCommands(
		CommandDispatcher<ServerCommandSource> dispatcher,
		CommandContext<ServerCommandSource> context,
		SuggestionsBuilder builder
	) {
		String text = builder.getRemaining();
		int quoteIndex = text.indexOf('"');
		String commandInput = quoteIndex == -1 ? text : text.substring(0, quoteIndex).trim();
		boolean hasSlash = commandInput.startsWith("/");
		String parseTarget = hasSlash ? commandInput.substring(1) : commandInput;
		var parseResults = dispatcher.parse(parseTarget, context.getSource());
		return dispatcher.getCompletionSuggestions(parseResults).thenCompose(suggestions -> {
			for (Suggestion suggestion : suggestions.getList()) {
				String applied = suggestion.apply(parseTarget);
				if (hasSlash) {
					applied = "/" + applied;
				}
				builder.suggest(applied);
			}
			return builder.buildFuture();
		});
	}

	private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestEditTargets(
		MemoManager manager,
		SuggestionsBuilder builder
	) {
		// メモ名を引用符付きで提示し、部分一致で絞り込み
		String remaining = builder.getRemaining().replace("\"", "").trim().toLowerCase(Locale.ROOT);
		for (MemoManager.MemoEntry entry : manager.getAllMemos()) {
			String note = entry.getNote();
			if (remaining.isEmpty() || note.toLowerCase(Locale.ROOT).startsWith(remaining)) {
				builder.suggest("\"" + note + "\"", createMemoContentDisplay(entry));
			}
		}
		return builder.buildFuture();
	}

	private static void sendEditPrompt(ServerCommandSource source, MemoManager.MemoEntry entry) {
		MutableText contentText = createMemoContentDisplay(entry);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.memo.edit.begin", entry.getNote()).formatted(Formatting.GOLD), false);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.memo.edit.current_command").append(contentText).formatted(Formatting.WHITE), false);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.memo.edit.current_note", entry.getNote()).formatted(Formatting.WHITE), false);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.memo.edit.help", entry.getNote()).formatted(Formatting.YELLOW), false);
	}

	private static ServerPlayerEntity getPlayer(ServerCommandSource source) {
		return source.getEntity() instanceof ServerPlayerEntity player ? player : null;
	}

	/**
	 * メモの内容表示用に共通のテキストを生成する。
	 */
	private static MutableText createMemoContentDisplay(MemoManager.MemoEntry entry) {
		if (entry.getCommand().isBlank()) {
			if (entry.getDetails().isBlank()) {
				return Text.translatable(NO_CONTENT_TRANSLATION_KEY).formatted(Formatting.DARK_GRAY);
			}
			// 通常メモの内容は白色で強調して視認性を確保する
			return Text.literal(entry.getDetails()).formatted(Formatting.WHITE);
		}
		String fullCommand = "/" + entry.getCommand();
		return Text.literal(fullCommand)
			.styled(style -> style
				.withColor(Formatting.LIGHT_PURPLE)
				.withClickEvent(new CopyToClipboard(fullCommand))
				.withHoverEvent(new ShowText(Text.translatable("command.utility-toolkit.memo.list.copy_hint"))));
	}

	/**
	 * 翻訳メッセージにコマンド / 通常メモの内容を埋め込む際の表記を返す。
	 */
	private static String describeMemoContent(MemoManager.MemoEntry entry) {
		if (entry.getCommand().isBlank()) {
			if (entry.getDetails().isBlank()) {
				return Text.translatable(NO_CONTENT_TRANSLATION_KEY).getString();
			}
			return entry.getDetails();
		}
		return "/" + entry.getCommand();
	}

	private record ParsedInput(String command, String note) {
		static ParsedInput parse(String raw) {
			if (raw == null) {
				return new ParsedInput("", "");
			}
			String trimmed = raw.trim();
			String command = trimmed;
			String note = "";
			int firstQuote = trimmed.indexOf('"');
			int lastQuote = trimmed.lastIndexOf('"');
			if (firstQuote != -1 && lastQuote > firstQuote) {
				note = trimmed.substring(firstQuote + 1, lastQuote);
				command = trimmed.substring(0, firstQuote).trim();
			}
			return new ParsedInput(command, note);
		}

		boolean hasNote() {
			return note != null && !note.isBlank();
		}
	}
	private record NoteMemoInput(String note, String body) {
		static NoteMemoInput parse(String raw) {
			if (raw == null) {
				return null;
			}
			String trimmed = raw.trim();
			if (trimmed.isEmpty() || trimmed.charAt(0) != '"') {
				return null;
			}
			StringBuilder current = new StringBuilder();
			boolean inQuote = false;
			boolean escaping = false;
			List<String> parts = new ArrayList<>(2);
			for (int i = 0; i < trimmed.length(); i++) {
				char c = trimmed.charAt(i);
				if (escaping) {
					current.append(c);
					escaping = false;
					continue;
				}
				if (c == '\\' && inQuote) {
					escaping = true;
					continue;
				}
				if (c == '"') {
					inQuote = !inQuote;
					if (!inQuote) {
						parts.add(current.toString());
						current.setLength(0);
						if (parts.size() == 2) {
							for (int j = i + 1; j < trimmed.length(); j++) {
								if (!Character.isWhitespace(trimmed.charAt(j))) {
									return null;
								}
							}
							break;
						}
					}
					continue;
				}
				if (inQuote) {
					current.append(c);
				} else if (!Character.isWhitespace(c)) {
					return null;
				}
			}
			if (inQuote || parts.size() != 2) {
				return null;
			}
			return new NoteMemoInput(parts.get(0), parts.get(1));
		}
	}
}
