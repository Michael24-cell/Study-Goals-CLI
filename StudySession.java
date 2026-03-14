package planner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StudySession {

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public String subjectTitle;
    public LocalDateTime start;
    public LocalDateTime end;

    public StudySession() {}

    public StudySession(String subjectTitle, LocalDateTime start, LocalDateTime end) {
        this.subjectTitle = subjectTitle;
        this.start = start;
        this.end = end;
    }

    /** Duration in fractional hours. */
    public double durationHours() {
        long minutes = java.time.Duration.between(start, end).toMinutes();
        return minutes / 60.0;
    }

    /**
     * The "study day" this session belongs to.
     * Days start at 04:00, so sessions before 04:00 belong to the previous calendar date.
     */
    public LocalDate studyDay() {
        return studyDayOf(start);
    }

    public static LocalDate studyDayOf(LocalDateTime t) {
        if (t.getHour() < 4) {
            return t.toLocalDate().minusDays(1);
        }
        return t.toLocalDate();
    }

    @Override
    public String toString() {
        return String.format("[%s]  %s → %s  (%.2fh)",
            subjectTitle, start.format(FMT), end.format(FMT), durationHours());
    }

    // --- Simple JSON serialization ---

    public String toJson() {
        return String.format(
            "{\"subjectTitle\":%s,\"start\":%s,\"end\":%s}",
            JsonUtil.quote(subjectTitle),
            JsonUtil.quote(start.format(FMT)),
            JsonUtil.quote(end.format(FMT))
        );
    }

    public static StudySession fromJson(java.util.Map<String, String> m) {
        StudySession ss = new StudySession();
        ss.subjectTitle = m.get("subjectTitle");
        ss.start = LocalDateTime.parse(m.get("start"), FMT);
        ss.end   = LocalDateTime.parse(m.get("end"),   FMT);
        return ss;
    }
}
