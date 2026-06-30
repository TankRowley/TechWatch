package com.example.techwatch.display;

public class DisplayLabelMapper {
    public String articleLabel(String value) {
        return switch (safe(value)) {
            case "Must Read" -> "必読";
            case "Watch" -> "注視";
            case "Skim" -> "軽く確認";
            case "Archive" -> "保存のみ";
            case "Ignore" -> "対象外";
            case "UNRATED" -> "未評価";
            default -> safe(value);
        };
    }

    public String keywordStatus(String value) {
        return switch (safe(value)) {
            case "Core" -> "基礎";
            case "Watch" -> "注視";
            case "Buzz" -> "流行疑い";
            case "Decline" -> "低下";
            case "Ignore" -> "対象外";
            case "Candidate" -> "候補";
            default -> safe(value);
        };
    }

    public String learningPriority(String value) {
        return switch (safe(value)) {
            case "Now" -> "今やる";
            case "Soon" -> "近いうち";
            case "Later" -> "後で";
            case "Watch Only" -> "眺めるだけ";
            case "Ignore" -> "対象外";
            default -> safe(value);
        };
    }

    public String category(String value) {
        return switch (safe(value)) {
            case "Backend" -> "バックエンド";
            case "Frontend" -> "フロントエンド";
            case "Infrastructure" -> "インフラ";
            case "Cloud" -> "クラウド";
            case "AI" -> "AI";
            case "Data Engineering" -> "データ基盤";
            case "Security" -> "セキュリティ";
            case "IoT" -> "IoT";
            case "Mobile" -> "モバイル";
            case "Game Development" -> "ゲーム開発";
            case "Java" -> "Java";
            default -> safe(value);
        };
    }

    private String safe(String value) { return value == null ? "" : value; }
}
