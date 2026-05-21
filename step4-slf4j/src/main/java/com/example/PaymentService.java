package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    public boolean charge(String paymentId, int amount) {
        MDC.put("paymentId", paymentId);
        try {
            logger.info("決済開始: paymentId={}, amount={}", paymentId, amount);

            if (amount > 1_000_000) {
                // FATAL は SLF4J に存在しない → ERROR で統一
                logger.error("決済上限超過。不正な取引の可能性: amount={}", amount);
                return false;
            }

            simulateExternalCall(paymentId);

            // SLF4J 2.x Fluent API でのエラー詳細ログ
            logger.atInfo()
                  .addKeyValue("event", "payment.success")
                  .addKeyValue("paymentId", paymentId)
                  .log("決済成功");
            return true;

        } catch (ExternalApiException e) {
            logger.error("外部決済 API 呼び出し失敗", e);
            return false;
        } finally {
            MDC.remove("paymentId");
        }
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
