package chihalu.building.support.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * /UtilityToolkit help の案内を表示するコマンド。
 */
public final class UtilityToolkitHelpCommand {
	private UtilityToolkitHelpCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		// 要望通り大文字コマンドをサポートしつつ、小文字エイリアスも登録する
		dispatcher.register(
			CommandManager.literal("UtilityToolkit")
				.then(CommandManager.literal("help").executes(context -> showHelp(context.getSource())))
		);
		dispatcher.register(
			CommandManager.literal("utilitytoolkit")
				.then(CommandManager.literal("help").executes(context -> showHelp(context.getSource())))
		);
	}

	private static int showHelp(ServerCommandSource source) {
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.help.header"), false);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.help.memo"), false);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.help.preset"), false);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.help.village"), false);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.help.extinguish"), false);
		source.sendFeedback(() -> Text.translatable("command.utility-toolkit.help.footer"), false);
		return 1;
	}
}
