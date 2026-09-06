# Code tour

A map of this codebase for someone who programs, but not in Kotlin or Compose.

It has four parts: the **Kotlin** you need, the **Compose** you need, **where things
live**, and a few **walkthroughs** that follow one change all the way through.

For *what the app is supposed to do*, read [SPEC.md](SPEC.md) — that is the
authoritative product definition, and this document deliberately does not repeat it.
For *why a particular thing looks the way it does*, read the KDoc on the class
itself; the prose comments in `ui/` are the design record.

---

## 1. The Kotlin you need

Nine idioms account for almost everything unfamiliar in this repo.

### `val` / `var`

`val` is a binding that cannot be reassigned (Java's `final`), `var` can be. Almost
everything here is `val`. Note this is about the *reference*, not deep immutability.

### `data class` — a record

```kotlin
data class TodoItem(val id: String, val title: String, val isCompleted: Boolean = false)
```

The compiler generates `equals`, `hashCode`, `toString`, and `copy()`. A "change" is
a new instance:

```kotlin
val done = item.copy(isCompleted = true)
```

This matters for Compose, which decides whether to redraw by comparing old and new
values — that only works if values cannot mutate behind its back. Every model in
`domain/` is a `data class` with `val` fields.

### `sealed class` — a closed set

```kotlin
sealed class TodoListState {
    data object NotFound : TodoListState()
    data object Empty : TodoListState()
    data class Content(val activeItems: List<TodoItem>, ...) : TodoListState()
}
```

All subclasses must be declared in the same file, so the compiler knows the complete
set. A `when` over a sealed type needs no `else`, and adding a case makes every
`when` that handles it fail to compile until updated. Used for every UI state and for
`AnimationEvent` and `ListNotification`.

`data object` is a singleton — the right shape for a state that carries no payload.

### `operator fun invoke` — the use-case idiom

Every class in `domain/` ending in `UseCase` looks like this:

```kotlin
class AddTodoUseCase(private val repository: TodoRepository) {
    operator fun invoke(title: String, listId: String): TodoItem { ... }
}
```

`operator` on a method named `invoke` lets you call the object like a function:

```kotlin
addTodo(title, listId)          // sugar for addTodo.invoke(title, listId)
```

Why a class rather than a plain function? The constructor. Dependencies are injected
once (in `TodoListsActivity`), and the call site then passes only what varies.

### `fun interface` — a lambda-shaped interface

An interface with exactly one abstract method, so a lambda can be passed where one is
expected. `ListNotifier` and `NotificationScheduler` are both this — convenient in
tests.

### Nullability, `?.`, `?:`, and `let`

Types are non-null by default; `String?` may be null. Then:

- `a?.b` — call `b` only if `a` is non-null, otherwise the whole expression is null
- `a ?: b` — use `b` if `a` is null (Elvis operator)
- `a?.let { ... }` — run the block with `a` as `it`, only if non-null

Chained in `AddTodoUseCase`:

```kotlin
val position = repository.getAllByListId(listId)
    .filter { !it.isCompleted }
    .lastOrNull()      // null if the list has no active items
    ?.position
    ?.plus(1) ?: 0     // one past the last, or 0
```

**Smart casts**: after `if (x != null)` the compiler treats `x` as non-null inside
the branch — but only for a `val` it can prove cannot change. This is why the models
being immutable makes the code read more cleanly.

### `by` — property delegation

```kotlin
private val database by lazy { databaseFactory(context) }
var addRowText by mutableStateOf("")
```

`by` routes reads and writes of the property through a delegate object.
`lazy` computes once on first read, thread-safely. `mutableStateOf` is the Compose
one — see below; it is what makes assignment to a plain-looking property trigger a
redraw.

### Trailing lambdas and `it`

If the last parameter is a function, it goes outside the parentheses:

```kotlin
scope.launch(dispatcher) { ... }     // launch(dispatcher, block = { ... })
items.filter { !it.isCompleted }     // `it` is the implicit single parameter
```

A composable's `content` parameter is a lambda, which is why UI code reads as nested
blocks rather than nested calls.

### Extension functions

```kotlin
fun ComponentActivity.drawEdgeToEdge() { ... }
fun Modifier.penStrike(state: PenStrikeState, color: Color): Modifier = ...
```

A function declared *on* a type without modifying it, with `this` bound to the
receiver. Every custom modifier in `ui/paper/` is an extension on `Modifier`, which is
why they chain with the built-in ones.

### Coroutines, in one paragraph

`suspend` marks a function that can pause without blocking a thread. A coroutine runs
in a **scope** (`viewModelScope`, `lifecycleScope`) and is cancelled when its scope
dies. A **dispatcher** decides which thread it runs on. In this app: repository calls
are ordinary blocking functions, and the ViewModels wrap them in
`launch(databaseDispatcher)` — a single-threaded dispatcher, which conveniently
serialises every read and write against each other.

- **`StateFlow`** — always holds a current value; a new collector immediately gets it.
  Used for screen state.
- **`SharedFlow`** — has no current value; a late collector gets nothing. Used for
  one-off events (`animationEvents`) that must not be replayed after a rotation.

---

## 2. The Compose you need

### There is no view hierarchy

There is no `res/layout/`, no XML, no `findViewById`, no `RecyclerView`, no
`AppCompatActivity`. A `@Composable` function *is* the UI:

```kotlin
@Composable
fun TodoRow(item: TodoItem, checked: Boolean, onToggle: () -> Unit) { ... }
```

It returns nothing and holds nothing. It receives what to draw and reports what
happened. This is the "**state down, events up**" rule and it is followed everywhere
in this codebase — a composable here never calls a repository.

### Recomposition

When state a composable *read* changes, Compose re-runs that function. Not the whole
tree — just the parts that read the thing that changed. Consequences:

- A composable may run many times per second, in any order, on any thread.
- It must have no side effects. Don't start a coroutine, don't write a file, don't
  ask what today's date is. (That last one is exactly why `GetTodoListsWithStatusUseCase`
  exists: all date arithmetic is done once, outside the UI.)
