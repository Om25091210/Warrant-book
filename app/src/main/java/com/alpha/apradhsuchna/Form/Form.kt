package com.alpha.apradhsuchna.Form

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.alpha.apradhsuchna.R
import com.alpha.apradhsuchna.databinding.FragmentFormBinding
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.michaelrocks.paranoid.Obfuscate
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@Obfuscate
class form : Fragment() {

    lateinit var binding: FragmentFormBinding
    val PICK_FILE = 1
    var selected_uri_pdf = Uri.parse("")
    private var contextNullSafe: Context? = null
    var dialog: Dialog? = null
    var storageReference1: StorageReference? = null
    var dialog1: Dialog? = null
    var pdfpath = "CRIMINALS/"
    var pushkey: String? = null
    private var mAuth: FirebaseAuth? = null
    var user: FirebaseUser? = null
    var reference: DatabaseReference? = null
    val db = FirebaseFirestore.getInstance().collection("records")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding= FragmentFormBinding.inflate(inflater, container, false)
        if (contextNullSafe == null) getContextNullSafety()
        storageReference1 = FirebaseStorage.getInstance().reference.child(pdfpath)
        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser
        reference = FirebaseDatabase.getInstance().reference
        binding.submitTxt.setOnClickListener{
            submit_data()
        }
        binding.Upload.setOnClickListener{
            select_file()
        }
        binding.imageView4.setOnClickListener{
            back()
        }
        binding.putNumber.setOnClickListener{
            (getContextNullSafety() as FragmentActivity?)!!.supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.enter_from_right,
                    R.anim.exit_to_left,
                    R.anim.enter_from_left,
                    R.anim.exit_to_right
                )
                .add(R.id.drawer, Entries())
                .addToBackStack(null)
                .commit()
        }
        return binding.root
    }

    private fun submit_data() {
        if(binding.criminalNameEdt.text.toString().trim() != ""){
            if(binding.co.text.toString().trim() != ""){
                if(binding.psEdit.text.toString()!=""){
                    if(binding.caseNoEdt.text.toString()!=""){
                        if(selected_uri_pdf!=Uri.parse("")){
                            upload_to_database()
                        }
                        else{
                            val cal = Calendar.getInstance()
                            val simpleDateFormat = SimpleDateFormat(
                                "EEEE, dd MMMM yyyy HH:mm a",
                                Locale.getDefault()
                            )

                            val map= HashMap<String,String>()
                            map["criminal_name"]=binding.criminalNameEdt.text.toString().trim()
                            map["co"]=binding.co.text.toString().trim()
                            map["address"]=binding.address.text.toString().trim()
                            map["age"]=binding.age.text.toString().trim()
                            map["police_station"]=binding.psEdit.text.toString().trim()
                            map["case_no"]=binding.caseNoEdt.text.toString().trim()
                            map["section"]=binding.section.text.toString().trim()
                            map["court_name"]=binding.courtname.text.toString().trim()
                            map["more_details"]=binding.moredetails.text.toString().trim()
                            map["uid"]=user!!.uid
                            map["done"]="0"
                            map["uploaded_date"]=simpleDateFormat.format(cal.time)
                            pushkey = reference!!.push().key
                            map["key"]=pushkey!!
                            pushkey?.let {
                                db.document(it).set(map)
                            }
                            clear()
                            Snackbar.make(binding.lay1, "Recorded created successfully..", Snackbar.LENGTH_LONG)
                                .setActionTextColor(Color.parseColor("#000000"))
                                .setTextColor(Color.parseColor("#D99D1c"))
                                .setBackgroundTint(Color.parseColor("#000000"))
                                .show()
                        }
                    }
                    else{
                        binding.caseNoEdt.setError("Empty")
                        Snackbar.make(binding.lay1, "Case No. is empty.", Snackbar.LENGTH_LONG)
                            .setActionTextColor(Color.parseColor("#000000"))
                            .setTextColor(Color.parseColor("#D99D1c"))
                            .setBackgroundTint(Color.parseColor("#000000"))
                            .show()
                    }
                }
                else{
                    binding.psEdit.setError("Empty")
                    Snackbar.make(binding.lay1, "Police Station is empty.", Snackbar.LENGTH_LONG)
                        .setActionTextColor(Color.parseColor("#000000"))
                        .setTextColor(Color.parseColor("#D99D1c"))
                        .setBackgroundTint(Color.parseColor("#000000"))
                        .show()
                }
            }
            else{
                binding.co.setError("Empty")
                Snackbar.make(binding.lay1, "C/O is empty.", Snackbar.LENGTH_LONG)
                    .setActionTextColor(Color.parseColor("#000000"))
                    .setTextColor(Color.parseColor("#D99D1c"))
                    .setBackgroundTint(Color.parseColor("#000000"))
                    .show()
            }
        }
        else{
            binding.criminalNameEdt.setError("Empty")
            Snackbar.make(binding.lay1, "Criminal name is empty.", Snackbar.LENGTH_LONG)
                .setActionTextColor(Color.parseColor("#000000"))
                .setTextColor(Color.parseColor("#D99D1c"))
                .setBackgroundTint(Color.parseColor("#000000"))
                .show()
        }
    }

    private fun select_file() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select File"), PICK_FILE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                selected_uri_pdf = data.data
                val uriString: String = selected_uri_pdf.toString()
                val cursor: Cursor? = getContextNullSafety()?.getContentResolver()
                    ?.query(selected_uri_pdf, null, null, null, null)
                val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
                if (cursor != null) {
                    cursor.moveToFirst()
                }
                val name = nameIndex?.let { cursor.getString(it) }
                val str_txt = name + " (" + sizeIndex?.let { cursor.getLong(it) }?.let {
                    readableFileSize(
                        it
                    )
                } + ")"
                show_file_upload(str_txt)
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
    private fun upload_to_database() {
        dialog1 = Dialog(getContextNullSafety()!!)
        dialog1!!.setCancelable(false)
        dialog1!!.setContentView(R.layout.loading_dialog)
        dialog1!!.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent)
        Snackbar.make(binding.lay1, "Record Uploading...", Snackbar.LENGTH_LONG)
            .setActionTextColor(Color.parseColor("#000000"))
            .setTextColor(Color.parseColor("#D99D1c"))
            .setBackgroundTint(Color.parseColor("#000000"))
            .show()
        dialog1!!.show()
        val pdfstamp: String? = pushkey
        val filepath: StorageReference = storageReference1!!.child("$pdfstamp.png")
        filepath.putFile(selected_uri_pdf)
            .addOnSuccessListener { taskSnapshot1: UploadTask.TaskSnapshot ->
                taskSnapshot1.storage.downloadUrl.addOnCompleteListener { task1: Task<Uri> ->
                    val image_link = Objects.requireNonNull(task1.result).toString()
                    val cal = Calendar.getInstance()
                    val simpleDateFormat = SimpleDateFormat(
                        "EEEE, dd MMMM yyyy HH:mm a",
                        Locale.getDefault()
                    )
                    val map= HashMap<String,String>()
                    map["criminal_name"]=binding.criminalNameEdt.text.toString().trim()
                    map["co"]=binding.co.text.toString().trim()
                    map["address"]=binding.address.text.toString().trim()
                    map["age"]=binding.age.text.toString().trim()
                    map["police_station"]=binding.psEdit.text.toString().trim()
                    map["case_no"]=binding.caseNoEdt.text.toString().trim()
                    map["section"]=binding.section.text.toString().trim()
                    map["court_name"]=binding.courtname.text.toString().trim()
                    map["more_details"]=binding.moredetails.text.toString().trim()
                    map["uid"]=user!!.uid
                    map["image_link"]=image_link
                    map["done"]="0"
                    map["uploaded_date"]=simpleDateFormat.format(cal.time)
                    pushkey = reference!!.push().key
                    map["key"]=pushkey!!
                    pushkey?.let {
                        db.document(it).set(map)
                    }
                    dialog1!!.dismiss()
                    clear()
                    Snackbar.make(binding.lay1, "Record Uploaded Successfully.", Snackbar.LENGTH_LONG)
                        .setActionTextColor(Color.parseColor("#000000"))
                        .setTextColor(Color.parseColor("#D99D1c"))
                        .setBackgroundTint(Color.parseColor("#000000"))
                        .show()
                }
            }
    }
    fun clear(){
        binding.criminalNameEdt.text.clear()
        binding.co.text.clear()
        binding.address.text.clear()
        binding.age.text.clear()
        binding.psEdit.text.clear()
        binding.caseNoEdt.text.clear()
        binding.section.text.clear()
        binding.courtname.text.clear()
        binding.moredetails.text.clear()
        selected_uri_pdf=Uri.parse("")
    }
    private fun show_file_upload(str_txt: String) {
        dialog = Dialog(getContextNullSafety()!!)
        dialog!!.setCancelable(true)
        dialog!!.setContentView(R.layout.upload_dialog)
        dialog!!.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent)
        val file_name: TextView = dialog!!.findViewById<TextView>(R.id.file_name)
        file_name.text = str_txt
        val cancel: TextView = dialog!!.findViewById<TextView>(R.id.textView96)
        val yes: TextView = dialog!!.findViewById<TextView>(R.id.textView95)
        dialog!!.getWindow()?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog!!.show()
        cancel.setOnClickListener { vi: View? -> dialog!!.dismiss() }
        yes.setOnClickListener { vi: View? -> dialog!!.dismiss() }
    }
    fun readableFileSize(size: Long): String? {
        if (size <= 0) return "0"
        val units = arrayOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(
            size / Math.pow(
                1024.0,
                digitGroups.toDouble()
            )
        ) + " " + units[digitGroups]
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