package planner;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

public class StudyPlanner {

    private static final int CYCLE_DAYS = 7;
    private static final int DAY_START_HOUR = 4; // days begin at 04:00

    private List<Subject> subjects = new ArrayList<>();
    private List<StudySession> sessions = new ArrayList<>();
    private DayOfWeek cycleStartDay = DayOfWeek.MONDAY;
    private String dataFile;

    public StudyPlanner(String dataFile) {
        this.dataFile = dataFile;
        load();
    }

    // ─── Subjects ────────────────────────────────────────────────────────────

    public void addSubject(Subject s) {
        subjects.add(s);
        save();
    }

    public List<Subject> getSubjects() { return subjects; }

    public Subject findSubject(String title) {
        return subjects.stream()
            .filter(s -> s.title.equalsIgnoreCase(title))
            .findFirst().orElse(null);
    }

    // ─── Sessions ─────────────────────────────────────────────────────────────

    public void addSession(StudySession ss) {
        sessions.add(ss);
        save();
    }

    public List<StudySession> getSessions() { return sessions; }

    public void deleteSession(int index) {
        sessions.remove(index);
        save();
    }

    public void editSession(int index, LocalDateTime newStart, LocalDateTime newEnd) {
        StudySession ss = sessions.get(index);
        ss.start = newStart;
        ss.end   = newEnd;
        save();
    }

    // ─── Cycle Math ───────────────────────────────────────────────────────────

    public void setCycleStartDay(DayOfWeek day) {
        this.cycleStartDay = day;
        save();
    }

    public DayOfWeek getCycleStartDay() { return cycleStartDay; }

    /** Start of the current rolling 7-day cycle (at 04:00 on the cycle start day). */
    public LocalDateTime currentCycleStart() {
        LocalDate today = currentStudyDate();
        // Walk back to find the most recent occurrence of cycleStartDay
        LocalDate d = today;
        while (d.getDayOfWeek() != cycleStartDay) {
            d = d.minusDays(1);
        }
        return d.atTime(DAY_START_HOUR, 0);
    }

    /** How many full study-days remain in the cycle, including today. */
    public int daysRemainingInCycle() {
        LocalDate cycleStart = currentCycleStart().toLocalDate();
        LocalDate today = currentStudyDate();
        long elapsed = java.time.temporal.ChronoUnit.DAYS.between(cycleStart, today);
        return (int) (CYCLE_DAYS - elapsed);
    }

    /** Today's date according to the 4 AM boundary. */
    public LocalDate currentStudyDate() {
        return StudySession.studyDayOf(LocalDateTime.now());
    }

    // ─── Progress Calculations ────────────────────────────────────────────────

    /** Sessions in the current cycle for a given subject. */
    public List<StudySession> cycleSessionsFor(String subjectTitle) {
        LocalDateTime cycleStart = currentCycleStart();
        return sessions.stream()
            .filter(ss -> ss.subjectTitle.equalsIgnoreCase(subjectTitle))
            .filter(ss -> !ss.start.isBefore(cycleStart))
            .collect(Collectors.toList());
    }

    /** Total hours studied this cycle for a subject. */
    public double hoursThisCycle(String subjectTitle) {
        return cycleSessionsFor(subjectTitle).stream()
            .mapToDouble(StudySession::durationHours)
            .sum();
    }

    /** Hours studied today for a subject. */
    public double hoursToday(String subjectTitle) {
        LocalDate today = currentStudyDate();
        return sessions.stream()
            .filter(ss -> ss.subjectTitle.equalsIgnoreCase(subjectTitle))
            .filter(ss -> ss.studyDay().equals(today))
            .mapToDouble(StudySession::durationHours)
            .sum();
    }

    public static class ProgressReport {
        public Subject subject;
        public double studied;
        public double target;
        public double remaining;
        public double requiredPerDay;
        public int daysLeft;
        public boolean feasible;
        public String warning;
        public String projection; // e.g. "Wednesday"
    }

