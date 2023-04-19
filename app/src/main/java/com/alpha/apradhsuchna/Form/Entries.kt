package com.alpha.apradhsuchna.Form

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.alpha.apradhsuchna.databinding.FragmentEntriesBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import java.util.*
import com.alpha.apradhsuchna.R

class Entries : Fragment() {

    lateinit var binding: FragmentEntriesBinding
    private var contextNullSafe: Context? = null
    var district= mutableListOf<String>()
    var ps_list= mutableListOf<String>()
    var reference: DatabaseReference? = null
    var prefix: String? = null
    var reference_phone:DatabaseReference? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding= FragmentEntriesBinding.inflate(inflater, container, false)
        if (contextNullSafe == null) getContextNullSafety()
        reference = FirebaseDatabase.getInstance().reference.child("admin")
        reference_phone = FirebaseDatabase.getInstance().reference.child("Phone numbers")
        get_districts_phone()
        binding.linearLayout7.setOnClickListener(View.OnClickListener { v: View? ->
            binding.submitTxt.setVisibility(View.VISIBLE)
            binding.nameEdt.setVisibility(View.VISIBLE)
            binding.textView11.setVisibility(View.GONE)
            binding.linearLayout17.setVisibility(View.GONE)
            binding.adminNum.setVisibility(View.GONE)
            binding.num.setVisibility(View.GONE)
            binding.acDistrict.visibility = View.GONE
            binding.prRoll.setVisibility(View.GONE)
            binding.policeStation.visibility = View.GONE
            binding.subTxt.setVisibility(View.GONE)
        })

        binding.addStation.setOnClickListener(View.OnClickListener { v: View? ->
            binding.submitTxt.setVisibility(View.GONE)
            binding.nameEdt.setVisibility(View.GONE)
            binding.num.setVisibility(View.VISIBLE)
            binding.linearLayout17.setVisibility(View.VISIBLE)
            binding.adminNum.setVisibility(View.VISIBLE)
            binding.textView11.setVisibility(View.VISIBLE)
            binding.acDistrict.visibility = View.VISIBLE
            binding.prRoll.setVisibility(View.VISIBLE)
            binding.policeStation.visibility = View.VISIBLE
            binding.subTxt.setVisibility(View.VISIBLE)
        })

