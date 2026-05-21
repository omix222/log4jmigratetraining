package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step 4 — SLF4J + log4j 2.x のエントリポイント。
 *
 * 実行方法:
 *   mvn -pl step4-slf4j exec:java
 *
 * 確認ポイント:
 *   - アプリコードは SLF4J API のみを使う
 *   - pom.xml の log4j-slf4j2-impl を slf4j-logback 等に差し替えれば
 *     コードを変更せずに実装を切り替えられる
 *   - SLF4J 2.x Fluent API (atInfo().addKeyValue().log())
 */
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("=== Step 4: SLF4J + log4j 2.x デモ ===");

        // 実際にどの実装が使われているかを確認
        logger.info("SLF4J バインディング: {}", logger.getClass().getName());

        OrderService orderService = new OrderService();
        PaymentService paymentService = new PaymentService();

        // 通常フロー
        orderService.placeOrder("ORD-001", "CUST-A", 5000);

        // 高額注文
        orderService.placeOrder("ORD-002", "CUST-B", 150_000);

        // SLF4J 2.x Fluent API デモ
        logger.info("--- Fluent API デモ ---");
        orderService.placeOrderFluent("ORD-003", "CUST-C", 8000);

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

        logger.info("=== Step 4 デモ終了 ===");
    }
}
