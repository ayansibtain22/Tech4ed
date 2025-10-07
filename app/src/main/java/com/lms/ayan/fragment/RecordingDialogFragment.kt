package com.lms.ayan.fragment

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.lms.ayan.databinding.DialogAudioRecordBinding

class RecordingDialogFragment(val listener: RecordingStatusProvider) : DialogFragment() {

    private val ui = Handler(Looper.getMainLooper())
    private var running = false

    //private var provider: RecordingStatusProvider? = null

    /*override fun onAttach(context: Context) {
        super.onAttach(context)
        provider = when {
            parentFragment is RecordingStatusProvider -> parentFragment as RecordingStatusProvider
            context is RecordingStatusProvider -> context
            else -> throw IllegalStateException(
                "Host must implement com.lms.ayan.recorder.RecordingStatusProvider"
            )
        }
    }*/

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        //val provider = requireActivity() as RecordingStatusProvider

        val binding = DialogAudioRecordBinding.inflate(layoutInflater, null, false)

        binding.btnStopNow.setOnClickListener {
            // Why: ensure a single stop path owned by the activity
            listener.stopRecordingFromDialog()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        running = true
        ui.post(object : Runnable {
            override fun run() {
                if (!running) return
                /*val isRec = provider.isRecording()
                if (!isRec) {
                    dismissAllowingStateLoss()
                    return
                }*/
                listener.let { prov ->
                    val isRec = prov.isRecording()
                    if (!isRec) {
                        dismissAllowingStateLoss()
                        return
                    }
                    val elapsed = prov.getElapsedMillis() / 1000
                    binding.tvTimer.text = "%02d:%02d".format(elapsed / 60, elapsed % 60)
                    ui.postDelayed(this, 200L)
                }
            }
        })
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        running = false
        ui.removeCallbacksAndMessages(null)
    }

    companion object {
        const val TAG = "RecordingDialog"
        fun show(host: androidx.fragment.app.FragmentActivity, listener: RecordingStatusProvider) {
            if (host.supportFragmentManager.findFragmentByTag(TAG) == null) {
                RecordingDialogFragment(listener).show(host.supportFragmentManager, TAG)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        running = false
        ui.removeCallbacksAndMessages(null)
       // listener = null
    }
}

interface RecordingStatusProvider {
    fun isRecording(): Boolean
    fun getElapsedMillis(): Long
    fun getAmplitude(): Int
    fun stopRecordingFromDialog()
}