package fr.mandarine.todolist.data

import android.content.Context
import fr.mandarine.todolist.domain.TutorialStateRepository

class SharedPreferencesTutorialStateRepository(context: Context) : TutorialStateRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isTutorialSeen(): Boolean = prefs.getBoolean(KEY_SEEN, false)

    override fun markTutorialSeen() {
        prefs.edit().putBoolean(KEY_SEEN, true).apply()
    }

    override fun savePendingDemoListId(id: String) {
        prefs.edit().putString(KEY_DEMO_LIST_ID, id).apply()
    }

    override fun getPendingDemoListId(): String? = prefs.getString(KEY_DEMO_LIST_ID, null)

    override fun clearPendingDemoListId() {
        prefs.edit().remove(KEY_DEMO_LIST_ID).apply()
    }

    companion object {
        private const val PREFS_NAME = "tutorial_state"
        private const val KEY_SEEN = "tutorial_seen"
        private const val KEY_DEMO_LIST_ID = "tutorial_demo_list_id"
    }
}
