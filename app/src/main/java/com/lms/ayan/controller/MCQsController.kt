package com.lms.ayan.controller

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.lms.ayan.common.FirestoreCollection.MCQ_COLLECTION
import com.lms.ayan.common.FirestoreCollection.SCORE_COLLECTION
import com.lms.ayan.common.FirestoreCollection.USER_COLLECTION
import com.lms.ayan.model.MCQModel
import com.lms.ayan.model.QuizModel
import com.lms.ayan.model.ScoreModel
import com.lms.ayan.util.logD
import com.lms.ayan.util.logE

class MCQsController(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val tag: String = "MCQsController"
    fun getList(quizID: String, callback: (List<MCQModel?>?) -> Unit) {
        db.collection(MCQ_COLLECTION).whereEqualTo("quiz_id", quizID).get().addOnSuccessListener {
            val list = mutableListOf<MCQModel>()
            for (document in it) {
                list.add(
                    MCQModel(
                        id = document.id,
                        questionEn = document.get("question_en").toString(),
                        questionUr = document.get("question_ur").toString(),
                        optionEn = document.get("options_en") as Map<String?, String?>?,
                        optionUr = document.get("options_ur") as Map<String?, String?>?,
                        answer = document.get("ans").toString(),
                        quizId = document.get("quiz_id").toString(),
                        questionNumber = document.get("q_no").toString().toInt(),
                        imageURL = document.get("image_url").toString()
                    )
                )
            }
            context.logD(tag) { "List is $list" }
            callback(list.sortedBy { mcq -> mcq.questionNumber })
        }.addOnFailureListener { e ->
            context.logE(tag) { "Error getting list, error is $e" }
            callback(null)
        }
    }

    fun saveScore(scoreModel: ScoreModel, callback: (Boolean) -> Unit) {
        val score = hashMapOf(
            "id" to "",
            "userId" to scoreModel.userId,
            "quizId" to scoreModel.quizId,
            "answeredList" to scoreModel.answeredList,
            "totalScore" to scoreModel.totalScore,
            "obtainedScore" to scoreModel.obtainedScore
        )
        db.collection(SCORE_COLLECTION).add(score).addOnSuccessListener { documentReference ->
            context.logD(tag) { "saveScore()::DocumentSnapshot added with ID: ${documentReference.id}" }
            db.collection(SCORE_COLLECTION).document(documentReference.id)
                .update("id", documentReference.id).addOnSuccessListener {
                    context.logD(tag) { "updateScoreID::DocumentSnapshot added with ID: ${documentReference.id}" }
                    callback(true)
                }.addOnFailureListener { e ->
                    context.logE(tag) { "updateScoreID::Error adding document, error is $e" }
                    callback(false)
                }
        }.addOnFailureListener { e ->
            context.logE(tag) { "Add score::Error adding document, error is $e" }
            callback(false)
        }
    }

    fun updateScore(scoreModel: ScoreModel, callback: (Boolean) -> Unit) {
        val score = hashMapOf(
            "answeredList" to scoreModel.answeredList,
            "totalScore" to scoreModel.totalScore,
            "obtainedScore" to scoreModel.obtainedScore
        )
        db.collection(SCORE_COLLECTION).document(scoreModel.id ?: "").update(score)
            .addOnSuccessListener { documentReference ->
                context.logD(tag) { "updateScore()::DocumentSnapshot added with ID: ${scoreModel.id}" }
                callback(true)
            }.addOnFailureListener { e ->
                context.logE(tag) { "updateScore()::Error update document, error is $e" }
                callback(false)
            }
    }

    fun getScore(userId: String, quizId: String, callback: (ScoreModel?) -> Unit) {
        db.collection(SCORE_COLLECTION).whereEqualTo("userId", userId)
            .whereEqualTo("quizId", quizId).get().addOnSuccessListener {
                if (it.documents.isEmpty()) {
                    context.logD(tag) { "getScore()::No such document" }
                    callback(null)
                }else{
                    it.documents.firstOrNull()?.let { document ->
                        context.logD(tag) { "getScore()::Document found: ${document.data}" }
                        callback(
                            ScoreModel(
                                id = document.id,
                                userId = document.get("userId").toString(),
                                quizId = document.get("quizId").toString(),
                                answeredList = document.get("answeredList") as List<String>,
                                totalScore = document.get("totalScore").toString().toInt(),
                                obtainedScore = document.get("obtainedScore").toString().toInt()
                            )
                        )
                    }
                }
            }.addOnFailureListener { ex ->
                context.logE(tag) { "getScore()::Error getScore, error is $ex" }
                callback(null)
            }
    }
}