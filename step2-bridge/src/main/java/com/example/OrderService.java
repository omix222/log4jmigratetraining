package com.example;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/**
 * Step 2 — import は log4j 1.x のまま。
 * pom.xml で log4j-1.2-api を追加するだけでバックエンドが log4j 2.x になる。
 *
 * 実際に動くクラスは log4j2 の実装だが、ソースコードの変更は不要。
 * これがブリッジ移行の最大のメリット。
 */
public class OrderService {

    // import は 1.x のまま → log4j-1.2-api が内部で 2.x に委譲する
    private static final Logger logger = Logger.getLogger(OrderService.class);

    public void placeOrder(String orderId, String customerId, int amount) {
        MDC.put("orderId", orderId);
        MDC.put("customerId", customerId);

        logger.info("注文処理を開始します");

        if (logger.isDebugEnabled()) {
            logger.debug("注文詳細: orderId=" + orderId + ", amount=" + amount);
        }

        try {
            validateOrder(orderId, amount);
            processPayment(orderId, amount);
            logger.info("注文が正常に完了しました");
        } catch (IllegalArgumentException e) {
            logger.warn("注文バリデーションエラー: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("注文処理中に予期しないエラーが発生しました", e);
            throw new RuntimeException("注文処理失敗", e);
        } finally {
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
