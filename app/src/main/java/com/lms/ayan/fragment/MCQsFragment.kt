package com.lms.ayan.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.lms.ayan.R
import com.lms.ayan.adapter.MCQAdapterListener
import com.lms.ayan.adapter.MCQsAdapter
import com.lms.ayan.controller.MCQsController
import com.lms.ayan.databinding.FragmentMCQsBinding
import com.lms.ayan.model.MCQModel
import com.lms.ayan.model.ScoreModel
import com.lms.ayan.util.DialogUtil.initLoadingDialog
import com.lms.ayan.util.SessionUtil
import com.lms.ayan.util.isInternetAvailable
import com.lms.ayan.util.logD
import com.lms.ayan.util.replaceFragment
import com.lms.ayan.util.showShortToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val QUIZ_ID = "quiz_id"
private const val QUIZ_NAME = "quiz_name"
private const val TOPIC_ID = "topic_id"

class MCQsFragment : Fragment(), MCQAdapterListener {
    private var quizID: String? = null
    private var quizName: String? = null
    private var topicID: String? = null

    private lateinit var binding: FragmentMCQsBinding

    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var dialog: Dialog
    private lateinit var controller: MCQsController
    private lateinit var sessionUtil: SessionUtil


    private var adapter: MCQsAdapter? = null

    private var revealAnswers = false

    private var userId: String? = null

    private var previousScoreModel: ScoreModel? = null

    private var mcqList = mutableListOf<MCQModel?>()

    private var lang: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            quizID = it.getString(QUIZ_ID)
            quizName = it.getString(QUIZ_NAME)
            topicID = it.getString(TOPIC_ID)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMCQsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        context = requireContext()
        activity = requireActivity()
        dialog = initLoadingDialog(context)
        controller = MCQsController(context)
        sessionUtil = SessionUtil.getInstance(context)
        context.logD("MCQsFragment") { "onCreate()::quizID is $quizID, quizName is $quizName, topicID is $topicID"}
        binding.mcqRV.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        CoroutineScope(Dispatchers.Default).launch {
            lang = sessionUtil.getLanguage()
            userId = sessionUtil.getUserId()
            withContext(Dispatchers.Main) {
                if (lang == "ur") {
                    binding.materialToolbar.title = context.getString(R.string.quizzes)
                }
                initAdapter()
                getMCQsFromServer()
            }
        }

