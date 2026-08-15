package fr.mandarine.todolist.domain

interface TutorialStateRepository {
    fun isTutorialSeen(): Boolean
    fun markTutorialSeen()
    fun savePendingDemoListId(id: String)
    fun getPendingDemoListId(): String?
    fun clearPendingDemoListId()
}
