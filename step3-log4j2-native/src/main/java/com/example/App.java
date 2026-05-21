package com.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Step 3 — log4j 2.x ネイティブ API のエントリポイント。
 *
 * 実行方法:
 *   mvn -pl step3-log4j2-native exec:java
 *
 * 確認ポイント:
 *   - {} プレースホルダーによるパラメータ化ログ
 *   - ThreadContext (旧 MDC) の動作
 *   - MapMessage による構造化ログ
 *   - ラムダによる遅延評価
 *   - 非同期ロガー (-DcontextSelector=Async で有効)
 */
public class App {

    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("=== Step 3: log4j 2.x ネイティブ API デモ ===");

        OrderService orderService = new OrderService();
        PaymentService paymentService = new PaymentService();

        // 通常フロー
        orderService.placeOrder("ORD-001", "CUST-A", 5000);

        // 高額注文
        orderService.placeOrder("ORD-002", "CUST-B", 150_000);

        // 構造化ログのデモ
        logger.info("--- 構造化ログ (MapMessage) デモ ---");
        orderService.placeOrderWithStructuredLog("ORD-003", "CUST-C", 8000);

        // 決済フロー
        paymentService.charge("PAY-001", 3000);
        paymentService.charge("FAIL-999", 3000);
        paymentService.charge("PAY-XL", 2_000_000);

        // バリデーションエラー
        try {
            orderService.placeOrder("", "CUST-D", -1);
        } catch (IllegalArgumentException e) {
            logger.warn("メイン: バリデーションエラーをキャッチ: {}", e.getMessage());
        }

        logger.info("=== Step 3 デモ終了 ===");
    }
}
