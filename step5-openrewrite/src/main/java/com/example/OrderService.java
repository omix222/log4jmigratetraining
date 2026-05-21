package com.example;

// [OpenRewrite 変換対象]
// import org.apache.log4j.Logger   → org.apache.logging.log4j.Logger
// import org.apache.log4j.MDC      → org.apache.logging.log4j.ThreadContext
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

public class OrderService {

    // [OpenRewrite 変換対象]
    // Logger.getLogger(Clazz)  →  LogManager.getLogger(Clazz)
    private static final Logger logger = Logger.getLogger(OrderService.class);

    public void placeOrder(String orderId, String customerId, int amount) {
        // [OpenRewrite 変換対象]
        // MDC.put  →  ThreadContext.put
        MDC.put("orderId", orderId);
        MDC.put("customerId", customerId);

        logger.info("注文処理を開始します");

        // [OpenRewrite 変換対象]
        // isDebugEnabled() + 文字列結合  →  {} プレースホルダー
        if (logger.isDebugEnabled()) {
            logger.debug("注文詳細: orderId=" + orderId + ", amount=" + amount);
        }

        try {
            validateOrder(orderId, amount);
            processPayment(orderId, amount);
            logger.info("注文が正常に完了しました");
        } catch (IllegalArgumentException e) {
            // 文字列結合  →  {} プレースホルダー
            logger.warn("注文バリデーションエラー: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("注文処理中に予期しないエラーが発生しました", e);
            throw new RuntimeException("注文処理失敗", e);
        } finally {
            // [OpenRewrite 変換対象]
            // MDC.remove  →  ThreadContext.remove
            MDC.remove("orderId");
            MDC.remove("customerId");
        }
    }

    private void validateOrder(String orderId, int amount) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId が空です");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("金額は正の整数である必要があります: " + amount);
        }
        logger.debug("バリデーション通過: orderId=" + orderId);
    }

    private void processPayment(String orderId, int amount) {
        logger.info("支払い処理を実行します: orderId=" + orderId);
        if (amount > 100_000) {
            logger.warn("高額注文を検出しました。amount=" + amount);
        }
    }
}
