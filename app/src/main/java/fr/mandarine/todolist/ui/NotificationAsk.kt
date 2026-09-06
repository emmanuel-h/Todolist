package fr.mandarine.todolist.ui

import android.content.Context
import android.os.Build

/**
 * The app asks for notifications once, at the moment a reminder makes one
 * meaningful, and never again — whether that ask was granted or refused. A
 * refusal is only respected if the record of having asked outlives the process
 * that made it, so it is written to disk rather than held in a field.
 *
 * The record is written when the answer comes back, not when the dialog is
 * raised. Marking it beforehand spent the one ask on a question that may never
 * have been put — the reader could dismiss the window, or the date could have
 * been circled on a line that was never committed to a list at all.
 */
class NotificationAsk(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun alreadyAsked(): Boolean = prefs.getBoolean(KEY_ASKED, false)

    fun markAsked() {
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
    }

    private companion object {
        const val PREFS_NAME = "notification_ask"
        const val KEY_ASKED = "notification_ask_spent"
    }
}

/**
 * Before Android 13 notifications need no permission at all, so there is nothing
 * to ask for and no dialog to interrupt anything with.
 */
internal fun shouldAskForNotifications(sdkInt: Int, granted: Boolean, asked: Boolean): Boolean =
    sdkInt >= Build.VERSION_CODES.TIRAMISU && !granted && !asked

/**
 * Did the reader actually answer the permission dialog?
 *
 * The result alone cannot say. A dialog dismissed by the back key, by an incoming
 * call or by anything else that takes the window away reports exactly what a
 * refusal reports: not granted. Spending the one ask on that leaves a reader who
 * never saw the question routed to the system settings page for the rest of the
 * install.
 *
 * What separates them is the rationale flag, which the platform moves only when
 * the reader chooses: false to true on the first refusal, and true to false on the
 * refusal that makes it permanent. An interruption leaves it where it was.
 */
internal fun answeredTheAsk(
    granted: Boolean,
    rationaleBefore: Boolean,
    rationaleAfter: Boolean
): Boolean = granted || rationaleBefore != rationaleAfter
