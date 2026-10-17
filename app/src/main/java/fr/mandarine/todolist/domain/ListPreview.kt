package fr.mandarine.todolist.domain

/**
 * The titles of a list's first few open items, in position order.
 *
 * Used by the page of lists to draw a card preview of each list's content
 * without loading every item. The [titles] list is at most `PREVIEW_ITEMS`
 * entries long, or shorter when the list has fewer open items than that.
 *
 * Lists with no open items are not included in [TodoRepository.previewsByList]
 * at all — there is nothing to preview.
 */
data class ListPreview(
    val listId: String,
    val titles: List<String>
)