- Side effects go in an **effect**: `LaunchedEffect(key)` runs a coroutine when it
  enters the composition and restarts if `key` changes; `DisposableEffect` also gives
  you a cleanup block.

### `remember` and state

```kotlin
var text by remember { mutableStateOf("") }
```

- `mutableStateOf` creates an observable value. Reading it *inside* a composable
  subscribes that composable to it; writing it schedules a recomposition.
- `remember` keeps a value across recompositions of the same composable.
- `rememberSaveable` additionally survives rotation and process death — it needs to
  know how to serialise the value, which is what a `Saver` is (see
  `TodoListScreenState.Saver`).

`remember(key)` re-computes when `key` changes. This is load-bearing in lists: keying
on the item id is what stops a recycled row inheriting the previous item's state.

### `Modifier`

```kotlin
Modifier
    .fillMaxWidth()
    .seatOnRule()
    .penStrike(strike, ink)
    .clickable(onClick = onEditRequested)
```

A modifier is size, layout, drawing, and input, expressed as a chain. **Order
matters**: `.padding().background()` paints a smaller area than
`.background().padding()`.

By convention every composable takes a `modifier: Modifier = Modifier` parameter and
applies it to its outermost element, so callers can position it without it knowing
where it lands.

### `CompositionLocal` — the implicit parameter

Threading the palette through forty functions would be unbearable, so:

```kotlin
CompositionLocalProvider(LocalPaperPalette provides palette) { content() }
// ... anywhere below:
val palette = LocalPaperPalette.current
```

`PaperTheme` provides six of these and they *are* the design system. See §3.

### `LazyColumn`

The `RecyclerView` equivalent: only composes what is on screen. Always pass a stable
**key** per item:

```kotlin
// as this app does it, in TodoListsScreen.kt:
itemsIndexed(summaries, key = { _, it -> it.list.id }) { ... }
```

Without a key, Compose identifies items by position, so inserting at the top makes
every row believe it became a different list — animations run on the wrong rows and
per-row state follows the wrong item.

### Where the state actually lives here

Three tiers, and knowing which is which is most of navigating this app:

