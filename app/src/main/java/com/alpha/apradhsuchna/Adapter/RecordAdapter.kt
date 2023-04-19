package com.alpha.apradhsuchna.Adapter

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.alpha.apradhsuchna.R
import com.alpha.apradhsuchna.model.record_data
import com.alpha.apradhsuchna.view_details
import com.facebook.drawee.view.SimpleDraweeView
import io.michaelrocks.paranoid.Obfuscate

@Obfuscate
class recordAdapter (list: List<record_data>, context: Context?) : RecyclerView.Adapter<recordAdapter.Viewholder?>() {

    private val list: List<record_data>
    private var context: Context

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): Viewholder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.record_layout, parent, false)
        return Viewholder(view)
    }

    override fun onBindViewHolder(@NonNull holder: Viewholder, position: Int) {
        holder.name.text=list.get(position).criminal_name
        holder.upload_time.text=list.get(position).uploaded_date
        holder.address.text="Address: "+list.get(position).address
        holder.ps.text=list.get(position).police_station
        holder.co.text="C/O: "+list.get(position).co
        if(list.get(position).image_link!=null) {
            if(list.get(position).image_link!="") {
                holder.image.visibility = View.VISIBLE
                try {
                    val uri = Uri.parse(list[position].image_link)
                    holder.image.setImageURI(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            else{
                holder.image.visibility=View.GONE
            }
        }
        else{
            holder.image.visibility=View.GONE
        }
        holder.mainlayout.setOnClickListener{
            val bundle=Bundle()
            bundle.putString("key",list.get(position).key)
            val view_details=view_details()
            view_details.arguments=bundle
            (context as FragmentActivity).supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.enter_from_right,
                    R.anim.exit_to_left,
                    R.anim.enter_from_left,
                    R.anim.exit_to_right
                )
                .add(R.id.drawer, view_details)
                .addToBackStack(null)
                .commit()
        }
        if (list[position].done.equals("0")) {
            holder.layout.setBackgroundResource(R.drawable.content_an_bg)
            holder.view1.setBackgroundResource(R.drawable.view_bg)
        } else {
            holder.layout.setBackgroundResource(R.drawable.done_green)
            holder.view1.setBackgroundResource(R.drawable.done_view_bg)
            holder.name.setTextColor(Color.parseColor("#43A047"))
            holder.co.setTextColor(Color.parseColor("#43A047"))
            holder.address.setTextColor(Color.parseColor("#43A047"))
        }
        Log.e("datamsg",list.get(position).address)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    init {
        this.list = list
        this.context=context!!
    }

    inner class Viewholder (private val view: View) :
        RecyclerView.ViewHolder(view) {
            val name=view.findViewById<TextView>(R.id.textView28)
            val layout=view.findViewById<ConstraintLayout>(R.id.mainlayout)
            val co=view.findViewById<TextView>(R.id.co)
            val view1=view.findViewById<View>(R.id.viewSubtitleIndicator)
            val upload_time=view.findViewById<TextView>(R.id.textView29)
            val address=view.findViewById<TextView>(R.id.textView36)
            val ps=view.findViewById<TextView>(R.id.textView34)
            val image=view.findViewById<SimpleDraweeView>(R.id.profile_image)
            val mainlayout=view.findViewById<ConstraintLayout>(R.id.mainlayout)
    }
}