package com.example;

import org.apache.log4j.Logger;

/**
 * Step 1 — log4j 1.x のエントリポイント。
 *
 * 実行方法:
 *   mvn -pl step1-log4j1 exec:java
 */
public class App {

    private static final Logger logger = Logger.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("=== Step 1: log4j 1.x デモ ===");

        OrderService orderService = new OrderService();
        PaymentService paymentService = new PaymentService();

        // 正常ケース
        orderService.placeOrder("ORD-001", "CUST-A", 5000);

        // 高額注文 (WARN が出る)
        orderService.placeOrder("ORD-002", "CUST-B", 150_000);

        // 決済失敗ケース
        paymentService.charge("FAIL-999", 3000);

        // 決済上限超過 (FATAL が出る)
        paymentService.charge("PAY-XL", 2_000_000);

        // バリデーションエラー
        try {
            orderService.placeOrder("", "CUST-C", -1);
        } catch (IllegalArgumentException e) {
            logger.warn("メイン: バリデーションエラーをキャッチ: " + e.getMessage());
        }

        logger.info("=== Step 1 デモ終了 ===");
    }
}
