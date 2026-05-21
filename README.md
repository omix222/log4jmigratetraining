# log4j マイグレーション学習プロジェクト

log4j 1.x → 2.x への移行を 7 ステップで体験できる学習用プロジェクト。

## 移行パス

```
Step 1        Step 2        Step 3        Step 4
log4j 1.x ──► ブリッジ ──► log4j 2.x ──► SLF4J + log4j 2.x
(移行前)      (コード無変更) (ネイティブ)  (ベストプラクティス)

自動化ツール:
  Step 5: OpenRewrite — Java コードを自動変換 (Step 1 → Step 3 相当)
  Step 6: Log4j1ConfigurationConverter — properties 設定ファイルを自動変換
  Step 7: farnetto/log4jconverter — XML 設定ファイルを変換 (Step 6 が対応できない XML 形式)
```

## 各ステップの概要

| Step | 依存ライブラリ                         | import パッケージ               | 設定ファイル               | 特記事項                                  |
| ---- | -------------------------------------- | ------------------------------- | -------------------------- | ----------------------------------------- |
| 1    | `log4j:log4j:1.2.17`                   | `org.apache.log4j.*`            | `log4j.properties`         | EOL 済み・CVE あり                        |
| 2    | `log4j-1.2-api` + `log4j-core`         | `org.apache.log4j.*` (変更なし) | `log4j2.xml`               | ブリッジ: コード無変更で 2.x バックエンド |
| 3    | `log4j-api` + `log4j-core`             | `org.apache.logging.log4j.*`    | `log4j2.xml`               | 完全移行・新機能フル活用                  |
| 4    | `slf4j-api` + `log4j-slf4j2-impl`      | `org.slf4j.*`                   | `log4j2.xml`               | 実装に依存しない最終形                    |
| 5    | `log4j:log4j` + `rewrite-maven-plugin` | — (移行前コード)                | `log4j.properties`         | Java コードの自動変換ツール               |
| 6    | `log4j-1.2-api`                        | —                               | `log4j-complex.properties` | 設定ファイルの自動変換ツール (properties のみ) |
| 7    | `jaxb-api` + `jaxb-impl` (実行時のみ)  | —                               | `log4j-complex.xml`        | XML 形式の設定ファイルを変換             |

## 実行方法

```bash
# 全モジュールをビルド
mvn compile

# Step 1-4: アプリ実行
mvn -pl step1-log4j1        exec:java
mvn -pl step2-bridge        exec:java
mvn -pl step3-log4j2-native exec:java
mvn -pl step4-slf4j         exec:java

# Step 5: OpenRewrite (JDK 21 が必要)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # または sdk use java 21.x.x
mvn -pl step5-openrewrite rewrite:dryRun            # 差分確認 (ファイル変更なし)
cat step5-openrewrite/target/rewrite/rewrite.patch  # パッチ内容を確認
mvn -pl step5-openrewrite rewrite:run               # 実際に適用

# Step 6: Log4j1ConfigurationConverter
mvn -pl step6-config-converter exec:java@convert-properties
cat step6-config-converter/target/log4j2-from-properties.xml

# Step 7: farnetto/log4jconverter (XML 形式の設定ファイルを変換)
mvn -pl step7-farnetto-converter generate-resources
cat step7-farnetto-converter/target/log4j2-from-simple.xml
cat step7-farnetto-converter/target/log4j2-from-complex.xml
```

## 学習ポイント早見表

### API の変化

| 項目         | log4j 1.x                      | log4j 2.x                     | SLF4J                            |
| ------------ | ------------------------------ | ----------------------------- | -------------------------------- |
| Logger 取得  | `Logger.getLogger(Clazz)`      | `LogManager.getLogger(Clazz)` | `LoggerFactory.getLogger(Clazz)` |
| パラメータ化 | 文字列結合 or `isXxxEnabled()` | `logger.info("x={}", x)`      | `logger.info("x={}", x)`         |
| MDC 設定     | `MDC.put(k, v)`                | `ThreadContext.put(k, v)`     | `MDC.put(k, v)`                  |
| MDC 削除     | `MDC.remove(k)`                | `ThreadContext.remove(k)`     | `MDC.remove(k)`                  |
| FATAL レベル | `logger.fatal(msg)`            | `logger.fatal(msg)` (非推奨)  | 存在しない → `logger.error()`    |

