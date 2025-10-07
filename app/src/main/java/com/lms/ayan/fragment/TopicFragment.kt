package com.lms.ayan.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.lms.ayan.R
import com.lms.ayan.adapter.TopicAdapter
import com.lms.ayan.controller.TopicController
import com.lms.ayan.databinding.FragmentTopicBinding
import com.lms.ayan.util.DialogUtil.initLoadingDialog
import com.lms.ayan.util.SessionUtil
import com.lms.ayan.util.isInternetAvailable
import com.lms.ayan.util.replaceFragment
import com.lms.ayan.util.showShortToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SUBJECT_ID = "subjectID"
private const val SUBJECT_NAME = "subjectName"

class TopicFragment : Fragment() {
    private lateinit var binding: FragmentTopicBinding
    private lateinit var controller: TopicController
    private lateinit var context: Context
    private lateinit var activity: Activity
    private var subjectID: String? = null
    private var subjectName: String? = null
    private lateinit var dialog: Dialog

    private lateinit var sessionUtil: SessionUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            subjectID = it.getString(SUBJECT_ID)
            subjectName = it.getString(SUBJECT_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTopicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        context = requireContext()
        activity = requireActivity()
        controller = TopicController(context)
        dialog = initLoadingDialog(context)
        sessionUtil = SessionUtil.getInstance(context)
        subjectName?.let {
            binding.materialToolbar.title = it
        }

        initBackDispatcher()
        binding.topicRV.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        if (isInternetAvailable(context)) {
            subjectID?.let {
                showDialog()
                CoroutineScope(Dispatchers.Default).launch {
                    val lang = sessionUtil.getLanguage()
                    withContext(Dispatchers.Main) {
                        controller.getList(it) { topics ->
                            if (topics != null) {
                                binding.topicRV.adapter = TopicAdapter(topics, lang) { topic ->
                                    topic.id?.let {
                                        val topicTitle = if (lang == "ur") {
                                            topic.titleUr ?: ""
                                        } else {
                                            topic.titleEn ?: ""
                                        }
                                        val fragment =
                                            LessonFragment.newInstance(topic.id, topicTitle, (topic.topicNo?:0).toString())
                                        replaceFragment(
                                            parentFragmentManager,
                                            fragment,
                                            R.id.container,
                                            true
                                        )
                                    }

                                }
                            }
                            dismissDialog()
                        }
                    }
                }
            }
        } else {
            showShortToast(context, context.getString(R.string.internet_not_available))
        }

        binding.materialToolbar.setNavigationOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0)
                parentFragmentManager.popBackStack()
        }

    }

    private fun initBackDispatcher() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) { // Enable this callback
                override fun handleOnBackPressed() {
                    parentFragmentManager.popBackStack()
                }
            })
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
        super.onDestroy()
        dismissDialog()
    }

    companion object {
        @JvmStatic
        fun newInstance(subjectId: String, subjectName: String) =
            TopicFragment().apply {
                arguments = Bundle().apply {
                    putString(SUBJECT_ID, subjectId)
                    putString(SUBJECT_NAME, subjectName)
                }
            }
    }
}