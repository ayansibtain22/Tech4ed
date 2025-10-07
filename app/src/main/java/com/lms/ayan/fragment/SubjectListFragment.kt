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
import com.lms.ayan.adapter.SubjectAdapter
import com.lms.ayan.controller.SubjectController
import com.lms.ayan.databinding.FragmentSubjectListBinding
import com.lms.ayan.util.DialogUtil.initLoadingDialog
import com.lms.ayan.util.SessionUtil
import com.lms.ayan.util.isInternetAvailable
import com.lms.ayan.util.replaceFragment
import com.lms.ayan.util.showShortToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubjectListFragment : Fragment() {

    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var binding: FragmentSubjectListBinding

    private lateinit var controller: SubjectController

    private lateinit var dialog: Dialog

    private lateinit var sessionUtil: SessionUtil

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSubjectListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        context = requireContext()
        activity = requireActivity()
        sessionUtil = SessionUtil.getInstance(context)
        controller = SubjectController(context)
        dialog = initLoadingDialog(context)
        initBackDispatcher()
        binding.subjectRV.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        if (isInternetAvailable(context)) {
            showDialog()
            CoroutineScope(Dispatchers.Default).launch {
                val lang = sessionUtil.getLanguage()
                if (lang == "ur") {
                    binding.materialToolbar.title = context.getString(R.string.subjects)
                }
                withContext(Dispatchers.Main) {
                    controller.getList {
                        if (it != null) {
                            binding.subjectRV.adapter = SubjectAdapter(it, lang) { subject ->
                                val name = if (lang == "ur") {
                                    subject.nameUr?:""
                                } else {
                                    subject.name?:""
                                }
                                subject.id?.let {id->
                                    val fragment = TopicFragment.newInstance(id, name)
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
        dismissDialog()
        super.onDestroy()
    }
}