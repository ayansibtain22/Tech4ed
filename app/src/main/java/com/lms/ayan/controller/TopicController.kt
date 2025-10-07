package com.lms.ayan.controller

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.lms.ayan.common.FirestoreCollection.TOPIC_COLLECTION
import com.lms.ayan.model.TopicModel
import com.lms.ayan.util.logD
import com.lms.ayan.util.logE

class TopicController(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val tag: String = "TopicController"
    fun getList(subjectID: String, callback: (List<TopicModel?>?) -> Unit) {
        db.collection(TOPIC_COLLECTION).whereEqualTo("subject_id", subjectID).get().addOnSuccessListener {
            val list = mutableListOf<TopicModel>()
            for (document in it) {
                list.add(
                    TopicModel(
                        id = document.id,
                        subjectId = document.get("subject_id").toString(),
                        titleEn = document.get("title_en").toString(),
                        titleUr = document.get("title_ur").toString(),
                        detailEn = document.get("detail_en").toString(),
                        detailUr = document.get("detail_ur").toString(),
                        topicNo = document.get("topic_no").toString().toInt()
                    )
                )
            }
            context.logD(tag) { "List is $list" }
            callback(list.sortedBy { topic -> topic.topicNo })
        }.addOnFailureListener { e ->
            context.logE(tag) { "Error getting list, error is $e" }
            callback(null)
        }
    }
}