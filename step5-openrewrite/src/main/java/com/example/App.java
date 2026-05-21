package com.example;

import org.apache.log4j.Logger;

/**
 * Step 5 — OpenRewrite 自動マイグレーション対象コード。
 *
 * このモジュールは log4j 1.x で書かれた "移行前" の状態を保持している。
 * OpenRewrite を適用すると、このファイルを含む全 Java ファイルと
 * pom.xml が自動的に書き換えられる。
 *
 * 実行手順:
 *   # 1. 差分だけ確認 (ファイルは変更されない)
 *   mvn -pl step5-openrewrite rewrite:dryRun
 *   cat step5-openrewrite/target/rewrite/rewrite.patch
 *
 *   # 2. 実際に変換を適用
 *   mvn -pl step5-openrewrite rewrite:run
 *   git diff step5-openrewrite/
 *
 *   # 3. 変換後にコンパイル確認
 *   mvn -pl step5-openrewrite compile
 *
 *   # 4. 元の状態に戻す
 *   git checkout -- step5-openrewrite/
 */
public class App {

    private static final Logger logger = Logger.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("=== Step 5: OpenRewrite 変換前の状態 ===");
        logger.info("'mvn -pl step5-openrewrite rewrite:dryRun' を実行して差分を確認してください");

        OrderService orderService = new OrderService();
        PaymentService paymentService = new PaymentService();

        orderService.placeOrder("ORD-001", "CUST-A", 5000);
        orderService.placeOrder("ORD-002", "CUST-B", 150_000);
        paymentService.charge("FAIL-999", 3000);
        paymentService.charge("PAY-XL", 2_000_000);

        try {
            orderService.placeOrder("", "CUST-C", -1);
        } catch (IllegalArgumentException e) {
            logger.warn("メイン: バリデーションエラーをキャッチ: " + e.getMessage());
        }

        logger.info("=== Step 5 デモ終了 ===");
    }
}
