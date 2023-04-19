package com.alpha.apradhsuchna

import com.alpha.apradhsuchna.fcm.topic
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alpha.apradhsuchna.Adapter.commentAdapter
import com.alpha.apradhsuchna.databinding.FragmentViewDetailsBinding
import com.alpha.apradhsuchna.fcm.Specific
import com.alpha.apradhsuchna.model.Comments
import com.alpha.apradhsuchna.model.UsersData
import com.alpha.apradhsuchna.model.record_data
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.core.ImageTranscoderType
import com.facebook.imagepipeline.core.MemoryChunkType
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.michaelrocks.paranoid.Obfuscate
import java.text.SimpleDateFormat
import java.util.*

@Obfuscate
class view_details : Fragment() {

    lateinit var binding:FragmentViewDetailsBinding
    var list: MutableList<record_data> = ArrayList()
    private var contextNullSafe: Context? = null
    val db = FirebaseFirestore.getInstance()
    lateinit var key:String
    lateinit var auth:FirebaseAuth
    lateinit var user: FirebaseUser
    lateinit var userref:DocumentReference
    lateinit var seenRef:DocumentReference
    var user_model: UsersData? = UsersData()
    val comments = ArrayList<Comments>()
    var commetAdapter = commentAdapter(comments)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentViewDetailsBinding.inflate(inflater, container, false)
        if (contextNullSafe == null) getContextNullSafety()
        arguments?.let {
            key = it.getString("key").toString().trim()
        }

        auth= FirebaseAuth.getInstance()
        user= auth.currentUser!!

        Fresco.initialize(
            context,
            ImagePipelineConfig.newBuilder(context)
                .setMemoryChunkType(MemoryChunkType.BUFFER_MEMORY)
                .setImageTranscoderType(ImageTranscoderType.JAVA_TRANSCODER)
                .experiment().setNativeCodeDisabled(true)
                .build())

        val linearLayoutManager = LinearLayoutManager(context)
        binding.recyclerview.setItemViewCacheSize(500)
        binding.recyclerview.setDrawingCacheEnabled(true)
        binding.recyclerview.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
        binding.recyclerview.setItemViewCacheSize(500)
        binding.recyclerview.setLayoutManager(linearLayoutManager)

        val rootRef = FirebaseFirestore.getInstance().collection("records").document(key)
        userref = FirebaseFirestore.getInstance().collection("users").document(user.uid)
        seenRef = FirebaseFirestore.getInstance().collection("records")
            .document(key)
            .collection("seen")
            .document(user.uid)

