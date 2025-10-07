package com.lms.ayan.controller

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.lms.ayan.common.FirestoreCollection.LESSON_PROGRESS_COLLECTION
import com.lms.ayan.common.FirestoreCollection.TOPIC_COLLECTION
import com.lms.ayan.model.ProgressModel
import com.lms.ayan.model.TopicModel
import com.lms.ayan.util.logD
import com.lms.ayan.util.logE

class LessonController(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val tag: String = "LessonController"
    fun getLesson(lessonID: String, callback: (TopicModel?) -> Unit) {
        db.collection(TOPIC_COLLECTION).document(lessonID).get().addOnSuccessListener { document ->
            val topic = TopicModel(
                id = document.id,
                subjectId = document.get("subject_id").toString(),
                titleEn = document.get("title_en").toString(),
                titleUr = document.get("title_ur").toString(),
                detailEn = document.get("detail_en").toString(),
                detailUr = document.get("detail_ur").toString(),
                topicNo = document.get("topic_no")?.toString()?.toInt() ?: 0
            )
            context.logD(tag) { "Topic is $topic" }
            callback(topic)
        }.addOnFailureListener { e ->
            context.logE(tag) { "Error getting lesson, error is $e" }
            callback(null)
        }
    }

    fun saveProgress(progressModel: ProgressModel, callback: (Boolean) -> Unit) {
        val progress = hashMapOf(
            "id" to "",
            "userId" to progressModel.userId,
            "topicId" to progressModel.topicId,
            "isRead" to progressModel.isRead
        )
        db.collection(LESSON_PROGRESS_COLLECTION).add(progress)
            .addOnSuccessListener { documentReference ->
                context.logD(tag) { "saveProgress()::DocumentSnapshot added with ID: ${documentReference.id}" }
                db.collection(LESSON_PROGRESS_COLLECTION).document(documentReference.id)
                    .update("id", documentReference.id).addOnSuccessListener {
                        context.logD(tag) { "updateProgressID::DocumentSnapshot added with ID: ${documentReference.id}" }
                        callback(true)
                    }.addOnFailureListener { e ->
                        context.logE(tag) { "updateProgressID::Error adding document, error is $e" }
                        callback(false)
                    }
            }.addOnFailureListener { e ->
                context.logE(tag) { "Add Progress::Error adding document, error is $e" }
                callback(false)
            }
    }

    fun updateProgress(progressModel: ProgressModel, callback: (Boolean) -> Unit) {
        db.collection(LESSON_PROGRESS_COLLECTION).document(progressModel.id ?: "")
            .update("isRead", progressModel.isRead)
            .addOnSuccessListener { documentReference ->
                context.logD(tag) { "updateProgress()::DocumentSnapshot added with ID: ${progressModel.id}" }
                callback(true)
            }.addOnFailureListener { e ->
                context.logE(tag) { "updateProgress()::Error update document, error is $e" }
                callback(false)
            }
    }

    fun getProgress(userId: String, topicId: String, callback: (ProgressModel?) -> Unit) {
        db.collection(LESSON_PROGRESS_COLLECTION).whereEqualTo("userId", userId)
            .whereEqualTo("topicId", topicId).get().addOnSuccessListener {
                if (it.documents.isEmpty()) {
                    context.logD(tag) { "getProgress()::No such document" }
                    callback(null)
                } else {
                    it.documents.firstOrNull()?.let { document ->
                        context.logD(tag) { "getProgress()::Document found: ${document.data}" }
                        callback(
                            ProgressModel(
                                id = document.id,
                                userId = document.get("userId").toString(),
                                topicId = document.get("topicId").toString(),
                                isRead = document.get("isRead") as Boolean
                            )
                        )
                    }
                }
            }.addOnFailureListener { ex ->
                context.logE(tag) { "getProgress()::Error getScore, error is $ex" }
                callback(null)
            }
    }
}