### 設定ファイルの変化

```
log4j 1.x: src/main/resources/log4j.properties
log4j 2.x: src/main/resources/log4j2.xml  (または .json / .yaml)
```

主な進化点:
- `monitorInterval`: 設定ファイルの無停止リロード
- `RollingFile` + `JsonTemplateLayout`: 構造化 JSON ログ
- `AsyncLogger` / `AsyncAppender`: 高スループット非同期ロギング
- `Lookup`: `${env:VAR}`, `${ctx:mdcKey}` で動的な値埋め込み

### log4j 2.x の新機能 (Step 3 で確認)

```java
// {} プレースホルダー (isDebugEnabled() ガード不要)
logger.debug("orderId={}, amount={}", orderId, amount);

// ラムダによる遅延評価 (INFO 無効なら getDetailedInfo() は呼ばれない)
logger.info(() -> "詳細=" + getDetailedInfo());

// MapMessage による構造化ログ
MapMessage msg = new MapMessage<>()
    .with("event", "order.placed")
    .with("orderId", orderId);
logger.info(msg);

// ThreadContext (旧 MDC) のスタック操作
ThreadContext.push("request-123");
```

### SLF4J 2.x Fluent API (Step 4 で確認)

```java
logger.atInfo()
      .addKeyValue("event", "order.placed")
      .addKeyValue("orderId", orderId)
      .addKeyValue("amount", amount)
      .log("注文を受け付けました");
```

## OpenRewrite による自動移行 (Step 5)

OpenRewrite は AST ベースのリファクタリングツール。レシピ (`Recipe`) を適用することで Java ソースと `pom.xml` を自動変換する。

```bash
# 1. 何が変わるか確認 (ファイルは変更されない)
mvn -pl step5-openrewrite rewrite:dryRun
cat step5-openrewrite/target/rewrite/rewrite.patch

# 2. 実際に適用
mvn -pl step5-openrewrite rewrite:run
```

OpenRewrite の `Log4j1ToLog4j2` レシピが変換する内容:

| 変換前 (log4j 1.x)               | 変換後 (log4j 2.x)                              |
| -------------------------------- | ----------------------------------------------- |
| `import org.apache.log4j.Logger` | `import org.apache.logging.log4j.Logger`        |
| `Logger.getLogger(Clazz)`        | `LogManager.getLogger(Clazz)`                   |
| `import org.apache.log4j.MDC`    | `import org.apache.logging.log4j.ThreadContext` |
| `MDC.put/remove`                 | `ThreadContext.put/remove`                      |
| `log4j:log4j` 依存 (pom.xml)     | `log4j-api` + `log4j-core` 依存に置換           |

**Java バージョン要件**: OpenRewrite 8.36.0 は内部で `com.sun.tools.javac.*` を使うため、**JDK 21 以下が必要**。Java 25 では `NoClassDefFoundError: com/sun/tools/javac/code/Type$UnknownType` が発生する。  
pom.xml の依存更新のみは Java 25 でも動作する。

SLF4J に移行したい場合は `activeRecipes` を変更:
```xml
<recipe>org.openrewrite.java.logging.slf4j.Log4j1ToSlf4j</recipe>
```

## Log4j1ConfigurationConverter による設定変換 (Step 6)

`log4j-1.2-api` に含まれるツール。`log4j.properties` を `log4j2.xml` に変換する。

```bash
mvn -pl step6-config-converter exec:java@convert-properties
# 出力: step6-config-converter/target/log4j2-from-properties.xml
# 期待値: step6-config-converter/src/main/resources/expected/log4j2-from-properties.xml
```

**変換後に手動確認が必要な箇所**:
- `%-5v1Level` → `%-5level` に修正 (パターン文字の誤変換)
- `%properties{key}` → `%X{key}` に修正 (MDC 出力の誤変換)
- `additivity="false"` が欠落する場合がある
- `log4j.xml` (DOCTYPE ベース XML) は正しく変換されない。properties 形式のみ対応。

## farnetto/log4jconverter による XML 設定変換 (Step 7)

GitHub: https://github.com/farnetto/log4jconverter

