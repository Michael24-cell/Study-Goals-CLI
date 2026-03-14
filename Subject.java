package planner;

public class Subject {

    public enum LimitMode { HARD_CAP, PLANNING }

    public String title;
    public double weeklyTargetHours;
    public double dailyLimitHours;   // 0 = no limit set
    public LimitMode limitMode;

    public Subject() {}

    public Subject(String title, double weeklyTargetHours, double dailyLimitHours, LimitMode limitMode) {
        this.title = title;
        this.weeklyTargetHours = weeklyTargetHours;
        this.dailyLimitHours = dailyLimitHours;
        this.limitMode = limitMode;
    }

    public boolean hasDailyLimit() {
        return dailyLimitHours > 0;
    }

    @Override
    public String toString() {
        String limit = hasDailyLimit()
            ? String.format("  daily limit: %.1fh [%s]", dailyLimitHours, limitMode)
            : "  no daily limit";
        return String.format("[%s]  weekly target: %.1fh%s", title, weeklyTargetHours, limit);
    }

    // --- Simple JSON serialization (no external libraries) ---

    public String toJson() {
        return String.format(
            "{\"title\":%s,\"weeklyTargetHours\":%.2f,\"dailyLimitHours\":%.2f,\"limitMode\":%s}",
            JsonUtil.quote(title), weeklyTargetHours, dailyLimitHours, JsonUtil.quote(limitMode.name())
        );
    }

    public static Subject fromJson(java.util.Map<String, String> m) {
        Subject s = new Subject();
        s.title            = m.get("title");
        s.weeklyTargetHours = Double.parseDouble(m.get("weeklyTargetHours"));
        s.dailyLimitHours  = Double.parseDouble(m.get("dailyLimitHours"));
        s.limitMode        = LimitMode.valueOf(m.get("limitMode"));
        return s;
    }
}
