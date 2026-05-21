# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 概要

log4j 1.x → 2.x へのマイグレーションを学ぶための Maven マルチモジュールプロジェクト。  
各ステップは独立した Maven モジュールで、同一ビジネスロジック (`OrderService` / `PaymentService`) を異なるロギング API で実装している。

## ビルドと実行

```bash
# 全モジュールをコンパイル
mvn compile

# 各ステップを実行
mvn -pl step1-log4j1        exec:java
mvn -pl step2-bridge        exec:java
mvn -pl step3-log4j2-native exec:java
mvn -pl step4-slf4j         exec:java

# Step 5: OpenRewrite ドライラン (JDK 21 必須)
mvn -pl step5-openrewrite rewrite:dryRun
cat step5-openrewrite/target/rewrite/rewrite.patch

# Step 5: OpenRewrite 実際に適用 (JDK 21 必須)
mvn -pl step5-openrewrite rewrite:run

# Step 6: log4j.properties → log4j2.xml 変換
mvn -pl step6-config-converter exec:java@convert-properties
cat step6-config-converter/target/log4j2-from-properties.xml

# 全モジュールをクリーン
mvn clean
```

## モジュール構成と移行パス

| モジュール | 依存ライブラリ | 設定ファイル | 学習ポイント |
|-----------|-------------|------------|------------|
| `step1-log4j1` | `log4j:log4j:1.2.17` | `log4j.properties` | 1.x の典型的な使い方・問題点 |
| `step2-bridge` | `log4j-1.2-api` + `log4j-core` | `log4j2.xml` | コード変更なしでバックエンドを 2.x に切り替え |
| `step3-log4j2-native` | `log4j-api` + `log4j-core` | `log4j2.xml` | 2.x ネイティブ API・構造化ログ・非同期 |
| `step4-slf4j` | `slf4j-api` + `log4j-slf4j2-impl` | `log4j2.xml` | 実装非依存のベストプラクティス |
| `step5-openrewrite` | `log4j:log4j` + `rewrite-maven-plugin` | `log4j.properties` | OpenRewrite による Java コードの自動変換 |
| `step6-config-converter` | `log4j-1.2-api` | なし | Log4j1ConfigurationConverter による設定ファイル変換 |

## アーキテクチャ上の重要な点

- **step2 のブリッジ移行**: `log4j:log4j` の依存を `log4j-1.2-api` に差し替えるだけ。Java ソースの `import org.apache.log4j.*` はそのまま動く。`log4j.properties` は log4j 2.x バックエンドでは**無視される**ため `log4j2.xml` が必要。

- **JsonTemplateLayout の依存**: `<JsonTemplateLayout/>` を使う場合は `log4j-layout-template-json` を追加依存に含める必要がある（`log4j-core` には含まれない）。

- **SLF4J バージョン対応**: SLF4J 2.x 用ブリッジは `log4j-slf4j2-impl`、SLF4J 1.x 用は `log4j-slf4j-impl`（数字の位置が異なる）。混在させると実行時エラーになる。

- **step5 OpenRewrite と Java バージョン**: `rewrite-maven-plugin` 5.42.0 / `rewrite-logging-frameworks` 2.14.0 は内部で `com.sun.tools.javac.*` を使うが、このクラスが Java 25 で削除されているため、Java ソース解析に失敗する。**JDK 21 以下で実行すること**。pom.xml の依存更新は Java 25 でも動作する。バージョン対応: plugin の `rewrite-core` バージョン (`pom.xml` の `rewrite.version` プロパティ) と recipe の `rewrite-bom` バージョンを一致させる必要がある。

- **step6 ConfigurationConverter の制約**: `Log4j1ConfigurationConverter` は `log4j.properties` 形式のみ正確に変換できる。`log4j.xml` (DOCTYPE ベース) は正しく読み込めない。変換後のパターン文字列に `%-5v1Level` (誤変換) や `%properties{key}` (本来は `%X{key}`) が含まれる場合があり、手動修正が必要。`additivity=false` が欠落する場合もある。
