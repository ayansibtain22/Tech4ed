package com.lms.ayan.controller

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.lms.ayan.common.FirestoreCollection.SUBJECT_COLLECTION
import com.lms.ayan.model.SubjectModel
import com.lms.ayan.util.logD
import com.lms.ayan.util.logE

class SubjectController(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val tag: String = "SubjectController"
    fun getList(callback: (List<SubjectModel?>?) -> Unit) {
        db.collection(SUBJECT_COLLECTION).get().addOnSuccessListener {
            val list = mutableListOf<SubjectModel>()
            for (document in it) {
                list.add(
                    SubjectModel(
                        id = document.id,
                        name = document.get("name").toString(),
                        nameUr = document.get("name_ur").toString()
                    )
                )
            }
            context.logD(tag) { "List is $list" }
            callback(list)
        }.addOnFailureListener { e ->
            context.logE(tag) { "Error getting list, error is $e" }
            callback(null)
        }
    }
}