package com.lms.ayan.model

data class MCQModel(
    val id: String? = null,
    val questionEn: String? = null,
    val questionUr: String? = null,
    val optionEn: Map<String?, String?>? = null,
    val optionUr: Map<String?, String?>? = null,
    val answer: String? = null,
    val quizId: String? = null,
    val questionNumber: Int? = null,
    var showAnswer: Boolean = false,
    var selectedAnswer: String? = null,
    var imageURL: String? = null
)
