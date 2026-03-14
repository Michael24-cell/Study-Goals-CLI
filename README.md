# Study Planner

Plain Java CLI. No external dependencies. Java 21.

## Run (pre-built jar)
```
java -jar StudyPlanner.jar
```

## Compile from source
```
javac -d out src/planner/*.java
java -cp out planner.Main
```

## Commands
```
add-subject      Add a subject (title, weekly target, optional daily limit)
log-session      Log a study session with start/end timestamps
progress         Show cycle progress, required daily hours, and projection
list-sessions    List all sessions (filterable by subject)
edit-session     Change start/end time of a logged session
delete-session   Remove a session (recalculates everything automatically)
subjects         List all subjects and their settings
set-cycle-day    Choose which weekday starts your 7-day rolling cycle
help             Show command list
quit             Exit
```

## Notes
- Days start at **04:00** — sessions before 4 AM belong to the previous study day
- Cycle is a rolling **7-day window** starting on your chosen weekday
- `HARD_CAP` limit mode triggers a warning if the weekly target is unreachable given your daily cap
- `PLANNING` limit mode is advisory only — no feasibility blocking
- Data saved automatically to `study_data.json` after every change
