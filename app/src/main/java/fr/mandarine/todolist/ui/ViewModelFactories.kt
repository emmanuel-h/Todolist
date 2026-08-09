package fr.mandarine.todolist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal fun viewModelFactory(create: () -> ViewModel): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return create() as T
        }
    }