Log4j1ConfigurationConverter が対応できない **XML 形式** (DOCTYPE ベース) の設定ファイルを変換するコミュニティ製ツール。

### 仕組み
1. JAXB の `Unmarshaller` で `log4j.dtd` に従い XML をパース → JAXB POJO に変換
2. FreeMarker テンプレート (`log4j2.ftl`) で log4j2.xml を生成
3. XML コメントも保持する (ファイルを行単位で先読みしてコメントマップを作成)

### 実行方法

```bash
# XML 形式の log4j 設定ファイルを変換
mvn -pl step7-farnetto-converter generate-resources

# 出力確認
cat step7-farnetto-converter/target/log4j2-from-simple.xml   # シンプルな例
cat step7-farnetto-converter/target/log4j2-from-complex.xml  # 複雑な例 (step6 で失敗したXMLと同じ)
```

### step6 との比較

| 項目 | step6 (Log4j1ConfigurationConverter) | step7 (farnetto/log4jconverter) |
| ---- | ------------------------------------- | -------------------------------- |
| 入力形式 | `log4j.properties` のみ対応 | `log4j.xml` (DOCTYPE 形式) |
| XML 対応 | 不可 (XML を properties として誤読) | 可 (JAXB でパース) |
| Maven Central | あり (`log4j-1.2-api` に内包) | なし (要ソースビルド) |
| Java 要件 | Java 9+ | Java 8 でビルド、Java 9+ で実行 (JAXB 追加要) |

### 変換の注意点

- `DailyRollingFileAppender` は対応テンプレートなし → `<DailyRollingFileAppender>` タグのままフォールバック出力。手動で `<RollingFile>` + `<TimeBasedTriggeringPolicy>` に変換する。
- `ConsoleAppender` の `Threshold` は `<Filter type="ThresholdFilter">` に変換される (step6 と書式が微妙に異なる)。

### ツールのビルド方法 (再ビルドが必要な場合)

```bash
# Java 8 が必要 (JAXB 生成ツール jaxb2-maven-plugin が Java 9+ では動作しない)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home
git clone --depth=1 https://github.com/farnetto/log4jconverter.git
cd log4jconverter && mvn package -DskipTests
# → target/log4jconverter-0.0.1-SNAPSHOT.jar と target/lib/ が生成される
```

## よくある移行の落とし穴

1. **`log4j.properties` が無視される**: log4j 2.x は `log4j2.xml` / `log4j2.json` を読む。`log4j.properties` は `log4j-1.2-api` ブリッジ経由でも無視されるので注意。

2. **`FATAL` の扱い**: SLF4J には FATAL がない。Step 3 → Step 4 移行時に `logger.fatal()` → `logger.error()` に変更する。

3. **MDC のクリア忘れ**: スレッドプール環境では `finally` ブロックで必ずクリアすること。ThreadContext/MDC はスレッドローカルなので汚染が次のリクエストに引き継がれる。

4. **`log4j-slf4j-impl` vs `log4j-slf4j2-impl`**: SLF4J 1.x 用と 2.x 用でアーティファクト名が異なる。混在させると `SLF4J: No SLF4J providers were found.` エラーになる。

5. **依存の重複**: `log4j-1.2-api` と `log4j:log4j` を同時に依存に含めると競合する。ブリッジ移行後は `log4j:log4j` を除外すること。

6. **OpenRewrite のバージョン整合**: `rewrite-maven-plugin` の `rewrite-core` バージョンと `rewrite-logging-frameworks` の `rewrite-bom` バージョンが一致していないとレシピ検証エラーになる。

## 参考リンク

- [log4j 2.x 移行ガイド (公式)](https://logging.apache.org/log4j/2.x/manual/migration.html)
- [SLF4J マニュアル](https://www.slf4j.org/manual.html)
- [log4j 2.x JsonTemplateLayout](https://logging.apache.org/log4j/2.x/manual/json-template-layout.html)
- [OpenRewrite rewrite-logging-frameworks](https://github.com/openrewrite/rewrite-logging-frameworks)
- [farnetto/log4jconverter](https://github.com/farnetto/log4jconverter)
- https://logging.apache.org/log4j/2.x/migrate-from-log4j1.html
