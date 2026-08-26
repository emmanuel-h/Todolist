# To-Do List

A simple Android to-do list app built with Kotlin. Organize your tasks into multiple lists and check items off as you go.

## Features

- Create and manage multiple to-do lists
- Add, edit, and delete items within each list
- Check/uncheck items to track completion
- Give a list a target date (📅) or a due date (⏰) and get a reminder at 08:00
- Reorder by dragging; delete by swiping, with a few seconds to undo
- An icon-only interface drawn as ink on ruled paper, in daylight or by lamplight

## Tech Stack

- **Language:** Kotlin
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36
- **Package:** `fr.mandarine.todolist`

## Getting Started

1. Clone the repository
2. Open in Android Studio
3. Run on a device or emulator (Android 7.0+)

## Project Structure

The app is entirely Jetpack Compose — there is no `res/layout/` and no View-system
dependency. The palette, dimensions and motion specs are Kotlin objects in
`ui/paper/`, not resources.

```
app/src/main/
├── java/fr/mandarine/todolist/
│   ├── domain/       # Pure Kotlin: models, repository interfaces, use cases
│   ├── data/         # Room + WorkManager implementations
│   ├── presentation/ # ViewModels and UI state
│   └── ui/           # Compose screens and the paper/ design system
└── res/              # Vector icons, strings, a bare window theme
```

## License

MIT
