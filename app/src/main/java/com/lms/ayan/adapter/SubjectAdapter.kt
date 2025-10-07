package com.lms.ayan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lms.ayan.databinding.ItemLearningBinding
import com.lms.ayan.model.SubjectModel

class SubjectAdapter(
    val list: List<SubjectModel?>?,
    val lang: String,
    val listener: (SubjectModel) -> Unit
) :
    RecyclerView.Adapter<SubjectAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding =
            ItemLearningBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        list?.let {
            holder.setData(it[position])
        }
    }

    override fun getItemCount(): Int = list?.size ?: 0

    inner class ViewHolder(private val binding: ItemLearningBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(subject: SubjectModel?) {
            subject?.let { subjectModel ->
                if (lang == "ur") {
                    binding.textView.text = subjectModel.nameUr
                } else {
                    binding.textView.text = subjectModel.name
                }
                binding.cardView.setOnClickListener {
                    listener(subjectModel)
                }
            }
        }
    }
}