    public ProgressReport getProgress(Subject s) {
        ProgressReport r = new ProgressReport();
        r.subject   = s;
        r.studied   = hoursThisCycle(s.title);
        r.target    = s.weeklyTargetHours;
        r.remaining = Math.max(0, r.target - r.studied);
        r.daysLeft  = daysRemainingInCycle();

        if (r.remaining <= 0) {
            r.requiredPerDay = 0;
            r.feasible = true;
            r.projection = "already reached";
        } else if (r.daysLeft <= 0) {
            r.requiredPerDay = Double.POSITIVE_INFINITY;
            r.feasible = false;
            r.warning = "Cycle ended with target not met.";
            r.projection = "not reachable this cycle";
        } else {
            r.requiredPerDay = r.remaining / r.daysLeft;

            // Feasibility check
            if (s.hasDailyLimit() && s.limitMode == Subject.LimitMode.HARD_CAP) {
                double maxPossible = s.dailyLimitHours * r.daysLeft;
                if (maxPossible < r.remaining) {
                    r.feasible = false;
                    r.warning = String.format(
                        "WARNING: Hard cap (%.1fh/day) makes weekly target unreachable. " +
                        "Max possible: %.1fh, still needed: %.1fh.",
                        s.dailyLimitHours, maxPossible, r.remaining);
                } else {
                    r.feasible = true;
                }
            } else {
                r.feasible = true;
            }

            // Projection: at current pace, when will target be reached?
            r.projection = projectTargetDay(s);
        }

        return r;
    }

    /** Estimate the study-day name when the weekly target will be reached, based on current pace. */
    private String projectTargetDay(Subject s) {
        LocalDateTime cycleStart = currentCycleStart();
        LocalDate today = currentStudyDate();
        long daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(cycleStart.toLocalDate(), today) + 1;
        double studied = hoursThisCycle(s.title);

        if (daysElapsed <= 0 || studied <= 0) return "insufficient data";

        double pace = studied / daysElapsed; // hours per day
        if (pace <= 0) return "insufficient data";

        double remaining = s.weeklyTargetHours - studied;
        if (remaining <= 0) return "already reached";

        long daysNeeded = (long) Math.ceil(remaining / pace);
        LocalDate projected = today.plusDays(daysNeeded);

        // Format: "Friday Mar 20" or "beyond this cycle"
        long cycleEnd = CYCLE_DAYS - java.time.temporal.ChronoUnit.DAYS.between(cycleStart.toLocalDate(), today);
        if (daysNeeded > cycleEnd) return "beyond this cycle at current pace";

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE MMM d");
        return projected.format(fmt);
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    public void save() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"cycleStartDay\": \"").append(cycleStartDay.name()).append("\",\n");

        sb.append("  \"subjects\": [\n");
        for (int i = 0; i < subjects.size(); i++) {
            sb.append("    ").append(subjects.get(i).toJson());
            if (i < subjects.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"sessions\": [\n");
        for (int i = 0; i < sessions.size(); i++) {
            sb.append("    ").append(sessions.get(i).toJson());
            if (i < sessions.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");

        try {
            Files.writeString(Path.of(dataFile), sb.toString());
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    private void load() {
        Path p = Path.of(dataFile);
        if (!Files.exists(p)) return;
        try {
            String json = Files.readString(p);

            String day = JsonUtil.extractString(json, "cycleStartDay");
            if (day != null) cycleStartDay = DayOfWeek.valueOf(day);

            String subArr = JsonUtil.extractArray(json, "subjects");
            for (Map<String, String> m : JsonUtil.parseArray(subArr)) {
                subjects.add(Subject.fromJson(m));
            }

            String sesArr = JsonUtil.extractArray(json, "sessions");
            for (Map<String, String> m : JsonUtil.parseArray(sesArr)) {
                sessions.add(StudySession.fromJson(m));
            }
        } catch (Exception e) {
            System.err.println("Error loading data: " + e.getMessage());
        }
    }
}
