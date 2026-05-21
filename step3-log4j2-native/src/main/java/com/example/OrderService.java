package com.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.MapMessage;

/**
 * Step 3 — log4j 2.x ネイティブ API。
 *
 * 1.x からの主な変更点:
 *   import org.apache.log4j.*          → org.apache.logging.log4j.*
 *   Logger.getLogger(Clazz)            → LogManager.getLogger(Clazz)
 *   MDC.put/remove                     → ThreadContext.put/remove
 *   logger.debug("x=" + x)            → logger.debug("x={}", x)  ← 遅延評価
 *   isDebugEnabled() ガード            → {} プレースホルダーで不要に
 *   logger.fatal(msg)                  → logger.fatal(msg) は存在するが非推奨
 */
public class OrderService {

    // LogManager.getLogger — org.apache.logging.log4j パッケージ
    private static final Logger logger = LogManager.getLogger(OrderService.class);

    public void placeOrder(String orderId, String customerId, int amount) {
        // MDC → ThreadContext (スタックも使えるようになった)
        ThreadContext.put("orderId", orderId);
        ThreadContext.put("customerId", customerId);

        logger.info("注文処理を開始します");

        // 2.x の {} プレースホルダー: 引数は DEBUG が有効な場合のみ評価される
        // isDebugEnabled() ガードが不要になる
        logger.debug("注文詳細: orderId={}, amount={}", orderId, amount);

        try {
            validateOrder(orderId, amount);
            processPayment(orderId, amount);
            logger.info("注文が正常に完了しました");
        } catch (IllegalArgumentException e) {
            logger.warn("注文バリデーションエラー: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // 2.x でも Throwable を最後の引数に渡せばスタックトレースが出る
            logger.error("注文処理中に予期しないエラーが発生しました", e);
            throw new RuntimeException("注文処理失敗", e);
        } finally {
            // ThreadContext.clearAll() で一括クリアも可能
            ThreadContext.remove("orderId");
            ThreadContext.remove("customerId");
        }
    }

    public void placeOrderWithStructuredLog(String orderId, String customerId, int amount) {
        ThreadContext.put("orderId", orderId);

        // MapMessage: 構造化ログ (JSON Appender と組み合わせると非常に有効)
        MapMessage<?, ?> msg = new MapMessage<>()
                .with("event", "order.placed")
                .with("orderId", orderId)
                .with("customerId", customerId)
                .with("amount", String.valueOf(amount));
        logger.info(msg);

        ThreadContext.remove("orderId");
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
