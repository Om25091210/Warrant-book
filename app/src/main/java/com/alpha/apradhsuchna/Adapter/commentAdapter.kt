package com.alpha.apradhsuchna.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.alpha.apradhsuchna.R
import com.alpha.apradhsuchna.model.Comments
import io.michaelrocks.paranoid.Obfuscate

@Obfuscate
class commentAdapter(list: List<Comments>) : RecyclerView.Adapter<commentAdapter.Viewholder?>() {

    private val list: List<Comments>

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): Viewholder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.msg_layout, parent, false)
        return Viewholder(view)
    }

    override fun onBindViewHolder(@NonNull holder: Viewholder, position: Int) {
       holder.comment_name.text=list.get(position).name
       holder.comment_time.text=list.get(position).time
       holder.comment_content.text=list.get(position).content
    }

    override fun getItemCount(): Int {
        return list.size
    }

    init {
        this.list = list
    }

    inner class Viewholder (private val view: View) :
        RecyclerView.ViewHolder(view) {
        val comment_name=view.findViewById<TextView>(R.id.textView)
        val comment_time=view.findViewById<TextView>(R.id.time)
        val comment_content=view.findViewById<TextView>(R.id.textView2)
    }
}