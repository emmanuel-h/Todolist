package fr.mandarine.todolist.ui.paper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import java.util.Locale

private val processLocale: Locale
    get() = Locale.getDefault(Locale.Category.FORMAT)

/**
 * The hand every date is written in.
 *
 * Read from the configuration rather than from [Locale.getDefault] so that a
 * language chosen in the per-app picker redraws the dates on the page, instead
 * of leaving yesterday's pattern in ink until the process is restarted. The
 * process locale stands behind it for the configuration that names none.
 */
val formatLocale: Locale
    @Composable
    @ReadOnlyComposable
    get() = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: processLocale
