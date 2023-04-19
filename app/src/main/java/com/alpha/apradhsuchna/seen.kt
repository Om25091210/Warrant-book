package com.alpha.apradhsuchna

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alpha.apradhsuchna.Adapter.commentAdapter
import com.alpha.apradhsuchna.Adapter.seenAdapter
import com.alpha.apradhsuchna.databinding.FragmentSeenBinding
import com.alpha.apradhsuchna.databinding.FragmentViewDetailsBinding
import com.alpha.apradhsuchna.model.Comments
import com.alpha.apradhsuchna.model.seenData
import com.google.firebase.firestore.FirebaseFirestore
import io.michaelrocks.paranoid.Obfuscate
import java.util.ArrayList
@Obfuscate
class seen : Fragment() {

    lateinit var binding: FragmentSeenBinding
    val db = FirebaseFirestore.getInstance()
    lateinit var key:String
    private var contextNullSafe: Context? = null
    val seen_list = ArrayList<seenData>()
    var seenAdapter = seenAdapter(seen_list)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding= FragmentSeenBinding.inflate(inflater, container, false)
        if (contextNullSafe == null) getContextNullSafety()
        arguments?.let {
            key = it.getString("key").toString().trim()
        }
        val linearLayoutManager = LinearLayoutManager(context)
        binding.rvHome.setItemViewCacheSize(500)
        binding.rvHome.setDrawingCacheEnabled(true)
        binding.rvHome.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
        binding.rvHome.setItemViewCacheSize(500)
        binding.rvHome.layoutManager = linearLayoutManager
        seens()
        return binding.root
    }
    fun seens(){
        db.collection("records")
            .document(key)
            .collection("seen")
            .get()
            .addOnSuccessListener { result ->
                // Handle successful retrieval of comments
                for (document in result) {
                    val seen = seenData(document.data.keys.toList().get(0)+"", "${document.data.values.toList().get(0)}")
                    seen_list.add(seen)
                    binding.progressBar2.visibility=View.GONE
                }
                seenAdapter = seenAdapter(seen_list)
                binding.rvHome.adapter = seenAdapter
                seenAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("Info","Failed")
            }
        binding.imageView4.setOnClickListener{
            back()
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