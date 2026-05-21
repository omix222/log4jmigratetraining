package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Step 4 — SLF4J API を使う最終形。
 *
 * SLF4J への移行ポイント:
 *   import org.apache.logging.log4j.*  → import org.slf4j.*
 *   LogManager.getLogger(Clazz)        → LoggerFactory.getLogger(Clazz)
 *   ThreadContext.put/remove           → MDC.put/remove  (SLF4J にも MDC がある)
 *   MapMessage                         → SLF4J 2.x の fluent API (LoggingEventBuilder)
 *
 * {} プレースホルダーは SLF4J も同じ書き方なので移行コストが低い。
 */
public class OrderService {

    // SLF4J のファクトリ — 実装 (log4j2/logback) に依存しない
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    public void placeOrder(String orderId, String customerId, int amount) {
        // SLF4J の MDC — ThreadContext と同等
        MDC.put("orderId", orderId);
        MDC.put("customerId", customerId);

        logger.info("注文処理を開始します");
        logger.debug("注文詳細: orderId={}, amount={}", orderId, amount);

        try {
            validateOrder(orderId, amount);
            processPayment(orderId, amount);
            logger.info("注文が正常に完了しました");
        } catch (IllegalArgumentException e) {
            logger.warn("注文バリデーションエラー: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("注文処理中に予期しないエラーが発生しました", e);
            throw new RuntimeException("注文処理失敗", e);
        } finally {
            MDC.remove("orderId");
            MDC.remove("customerId");
        }
    }

    public void placeOrderFluent(String orderId, String customerId, int amount) {
        MDC.put("orderId", orderId);

        // SLF4J 2.x の Fluent API (LoggingEventBuilder)
        // addKeyValue() で構造化ログを簡潔に書ける
        logger.atInfo()
              .addKeyValue("event", "order.placed")
              .addKeyValue("orderId", orderId)
              .addKeyValue("customerId", customerId)
              .addKeyValue("amount", amount)
              .log("注文を受け付けました (Fluent API)");

        MDC.remove("orderId");
    }

    private void validateOrder(String orderId, int amount) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId が空です");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("金額は正の整数である必要があります: " + amount);
        }
        logger.debug("バリデーション通過: orderId={}", orderId);
    }

    private void processPayment(String orderId, int amount) {
        logger.info("支払い処理を実行します: orderId={}", orderId);
        if (amount > 100_000) {
            logger.warn("高額注文を検出しました。amount={}", amount);
        }
    }
}
