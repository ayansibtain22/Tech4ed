package com.lms.ayan.controller

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.lms.ayan.common.FirestoreCollection.QUIZ_COLLECTION
import com.lms.ayan.common.FirestoreCollection.SCORE_COLLECTION
import com.lms.ayan.model.QuizModel
import com.lms.ayan.model.ScoreModel
import com.lms.ayan.util.logD
import com.lms.ayan.util.logE

class QuizListController(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val tag: String = "QuizListController"
    fun getList(callback: (List<QuizModel?>?) -> Unit) {
        db.collection(QUIZ_COLLECTION).get().addOnSuccessListener {doc->
            val quizzes = mutableListOf<QuizModel>()
            for (document in doc) {
                quizzes.add(
                    QuizModel(
                        id = document.id,
                        nameEn = document.get("name_en").toString(),
                        nameUr = document.get("name_ur").toString(),
                        order = document.get("order").toString().toInt(),
                        topicID = document.get("topic_id").toString()
                    )
                )
            }
            context.logD(tag) { "List is $quizzes" }
            callback(quizzes.sortedBy { it.order })
        }.addOnFailureListener { e ->
            context.logE(tag) { "Error getting list, error is $e" }
            callback(null)
        }
    }

    fun getScore(userId: String, callback: (List<ScoreModel>?) -> Unit) {
        db.collection(SCORE_COLLECTION)
            .whereEqualTo("userId", userId)
            .get().addOnSuccessListener {
                if (it.documents.isEmpty()) {
                    context.logD(tag) { "getScore()::No such document" }
                    callback(null)
                } else {
                    it.documents.firstOrNull()?.let { document ->
                        context.logD(tag) { "getScore()::Document found: ${document.data}" }
                        val list = mutableListOf<ScoreModel>()
                        for (doc in it) {
                            list.add(
                                ScoreModel(
                                    id = doc.id,
                                    userId = doc.get("userId").toString(),
                                    quizId = doc.get("quizId").toString(),
                                    answeredList = document.get("answeredList") as List<String>,
                                    totalScore = doc.get("totalScore").toString().toInt(),
                                    obtainedScore = doc.get("obtainedScore").toString().toInt()
                                )
                            )
                        }
                        callback(list)
                    }
                }
            }.addOnFailureListener { ex ->
                context.logE(tag) { "getScore()::Error getScore, error is $ex" }
                callback(null)
            }
    }
}