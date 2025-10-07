package com.lms.ayan.controller

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lms.ayan.R
import com.lms.ayan.common.FirestoreCollection.USER_COLLECTION
import com.lms.ayan.model.UserModel
import com.lms.ayan.util.SessionUtil
import com.lms.ayan.util.logD
import com.lms.ayan.util.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AuthController(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val sessionUtil: SessionUtil = SessionUtil.getInstance(context)
) {
    private val tag: String = "AuthController"
    fun register(student: UserModel, callback: (Boolean) -> Unit) {
        isUserAlreadyAvailable(student.email.orEmpty()) { isUserAvailable ->
            if (!isUserAvailable) {
                auth.createUserWithEmailAndPassword(student.email ?: "", student.password ?: "")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            context.logD(tag){ "Registration Successful" }
                            val user = auth.currentUser
                            user?.let {
                                student.id = it.uid
                                saveUserToFirestore(student) { response ->
                                    if (response) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.registration_successfully),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        callback(true)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.registration_failed),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        callback(false)
                                    }
                                }
                            } ?: run {
                                context.logE(tag) { "User is null" }
                                callback(false)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.registration_failed),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        } else {
                            context.logE(tag) { "Registration Failed, Exception is ${task.exception}" }
                            callback(false)
                            Toast.makeText(
                                context,
                                context.getString(R.string.registration_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
            } else {
                callback(false)
            }
        }
    }

    private fun saveUserToFirestore(student: UserModel, callback: (Boolean) -> Unit) {
        val user = hashMapOf(
            "uid" to student.id,
            "fullName" to student.fullName,
            "email" to student.email
        )
        db.collection(USER_COLLECTION)
            .add(user).addOnSuccessListener { documentReference ->
                context.logD(tag) { "DocumentSnapshot added with ID: ${documentReference.id}" }
                callback(true)
            }
            .addOnFailureListener { e ->
                context.logE(tag) { "Error adding document, error is $e" }
                callback(false)
            }
    }

    private fun isUserAlreadyAvailable(email: String, callback: (Boolean) -> Unit) {
        db.collection(USER_COLLECTION)
            .whereEqualTo("email", email)
            .get().addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    context.logD(tag) { "Email is available on db" }
                    callback(false)
                } else {
                    context.logD(tag) { "Email is not available on db" }
                    callback(true)
                    Toast.makeText(
                        context,
                        context.getString(R.string.user_already_exist),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    fun login(email: String, password: String, callback: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information

                    val user = auth.currentUser
                    context.logD(tag) { "signInWithEmail:success and uid is ${user?.uid}" }
                    user?.let {
                        getUserDetails(user.uid) {loggedIn->
                            if (loggedIn) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.login_successfully),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                callback(true)
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.something_went_wrong),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                callback(false)
                            }
                        }

                    } ?: run {
                        context.logE(tag) { "User is null" }
                        callback(false)
                        Toast.makeText(
                            context,
                            context.getString(R.string.something_went_wrong),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }

                } else {
                    // If sign in fails, display a message to the user.
                    context.logD(tag) { "signInWithEmail:failure, exception: ${task.exception}" }
                    Toast.makeText(
                        context,
                        context.getString(R.string.incorrect_email_or_password),
                        Toast.LENGTH_SHORT,
                    ).show()
                    callback(false)
                }
            }
    }

    private fun getUserDetails(userId: String, callback: (Boolean) -> Unit) {
        context.logD(tag) { "getUserDetails()::userId is $userId" }
        db.collection(USER_COLLECTION)
            .whereEqualTo("uid", userId)
            .get().addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    context.logD(tag) { "getUserDetails()::No such document" }
                    callback(false)
                } else {
                    if (documents.size() > 0) {
                        CoroutineScope(Dispatchers.IO).launch {
                            sessionUtil.setUserId(documents.first().get("uid").toString())
                            sessionUtil.setUserEmail(documents.first().get("email").toString())
                            sessionUtil.setUserFullName(
                                documents.first().get("fullName").toString()
                            )
                            sessionUtil.setSession(true)
                            withContext(Dispatchers.Main) {
                                context.logD(tag) { "getUserDetails()::Document found: ${documents.first().data}" }
                                callback(true)
                            }
                        }

                    } else {
                        context.logD(tag) { "getUserDetails()::Zero Document" }
                        callback(false)
                    }
                }
            }
            .addOnFailureListener { exception ->
                context.logE(tag) { "Error getting documents: $exception" }
                callback(false)
            }
    }
}