        check_admin()
        rootRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document != null && document.exists()) {
                    val productModel: record_data? = document.toObject(record_data::class.java)
                    list.add(productModel!!)
                    get_comments()
                    get_Details()
                } else {
                    // handle case where document does not exist
                    Log.e("reverse", "No data")
                }
            } else {
                // handle any errors
                Log.d("TAG", "Error getting document: ", task.exception)
            }
        }

        binding.btnSend.setOnClickListener{
            if(binding.editWriteMessage.text.toString().trim()!=""){
                val ref=rootRef.collection("comments")
                // Get the current time as a Date object
                val currentTime = Calendar.getInstance().time
                val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
                val formattedTime = timeFormat.format(currentTime)
                // Create a new comment
                val comment=Comments(user_model!!.name,formattedTime,binding.editWriteMessage.text.toString().trim(),user.uid)

                comments.add(0,comment)
                commetAdapter.notifyDataSetChanged()
                // Add the new comment with a unique ID to the comments subcollection
                val topic= topic()
                topic.noti(user_model!!.name
                    ,binding.editWriteMessage.text.toString().trim()+". Tap to see!!!"
                    ,key,"")
                binding.editWriteMessage.text.clear()
                ref.document()
                    .set(comment)
                    .addOnSuccessListener { documentReference ->
                        Log.d("TAG", "New comment added with ID")
                    }
                    .addOnFailureListener { e ->
                        Log.w("TAG", "Error adding new comment", e)
                    }
            }
        }
        binding.done.setOnClickListener{
            val dialogD = Dialog(getContextNullSafety()!!)
            dialogD.setCancelable(true)
            dialogD.setContentView(R.layout.dialog_for_sure)
            dialogD.getWindow()!!.setBackgroundDrawableResource(android.R.color.transparent)
            val cancel: TextView = dialogD.findViewById<TextView>(R.id.textView96)
            val text: TextView = dialogD.findViewById<TextView>(R.id.textView94)
            text.text = "Mark as done?"
            val yes: TextView = dialogD.findViewById<TextView>(R.id.textView95)
            dialogD.getWindow()?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialogD.show()
            cancel.setOnClickListener { vi: View? -> dialogD.dismiss() }
            yes.setOnClickListener{
                val newData = hashMapOf(
                    "done" to "1"
                )
                rootRef.update(newData as Map<String, Any>)

                val topic= topic()
                topic.noti(user_model!!.name
                    ,"Marked a record as DONE. Tap to see!!!."
                    ,key,"")
                Log.e("msg","msg")
                Snackbar.make(binding.lay1, "Record marked as DONE successfully..", Snackbar.LENGTH_LONG)
                    .setActionTextColor(Color.parseColor("#000000"))
                    .setTextColor(Color.parseColor("#D99D1c"))
                    .setBackgroundTint(Color.parseColor("#000000"))
                    .show()
                dialogD.dismiss()
            }
        }
        binding.imageView4.setOnClickListener{
            back()
        }
        binding.seen.setOnClickListener{
            val bundle=Bundle()
            bundle.putString("key",key)
            val seen=seen()
            seen.arguments=bundle
            (context as FragmentActivity).supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.enter_from_right,
                    R.anim.exit_to_left,
                    R.anim.enter_from_left,
                    R.anim.exit_to_right
                )
                .add(R.id.drawer, seen)
                .addToBackStack(null)
                .commit()
        }
        binding.delete.setOnClickListener{
            val dialogD = Dialog(getContextNullSafety()!!)
            dialogD.setCancelable(true)
            dialogD.setContentView(R.layout.dialog_for_sure)
            dialogD.getWindow()!!.setBackgroundDrawableResource(android.R.color.transparent)
            val cancel: TextView = dialogD.findViewById<TextView>(R.id.textView96)
            val text: TextView = dialogD.findViewById<TextView>(R.id.textView94)
            text.text = "Do you want to delete?"
            val yes: TextView = dialogD.findViewById<TextView>(R.id.textView95)
            dialogD.getWindow()?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialogD.show()
            cancel.setOnClickListener { vi: View? -> dialogD.dismiss() }
            yes.setOnClickListener {
                rootRef.delete()
                Snackbar.make(
                    binding.lay1,
                    "Record deleted successfully, Press back and refresh.",
                    Snackbar.LENGTH_LONG
                )
                    .setActionTextColor(Color.parseColor("#000000"))
                    .setTextColor(Color.parseColor("#D99D1c"))
                    .setBackgroundTint(Color.parseColor("#000000"))
                    .show()
                dialogD.dismiss()
            }
        }
        return binding.root
    }

    private fun check_admin() {
        userref.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document != null && document.exists()) {
                    user_model = document.toObject(UsersData::class.java)
                    if(user_model!!.role=="admin"){
                        binding.done.visibility=View.VISIBLE
                        binding.delete.visibility=View.VISIBLE
                    }
                    mark_seen()
                } else {
                    // handle case where document does not exist
                    Log.e("reverse", "No data")
                }
            } else {
                // handle any errors
                Log.d("TAG", "Error getting document: ", task.exception)
            }
        }
    }

    private fun mark_seen() {
        seenRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (!document.exists()) {
                    // create a new document with the specified field
                    val data = hashMapOf(user_model!!.phone to user_model!!.name)
                    seenRef.set(data).addOnSuccessListener {
                        Log.d(TAG, "Document created successfully")
                    }.addOnFailureListener { e ->
                        Log.w(TAG, "Error creating document", e)
                    }
                } else {
                    Log.d(TAG, "Document already exists")
                }
            } else {
                Log.d(TAG, "Error getting document: ", task.exception)
            }
        }
    }

    fun back() {
        val fm = (getContextNullSafety() as FragmentActivity?)!!.supportFragmentManager
        val ft = fm.beginTransaction()
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
        }
        ft.commit()
    }

    private fun get_comments() {
        db.collection("records")
            .document(key)
            .collection("comments")
            .get()
            .addOnSuccessListener { result ->
                // Handle successful retrieval of comments
                for (document in result) {
                    val comment = document.toObject(Comments::class.java)
                    comments.add(comment)
                }
                commetAdapter = commentAdapter(comments)
                binding.recyclerview.adapter = commetAdapter
                commetAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
               Log.e("Info","Failed")
            }
    }

    private fun get_Details() {
        if(list[0].image_link!=null) {
            try {
                val uri = Uri.parse(list[0].image_link)
                binding.profileImage.setImageURI(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        else{
            binding.profileImage.setImageURI("")
        }
        binding.textView11.text=list[0].criminal_name
        binding.time.text=list[0].uploaded_date
        binding.co.text=list[0].co
        binding.address.text=list[0].address
        binding.ps.text=list[0].police_station
        binding.caseNo.text=list[0].case_no
        binding.sec.text=list[0].section
        binding.court.text=list[0].court_name
        binding.des.text=list[0].more_details
    }
    fun getContextNullSafety(): Context? {
        if (context != null) return context
        if (activity != null) return activity
        if (contextNullSafe != null) return contextNullSafe
        if (view != null && requireView().context != null) return requireView().context
        if (requireContext() != null) return requireContext()
        if (requireActivity() != null) return requireActivity()
        return if (requireView() != null && requireView().context != null) requireView().context else null
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        contextNullSafe = context
    }
}