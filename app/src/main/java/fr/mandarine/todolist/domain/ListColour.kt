package fr.mandarine.todolist.domain

/**
 * The colour a list is written on, chosen by the reader in the rename dialog.
 *
 * Deliberately a domain enum of *names*, not of `Color` values — `domain/` must
 * never import `android.*` or Compose. The name is what is stored in the database
 * (as text, see `TodoListEntity.colour`) and the actual paper tint for each name
 * is resolved in the UI, in `ui/paper/PaperPalette.kt`, so light and dark rooms can
 * use different tints for the same list.
 *
 * Because the stored value is the enum's `name`, renaming a constant here is a
 * database migration, not a rename.
 */
enum class ListColour {
    None,
    Butter,
    Mint,
    Rose,
    Sky,
    Peach,
    Lilac
}