| Tier | Where | Survives rotation? | Example |
|---|---|---|---|
| Stored data | `TodoListViewModel` → repository → SQLite | yes (it's on disk) | the items |
| Screen state | `TodoListScreenState`, `TodoListsScreenState` | partly, via `Saver` | half-typed text |
| Draw state | `remember` inside a composable | no | a strike-through's progress |

---

## 3. Where things live

```
app/src/main/java/fr/mandarine/todolist/
├── AppContainer.kt          ← the composition root: who implements what
├── TodoListApplication.kt   ← owns the container
├── DailyNotificationWork.kt ← the Android Worker shell
├── domain/                  ← pure Kotlin. No android.*, no Compose.
├── data/                    ← Room, SharedPreferences, WorkManager, notifications
├── presentation/            ← ViewModels + UI state sealed classes
└── ui/                      ← all Compose
    ├── paper/               ← the design system
    ├── todolists/           ← screen 1, the page of lists
    ├── todolist/            ← screen 2, the items on one list
    ├── nav/                 ← the back stack and the transition between the two
    ├── reorder/             ← drag-to-reorder, shared by both screens
    └── listmeta/            ← the tally and the date jot, shared by both screens
```

**The rule**: a class must not import from a layer above it, and `domain/` must not
import `android.*`. `AppContainer` is the single exception — it knows about all of
them, which is what lets, say, the notifier be told which window to open without
`data/` importing `ui/`.

### `domain/` — the vocabulary

Nothing here knows about Android, storage, or screens; it is unit-testable with no
emulator. Three kinds of file:

- **Models** — `TodoItem`, `TodoList`, `TodoListSummary`, `TodoCounts`, plus the
  enums `ListColour` and `DueDateStatus`.
- **Repository interfaces** — `TodoRepository`, `TodoListRepository`,
  `ReminderTimeRepository`, `Clock`, `ListNotifier`, `NotificationScheduler`.
  Declared here, implemented in `data/`. This inversion is the whole point of the
  layer split.
- **Use cases** — one operation each, all with `operator fun invoke`. Validation
  (`require(...)`) lives here, at the boundary.

The one that repays reading is **`GetTodoListsWithStatusUseCase`** — the only place
in the app that decides how a list "stands" today.

### `data/` — the implementations

- **Room**: `TodoDatabase` (version 8, with the full migration ladder),
  `TodoListDao` / `TodoItemDao` (SQL, checked at compile time),
  `TodoListEntity` / `TodoItemEntity` (table rows), and
  `RoomTodoListRepository` / `RoomTodoRepository` (which map entity ⇄ domain).
- `SharedPreferencesReminderTimeRepository` — the one setting.
- `AndroidListNotifier` — builds real notifications.
- `WorkManagerNotificationScheduler` — books the daily check.

Entities are separate types from domain models even where the fields match, so the
storage shape can change without the rest of the app knowing.

### `presentation/` — the ViewModels

Two screens, two ViewModels, plus `ReminderSettingsViewModel`. Both main ones follow
one shape and it is worth internalising:

> **write → re-read everything → publish a new state**

There is no observable query and no optimistic update. `refresh()` is that same cycle
with an empty write. For a notebook-sized dataset the full re-read costs nothing and
the screen can never drift from what is stored.

### `ui/paper/` — the design system

This is the largest and least obvious part of the codebase. It is a **paper
metaphor**, built out of Compose primitives, and it replaces most of what an app
would normally take from Material.

Entry points, in the order you'd want them:

| File | What you'd change there |
|---|---|
| `PaperPalette.kt` | any colour, light and dark together |
| `PaperDimens.kt` | any fixed size (there is no `dimens.xml`) |
| `PaperType.kt` | the type scale and the handwriting font |
| `PaperMotion.kt` | any animation's speed — five springs, and everything uses one |
| `PaperTheme.kt` | what gets provided to the whole tree |
| `PaperPreviews.kt` | try a change in the IDE without running the app |

The rest are primitives: `RuledPage` and `RuledRow` (the ground and the line),
`InkRing` (the checkbox), `PenStrike` (the strike-through), `InkIcon`, `TearOff`,
`StickyNotePad`, `PaperCalendar`, `PaperDialog`, and so on.

Two conventions to know:
- **`InkTone`** — code asks for a *role* (`palette.inked(InkTone.Margin)`), not a
  colour, so light and dark stay coherent.
- **Custom modifiers** — `seatOnRule()`, `penStrike()`, `tearOff()`, `paperRuling()`
  are `Modifier` extensions and chain like any other.

### `ui/nav/` — one window, two pages

Navigation 3, not Fragments and not the older Navigation component. `PaperRoutes.kt`
declares the two keys; `TodoListsActivity` owns a `NavBackStack`; `PageStack.kt` maps
each key to a screen and defines the two transitions. `PageTravel.kt` is what makes a
list's name physically travel from a row into the next page's header.

### Outside `java/`

- `app/src/main/res/values/strings.xml` and `values-fr/` — all text, both languages.
  Both must be updated together.
- `values/colors.xml`, `themes.xml`, `integers.xml` — window themes only. The app
  palette is Kotlin, not resources.
- `app/schemas/` — exported Room schemas, checked in; migration tests compare against
  them.
- `app/src/test/` — 125 test files, mirroring the main source layout.

---

## 4. Walkthroughs

### Tapping a row to open a list

1. `TodoListRow` calls its `onOpen` lambda.
2. `TodoListsScreen` forwards it as `onOpenList(summary.list)`; `ListsPage` in
   `PageStack.kt` receives it and calls `stage.open(list)`.
3. `NavStage.open` pushes `ItemsRoute(list.id)` onto the `NavBackStack`.
4. `NavDisplay` sees a new top key and composes the `ItemsRoute` entry, running
   `pageArrives()` — the new sheet rises and fades in over the page of lists, which
   is *held* rather than removed.
5. `ItemsPage` constructs a `TodoListViewModel` for that id (via the factory the
   activity supplied) and a `TodoListScreenState` remembered against the id.
6. `LaunchedEffect(viewModel) { viewModel.refresh() }` triggers the first read.
7. The list's name, marked `Modifier.travellingName(listId)` on both pages, animates
   from the row into the new page's head rule.

Back reverses it: `stage.leave()` pops the stack and `pagePeels()` slides the sheet
off sideways.

### Ticking an item

Worth following, because the database is deliberately *not* the first thing to change.

1. `InkRing` reports a tap; `TodoListScreen.requestToggle` runs.
2. It calls `screenState.startToggle(id)` — adding the id to `pendingToggles`. It does
   **not** call the ViewModel.
3. `TodoListScreenState.inked(item)` now returns the opposite of the stored value, so
   the ring inks and `PenStrike` begins drawing. The reader sees the tick immediately.
4. A `LaunchedEffect` keyed on that id waits `INK_TICK_MILLIS + INK_STRIKE_MILLIS`,
   then calls `finishToggle(id)` and `onToggle(id)` for real.
5. `TodoListViewModel.toggleTodo` runs `ToggleTodoUseCase` on the database dispatcher,
   then re-reads and publishes a new `TodoListState`.
6. `RoomTodoRepository.toggle` stamps `completedAt` and freezes the item's position
   (un-ticking instead clears the stamp and sends it to the foot of the active run).
7. The ViewModel emits `AnimationEvent.ItemCompleted` — and, if that tick emptied the
   list, `ListCompleted` immediately after it.
8. `ItemsPage` collects `ListCompleted` and sets `screenState.finishedOn`, which is
   what launches the flourish, thrown from `screenState.lastTouch`.

If animations are off, step 2 is skipped and step 4 happens at once.

### Adding a field to a list — e.g. a "notes" string

This is the change that touches the most layers. Seven places, in order:

1. **`domain/TodoList.kt`** — add the field (and any invariant to its `init`).
2. **`domain/TodoListRepository.kt`** — add it to `update(...)`.
3. **`data/TodoListEntity.kt`** — add the column.
4. **`data/TodoDatabase.kt`** — bump `version` to 9, add `MIGRATION_8_9` with the
   `ALTER TABLE`, and list it in `addMigrations(...)`. **Skipping this crashes every
   existing install on upgrade.**
5. **`data/TodoListDao.kt`** — add it to the `@Query` for `update`.
6. **`data/RoomTodoListRepository.kt`** — map it both ways.
7. **`domain/CreateTodoListUseCase.kt` / `EditTodoListUseCase.kt`** — accept and pass
   it; add validation if needed.

Then the UI: `TodoListSummary` if the row needs it derived, `TodoListsViewModel`,
whichever composable draws it, and `strings.xml` in **both** `values/` and
`values-fr/`.

Finally the gates: `domain/`, `data/` and `presentation/` are held at 100% line,
branch and mutation coverage, so every new branch needs a test. See below.

### Adding a screen

1. Add a key to `ui/nav/PaperRoutes.kt` (a `data class` if it carries an argument —
   remember it must be a serialisable value, not a domain object).
2. Add an `entry<YourRoute>` block in `PageStack.kt`'s `entryProvider`.
3. Push it with `backStack.add(YourRoute(...))`.
4. If it needs a ViewModel with arguments, follow the `itemsViewModelFactory` pattern
   in `TodoListsActivity`.

---

## 5. Building and the quality gates

```bash
./gradlew testDebugUnitTest                        # unit tests
./gradlew createDebugUnitTestCoverageReport        # JaCoCo
./gradlew pitest                                   # mutation testing
./gradlew :app:lintDebug                           # lint — NewApi is FATAL
./gradlew assembleDebug                            # APK
```

Three things will bite you:

- **Coverage and mutation are gates, not reports.** `domain/`, `data/` and
  `presentation/` must stay at 100% line, branch *and* mutation score. `ui/` is not
  measured at all — so a green gate is **not** evidence that a UI change is tested.
- **`NewApi` lint is fatal.** The app targets `minSdk 24` but is written in
  `java.time`, which works only because core library desugaring is enabled. Lint is
  what keeps that honest.
- **`pitest` has traps.** Data-class getters survive mutation if tests only compare
  whole instances (assert individual properties instead), and test helper classes must
  be *nested inside* the test class or they become mutation targets. `CLAUDE.md` has
  the full list.

## Where to read next

- [SPEC.md](SPEC.md) — what the app does, screen by screen, and what is not built yet
- [index.md](index.md) — one document per feature, written as each shipped
- [paper-design-system.md](paper-design-system.md) — the design system in depth
- [motion-and-haptics.md](motion-and-haptics.md) — the springs and what uses them
