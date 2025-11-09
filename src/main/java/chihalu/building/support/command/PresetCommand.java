package chihalu.building.support.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public final class PresetCommand {
	private PresetCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandPresetManager presetManager) {
		dispatcher.register(CommandManager.literal("preset")
			.requires(source -> source.hasPermissionLevel(0))
			.then(CommandManager.literal("add")
				.then(CommandManager.argument("slot", IntegerArgumentType.integer(1))
					.then(CommandManager.argument("command", StringArgumentType.greedyString())
						.suggests((context, builder) -> suggestCommands(dispatcher, context, builder))
						.executes(context -> addPreset(context, presetManager)))))
			.then(CommandManager.literal("remove")
				.then(CommandManager.argument("slot", StringArgumentType.greedyString())
					.suggests((context, builder) -> suggestSlots(presetManager, builder))
					.executes(context -> removePreset(context, presetManager))))
			.then(CommandManager.literal("list")
				.executes(context -> listPresets(context.getSource(), presetManager)))
			.then(CommandManager.argument("slot", StringArgumentType.greedyString())
				.suggests((context, builder) -> suggestSlots(presetManager, builder))
				.executes(context -> executePreset(context, presetManager))));
	}

	private static int addPreset(CommandContext<ServerCommandSource> context, CommandPresetManager manager) {
		int slot = IntegerArgumentType.getInteger(context, "slot");
		String raw = StringArgumentType.getString(context, "command").trim();
		String command = raw;
		String description = "";
		if (!raw.isEmpty()) {
			int lastQuote = raw.lastIndexOf('"');
			int firstQuote = raw.indexOf('"');
			if (firstQuote != -1 && lastQuote > firstQuote) {
				description = raw.substring(firstQuote + 1, lastQuote);
				command = raw.substring(0, firstQuote).trim();
			}
		}
		if (command.endsWith(")")) {
			int open = command.lastIndexOf('(');
			if (open >= 0 && open < command.length() - 1) {
				description = command.substring(open + 1, command.length() - 1).trim();
				command = command.substring(0, open).trim();
			}
		}
		if (command.startsWith("\"") && command.endsWith("\"") && command.length() > 1) {
			command = command.substring(1, command.length() - 1).trim();
		}
		Text result = manager.addPreset(slot, command, description);
		context.getSource().sendFeedback(() -> result, false);
		return 1;
	}

	private static int removePreset(CommandContext<ServerCommandSource> context, CommandPresetManager manager) {
		Integer slot = parseSlot(StringArgumentType.getString(context, "slot"));
		if (slot == null) {
			context.getSource().sendFeedback(() -> Text.translatable("command.utility-toolkit.preset.invalid_slot"), false);
			return 0;
		}
		Text result = manager.removePreset(slot);
		context.getSource().sendFeedback(() -> result, false);
		return 1;
	}

	private static int listPresets(ServerCommandSource source, CommandPresetManager manager) {
		var presets = manager.getAllPresets();
		if (presets.isEmpty()) {
			source.sendFeedback(() -> Text.translatable("command.utility-toolkit.preset.list.empty"), false);
			return 0;
		}
		MutableText message = Text.empty();
		boolean first = true;
		for (var entry : presets) {
			String display = entry.getDescription().isBlank() ? entry.getCommand() : entry.getDescription();
			if (!first) {
				message.append(Text.literal(", "));
			}
			message.append(Text.translatable("command.utility-toolkit.preset.list.entry", entry.getSlot(), display));
			first = false;
		}
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.preset.list", message), false);
		return presets.size();
	}

	private static int executePreset(CommandContext<ServerCommandSource> context, CommandPresetManager manager) throws CommandSyntaxException {
		Integer slot = parseSlot(StringArgumentType.getString(context, "slot"));
		if (slot == null) {
			context.getSource().sendFeedback(() -> Text.translatable("command.utility-toolkit.preset.invalid_slot"), false);
			return 0;
		}
		CommandPresetManager.PresetEntry entry = manager.getPreset(slot);
		if (entry == null) {
			context.getSource().sendFeedback(() -> Text.translatable("command.utility-toolkit.preset.not_found", slot), false);
			return 0;
		}
		String command = entry.getCommand();
		var commandManager = context.getSource().getServer().getCommandManager();
		commandManager.execute(commandManager.getDispatcher().parse(command, context.getSource()), command);
		String display = entry.getDescription().isBlank() ? entry.getCommand() : entry.getDescription();
		context.getSource().sendFeedback(() -> Text.translatable("command.utility-toolkit.preset.executed", slot, display), false);
		return 1;
	}

	private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestSlots(CommandPresetManager manager, SuggestionsBuilder builder) {
		for (var entry : manager.getAllPresets()) {
			String label = entry.getDescription().isBlank() ? "/" + entry.getCommand() : entry.getDescription();
			String suggestion = entry.getSlot() + " " + label;
			String tooltip = "/" + entry.getCommand();
			builder.suggest(suggestion, Text.literal(tooltip));
		}
		return builder.buildFuture();
	}

	private static Integer parseSlot(String input) {
		if (input == null) {
			return null;
		}
		String trimmed = input.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		int endIndex = 0;
		while (endIndex < trimmed.length() && Character.isDigit(trimmed.charAt(endIndex))) {
			endIndex++;
		}
		if (endIndex == 0) {
			return null;
		}
		try {
			return Integer.parseInt(trimmed.substring(0, endIndex));
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
		String partialInput = builder.getRemaining();
		boolean hasLeadingSlash = partialInput.startsWith("/");
		String commandInput = hasLeadingSlash ? partialInput.substring(1) : partialInput;
		var parseResults = dispatcher.parse(commandInput, context.getSource());
		return dispatcher.getCompletionSuggestions(parseResults).thenCompose(suggestions -> {
			for (Suggestion suggestion : suggestions.getList()) {
				String applied = suggestion.apply(commandInput);
				if (hasLeadingSlash) {
					applied = "/" + applied;
				}
				var tooltipMessage = suggestion.getTooltip();
				Text tooltip = tooltipMessage == null ? Text.literal("/" + applied) : Text.literal(tooltipMessage.getString());
				builder.suggest(applied, tooltip);
			}
			return builder.buildFuture();
		});
	}
}
