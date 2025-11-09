package chihalu.building.support.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent.CopyToClipboard;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class MemoCommand {
	private static final Map<UUID, String> EDIT_SESSIONS = new ConcurrentHashMap<>();

	private MemoCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, MemoManager memoManager) {
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
				.executes(context -> listMemos(context.getSource(), memoManager))));
	}

	private static int addMemo(CommandContext<ServerCommandSource> context, MemoManager manager) {
		ParsedInput input = ParsedInput.parse(StringArgumentType.getString(context, "command"));
		if (!input.hasNote()) {
			return sendFeedback(context.getSource(), Text.translatable("command.utility-toolkit.memo.invalid_note").formatted(Formatting.RED));
		}
		Text result = manager.addMemo(input.command(), input.note());
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
			Text before = Text.translatable("command.utility-toolkit.memo.edit.before", result.before().getCommand(), result.before().getNote()).formatted(Formatting.BLUE);
			Text after = Text.translatable("command.utility-toolkit.memo.edit.after", result.after().getCommand(), result.after().getNote()).formatted(Formatting.AQUA);
			source.sendFeedback(() -> before, false);
			source.sendFeedback(() -> after, false);
			EDIT_SESSIONS.remove(player.getUuid());
		} else {
			// 失敗時はセッションを保持し、ユーザーに再入力を促す
		}
		return 1;
	}

	private static int listMemos(ServerCommandSource source, MemoManager manager) {
		var memos = manager.getAllMemos();
		if (memos.isEmpty()) {
			source.sendFeedback(() -> Text.translatable("command.utility-toolkit.memo.list.empty"), false);
			return 0;
		}
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.memo.list.header"), false);
		for (MemoManager.MemoEntry entry : memos) {
			String fullCommand = "/" + entry.getCommand();
			MutableText line = Text.literal("- ")
				.append(Text.literal(entry.getNote()).formatted(Formatting.YELLOW))
				.append(Text.literal(" : "))
				.append(Text.literal(fullCommand)
					.styled(style -> style
						.withColor(Formatting.AQUA)
						.withUnderline(true)
						.withClickEvent(new CopyToClipboard(fullCommand))
						.withHoverEvent(new ShowText(Text.translatable("command.utility-toolkit.memo.list.copy_hint")))));
			source.sendFeedback(() -> line, false);
		}
		return memos.size();
	}

	private static int sendFeedback(ServerCommandSource source, Text message) {
		source.sendFeedback(() -> message, false);
		return 1;
	}

	private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestNotes(MemoManager manager, SuggestionsBuilder builder) {
		for (MemoManager.MemoEntry entry : manager.getAllMemos()) {
			builder.suggest(entry.getNote(), Text.literal("/" + entry.getCommand()));
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
				builder.suggest("\"" + note + "\"", Text.literal("/" + entry.getCommand()));
			}
		}
		return builder.buildFuture();
	}

	private static void sendEditPrompt(ServerCommandSource source, MemoManager.MemoEntry entry) {
		String fullCommand = "/" + entry.getCommand();
		MutableText commandText = Text.literal(fullCommand)
			.styled(style -> style
				.withColor(Formatting.AQUA)
				.withUnderline(true)
				.withClickEvent(new CopyToClipboard(fullCommand))
				.withHoverEvent(new ShowText(Text.translatable("command.utility-toolkit.memo.list.copy_hint"))));
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.memo.edit.begin", entry.getNote()).formatted(Formatting.GOLD), false);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.memo.edit.current_command").append(commandText).formatted(Formatting.WHITE), false);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.memo.edit.current_note", entry.getNote()).formatted(Formatting.WHITE), false);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.memo.edit.help", entry.getNote()).formatted(Formatting.YELLOW), false);
	}

	private static ServerPlayerEntity getPlayer(ServerCommandSource source) {
		return source.getEntity() instanceof ServerPlayerEntity player ? player : null;
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
}
