# てっくにゅーす

てっくにゅーすは、RSS/Atomから技術記事を集め、記事とキーワードを別々に評価し、毎週の学習優先度をMarkdown週報にまとめるJavaアプリです。単なるRSSリーダーではなく、「流行しているか」と「今の自分が学ぶ価値があるか」を分ける技術トレンド・フィルターを目指しています。

CLIとJavaFX GUIは同じ`WeeklyRunService`を利用します。GitHub Actionsはテストとビルドだけを行い、記事収集や週報生成は各ユーザーのPC上で実行します。

## できること

- 3つ以上のRSS/Atom情報源から記事を収集（1ソースの失敗で全体を止めない）
- SQLiteへ冪等保存し、同一URLを重複登録しない
- タイトル・概要から登録キーワードを抽出
- 情報源の信頼度、キーワード重み、Core/Buzzを使って記事を評価
- `Core / Watch / Buzz / Decline / Ignore`でキーワードを自動評価
- 取得記事数で補正した言及率を4・13・52週で比較し、最大104週の履歴から`急上昇 / 安定 / 減速 / 休眠`を評価
- `Core（一般評価）/ 学習中（自分の状態）/ 固定（継続監視）`を独立して管理
- 登録外の技術語を「探索」へ集め、説明・前提知識・学習判断を表示
- 未知キーワードをユーザー操作で学習中または固定へ昇格
- 手動CSVから米国・日本の求人シグナルを評価（求人サイトはスクレイピングしない）
- キーワード詳細で過去52週の記事言及率と過去12週の求人数を折れ線グラフ表示
- 初回起動時に学習中の技術・固定キーワード・興味領域を選択
- 学習中・固定・興味領域を記事スコアへ反映
- 記事本文の候補を抽出し、失敗時はRSS概要へフォールバック
- 任意でOpenAI Responses APIによる構造化要約
- AI要約・GUI・週報の説明文を日本語で表示
- 固定キーワードと学習中キーワードの章を含む週報を生成
- 概要、記事、キーワード、探索、求人市場、週報、設定、ログをJavaFX GUIで表示

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
       CLI / JavaFX
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

OpenAI APIやLM Studioは任意です。設定しなくても、記事収集、ルールベース評価、履歴、求人CSV、週報、GUIを利用できます。

## GitHubからクローンして使う

```powershell
git clone https://github.com/TankRowley/TechWatch.git
cd TechWatch

# 動作確認
mvn test

# GUIを起動
mvn javafx:run
```

初回起動時にローカルDBと週報フォルダが自動作成されます。APIキー、DB、実行ログ、生成した週報、メール送信先はGitへ追加されません。

## その他の実行方法

```powershell
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

### 初回セットアップ

GUIの初回起動時に、次の項目を選択します。

- 現在学習中の技術
- 継続監視したい固定キーワード
- 興味のある技術領域

設定後も「設定」タブの「学習設定を開く」から変更できます。キーワード画面では、個別に学習中・固定の切替、固定理由の編集、絞り込みができます。

`Core`は技術としての一般評価、`学習中`は現在の本人の状態、`固定`は継続監視対象です。3つは互いに独立しています。

### YAML設定

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

配布版では同梱した設定を初回に`%LOCALAPPDATA%\てっくにゅーす\config`へコピーし、DBと週報も同じ場所に保存します。旧TechWatch版のデータがある環境では、履歴を失わないよう従来の`%LOCALAPPDATA%\TechWatch`をそのまま使用します。CLI版はプロジェクト直下を使います。保存場所を明示したい場合は`TECHWATCH_HOME`環境変数または`-Dtechwatch.home=...`を指定できます。

ユーザーのDB、実行ログ、週報、メール送信先はローカルにだけ保存し、GitHub Actionsへアップロードしません。配布物には空のメール設定テンプレートだけを同梱します。

### 求人市場CSV

`job-market.csv`へ、規約上利用できるAPIや手動検索で確認した件数を入力します。求人サイトの無断スクレイピングは行いません。複数の検索語がある場合、重複を過大評価しないよう地域ごとの最大値を使います。

```csv
week_start,keyword,region,source,query,job_count
2026-06-29,Java,US,manual,Java developer,12000
2026-06-29,Java,JP,manual,Java エンジニア,8500
2026-06-29,Databricks,US,manual,Databricks,1800
2026-06-29,Databricks,JP,manual,Databricks,120
```

配布版では設定画面に表示される保存先の`config\job-market.csv`を編集し、週報を生成すると履歴・市場評価・グラフへ反映されます。件数は検索条件やデータ元で変わるため、絶対値ではなく傾向比較として扱います。

### 探索と履歴グラフ

「探索」タブはAI要約の重要技術語と技術カタログから登録外の語を抽出します。見つけただけでは学習中にせず、「学習中にする」「固定する」を押したものだけ昇格します。

「キーワード」タブで行をダブルクリックすると、最近の動き、求人評価、判断理由、過去52週の記事言及率・米国求人数・日本求人数、関連記事を確認できます。

### キーワード評価の考え方

キーワード評価はAIではなく説明可能な統計ルールで行います。週ごとの単純な記事件数ではなく、正常取得できた全記事数に占める言及率を使い、RSS未実行・全取得失敗は0件ではなく欠測として扱います。4週は最新動向、13週は定着、52週は長期基準として指数移動平均を計算し、取得成功率、情報源の集中度、高信頼情報源の比率から評価の信頼度とバズリスクを求めます。

`Core`はIT全体を理解するための基礎技術として永続保護され、短期の話題量だけでは降格しません。客観的な活動度・長期安定度・求人需要と、学習中・固定による個人優先度は別々に計算します。観測が不足している場合は衰退と断定せず「データ蓄積中」と表示します。

### データ保存期間と整理

`retention.yml`で記事本文、raw HTML、実行ログ、記事メタデータ、求人詳細などの保存日数を設定できます。週報生成後には古いデータを自動整理し、「設定」タブの「今すぐ整理する」から手動実行することもできます。

```yaml
retention:
  articleBodyDays: 90
  rawHtmlDays: 30
  executionLogDays: 90
  unselectedArticleDays: 365
  articleMetadataDays: 730
  jobSnapshotDays: 730
  htmlReportDays: 730
  keepMarkdownReports: true
  keepWeeklyKeywordStats: true
  keepKeywordMarketStats: true
