package com.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 * 決済サービス — log4j 2.x ネイティブ API。
 *
 * 注目ポイント:
 *   - FATAL → ERROR に置き換え推奨
 *   - ラムダによる遅延評価: logger.debug(() -> "heavy: " + expensive())
 */
public class PaymentService {

    private static final Logger logger = LogManager.getLogger(PaymentService.class);

    public boolean charge(String paymentId, int amount) {
        ThreadContext.put("paymentId", paymentId);
        try {
            logger.info("決済開始: paymentId={}, amount={}", paymentId, amount);

            if (amount > 1_000_000) {
                // 2.x では FATAL よりも ERROR + アラート通知の組み合わせが推奨
                // FATAL は互換のために残っているが新規コードでは使わない
                logger.error("決済上限超過。不正な取引の可能性: amount={}", amount);
                return false;
            }

            simulateExternalCall(paymentId);

            // ラムダによる遅延評価 — INFO が無効なら getDetailedInfo() は呼ばれない
            logger.info(() -> "決済成功: 詳細=" + getDetailedInfo(paymentId));
            return true;

        } catch (ExternalApiException e) {
            logger.error("外部決済 API 呼び出し失敗", e);
            return false;
        } finally {
            ThreadContext.remove("paymentId");
        }
    }

    private String getDetailedInfo(String paymentId) {
        // 実際には重い処理を想定。ラムダがあるから安全に渡せる。
        return "paymentId=" + paymentId + ", ts=" + System.currentTimeMillis();
    }

    private void simulateExternalCall(String paymentId) {
        if (paymentId.startsWith("FAIL")) {
            throw new ExternalApiException("外部 API タイムアウト: " + paymentId);
        }
        logger.debug("外部 API 呼び出し成功: paymentId={}", paymentId);
    }

    static class ExternalApiException extends RuntimeException {
        ExternalApiException(String message) {
            super(message);
        }
    }
}