        initListeners()
    }

    private fun initListeners() {
        binding.materialToolbar.setNavigationOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0)
                parentFragmentManager.popBackStack()
        }

        binding.submitBtn.setOnClickListener {
            if (mcqList.isNotEmpty()) {
                val attemptedCount = mcqList.count { !it?.selectedAnswer.isNullOrEmpty() }
                if (attemptedCount == mcqList.size) {
                    checkMCQs()
                } else {
                    showShortToast(
                        context,
                        context.getString(R.string.please_answer_all_questions)
                    )
                }
            } else {
                showShortToast(
                    context,
                    context.getString(R.string.quiz_not_available_on_this_topic)
                )
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getMCQsFromServer() {
        if (isInternetAvailable(context)) {
            quizID?.let { quizID ->
                showDialog()
                controller.getList(quizID) { mcqs ->
                    mcqs?.let {
                        mcqList.addAll(it)
                        adapter?.notifyDataSetChanged()
                    }
                    dismissDialog()
                    getPreviousScore()
                }
            }
        } else {
            dismissDialog()
            showShortToast(context, context.getString(R.string.internet_not_available))
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun initAdapter() {
        lang?.let { language ->
            adapter = MCQsAdapter(mcqList, language, this)
            binding.mcqRV.adapter = adapter
        }
    }

    private fun getPreviousScore() {
        userId?.let {
            quizID?.let { qID ->
                showDialog()
                userId?.let { uID ->
                    controller.getScore(uID, qID) { score ->
                        previousScoreModel = score
                        dismissDialog()
                    }
                }

            }
        }
    }

    private fun checkMCQs() {
        adapter?.notifyDataSetChanged()
        binding.submitBtn.isEnabled = false
        showDialog()
        CoroutineScope(Dispatchers.Default).launch {
            var correctAnswers = 0
            val selectedAnswers = mutableListOf<String>()
            for (quiz in mcqList) {
                quiz?.let { item ->
                    selectedAnswers.add(item.selectedAnswer ?: "")
                    if (item.selectedAnswer == item.answer) {
                        correctAnswers++
                    }
                }
            }
            delay(1000)
            withContext(Dispatchers.Main) {
                showMarks(correctAnswers, selectedAnswers)
            }
        }
    }

    fun showMarks(obtainedMarks: Int, selectedAnswers: List<String>) {
        binding.scoreTV.text =
            context.getString(R.string.score).plus(" ")
                .plus(obtainedMarks.toString()).plus(" ")
                .plus(context.getString(R.string.out_of))
                .plus(" ")
                .plus(mcqList.size.toString())

        if (isInternetAvailable(context)) {
            saveDataOnFirestore(obtainedMarks, selectedAnswers)
        } else {
            binding.submitBtn.isEnabled = true
            dismissDialog()
            showShortToast(
                context,
                context.getString(R.string.internet_not_available)
            )
        }
    }

    private fun saveDataOnFirestore(obtainedMarks: Int, selectedAnswers: List<String>) {
        userId?.let { uid ->
            quizID?.let { quizId ->
                previousScoreModel?.let { score ->
                    if (!revealAnswers) {
                        previousScoreModel = ScoreModel(
                            id = score.id,
                            userId = score.userId,
                            quizId = score.quizId,
                            answeredList = selectedAnswers,
                            totalScore = mcqList.size,
                            obtainedScore = obtainedMarks
                        )
                        previousScoreModel?.let { updatedScore ->
                            controller.updateScore(
                                updatedScore
                            ) {
                                binding.submitBtn.isEnabled = true
                                showShortToast(
                                    context,
                                    context.getString(R.string.quiz_already_submitted)
                                )
                            }
                        }
                        revealAnswers = true
                        adapter?.revealAnswers()
                        decideToRedirectToTopic(obtainedMarks)
                    } else {
                        showShortToast(
                            context,
                            context.getString(R.string.quiz_already_submitted)
                        )
                        binding.submitBtn.isEnabled = true
                        context.logD("MCQsFragment") { "Quiz already submitted" }
                    }
                    dismissDialog()
                } ?: run {
                    previousScoreModel = ScoreModel(
                        userId = uid,
                        quizId = quizId,
                        answeredList = selectedAnswers,
                        totalScore = mcqList.size,
                        obtainedScore = obtainedMarks
                    )
                    previousScoreModel?.let { scoreModel ->
                        controller.saveScore(
                            scoreModel
                        ) { callback ->
                            if (!callback) {
                                showShortToast(
                                    context,
                                    context.getString(R.string.something_went_wrong)
                                )
                            }
                            revealAnswers = true
                            adapter?.revealAnswers()
                            binding.submitBtn.isEnabled = true
                            decideToRedirectToTopic(obtainedMarks)
                        }
                    }
                    dismissDialog()
                }
            }
        }
    }

    private fun decideToRedirectToTopic(obtainedMarks: Int) {
        val percentage = (obtainedMarks.toDouble() / mcqList.size.toDouble()) * 100.0
        if (percentage < 60.0) {
            showShortToast(context,context.getString(R.string.bad_progrss))
            quizID?.let { qID->
                topicID?.let { tID ->
                    val fragment = LessonFragment.newInstance(tID, quizName ?: "", qID)
                    replaceFragment(
                        parentFragmentManager,
                        fragment,
                        R.id.container,
                        true
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(quizId: String, quizName: String, topicID: String) =
            MCQsFragment().apply {
                arguments = Bundle().apply {
                    putString(QUIZ_ID, quizId)
                    putString(QUIZ_NAME, quizName)
                    putString(TOPIC_ID, topicID)
                }
            }
    }

    private fun showDialog() {
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    private fun dismissDialog() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    override fun onStop() {
        super.onStop()
        dismissDialog()
    }

    override fun onDestroy() {
        dismissDialog()
        super.onDestroy()
    }

    override fun onMCQSelection(index: Int, selectedAnswer: String) {
        mcqList[index]?.selectedAnswer = selectedAnswer
    }
}