package com.lms.ayan.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lms.ayan.R
import com.lms.ayan.databinding.ItemMcqsBinding
import com.lms.ayan.model.MCQModel

class MCQsAdapter(
    val list: List<MCQModel?>?,
    val lang: String,
    val listener: MCQAdapterListener
) :
    RecyclerView.Adapter<MCQsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding =
            ItemMcqsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        list?.let {
            holder.setData(it[position], position)
        }
    }

    override fun getItemCount(): Int = list?.size ?: 0

    inner class ViewHolder(private val binding: ItemMcqsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun setData(mcq: MCQModel?, position: Int) {
            mcq ?: return

            // ----- 1) Fill question + options -----
            if (lang == "ur") {
                binding.questionTV.text = mcq.questionUr
                mcq.optionUr?.let {
                    binding.aRadioButton.text = it.getOrElse("a") { "" }
                    binding.bRadioButton.text = it.getOrElse("b") { "" }
                    binding.cRadioButton.text = it.getOrElse("c") { "" }
                    binding.dRadioButton.text = it.getOrElse("d") { "" }
                }
            } else {
                binding.questionTV.text = mcq.questionEn
                mcq.optionEn?.let {
                    binding.aRadioButton.text = it.getOrElse("a") { "" }
                    binding.bRadioButton.text = it.getOrElse("b") { "" }
                    binding.cRadioButton.text = it.getOrElse("c") { "" }
                    binding.dRadioButton.text = it.getOrElse("d") { "" }
                }
            }

            // ----- 2) Prepare colors + reset state (important for recycling) -----
            val context = binding.root.context
            val defaultColor =
                context.getColor(R.color.black)          // adjust to your default text color
            val green = context.getColor(R.color.green)
            val red = context.getColor(R.color.error)

            // remove listener to avoid firing while we programmatically check radios
            binding.radioGroup.setOnCheckedChangeListener(null)

            // reset checks
            binding.radioGroup.clearCheck()

            // reset colors
            binding.aRadioButton.setTextColor(defaultColor)
            binding.bRadioButton.setTextColor(defaultColor)
            binding.cRadioButton.setTextColor(defaultColor)
            binding.dRadioButton.setTextColor(defaultColor)

            // re-apply user's previous selection (model source of truth)
            when (mcq.selectedAnswer) {
                "a" -> binding.aRadioButton.isChecked = true
                "b" -> binding.bRadioButton.isChecked = true
                "c" -> binding.cRadioButton.isChecked = true
                "d" -> binding.dRadioButton.isChecked = true
            }

            // ----- 3) Show answer & color logic driven by model -----
            if (mcq.showAnswer) {
                val answerText = if (lang == "ur") {
                    mcq.optionUr?.getOrElse(mcq.answer) { "" }
                } else {
                    mcq.optionEn?.getOrElse(mcq.answer) { "" }
                }
                binding.answerTV.text =
                    context.getString(R.string.correct_answer_is) + " " + (answerText ?: "")
                binding.answerTV.visibility = View.VISIBLE

                // Mark the correct option green
                when (mcq.answer) {
                    "a" -> binding.aRadioButton.setTextColor(green)
                    "b" -> binding.bRadioButton.setTextColor(green)
                    "c" -> binding.cRadioButton.setTextColor(green)
                    "d" -> binding.dRadioButton.setTextColor(green)
                }

                // If user selected a wrong option, mark that one red
                val wrongSelected = mcq.selectedAnswer?.takeIf { it != mcq.answer }
                when (wrongSelected) {
                    "a" -> binding.aRadioButton.setTextColor(red)
                    "b" -> binding.bRadioButton.setTextColor(red)
                    "c" -> binding.cRadioButton.setTextColor(red)
                    "d" -> binding.dRadioButton.setTextColor(red)
                }

                // (Optional) lock changes after reveal:
                // binding.radioGroup.isEnabled = false
                // binding.aRadioButton.isEnabled = false
                // binding.bRadioButton.isEnabled = false
                // binding.cRadioButton.isEnabled = false
                // binding.dRadioButton.isEnabled = false
            } else {
                binding.answerTV.visibility = View.GONE
                // binding.radioGroup.isEnabled = true // if you locked on reveal
                // binding.aRadioButton.isEnabled = true
                // binding.bRadioButton.isEnabled = true
                // binding.cRadioButton.isEnabled = true
                // binding.dRadioButton.isEnabled = true
            }

            // ----- 4) Re-attach listener for user selections -----
            binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    binding.aRadioButton.id -> listener.onMCQSelection(bindingAdapterPosition, "a")
                    binding.bRadioButton.id -> listener.onMCQSelection(bindingAdapterPosition, "b")
                    binding.cRadioButton.id -> listener.onMCQSelection(bindingAdapterPosition, "c")
                    binding.dRadioButton.id -> listener.onMCQSelection(bindingAdapterPosition, "d")
                }
            }

            mcq.imageURL?.let {imgURL->
                if(imgURL.isNotBlank()){
                    if(imgURL!="null") {
                        binding.imageView.visibility = View.VISIBLE
                        Glide
                            .with(binding.imageView)
                            .load(imgURL)
                            .into(binding.imageView)
                    }else{
                        binding.imageView.visibility = View.GONE
                    }
                }else{
                    binding.imageView.visibility = View.GONE
                }
            } ?: run {
                binding.imageView.visibility = View.GONE
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun revealAnswers() {
        list?.let {
            for (question in it) {
                question?.showAnswer = true
            }
            notifyDataSetChanged() // Notify the adapter that data has changed
        }

    }
}

interface MCQAdapterListener {
    fun onMCQSelection(index: Int, selectedAnswer: String)
}