# TechWatch

TechWatchは、RSS/Atomから技術記事を集め、記事とキーワードを別々に評価し、毎週の学習優先度をMarkdown週報にまとめるJavaアプリです。単なるRSSリーダーではなく、「流行しているか」と「今の自分が学ぶ価値があるか」を分ける技術トレンド・フィルターを目指しています。

CLI、JavaFX GUI、GitHub Actions、将来のWindowsアプリ化が、すべて同じ`WeeklyRunService`を利用します。

## できること

- 3つ以上のRSS/Atom情報源から記事を収集（1ソースの失敗で全体を止めない）
- SQLiteへ冪等保存し、同一URLを重複登録しない
- タイトル・概要から登録キーワードを抽出
- 情報源の信頼度、キーワード重み、Core/Buzzを使って記事を評価
- `Core / Watch / Buzz / Decline / Ignore`でキーワードを自動評価
- 記事本文の候補を抽出し、失敗時はRSS概要へフォールバック
- 任意でOpenAI Responses APIによる構造化要約
- `reports/weekly/YYYY-MM-DD.md`へ週報を生成
- Dashboard、記事、キーワード、週報、設定、ログをJavaFX GUIで表示

## 技術構成

- Java 21（Java 25でもビルド確認）
- Maven / JUnit 5
- SQLite JDBC
- ROME（RSS/Atom）
- jsoup（本文候補抽出）
- SnakeYAML
- JavaFX
- OpenAI Responses API（任意）

## 設計

```text
CLI / JavaFX / GitHub Actions
             ↓
      WeeklyRunService
       ↙     ↓      ↘
ArticleService  KeywordService  ReportService
       ↓             ↓              ↓
 RSS・本文・評価   出現履歴・昇降格   Markdown
             ↘     ↓      ↙
                  SQLite
```

GUIのボタン処理には収集やDB操作を直接書かず、Coreのサービスを呼び出すだけにしています。AI要約も`ArticleSummarizer`インターフェースの実装なので、APIなしのローカル要約や別プロバイダーへ交換できます。

## 必要なもの

- JDK 21以上
- Maven 3.9以上
- インターネット接続（RSSおよび本文取得用）

## セットアップと実行

```powershell
mvn test

# CLI
mvn exec:java

# GUI（開発時）
mvn javafx:run

# 実行可能jarを生成
mvn package
java --enable-native-access=ALL-UNNAMED -jar target/techwatch.jar
java --enable-native-access=ALL-UNNAMED -jar target/techwatch-gui.jar
```

初回実行時に`techwatch.db`と`reports/weekly/`が作られます。処理ログは標準出力とGUIのLogsタブに表示されます。

## 設定

`sources.yml`でRSS/Atom情報源と信頼度（1〜5）を編集します。

```yaml
sources:
  - name: Cloudflare Blog
    type: rss
    url: https://blog.cloudflare.com/rss/
    trustScore: 5
```

`keywords.yml`で初期ステータスと記事評価用の重みを編集します。

```yaml
keywords:
  - name: Java
    category: Java
    status: Core
    weight: 5
```

配布版では同梱した`config/sources.yml`と`config/keywords.yml`を初回に`%LOCALAPPDATA%\TechWatch\config`へコピーし、DBと週報も`%LOCALAPPDATA%\TechWatch`に保存します。CLI版はプロジェクト直下を使います。保存場所を明示したい場合は`TECHWATCH_HOME`環境変数または`-Dtechwatch.home=...`を指定できます。

## AI要約（任意）

APIキーがない場合、RSS概要を使うため、費用なしで全機能を実行できます。キーがある場合だけ、本文をOpenAI Responses APIへ送り、JSON Schemaに沿った要約・重要理由・学習優先度を取得します。APIエラー時もローカル要約へフォールバックします。

```powershell
$env:OPENAI_API_KEY = "your-api-key"
$env:OPENAI_MODEL = "gpt-5-mini" # 省略可
mvn exec:java
```

本文取得自体を止めたい場合は`TECHWATCH_SKIP_BODY=true`を設定します。APIキーはファイルやGitへ保存しないでください。

## Windowsアプリを作る

JDKに含まれる`jpackage.exe`を使い、ランタイム同梱のapp-imageを作成します。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1
```

成功すると`dist/TechWatch/TechWatch.exe`ができます。`dist/TechWatch/config/`も同梱されるため、exeをダブルクリックしてGUIを起動できます。

WiX Toolsetを用意した環境では、インストーラーも生成できます。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -Installer
```

## 週報の構成

1. 今週の結論
2. Must Read
3. Watch
4. Skim
5. 今週伸びたキーワード
6. Buzz疑い
7. Core技術の動き
8. 今の自分が学ぶべきこと
9. 後回しでよいこと

## テスト

```powershell
mvn test
```

設定、SQLiteの重複防止、RSSパース、キーワード抽出、記事評価、キーワード評価、OpenAIレスポンス解析、Markdown、全体統合を外部通信なしでテストします。

## 現在の割り切りと次の拡張

- 本文抽出は汎用的なHTMLヒューリスティックであり、サイト別最適化は未実装
- 求人市場・GitHub Trending・Hacker Newsのデータは未統合
- Settingsタブからの設定編集、グラフ、通知、自動更新は後続フェーズ
- AI評価は記事要約に使用し、記事スコア自体は説明可能なルールベースを維持

次の改善候補は、情報源の採用率評価、サイト別本文抽出、学習プロフィール、キーワード推移グラフです。
