package planner;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {

    private static final Scanner sc = new Scanner(System.in);
    private static StudyPlanner planner;

    public static void main(String[] args) {
        String dataFile = args.length > 0 ? args[0] : "study_data.json";
        planner = new StudyPlanner(dataFile);

        System.out.println("=== Study Planner ===");
        System.out.println("Data file: " + dataFile);
        System.out.println("Cycle starts on: " + planner.getCycleStartDay());
        System.out.println("Today (study day): " + planner.currentStudyDate());
        System.out.println("Days remaining in cycle: " + planner.daysRemainingInCycle());
        System.out.println();
        printHelp();

        while (true) {
            System.out.print("\n> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            switch (line.toLowerCase()) {
                case "add-subject"    -> cmdAddSubject();
                case "log-session"    -> cmdLogSession();
                case "progress"       -> cmdProgress();
                case "list-sessions"  -> cmdListSessions();
                case "edit-session"   -> cmdEditSession();
                case "delete-session" -> cmdDeleteSession();
                case "set-cycle-day"  -> cmdSetCycleDay();
                case "subjects"       -> cmdListSubjects();
                case "help"           -> printHelp();
                case "quit", "exit"   -> { System.out.println("Goodbye."); return; }
                default               -> System.out.println("Unknown command. Type 'help'.");
            }
        }
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    private static void cmdAddSubject() {
        String title = prompt("Subject title: ");
        if (planner.findSubject(title) != null) {
            System.out.println("Subject already exists.");
            return;
        }
        double weeklyTarget = promptDouble("Weekly target hours: ");
        double dailyLimit   = promptDouble("Daily limit hours (0 = none): ");

        Subject.LimitMode mode = Subject.LimitMode.PLANNING;
        if (dailyLimit > 0) {
            String modeStr = prompt("Limit mode — hard-cap or planning? [h/p]: ");
            mode = modeStr.equalsIgnoreCase("h")
                ? Subject.LimitMode.HARD_CAP
                : Subject.LimitMode.PLANNING;
        }

        planner.addSubject(new Subject(title, weeklyTarget, dailyLimit, mode));
        System.out.println("Subject added: " + title);
    }

    private static void cmdLogSession() {
        Subject s = pickSubject();
        if (s == null) return;

        System.out.println("Enter times as: yyyy-MM-dd HH:mm  (or press Enter to use now)");
        LocalDateTime start = promptDateTime("Start time: ");
        LocalDateTime end   = promptDateTime("End time (Enter = now): ");

        if (end.isBefore(start)) {
            System.out.println("End time is before start time. Session not logged.");
            return;
        }

        StudySession ss = new StudySession(s.title, start, end);
        System.out.printf("Logging %.2fh for [%s] on study-day %s%n",
            ss.durationHours(), s.title, ss.studyDay());

        // Daily limit advisory
        if (s.hasDailyLimit()) {
            double todayTotal = planner.hoursToday(s.title) + ss.durationHours();
            if (todayTotal > s.dailyLimitHours) {
                if (s.limitMode == Subject.LimitMode.HARD_CAP) {
                    System.out.printf("WARNING: This puts today's total at %.2fh, " +
                        "exceeding your hard cap of %.1fh.%n", todayTotal, s.dailyLimitHours);
                } else {
                    System.out.printf("Note: Today's total will be %.2fh " +
                        "(daily guidance is %.1fh).%n", todayTotal, s.dailyLimitHours);
                }
            }
        }

        planner.addSession(ss);
        System.out.println("Session logged.");
    }

    private static void cmdProgress() {
        if (planner.getSubjects().isEmpty()) {
            System.out.println("No subjects yet.");
            return;
        }

        System.out.println();
        System.out.printf("Cycle: %s  |  Days remaining: %d%n",
            planner.currentCycleStart().toLocalDate(), planner.daysRemainingInCycle());
        System.out.println("-".repeat(60));

        for (Subject s : planner.getSubjects()) {
            StudyPlanner.ProgressReport r = planner.getProgress(s);
            System.out.printf("%n[%s]%n", s.title);
            System.out.printf("  Progress   : %.2f / %.1f hours%n", r.studied, r.target);
            System.out.printf("  Remaining  : %.2f hours%n", r.remaining);
            if (r.daysLeft > 0 && r.remaining > 0) {
                System.out.printf("  Needed/day : %.2f hours (%d days left)%n", r.requiredPerDay, r.daysLeft);
            }
            System.out.printf("  Today      : %.2fh studied%n", planner.hoursToday(s.title));
            System.out.printf("  Projection : %s%n", r.projection);
            if (r.warning != null) {
                System.out.println("  " + r.warning);
            }
        }
        System.out.println();
    }

    private static void cmdListSessions() {
        List<StudySession> list;

        if (planner.getSubjects().isEmpty()) {
            list = planner.getSessions();
        } else {
            System.out.println("Filter by subject? (Enter name or leave blank for all)");
            String filter = prompt("Subject: ");
            if (filter.isBlank()) {
                list = planner.getSessions();
            } else {
                list = planner.getSessions().stream()
                    .filter(ss -> ss.subjectTitle.equalsIgnoreCase(filter))
                    .toList();
            }
        }

        if (list.isEmpty()) {
            System.out.println("No sessions found.");
            return;
        }

        List<StudySession> all = planner.getSessions();
        for (StudySession ss : list) {
            int idx = all.indexOf(ss);
            System.out.printf("[%d] %s%n", idx, ss);
        }
    }

    private static void cmdEditSession() {
        cmdListSessions();
        int idx = promptInt("Session index to edit: ");
        if (idx < 0 || idx >= planner.getSessions().size()) {
            System.out.println("Invalid index.");
            return;
        }

        StudySession ss = planner.getSessions().get(idx);
        System.out.println("Current: " + ss);
        System.out.println("Enter new times (Enter to keep current):");

        LocalDateTime newStart = promptDateTimeOrKeep("New start [" + ss.start.format(StudySession.FMT) + "]: ", ss.start);
        LocalDateTime newEnd   = promptDateTimeOrKeep("New end   [" + ss.end.format(StudySession.FMT) + "]: ", ss.end);

        if (newEnd.isBefore(newStart)) {
            System.out.println("End is before start. Edit cancelled.");
            return;
        }

        planner.editSession(idx, newStart, newEnd);
        System.out.println("Session updated.");
    }

    private static void cmdDeleteSession() {
        cmdListSessions();
        int idx = promptInt("Session index to delete: ");
        if (idx < 0 || idx >= planner.getSessions().size()) {
            System.out.println("Invalid index.");
            return;
        }
        System.out.println("Deleting: " + planner.getSessions().get(idx));
        String confirm = prompt("Confirm? [y/n]: ");
        if (confirm.equalsIgnoreCase("y")) {
            planner.deleteSession(idx);
            System.out.println("Deleted.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    private static void cmdSetCycleDay() {
        System.out.println("Day of week (MONDAY, TUESDAY, ... SUNDAY):");
        String day = prompt("Cycle start day: ").toUpperCase();
        try {
            DayOfWeek d = DayOfWeek.valueOf(day);
            planner.setCycleStartDay(d);
            System.out.println("Cycle start day set to " + d);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid day.");
        }
    }

    private static void cmdListSubjects() {
        if (planner.getSubjects().isEmpty()) {
            System.out.println("No subjects.");
            return;
        }
        planner.getSubjects().forEach(System.out::println);
    }

    // ─── Input Helpers ────────────────────────────────────────────────────────

    private static Subject pickSubject() {
        List<Subject> subs = planner.getSubjects();
        if (subs.isEmpty()) {
            System.out.println("No subjects. Use 'add-subject' first.");
            return null;
        }
        for (int i = 0; i < subs.size(); i++) {
            System.out.printf("[%d] %s%n", i, subs.get(i).title);
        }
        int idx = promptInt("Pick subject number: ");
        if (idx < 0 || idx >= subs.size()) {
            System.out.println("Invalid selection.");
            return null;
        }
        return subs.get(idx);
    }

    private static String prompt(String msg) {
        System.out.print(msg);
        return sc.nextLine().trim();
    }

    private static int promptInt(String msg) {
        try { return Integer.parseInt(prompt(msg)); }
        catch (NumberFormatException e) { return -1; }
    }

    private static double promptDouble(String msg) {
        try { return Double.parseDouble(prompt(msg)); }
        catch (NumberFormatException e) { return 0; }
    }

    private static LocalDateTime promptDateTime(String msg) {
        String input = prompt(msg);
        if (input.isBlank()) return LocalDateTime.now().withSecond(0).withNano(0);
        try {
            return LocalDateTime.parse(input, StudySession.FMT);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid format, using now.");
            return LocalDateTime.now().withSecond(0).withNano(0);
        }
    }

    private static LocalDateTime promptDateTimeOrKeep(String msg, LocalDateTime fallback) {
        String input = prompt(msg);
        if (input.isBlank()) return fallback;
        try {
            return LocalDateTime.parse(input, StudySession.FMT);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid format, keeping original.");
            return fallback;
        }
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  add-subject     Add a new subject with weekly target");
        System.out.println("  log-session     Log a study session for a subject");
        System.out.println("  progress        Show progress for all subjects");
        System.out.println("  list-sessions   List logged sessions");
        System.out.println("  edit-session    Edit a session's start/end time");
        System.out.println("  delete-session  Delete a session");
        System.out.println("  subjects        List all subjects");
        System.out.println("  set-cycle-day   Set the first day of the 7-day cycle");
        System.out.println("  help            Show this help");
        System.out.println("  quit            Exit");
    }
}
