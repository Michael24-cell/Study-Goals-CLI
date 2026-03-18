# Study Planner CLI

A minimal Java command-line study planner that tracks study sessions, calculates weekly progress, adjusts required daily averages, and projects when a target will be reached based on current pace.

Built to focus on time-based logic, constraint handling, and clean system design rather than UI frameworks.

---

## Demo

```
$ java Main

> progress Algorithms

Algorithms
-------------------------
Progress:       18 / 40 hrs
Remaining:      22 hrs
Required/day:   4.4 hrs

Projection: Target reached Friday
```

---

## Features

- Track study time by subject
- Set weekly study targets
- Log multiple study sessions per day
- Rolling 7-day study cycle
- Custom cycle start day
- 4:00 AM day boundary instead of midnight
- Automatic progress tracking
- Required hours per remaining day
- Optional daily study limit
- Hard Cap mode (enforces feasibility)
- Planning mode (advisory only)
- Warning when weekly targets become unreachable
- Edit or delete study sessions
- Projection system estimating target completion
- JSON file persistence (auto-save and reload)

---

## Key Design Decisions

- CLI-first design  
  Keeps the focus on logic, calculations, and system behavior rather than UI complexity.

- 4:00 AM day boundary  
  Late-night sessions are attributed to the previous study day.

- Rolling 7-day cycle  
  Avoids rigid calendar weeks and supports flexible scheduling.

- Session attribution by start time  
  Simplifies time-boundary handling.

- Dynamic recalculation  
  All metrics are derived from session data rather than stored totals.

- Lightweight persistence  
  JSON storage avoids the need for a database.

---

## Architecture

```
CLI (Main)
   ↓
StudyPlanner (core logic)
   ↓
Subject + StudySession
   ↓
JSON storage
```

---

## Project Structure

```
src/planner/
├── Main.java
├── StudyPlanner.java
├── Subject.java
├── StudySession.java
```

```
study_data.json   (auto-generated)
```

---

## Running the Program

### Compile

```
javac -d out src/planner/*.java
```

### Run

```
java -cp out planner.Main
```

### Or run pre-built jar

```
java -jar StudyPlanner.jar
```

---

## Commands

```
add-subject      Add a subject (title, weekly target, optional daily limit)
log-session      Log a study session with start/end timestamps
progress         Show cycle progress, required daily hours, and projection
list-sessions    List all sessions (filterable by subject)
edit-session     Modify start/end time of a session
delete-session   Remove a session (auto-recalculates all metrics)
subjects         List all subjects and settings
set-cycle-day    Set the start day of the 7-day cycle
help             Show available commands
quit             Exit the program
```

---

## Purpose

This project was built to practice:

- object-oriented design
- time-based calculations
- constraint systems
- command-line application structure
- data persistence

---

## Future Improvements

- terminal progress bars
- lightweight web interface
- session analytics
- study streak tracking
