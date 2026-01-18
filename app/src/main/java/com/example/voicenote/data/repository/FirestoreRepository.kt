package com.example.voicenote.data.repository

import android.net.Uri
import android.util.Log
import com.example.voicenote.data.model.*
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File

class FirestoreRepository {
    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private val notesCollection = db.collection("notes")
    private val tasksCollection = db.collection("tasks")
    private val configCollection = db.collection("config")
    private val usersCollection = db.collection("users")

    // --- User Management ---
    suspend fun saveUser(user: User) {
        if (user.deviceId.isEmpty()) return
        usersCollection.document(user.deviceId).set(user).await()
    }

    suspend fun getUserByDeviceId(deviceId: String): User? {
        return try {
            val doc = usersCollection.document(deviceId).get().await()
            if (doc.exists()) doc.toObject(User::class.java) else null
        } catch (e: Exception) {
            null
        }
    }

    fun getUserFlow(deviceId: String): Flow<User?> = callbackFlow {
        val subscription = usersCollection.document(deviceId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { subscription.remove() }
    }

    // --- Audio Upload ---
    suspend fun uploadAudio(userId: String, file: File): String? {
        return try {
            val ref = storage.reference.child("audio/$userId/${file.name}")
            ref.putFile(Uri.fromFile(file)).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("Storage", "Upload failed", e)
            null
        }
    }

    // --- Config Management ---
    fun getAppConfig(): Flow<AppConfig?> = callbackFlow {
        val subscription = configCollection.document("settings")
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(AppConfig::class.java))
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateAppConfig(config: AppConfig) {
        configCollection.document("settings").set(config).await()
    }

    suspend fun rotateApiKey() {
        val configDoc = configCollection.document("settings").get().await()
        val config = configDoc.toObject(AppConfig::class.java) ?: return
        if (config.apiKeys.isEmpty()) return
        
        val nextIndex = (config.currentKeyIndex + 1) % config.apiKeys.size
        configCollection.document("settings").update("currentKeyIndex", nextIndex).await()
    }

    // --- Notes Management ---
    fun getNotes(): Flow<List<Note>> = callbackFlow {
        val subscription = notesCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepository", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notes = snapshot.documents.mapNotNull { doc ->
                        try {
                            val note = doc.toObject(Note::class.java)
                            val isDeleted = doc.getBoolean("isDeleted") ?: false
                            if (note != null && !isDeleted) {
                                note.copy(id = doc.id)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(notes)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getNoteById(noteId: String): Flow<Note?> = callbackFlow {
        val subscription = notesCollection.document(noteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObject(Note::class.java)?.copy(id = snapshot.id))
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveNote(note: Note) {
        val docRef = if (note.id.isEmpty()) notesCollection.document() else notesCollection.document(note.id)
        docRef.set(note.copy(id = docRef.id, updatedAt = System.currentTimeMillis())).await()
    }

    suspend fun updateStatus(noteId: String, status: NoteStatus) {
        notesCollection.document(noteId).update("status", status).await()
    }

    suspend fun softDeleteNote(noteId: String) {
        notesCollection.document(noteId).update(
            "isDeleted", true,
            "deletedAt", System.currentTimeMillis()
        ).await()
    }

    suspend fun restoreNote(noteId: String) {
        notesCollection.document(noteId).update(
            "isDeleted", false,
            "deletedAt", null
        ).await()
    }

    suspend fun deleteNotes(noteIds: List<String>) {
        val batch = db.batch()
        noteIds.forEach { id ->
            batch.update(notesCollection.document(id), "isDeleted", true, "deletedAt", System.currentTimeMillis())
        }
        batch.commit().await()
    }

    // --- Tasks Management ---
    fun getTasksForNote(noteId: String): Flow<List<Task>> = callbackFlow {
        val subscription = tasksCollection
            .whereEqualTo("noteId", noteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val tasks = snapshot.toObjects(Task::class.java).filter { !it.isDeleted }
                    trySend(tasks)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getAllTasks(): Flow<List<Task>> = callbackFlow {
        val subscription = tasksCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val tasks = snapshot.toObjects(Task::class.java).filter { !it.isDeleted }
                    trySend(tasks)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addTask(task: Task) {
        val docRef = tasksCollection.document()
        docRef.set(task.copy(id = docRef.id)).await()
    }

    suspend fun updateTask(task: Task) {
        if (task.id.isEmpty()) return
        tasksCollection.document(task.id).set(task).await()
    }

    suspend fun updateTaskStatus(taskId: String, isDone: Boolean) {
        tasksCollection.document(taskId).update("isDone", isDone).await()
    }

    suspend fun softDeleteTask(taskId: String) {
        tasksCollection.document(taskId).update(
            "isDeleted", true,
            "deletedAt", System.currentTimeMillis()
        ).await()
    }

    suspend fun restoreTask(taskId: String) {
        tasksCollection.document(taskId).update(
            "isDeleted", false,
            "deletedAt", null
        ).await()
    }

    suspend fun deleteTasks(taskIds: List<String>) {
        val batch = db.batch()
        taskIds.forEach { id ->
            batch.update(tasksCollection.document(id), "isDeleted", true, "deletedAt", System.currentTimeMillis())
        }
        batch.commit().await()
    }
}
