package com.gorilla.music.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.gorilla.music.AppContainer
import com.gorilla.music.GorillaApp

/** Pulls the [AppContainer] out of CreationExtras for ViewModel factories. */
val CreationExtras.container: AppContainer
    get() = (this[APPLICATION_KEY] as GorillaApp).container

/** Convenience for building a factory from a single creator lambda. */
inline fun <reified T : androidx.lifecycle.ViewModel> viewModelFactory(
    crossinline creator: (AppContainer) -> T,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    override fun <VM : androidx.lifecycle.ViewModel> create(
        modelClass: Class<VM>,
        extras: CreationExtras,
    ): VM {
        @Suppress("UNCHECKED_CAST")
        return creator(extras.container) as VM
    }
}
