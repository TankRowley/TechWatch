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

    public String trendState(String value) {
        return switch (safe(value)) {
            case "Rising" -> "急上昇";
            case "Stable" -> "安定";
            case "Cooling" -> "減速";
            case "Dormant" -> "休眠";
            case "Insufficient" -> "データ蓄積中";
            default -> safe(value);
        };
    }

    public String marketLabel(String value) {
        return switch (safe(value)) {
            case "Hot" -> "熱い";
            case "Stable Demand" -> "安定需要";
            case "Emerging" -> "伸び始め";
            case "US Leading" -> "米国先行";
            case "JP Strong" -> "国内需要あり";
            case "Buzz Only" -> "話題先行";
            case "Declining Demand" -> "需要低下";
            case "Unknown", "UNKNOWN" -> "データ不足";
            default -> safe(value);
        };
    }

    public String exploreJudgement(String value) {
        return switch (safe(value)) {
            case "NOW" -> "今学ぶ";
            case "LATER" -> "後で学ぶ";
            case "NAME_ONLY" -> "名前だけ覚える";
            case "IGNORE" -> "無視";
            default -> "未判断";
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
            case "DevOps" -> "DevOps・運用監視";
            case "Development Tools" -> "開発ツール";
            case "Cloud / IoT" -> "クラウド・IoT";
            case "AI / Data Engineering" -> "AI・データ基盤";
            case "Infrastructure / Security" -> "インフラ・セキュリティ";
            case "Java" -> "Java";
            default -> safe(value);
        };
    }

    private String safe(String value) { return value == null ? "" : value; }
}
