package com.example;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

public class PaymentService {

    private static final Logger logger = Logger.getLogger(PaymentService.class);

    public boolean charge(String paymentId, int amount) {
        MDC.put("paymentId", paymentId);
        try {
            logger.info("決済開始: paymentId=" + paymentId + ", amount=" + amount);

            if (amount > 1_000_000) {
                // [OpenRewrite 変換対象]
                // FATAL は log4j 2.x にも存在するが、SLF4J レシピ適用時は ERROR に変換される
                logger.fatal("決済上限超過。不正な取引の可能性: amount=" + amount);
                return false;
            }

            simulateExternalCall(paymentId);
            logger.info("決済成功: paymentId=" + paymentId);
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
        logger.debug("外部 API 呼び出し成功: paymentId=" + paymentId);
    }

    static class ExternalApiException extends RuntimeException {
        ExternalApiException(String message) {
            super(message);
        }
    }
}
