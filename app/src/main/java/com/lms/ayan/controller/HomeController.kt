package com.lms.ayan.controller

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lms.ayan.common.FirestoreCollection.LESSON_PROGRESS_COLLECTION
import com.lms.ayan.common.FirestoreCollection.TOPIC_COLLECTION
import com.lms.ayan.model.ProgressModel
import com.lms.ayan.model.TopicModel
import com.lms.ayan.util.SessionUtil
import com.lms.ayan.util.logD
import com.lms.ayan.util.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "HOME_CONTROLLER"

class HomeController(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val sessionUtil: SessionUtil = SessionUtil.getInstance(context)
) {
    fun getUserName(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val name = sessionUtil.getUserFullName()
            withContext(Dispatchers.Main) {
                callback(name)
            }
        }
    }

    fun logout(callback: () -> Unit) {
        auth.signOut()
        CoroutineScope(Dispatchers.IO).launch {
            sessionUtil.clearSession()
            withContext(Dispatchers.Main) {
                callback()
            }
        }
    }

    fun getLessonProgressCount(userId: String, callback: (Int) -> Unit) {
        db.collection(LESSON_PROGRESS_COLLECTION).whereEqualTo("userId", userId)
            .whereEqualTo("isRead", true).get()
            .addOnSuccessListener {
                context.logD(TAG) { "Lesson progress count ${it.documents.size}" }
                callback(it.documents.size)
            }.addOnFailureListener { ex ->
                context.logE(TAG) { "home::getLessonProgressCount()::Error, error is $ex" }
                callback(0)
            }
    }

    fun getLessonCount(callback: (Int) -> Unit) {
        db.collection(TOPIC_COLLECTION).get().addOnSuccessListener { document ->
            context.logD(TAG) { "Topic count ${document.documents.size}" }
            callback(document.documents.size)
        }.addOnFailureListener { e ->
            context.logE(TAG) { "Error getting lesson count, error is $e" }
            callback(0)
        }
    }

}