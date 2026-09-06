package fr.mandarine.todolist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Boilerplate remover for constructing a ViewModel that has constructor arguments.
 *
 * `ViewModelProvider` insists on creating the instance itself — that is how it can
 * hand back the *same* instance across a configuration change instead of a fresh
 * one. So a ViewModel with dependencies needs a `Factory`, and writing that object
 * expression out at each of the three call sites in `TodoListsActivity` would be
 * three near-identical blocks. This collapses it to
 * `viewModelFactory { TodoListsViewModel(...) }`.
 *
 * The unchecked cast is safe in practice because each factory is used at exactly
 * one call site, which immediately asks for the type it just constructed.
 */
internal fun viewModelFactory(create: () -> ViewModel): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return create() as T
        }
    }
