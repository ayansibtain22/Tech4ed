package com.lms.ayan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lms.ayan.databinding.ItemLearningBinding
import com.lms.ayan.model.SubjectModel
import com.lms.ayan.model.TopicModel

class TopicAdapter(
    val list: List<TopicModel?>?,
    val lang: String,
    val listener: (TopicModel) -> Unit
) :
    RecyclerView.Adapter<TopicAdapter.ViewHolder>() {
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
        fun setData(topicModel: TopicModel?) {
            topicModel?.let { topic ->
                if (lang == "ur") {
                    binding.textView.text = topic.titleUr
                } else {
                    binding.textView.text = topic.titleEn
                }

                binding.cardView.setOnClickListener {
                    listener(topic)
                }
            }
        }
    }
}