package com.lms.ayan.fragment

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
import com.lms.ayan.adapter.QuizAdapter
import com.lms.ayan.adapter.SubjectAdapter
import com.lms.ayan.controller.QuizListController
import com.lms.ayan.databinding.FragmentQuizListBinding
import com.lms.ayan.util.DialogUtil.initLoadingDialog
import com.lms.ayan.util.SessionUtil
import com.lms.ayan.util.isInternetAvailable
import com.lms.ayan.util.logD
import com.lms.ayan.util.replaceFragment
import com.lms.ayan.util.showShortToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizListFragment : Fragment() {
    private lateinit var binding: FragmentQuizListBinding
    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var dialog: Dialog
    private lateinit var controller: QuizListController
    private lateinit var sessionUtil: SessionUtil
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentQuizListBinding.inflate(inflater, container, false)
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
        controller = QuizListController(context)
        sessionUtil = SessionUtil.getInstance(context)
        binding.quizRV.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        if (isInternetAvailable(context)) {
            showDialog()
            CoroutineScope(Dispatchers.Default).launch {
                val lang = sessionUtil.getLanguage()
                if (lang == "ur") {
                    binding.materialToolbar.title = context.getString(R.string.quizzes)
                }
                withContext(Dispatchers.Main) {
                    controller.getList {
                        if (it != null) {
                            binding.quizRV.adapter = QuizAdapter(it, lang) { quiz ->
                                val name = if (lang == "ur") {
                                    quiz.nameUr ?: ""
                                } else {
                                    quiz.nameEn ?: ""
                                }

                                quiz.id?.let { id ->
                                    quiz.topicID?.let { tpcID ->
                                        context.logD("QuizListFragment") { "Topic ID is $tpcID" }
                                        val fragment = MCQsFragment.newInstance(id, name, tpcID)
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
                        dismissDialog()
                    }
                }
            }

        } else {
            showShortToast(context, context.getString(R.string.internet_not_available))
        }

        getScores()

        binding.materialToolbar.setNavigationOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0)
                parentFragmentManager.popBackStack()
        }
    }

    private fun getScores() {
        if (isInternetAvailable(context)) {
            showDialog()
            CoroutineScope(Dispatchers.Default).launch {
                val userID = sessionUtil.getUserId()
                withContext(Dispatchers.Main) {
                    controller.getScore(userID) {
                        if (it != null) {
                            binding.totalQuizzesTV.text = it.size.toString()
                            var totalScore = 0
                            var obtainedScore = 0
                            for (score in it) {
                                totalScore += score.totalScore
                                obtainedScore += score.obtainedScore
                            }
                            binding.totalNumberTV.text = totalScore.toString()
                            binding.obtainedNumberTV.text = obtainedScore.toString()
                        }
                        dismissDialog()
                    }
                }
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
}