package com.lms.ayan.model

data class ScoreModel(
    var id: String?=null,
    var userId: String,
    var quizId: String,
    var answeredList: List<String>,
    var totalScore: Int,
    var obtainedScore: Int
)