```

固定・学習中キーワードに関係する記事、週報採用記事、評価点8以上の記事、「この記事を保存する」で指定した記事は整理から保護されます。記事を削除する前に対応する週次統計が作成済みかも確認します。既定ではMarkdown週報、週次キーワード統計、市場統計を削除しません。

`keepMarkdownReports`、`keepWeeklyKeywordStats`、`keepKeywordMarketStats`を`false`にすると、それぞれ`htmlReportDays`、`articleMetadataDays`、`jobSnapshotDays`を超えたデータも整理対象になります。既定値はすべて`true`です。

CLIでは整理とSQLiteの圧縮を個別に実行できます。`VACUUM`はDB全体を再構築するため、頻繁には実行せず月1回程度が目安です。

```powershell
java -jar target/techwatch.jar cleanup
java -jar target/techwatch.jar vacuum
```

## AI要約（任意）

接続設定がない場合は「日本語要約は未生成」と明示し、英語概要を日本語要約として扱いません。OpenAIのResponses APIまたはLM StudioのOpenAI互換Chat Completions APIを設定すると、JSON Schemaに沿った日本語の要約・重要理由・学習優先度を取得します。APIエラー時もアプリ全体は停止しません。

```powershell
$env:OPENAI_API_KEY = "your-api-key"
$env:OPENAI_MODEL = "gpt-5-mini" # 省略可
mvn exec:java
```

LM Studioを使う場合はDeveloper画面でLocal Serverを起動し、ロードしたモデルIDを指定します。ローカル接続ではAPIキーを省略できます。

```powershell
$model = "qwen3.5-9b-uncensored-hauhaucs-aggressive"
lms server start -p 1234
lms load $model --identifier techwatch-local -y
$env:OPENAI_BASE_URL = "http://localhost:1234/v1"
$env:OPENAI_MODEL = "techwatch-local"
mvn exec:java
```

本文取得自体を止めたい場合は`TECHWATCH_SKIP_BODY=true`を設定します。実際のAPIキーはLM Studio向けのダミー値として使わず、ファイルやGitへ保存しないでください。

## Gmailで簡単送信

設定タブの「Gmailで送る」に送信先メールアドレスを保存すると、画面上部の「Gmailで送る」から、必読記事と注視記事の要約・リンクを入力済みのGmail作成画面を開けます。内容を確認してGmail側の「送信」を押してください。

ログイン済みのGmailをブラウザで利用するため、送信元、Gmailパスワード、アプリパスワードの設定は不要です。アプリがパスワードやGoogleの認証情報を保存することもありません。メールを自動で送信完了する方式ではなく、誤送信防止のため最後の送信操作だけはGmail上で行います。

## Windowsアプリを作る

JDKに含まれる`jpackage.exe`を使い、ランタイム同梱のapp-imageを作成します。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1
```

成功すると`dist/てっくにゅーす/てっくにゅーす.exe`ができます。`dist/てっくにゅーす/config/`も同梱されるため、exeをダブルクリックしてGUIを起動できます。

WiX Toolsetを用意した環境では、インストーラーも生成できます。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -Installer
```

## 週報の構成

1. 今週の結論
2. 必読記事
3. 注視する記事
4. 軽く確認する記事
5. 固定キーワードの動き
6. 学習中キーワードに関係する記事
7. 今週伸びたキーワード
8. 流行疑いのキーワード
9. IT全体の地図 — 基礎技術
10. 今の自分が学ぶべきこと
11. 後回しでよいこと

## テスト

```powershell
mvn test
```

設定、SQLiteの重複防止、RSSパース、キーワード抽出、記事評価、キーワード評価、OpenAIレスポンス解析、Markdown、全体統合を外部通信なしでテストします。

## 現在の割り切りと次の拡張

- 本文抽出は汎用的なHTMLヒューリスティックであり、サイト別最適化は未実装
- 求人市場は手動CSVに対応済み。求人API、GitHub Trending、Hacker Newsの自動連携は未実装
- 設定値のGUI直接編集、通知、自動更新は後続フェーズ
- AI評価は記事要約に使用し、記事スコア自体は説明可能なルールベースを維持

次の改善候補は、情報源の採用率評価、サイト別本文抽出、学習プロフィール、キーワード推移グラフです。
