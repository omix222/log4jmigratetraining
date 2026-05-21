package com.example;

import org.apache.log4j.Logger;

/**
 * Step 2 — ブリッジ移行のエントリポイント。
 *
 * 実行方法:
 *   mvn -pl step2-bridge exec:java
 *
 * 確認ポイント:
 *   - Step 1 と同じ出力だが、バックエンドは log4j 2.x
 *   - log4j2.xml の設定が有効になっている (JSON 形式ファイルが出力される)
 *   - log4j.properties は無視される
 */
public class App {

    private static final Logger logger = Logger.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("=== Step 2: log4j 1.x → 2.x ブリッジ デモ ===");
        logger.info("バックエンドは log4j " + org.apache.logging.log4j.LogManager.getRootLogger().getClass().getName());

        OrderService orderService = new OrderService();
        PaymentService paymentService = new PaymentService();

        orderService.placeOrder("ORD-001", "CUST-A", 5000);
        orderService.placeOrder("ORD-002", "CUST-B", 150_000);
        paymentService.charge("FAIL-999", 3000);
        paymentService.charge("PAY-XL", 2_000_000);

        try {
            orderService.placeOrder("", "CUST-C", -1);
        } catch (IllegalArgumentException e) {
            logger.warn("メイン: バリデーションエラーをキャッチ: " + e.getMessage());
        }

        logger.info("=== Step 2 デモ終了 ===");
    }
}
