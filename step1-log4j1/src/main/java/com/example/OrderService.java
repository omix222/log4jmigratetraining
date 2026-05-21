package com.example;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/**
 * log4j 1.x スタイルのサービスクラス。
 *
 * 移行時に注意が必要なポイント:
 *   1. import が org.apache.log4j.* (2.x では org.apache.logging.log4j.*)
 *   2. パラメータ化ログは文字列結合か自前ガード (isDebugEnabled) が必要
 *   3. MDC は org.apache.log4j.MDC (2.x では ThreadContext)
 *   4. FATAL レベルは 2.x でも存在するが非推奨扱い
 */
public class OrderService {

    // log4j 1.x: Logger.getLogger(Class)
    private static final Logger logger = Logger.getLogger(OrderService.class);

    public void placeOrder(String orderId, String customerId, int amount) {
        // MDC: リクエストスコープの情報を全ログに付加
        MDC.put("orderId", orderId);
        MDC.put("customerId", customerId);

        logger.info("注文処理を開始します");

        // 1.x では {} プレースホルダーが使えない → 文字列結合 or isXxxEnabled ガード
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
            // 例外オブジェクトを第2引数に渡すとスタックトレースが出力される
            logger.error("注文処理中に予期しないエラーが発生しました", e);
            throw new RuntimeException("注文処理失敗", e);
        } finally {
            // MDC は必ずクリアすること (スレッドプールでの汚染防止)
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
        // 模擬処理
        if (amount > 100_000) {
            logger.warn("高額注文を検出しました。amount=" + amount);
        }
    }
}
