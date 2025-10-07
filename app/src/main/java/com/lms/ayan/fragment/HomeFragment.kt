package com.lms.ayan.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lms.ayan.R
import com.lms.ayan.activity.AuthenticationActivity
import com.lms.ayan.controller.HomeController
import com.lms.ayan.databinding.FragmentHomeBinding
import com.lms.ayan.util.DialogUtil.initLoadingDialog
import com.lms.ayan.util.LocalizationUtil.setLanguage
import com.lms.ayan.util.SessionUtil
import com.lms.ayan.util.isInternetAvailable
import com.lms.ayan.util.logD
import com.lms.ayan.util.replaceFragment
import com.lms.ayan.util.showShortToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var binding: FragmentHomeBinding

    private lateinit var sessionUtil: SessionUtil

    private lateinit var lang: String

    private lateinit var dialog: Dialog
    private var userId: String? = null

    private lateinit var controller: HomeController
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        context = requireContext()
        activity = requireActivity()
        controller = HomeController(context)
        sessionUtil = SessionUtil.getInstance(context)
        dialog = initLoadingDialog(context)
        controller.getUserName {
            binding.materialToolbar.title = "Hi, $it"
        }
        CoroutineScope(Dispatchers.Default).launch {
            lang = sessionUtil.getLanguage()
            userId = sessionUtil.getUserId()
            withContext(Dispatchers.Main) {
                if (isInternetAvailable(context)) {
                    showDialog()
                    userId?.let { uid ->
                        controller.getLessonCount { lessonCount ->
                            controller.getLessonProgressCount(uid) { progressCount ->
                                dismissDialog()
                                binding.progressBar.max = lessonCount
                                binding.progressBar.progress = progressCount
                                context.logD("HOME") { "Lesson Count: $lessonCount and Progress Count: $progressCount" }
                                if (lessonCount > 0) {
                                    try {
                                        val div = progressCount.toDouble() / lessonCount.toDouble()
                                        val progress = ((div * 100.0).toInt()).toString()
                                        context.logD("HOME") { "Progress: $progress" }
                                        binding.progressTV.text = progress.plus("%")
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }

                                } else {
                                    binding.progressTV.text = "0%"
                                }
                            }
                        }
                    }
                } else {
                    showShortToast(context, context.getString(R.string.internet_not_available))
                }
            }
        }
        binding.materialToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.logout -> {
                    logout()
                    true
                }

                R.id.switchLanguage -> {
                    switchLanguage()
                    true
                }

                else -> false
            }

        }

        binding.startLearningCV.setOnClickListener {
            if (isInternetAvailable(context)) {
                replaceFragment(parentFragmentManager, SubjectListFragment(), R.id.container, true)
            } else {
                showShortToast(context, context.getString(R.string.internet_not_available))
            }
        }

        binding.StartTestCV.setOnClickListener {
            if (isInternetAvailable(context)) {
                replaceFragment(parentFragmentManager, QuizListFragment(), R.id.container, true)
            } else {
                showShortToast(context, context.getString(R.string.internet_not_available))
            }
        }

        binding.learnWitAICV.setOnClickListener {
            if (isInternetAvailable(context)) {
                replaceFragment(parentFragmentManager, LearnWithAIFragment(), R.id.container, true)
            } else {
                showShortToast(context, context.getString(R.string.internet_not_available))
            }
        }

        binding.helpUsCV.setOnClickListener {
            replaceFragment(parentFragmentManager, HelpFragment(), R.id.container, true)
        }
    }

    private fun logout() {
        controller.logout {
            requireActivity().finish()
            startActivity(Intent(context, AuthenticationActivity::class.java))
        }
    }

    private fun switchLanguage() {
        if (lang == "en") {
            CoroutineScope(Dispatchers.Default).launch {
                sessionUtil.setLanguage("ur")
                withContext(Dispatchers.Main) {
                    context.setLanguage("ur")
                }
            }
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                sessionUtil.setLanguage("en")
                withContext(Dispatchers.Main) {
                    context.setLanguage("en")
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