        binding.submitTxt.setOnClickListener(View.OnClickListener { v: View? ->
            if (binding.nameEdt.getText().toString().trim { it <= ' ' } != "") {
                if (binding.nameEdt.getText().toString().contains("/")) {
                    binding.nameEdt.setText(
                        binding.nameEdt.getText().toString().replace("[^-()a-zA-Z0-9]".toRegex(), "")
                    )
                }
                reference!!.child(binding.nameEdt.getText().toString().trim { it <= ' ' })
                    .setValue(binding.nameEdt.getText().toString().trim { it <= ' ' }
                        .replace("[^-()a-zA-Z0-9]".toRegex(), ""))
                binding.nameEdt.setText("")
                Snackbar.make(binding.linearLayout7, "Number Uploaded!!", Snackbar.LENGTH_LONG)
                    .setActionTextColor(Color.parseColor("#000000"))
                    .setTextColor(Color.parseColor("#D99D1c"))
                    .setBackgroundTint(Color.parseColor("#000000"))
                    .show()
            }
        })
        binding.subTxt.setOnClickListener(View.OnClickListener { v: View? ->
            if (binding.num.getText().toString().trim { it <= ' ' } != ""
                && binding.acDistrict.text.toString().trim { it <= ' ' } != ""
                && binding.policeStation.text.toString().trim { it <= ' ' } != "") {
                if (binding.policeStation.text.toString().contains("/")) {
                    binding.policeStation.setText(
                        binding.policeStation.text.toString()
                            .replace("[^-()a-zA-Z0-9]".toRegex(), "")
                    )
                }
                reference_phone!!.child(
                    binding.acDistrict.text.toString().uppercase(Locale.getDefault())
                        .trim { it <= ' ' })
                    .child(
                        prefix + " " + binding.policeStation.text.toString()
                            .uppercase(Locale.getDefault()).trim { it <= ' ' }
                            .replace("[^-()a-zA-Z0-9]".toRegex(), ""))
                    .setValue(binding.num.getText().toString().trim { it <= ' ' }
                        .replace("[^-()a-zA-Z0-9]".toRegex(), ""))
                binding.num.setText("")
                binding.acDistrict.setText("")
                binding.policeStation.setText("")
                Snackbar.make(binding.linearLayout7, "Number Uploaded!!", Snackbar.LENGTH_LONG)
                    .setActionTextColor(Color.parseColor("#000000"))
                    .setTextColor(Color.parseColor("#D99D1c"))
                    .setBackgroundTint(Color.parseColor("#000000"))
                    .show()
            }
        })
        binding.acDistrict!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (binding.acDistrict!!.text.toString().trim { it <= ' ' }.contains("/")) {
                    binding.acDistrict!!.setText(
                        binding.acDistrict!!.text.toString().replace("[^-()a-zA-Z0-9]".toRegex(), "")
                    )
                    Toast.makeText(contextNullSafe, "Wrong entry.", Toast.LENGTH_SHORT).show()
                }
                get_police_station(binding.acDistrict!!.text.toString().trim { it <= ' ' }
                    .replace("[^-()a-zA-Z0-9]".toRegex(), ""))
            }
        })
        binding.ps.setOnClickListener{
            prefix = "PS"
            binding.ps.setBackgroundResource(R.drawable.bg_bg_active)
            binding.csp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sdop.setBackgroundResource(R.drawable.border_amount_bg)
            binding.asp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dsp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.ig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.aig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dig.setBackgroundResource(R.drawable.border_amount_bg)
        }

        binding.csp.setOnClickListener {
            prefix = "CSP"
            binding.ps.setBackgroundResource(R.drawable.border_amount_bg)
            binding.csp.setBackgroundResource(R.drawable.bg_bg_active)
            binding.sdop.setBackgroundResource(R.drawable.border_amount_bg)
            binding.asp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dsp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.ig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.aig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dig.setBackgroundResource(R.drawable.border_amount_bg)
        }

        binding.sdop.setOnClickListener{
            prefix = "SDOP"
            binding.ps.setBackgroundResource(R.drawable.border_amount_bg)
            binding.csp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sdop.setBackgroundResource(R.drawable.bg_bg_active)
            binding.asp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dsp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.ig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.aig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dig.setBackgroundResource(R.drawable.border_amount_bg)
        }

        binding.asp.setOnClickListener{
            prefix = "ASP"
            binding.ps.setBackgroundResource(R.drawable.border_amount_bg)
            binding.csp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sdop.setBackgroundResource(R.drawable.border_amount_bg)
            binding.asp.setBackgroundResource(R.drawable.bg_bg_active)
            binding.sp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dsp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.ig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.aig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dig.setBackgroundResource(R.drawable.border_amount_bg)
        }

        binding.sp.setOnClickListener{
            prefix = "SP"
            binding.ps.setBackgroundResource(R.drawable.border_amount_bg)
            binding.csp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sdop.setBackgroundResource(R.drawable.border_amount_bg)
            binding.asp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sp.setBackgroundResource(R.drawable.bg_bg_active)
            binding.dsp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.ig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.aig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dig.setBackgroundResource(R.drawable.border_amount_bg)
        }

        binding.dsp.setOnClickListener{
            prefix = "DSP"
            binding.ps.setBackgroundResource(R.drawable.border_amount_bg)
            binding.csp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sdop.setBackgroundResource(R.drawable.border_amount_bg)
            binding.asp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dsp.setBackgroundResource(R.drawable.bg_bg_active)
            binding.ig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.aig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dig.setBackgroundResource(R.drawable.border_amount_bg)
        }
        binding.ig.setOnClickListener{
            prefix = "IG"
            binding.ps.setBackgroundResource(R.drawable.border_amount_bg)
            binding.csp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sdop.setBackgroundResource(R.drawable.border_amount_bg)
            binding.asp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dsp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.aig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.ig.setBackgroundResource(R.drawable.bg_bg_active)
        }
        binding.aig.setOnClickListener{
            prefix = "AIG"
            binding.ps.setBackgroundResource(R.drawable.border_amount_bg)
            binding.csp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sdop.setBackgroundResource(R.drawable.border_amount_bg)
            binding.asp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dsp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.ig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.aig.setBackgroundResource(R.drawable.bg_bg_active)
        }
        binding.dig.setOnClickListener{
            prefix = "DIG"
            binding.ps.setBackgroundResource(R.drawable.border_amount_bg)
            binding.csp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sdop.setBackgroundResource(R.drawable.border_amount_bg)
            binding.asp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.sp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dsp.setBackgroundResource(R.drawable.border_amount_bg)
            binding.ig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.aig.setBackgroundResource(R.drawable.border_amount_bg)
            binding.dig.setBackgroundResource(R.drawable.bg_bg_active)
        }

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fm = (getContextNullSafety() as FragmentActivity?)!!.supportFragmentManager
                val ft = fm.beginTransaction()
                if (fm.backStackEntryCount > 0) {
                    fm.popBackStack()
                }
                ft.commit()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)


        return binding.root
    }
    private fun get_districts_phone() {
        reference_phone!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds in snapshot.children) {
                    district.add(ds.key!!)
                    //Creating the instance of ArrayAdapter containing list of language names
                    val adapter = ArrayAdapter(
                        getContextNullSafety()!!,
                        R.layout.select_dialog_item,
                        district
                    )
                    //Getting the instance of AutoCompleteTextView
                    binding.acDistrict.setThreshold(1) //will start working from first character
                    binding.acDistrict.setAdapter<ArrayAdapter<String>>(adapter) //setting the adapter data into the AutoCompleteTextView
                    binding.acDistrict.setTextColor(Color.RED)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun get_police_station(district: String) {
        reference_phone!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.child(district).children) {
                    if (dataSnapshot.key!!.substring(0, 2) == "PS") {
                        ps_list.add(dataSnapshot.key!!.substring(3))
                        //Creating the instance of ArrayAdapter containing list of language names
                        val adapter = ArrayAdapter(
                            getContextNullSafety()!!,
                            R.layout.select_dialog_item,
                            ps_list
                        )
                        //Getting the instance of AutoCompleteTextView
                        binding.policeStation.setThreshold(1) //will start working from first character
                        binding.policeStation.setAdapter<ArrayAdapter<String>>(adapter) //setting the adapter data into the AutoCompleteTextView
                        binding.policeStation.setTextColor(Color.RED)
                    }
                }
                Log.e("PS = ", ps_list.toString() + "")
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
    /**CALL THIS IF YOU NEED CONTEXT */
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
    private fun back() {
        val fm = (getContextNullSafety() as FragmentActivity?)!!.supportFragmentManager
        val ft = fm.beginTransaction()
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
        }
        ft.commit()
    }
}