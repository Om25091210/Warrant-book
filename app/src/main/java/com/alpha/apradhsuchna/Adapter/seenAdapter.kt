package com.alpha.apradhsuchna.Adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.alpha.apradhsuchna.R
import com.alpha.apradhsuchna.model.Comments
import com.alpha.apradhsuchna.model.seenData
import io.michaelrocks.paranoid.Obfuscate

@Obfuscate
class seenAdapter(list: List<seenData>) : RecyclerView.Adapter<seenAdapter.Viewholder?>() {

    private val list: List<seenData>

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): Viewholder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.seen_layout, parent, false)
        return Viewholder(view)
    }

    override fun onBindViewHolder(@NonNull holder: Viewholder, position: Int) {
        holder.seen_name.text = list.get(position).name
        holder.seen_number.text = list.get(position).phone
        holder.head.text = list.get(position).name.substring(0, 1)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    init {
        this.list = list
    }

    inner class Viewholder (private val view: View) :
        RecyclerView.ViewHolder(view) {
        val seen_name=view.findViewById<TextView>(R.id.textView84)
        val head=view.findViewById<TextView>(R.id.head1)
        val seen_number=view.findViewById<TextView>(R.id.textView86)
    }
}