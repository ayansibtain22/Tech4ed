package com.lms.ayan.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import com.lms.ayan.R
import com.lms.ayan.controller.LessonController
import com.lms.ayan.databinding.FragmentLessonBinding
import com.lms.ayan.model.ProgressModel
import com.lms.ayan.util.DialogUtil.initLoadingDialog
import com.lms.ayan.util.DialogUtil.showMessageDialog
import com.lms.ayan.util.EnhancedTTSManager
import com.lms.ayan.util.GlideImageGetter
import com.lms.ayan.util.SessionUtil
import com.lms.ayan.util.isInternetAvailable
import com.lms.ayan.util.logD
import com.lms.ayan.util.replaceFragment
import com.lms.ayan.util.showShortToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

private const val TOPIC_ID = "topic_id"
private const val TOPIC_NAME = "topic_name"
private const val TOPIC_NUMBER = "topic_number"

class LessonFragment : Fragment(), EnhancedTTSManager.Listener {

    private var topicID: String? = null
    private var topicName: String? = null

    private var topicNumber: String? = null

    private var userId: String? = null

    private lateinit var binding: FragmentLessonBinding

    private lateinit var context: Context
    private lateinit var activity: Activity

    private lateinit var lessonController: LessonController

    private lateinit var dialog: Dialog

    //private lateinit var ttsManager: TTSManager
    private lateinit var ttsManager: EnhancedTTSManager

    private lateinit var sessionUtil: SessionUtil

    private lateinit var lang: String

    private var progressModel: ProgressModel? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            topicID = it.getString(TOPIC_ID)
            topicName = it.getString(TOPIC_NAME)
            topicNumber = it.getString(TOPIC_NUMBER)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLessonBinding.inflate(inflater, container, false)
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
        initBackDispatcher()
        topicName?.let {
            binding.materialToolbar.title = it
        }

        lessonController = LessonController(context)
        sessionUtil = SessionUtil.getInstance(context)


        CoroutineScope(Dispatchers.Default).launch {
            lang = sessionUtil.getLanguage()
            userId = sessionUtil.getUserId()

            withContext(Dispatchers.Main) {
                initTTS()
                topicID?.let {
                    showDialog()
                    lessonController.getLesson(it) { topic ->
                        topic?.let {
                            if (lang == "ur") {
                                binding.htmlTV.text = Html.fromHtml(
                                    topic.detailUr,
                                    Html.FROM_HTML_MODE_LEGACY,
                                    GlideImageGetter(binding.htmlTV),
                                    null
                                )
                            } else {
                                binding.htmlTV.text = Html.fromHtml(
                                    topic.detailEn,
                                    Html.FROM_HTML_MODE_LEGACY,
                                    GlideImageGetter(binding.htmlTV),
                                    null
                                )
                            }
                        }

                    }
                    userId?.let { uId ->
                        lessonController.getProgress(uId, it) { progress ->
                            progress?.let {
                                if (progress.isRead) {
                                    binding.markAsRead.text =
                                        context.getString(R.string.already_read)
                                    binding.markAsRead.isEnabled = false
                                } else {
                                    binding.markAsRead.text =
                                        context.getString(R.string.mark_as_read)
                                    binding.markAsRead.isEnabled = true
                                }
                                progressModel = progress
                            }
                            dismissDialog()
                        }

                    }
                }

            }
        }


        binding.materialToolbar.setNavigationOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            }
        }

        binding.materialToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.readAloud -> {
                    readAloud()
                    true
                }

                else -> false
            }

        }

        binding.markAsRead.setOnClickListener {
            if (isInternetAvailable(context)) {
                userId?.let { uid ->
                    topicID?.let { tID ->
                        if (progressModel == null) {
                            progressModel =
                                ProgressModel(userId = uid, topicId = tID, isRead = true)
                            progressModel?.let { model ->
                                showDialog()
                                lessonController.saveProgress(model) {
                                    dismissDialog()
                                    if (it) {
                                        showShortToast(
                                            context,
                                            context.getString(R.string.mark_as_read_successfully)
                                        )
                                        binding.markAsRead.text =
                                            context.getString(R.string.already_read)
                                        binding.markAsRead.isEnabled = false
                                        showMessageDialog(
                                            context,
                                            context.getString(R.string.ready_for_quiz),
                                            "",
                                            context.getString(R.string.yes),
                                            onYesBtnClicked = {
                                                replaceFragment(
                                                    parentFragmentManager,
                                                    MCQsFragment.newInstance(
                                                        topicNumber ?: "0",
                                                        topicName ?: "",
                                                        topicID ?: ""
                                                    ),
                                                    R.id.container,
                                                    true
                                                )
                                            }, onNoBtnClicked = {}
                                        )
                                    } else {
                                        showShortToast(
                                            context,
                                            context.getString(R.string.something_went_wrong)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                showShortToast(context, context.getString(R.string.internet_not_available))
            }
        }
    }

    private fun initTTS() {
        val locale = if (lang == "ur") {
            Locale("ur", "PK")
        } else {
            Locale.ENGLISH
        }

        ttsManager = EnhancedTTSManager(
            context = requireContext(),
            locale = locale,
            speechRate = 1.0f,
            pitch = 1.0f,
            preferredEngine = "com.google.android.tts" // optional, good Urdu coverage
        )
        ttsManager.setListener(this)
    }

    private fun readAloud() {
        showShortToast(context, context.getString(R.string.read_aloud_started))
        binding.htmlTV.text?.let { text ->
            if (text.isEmpty()) {
                showShortToast(context, context.getString(R.string.no_text_to_read))
            } else {
                if (ttsManager.isSpeaking()) {
                    ttsManager.stop()
                } else {
                    // IMPORTANT: call speak() even if not ready; it will queue and auto-start onReady()
                    ttsManager.speak(text.toString())
                }
            }
            /*if (!ttsManager.isReady) {
                context.logD("tts") { "TTS is ready!" }
                return
            }
            if (ttsManager.isSpeaking()) {
                ttsManager.stop()
                // Immediate UI update; onDone() may not fire on stop().
                //btnToggle.text = getString(R.string.action_read_aloud)
            } else {
                ttsManager.speak(it.toString())
                // UI will be finalized in onStart().
            }*/
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
        if (this::ttsManager.isInitialized) ttsManager.stop()
    }

    override fun onDestroyView() {
        if (this::ttsManager.isInitialized) ttsManager.shutdown()
        super.onDestroyView()
    }

    override fun onDestroy() {
        dismissDialog()
        ttsManager.shutdown()
        super.onDestroy()
    }

    override fun onReadyTTS() {
        context.logD("tts") { "TTS is ready!" }

    }


    override fun onStartTTS() {
    }

    override fun onDoneTTS() {
        context.logD("tts") { "TTS is done!" }
    }

    override fun onErrorTTS(message: String) {
        context.logD("tts") { "TTS error: $message" }
        showShortToast(context, context.getString(R.string.language_not_supported))
        // If Urdu data is missing/not supported, guide user to install voice data.
        if (message.contains("missing", true) || message.contains("not supported", true)) {
            try {
                ttsManager.promptInstallVoiceData(requireActivity())
            } catch (_: Throwable) { /* ignore */
            }
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(topicId: String, topicName: String, topicNumber: String) =
            LessonFragment().apply {
                arguments = Bundle().apply {
                    putString(TOPIC_ID, topicId)
                    putString(TOPIC_NAME, topicName)
                    putString(TOPIC_NUMBER, topicNumber)
                }
            }
    }
}