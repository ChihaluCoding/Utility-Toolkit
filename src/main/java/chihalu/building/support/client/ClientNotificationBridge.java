package chihalu.building.support.client;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * クライアント専用APIに直接依存せず、共通コードから通知を橋渡しするユーティリティ。
 */
public final class ClientNotificationBridge {
	private static final Consumer<String> NO_OP = key -> {};
	private static final AtomicReference<Consumer<String>> HANDLER = new AtomicReference<>(NO_OP);

	private ClientNotificationBridge() {
	}

	public static void setHandler(Consumer<String> handler) {
		HANDLER.set(handler == null ? NO_OP : handler);
	}

	public static void notify(String translationKey) {
		if (translationKey == null || translationKey.isBlank()) {
			return;
		}
		HANDLER.get().accept(translationKey);
	}
}
