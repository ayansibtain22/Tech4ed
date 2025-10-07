package com.lms.ayan.fragment

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.lms.ayan.R
import com.lms.ayan.controller.AIController
import com.lms.ayan.databinding.FragmentLearnWithAIBinding
import com.lms.ayan.util.DialogUtil.initLoadingDialog
import com.lms.ayan.util.UriUtils.asContentUri
import com.lms.ayan.util.isInternetAvailable
import com.lms.ayan.util.logD
import com.lms.ayan.util.showShortToast
import com.lms.ayan.util.typeWrite
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class LearnWithAIFragment : Fragment(), RecordingStatusProvider {
    private lateinit var binding: FragmentLearnWithAIBinding
    private lateinit var controller: AIController

    private lateinit var context: Context
    private lateinit var activity: Activity

    private lateinit var dialog: Dialog

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var startedAt = 0L

    private val reqMic = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else showShortToast(context, "Microphone permission required")
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLearnWithAIBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        context = requireContext()
        activity = requireActivity()
        controller = AIController(context)
        dialog = initLoadingDialog(context)
        initBackDispatcher()
        binding.materialToolbar.setNavigationOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            }
        }

        binding.sendButton.setOnClickListener {
            if (binding.promptET.text.isNullOrEmpty()) {
                binding.promptET.error = context.getString(R.string.please_enter_a_prompt)
            } else {
                if (isInternetAvailable(context)) {
                    showDialog()
                    controller.generateText(binding.promptET.text.toString()) {
                        dismissDialog()
                        /*binding.responseTV.text = it*/
                        it?.let { text ->
                            binding.responseTV.typeWrite(viewLifecycleOwner, text)
                        }

                    }
                } else {
                    showShortToast(context, context.getString(R.string.internet_not_available))
                }
            }
        }

        binding.audioButton.setOnClickListener {
            ensurePermissionAndStart()
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

    override fun isRecording(): Boolean = isRecording
    override fun getElapsedMillis(): Long =
        if (isRecording) System.currentTimeMillis() - startedAt else 0L

    override fun getAmplitude(): Int = try {
        recorder?.maxAmplitude ?: 0
    } catch (_: Exception) {
        0
    }

    override fun stopRecordingFromDialog() = stopRecordingSafely()

    private fun ensurePermissionAndStart() {
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        if (granted) startRecording() else reqMic.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startRecording() {
        if (isRecording) return

        outputFile = createOutputFile()
        val file = outputFile ?: run {
            showShortToast(context, "Failed to create file")
            return
        }

        recorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()

        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                releaseQuiet()
                showShortToast(context, "Recorder error: ${e.message}")
                return
            }
        }

        isRecording = true
        startedAt = System.currentTimeMillis()
        binding.audioButton.isEnabled = false
        /*tvPath.text = "File: ${file.absolutePath}"*/

        // Show status dialog
        RecordingDialogFragment.show(requireActivity(), this)
    }

    private fun stopRecordingSafely() {
        if (!isRecording) return
        try {
            recorder?.stop() // may throw if too short
        } catch (e: Exception) {
            outputFile?.delete()
            e.printStackTrace()
            showShortToast(context, "Recording too short or error. Discarded.")
        } finally {
            stopAndRelease(quiet = false)
        }
    }

    private fun stopAndRelease(quiet: Boolean) {
        if (!isRecording && recorder == null) return
        //recorder?.releaseQuiet()
        recorder?.release()
        recorder = null
        val saved = outputFile?.exists() == true && (outputFile?.length() ?: 0L) > 0L
        isRecording = false
        binding.audioButton.isEnabled = true
        //btnStop.isEnabled = false
        if (!quiet) {
            //tvStatus.text = if (saved) "Saved" else "Idle"
            //if (saved) toast("Saved: ${outputFile!!.name}")
            context.logD("audio") { "Is saved: $saved, Saved: ${outputFile!!.name}" }
            outputFile?.let {file->
                if (isInternetAvailable(context)) {
                    showDialog()
                    val uri = file.asContentUri(context)
                    controller.generateTextFromAudio(uri ){
                        dismissDialog()
                        it?.let { text ->
                            binding.responseTV.typeWrite(viewLifecycleOwner, text)
                        }
                    }
                } else {
                    showShortToast(context, context.getString(R.string.internet_not_available))
                }
            }

        }
    }

    private fun MediaRecorder.releaseQuiet() {
        try {
            reset()
        } catch (_: Exception) {
        }
        try {
            release()
        } catch (_: Exception) {
        }
    }

    private fun createOutputFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        return File(dir, "REC_$ts.m4a")
    }

}