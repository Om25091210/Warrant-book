package com.alpha.apradhsuchna

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.SearchView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alpha.apradhsuchna.Adapter.recordAdapter
import com.alpha.apradhsuchna.databinding.ActivityDashboardBinding
import com.alpha.apradhsuchna.model.record_data
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.core.ImageTranscoderType
import com.facebook.imagepipeline.core.MemoryChunkType
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import io.michaelrocks.paranoid.Obfuscate
import java.lang.Runnable
import java.util.*
import com.alpha.apradhsuchna.Form.form
import com.alpha.apradhsuchna.ViewModel.MyViewModel

@Obfuscate
class Dashboard : AppCompatActivity() {

    lateinit var lastVisible:DocumentSnapshot
    lateinit var binding:ActivityDashboardBinding
    var list: MutableList<record_data> = ArrayList()
    var list_all: MutableList<record_data> = ArrayList()
    var newlist: MutableList<record_data> = ArrayList()
    var list_s: MutableList<String> = ArrayList()
    var mylist: MutableList<record_data> = ArrayList()
    lateinit var query: Query
    lateinit var rootRef:CollectionReference
    var auth:FirebaseAuth?=null
    var user: FirebaseUser?=null
    private lateinit var viewModel: MyViewModel
    var filter_select:String="all"
    // Declare a boolean flag to track if the loadMoreData() method is in progress or not
    private var isLoading = false
    lateinit var recordAdapter:recordAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        binding=ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        rootRef = FirebaseFirestore.getInstance().collection("records")
        query = rootRef.orderBy("uploaded_date", Query.Direction.ASCENDING).limit(35) // Always have this limit as 15, 25, 35, 45 so on . . .
        // item ID 0 was clicked
        val gridLayoutManager = GridLayoutManager(this,2)

        auth=FirebaseAuth.getInstance()
        user=auth!!.currentUser

        binding.recyclerView.setItemViewCacheSize(500)
        binding.recyclerView.setDrawingCacheEnabled(true)
        binding.recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
        binding.recyclerView.setItemViewCacheSize(500)
        binding.recyclerView.setLayoutManager(gridLayoutManager)

        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this).get(MyViewModel::class.java)

        viewModel.recordDataList.observe(this) { recordDataList ->
            list_all = recordDataList.toMutableList()
            Log.e("Now fetched","${list_all.size}")
        }

        viewModel.loadData()

        val toggle = ActionBarDrawerToggle(this, binding.drawer1, binding.toolbar, R.string.open, R.string.close)
        binding.drawer1.addDrawerListener(toggle)
        toggle.drawerArrowDrawable.color = resources.getColor(R.color.use_orange)
        toggle.syncState()

        binding.entry.setOnClickListener{
            this@Dashboard.getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.enter_from_right,
                    R.anim.exit_to_left,
                    R.anim.enter_from_left,
                    R.anim.exit_to_right
                )
                .add(R.id.drawer, form())
                .addToBackStack(null)
                .commit()
        }
        val startActivityForResult = registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                binding.search.setQuery("", false)
                val data = result.data!!
                val result_voice = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val voice_text_Str: String = binding.search.query.toString()
                    .trim { it <= ' ' } + " " + result_voice!![0] + ""
                binding.search.setQuery(voice_text_Str.trim(),true)
                search(voice_text_Str.trim())
            }
        }

        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.e("search",query.toString()+"")
                // This method will be called when the user submits the query
                if (query != null) {
                    // Do something with the query, for example search in your data
                    search(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // This method will be called whenever the user types in the search box
                if(newText==""){
                    val recordAdapter= recordAdapter(list,this@Dashboard)
                    binding.recyclerView.setAdapter(recordAdapter);
                    recordAdapter.notifyDataSetChanged()
                }
                return false
            }
        })


        binding.voice.setOnClickListener{
            val languagePref = "hi"
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languagePref)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languagePref)
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languagePref)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Need to speak")

            try {
                startActivityForResult.launch(intent)
            } catch (a: ActivityNotFoundException) {
                Snackbar.make(binding.recyclerView, "Sorry your device not supported.", Snackbar.LENGTH_LONG)
                    .setActionTextColor(Color.parseColor("#000000"))
                    .setTextColor(Color.parseColor("#D99D1c"))
                    .setBackgroundTint(Color.parseColor("#000000"))
                    .show()
            }
        }
        Fresco.initialize(
            applicationContext,
            ImagePipelineConfig.newBuilder(applicationContext)
                .setMemoryChunkType(MemoryChunkType.BUFFER_MEMORY)
                .setImageTranscoderType(ImageTranscoderType.JAVA_TRANSCODER)
                .experiment().setNativeCodeDisabled(true)
                .build())
        binding.animationView.setOnClickListener{
            binding.animationView.playAnimation()
            query = rootRef.orderBy("uploaded_date", Query.Direction.ASCENDING).limit(35)
            get_data()
        }
        get_total()
        get_data()
        // Add a scroll listener to the RecyclerView
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // Check if the last visible item has been reached
                if (lastVisibleItemPosition == totalItemCount - 1) {
                    // Trigger your method here
                    query = query
                        .startAfter(lastVisible)
                        .limit(10)
                    loadMoreData(layoutManager,totalItemCount,lastVisibleItemPosition)
                }
            }
        })


        binding.navView!!.setNavigationItemSelectedListener(object :
            NavigationView.OnNavigationItemSelectedListener {
            var fragment: Fragment? = null
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.nav_about -> {

                    }
                    R.id.nav_share -> {

                    }
                    R.id.nav_developer -> {
                    }
                    R.id.nav_privacy -> {

                    }
                    R.id.nav_terms -> {

                    }
                    R.id.nav_logout -> {
                        auth!!.signOut()
                        startActivity(Intent(this@Dashboard, Login::class.java))
                        finish()
                    }
                }
                return true
            }
        })

        val dropDownMenu = PopupMenu(this, binding.filter)

        val menu = dropDownMenu.menu
        // add your items:
        // add your items:
        menu.add(0, 0, 0, "All")
        menu.add(0, 1, 0, "Name")
        menu.add(0, 2, 0, "C/O")
        menu.add(0, 3, 0, "Police Station")
        menu.add(0, 4, 0, "Address")
        menu.add(0, 5, 0, "Court Name")
        menu.add(0, 6, 0, "Age")
        menu.add(0, 7, 0, "Section")
        // OR inflate your menu from an XML:
        // OR inflate your menu from an XML:
        dropDownMenu.menuInflater.inflate(R.menu.menu_main, menu)
        dropDownMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                0 -> {
                    // item ID 0 was clicked
                    filter_select="all"
                    return@setOnMenuItemClickListener true
                }
                1 -> {
                    filter_select="name"
                    return@setOnMenuItemClickListener true
                }
                2 -> {
                    filter_select="co"
                    return@setOnMenuItemClickListener true
                }
                3 -> {
                    filter_select="ps"
                    return@setOnMenuItemClickListener true
                }
                4 -> {
                    filter_select="address"
                    return@setOnMenuItemClickListener true
                }
                5 -> {
                    filter_select="cn"
                    return@setOnMenuItemClickListener true
                }
                6 -> {
                    filter_select="age"
                    return@setOnMenuItemClickListener true
                }
                7 -> {
                    filter_select="section"
                    return@setOnMenuItemClickListener true
                }
            }
            false
        }
        binding.filter.setOnClickListener(View.OnClickListener { v: View? -> dropDownMenu.show() })
        val key=intent.getStringExtra("sending_msg_data")
        Log.e("key",key+"")
        if(key!=null){
            if(key!=""){
                val bundle=Bundle()
                bundle.putString("key",key)
                val view_details=view_details()
                view_details.arguments=bundle
                supportFragmentManager
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
        }
    }



    private fun get_total() {
        rootRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val count = task.result?.size() ?: 0
                binding.textTotal.text="Total - "+count
                // Do something with the count here
            } else {
                // Handle the error here
            }
        }
    }

    private fun search(voice_text_Str: String) {
        if (voice_text_Str == "") {
            val recordAdapter= recordAdapter(list,this)
            binding.recyclerView.setAdapter(recordAdapter);
            recordAdapter.notifyDataSetChanged()
        } else {
            val str_Args: Array<String> = voice_text_Str.lowercase(Locale.getDefault()).split(" ").toTypedArray()
            mylist.clear()
            var count = 0
            val c_list: MutableList<Int> = ArrayList()
            for (objects in list_all) {
                if(filter_select.equals("all"))
                    convert_to_list_all(objects)
                else if(filter_select.equals("name"))
                    convert_to_list_name(objects)
                else if(filter_select.equals("co"))
                    convert_to_list_co(objects)
                else if(filter_select.equals("ps"))
                    convert_to_list_ps(objects)
                else if(filter_select.equals("address"))
                    convert_to_list_address(objects)
                else if(filter_select.equals("age"))
                    convert_to_list_age(objects)
                else if(filter_select.equals("section"))
                    convert_to_list_section(objects)
                else
                    convert_to_list_cn(objects)
                for (s in list_s) {
                    println(s+ "")
                    for (str_arg in str_Args) {
                        println(str_arg + "")
                        if (s.contains(str_arg)) {
                            count++
                        }
                    }
                }
                c_list.add(count)
                println(c_list.toString() + "")
                if (count >= str_Args.size) mylist.add(objects)
                count = 0
            }
            val recordAdapter= recordAdapter(mylist,this)
            binding.recyclerView.setAdapter(recordAdapter);
            recordAdapter.notifyDataSetChanged()
        }
    }
    private fun convert_to_list_all(objects: record_data) {
        list_s.clear()
        try {
            list_s.add(objects.address!!.lowercase())
            list_s.add(objects.criminal_name!!.lowercase())
            list_s.add(objects.co!!.lowercase())
            list_s.add(objects.case_no!!.lowercase())
            list_s.add(objects.age!!.lowercase())
            list_s.add(objects.court_name!!.lowercase())
            list_s.add(objects.more_details!!.lowercase())
            list_s.add(objects.police_station!!.lowercase())
            list_s.add(objects.section!!.lowercase())
        } catch (e: NullPointerException) {
            println("Error")
        }
    }

    private fun convert_to_list_name(objects: record_data) {
        list_s.clear()
        try {
            list_s.add(objects.criminal_name!!.lowercase())
        } catch (e: NullPointerException) {
            println("Error")
        }
    }

    private fun convert_to_list_co(objects: record_data) {
        list_s.clear()
        try {
            list_s.add(objects.co!!.lowercase())
        } catch (e: NullPointerException) {
            println("Error")
        }
    }

    private fun convert_to_list_ps(objects: record_data) {
        list_s.clear()
        try {
            list_s.add(objects.police_station!!.lowercase())
        } catch (e: NullPointerException) {
            println("Error")
        }
    }

    private fun convert_to_list_address(objects: record_data) {
        list_s.clear()
        try {
            list_s.add(objects.address!!.lowercase())
        } catch (e: NullPointerException) {
            println("Error")
        }
    }

    private fun convert_to_list_cn(objects: record_data) {
        list_s.clear()
        try {
            list_s.add(objects.court_name!!.lowercase())
        } catch (e: NullPointerException) {
            println("Error")
        }
    }

    private fun convert_to_list_age(objects: record_data) {
        list_s.clear()
        try {
            list_s.add(objects.age!!.lowercase())
        } catch (e: NullPointerException) {
            println("Error")
        }
    }

    private fun convert_to_list_section(objects: record_data) {
        list_s.clear()
        try {
            list_s.add(objects.section!!.lowercase())
        } catch (e: NullPointerException) {
            println("Error")
        }
    }

    private fun get_data() {
        list.clear()
        query.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documentSnapshots = task.result
                if (documentSnapshots != null) {
                    for (document in documentSnapshots) {
                        Log.e("test", document.id)
                        val productModel: record_data = document.toObject(record_data::class.java)
                        list.add(productModel)
                    }

                    // Get the last visible document
                    lastVisible = documentSnapshots.documents[documentSnapshots.size() - 1]

                    Handler(Looper.myLooper()!!).postDelayed(object :Runnable{
                        override fun run() {
                            binding.animationView.pauseAnimation()
                        }
                    },1000)
                    recordAdapter= recordAdapter(list,this)
                    binding.recyclerView.setAdapter(recordAdapter);
                    recordAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    // Method to load more data
    private fun loadMoreData(
        layoutManager: LinearLayoutManager,
        totalItemCount: Int,
        lastVisibleItemPosition: Int
    ) {
        // If the loadMoreData() method is already in progress, return
        if (isLoading) return

        // Set the flag to true to indicate that the loadMoreData() method is in progress
        isLoading = true

        // Implement your logic to load more data here
        query.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documentSnapshots = task.result
                if (documentSnapshots != null) {
                    for (document in documentSnapshots) {
                        val productModel: record_data = document.toObject(record_data::class.java)
                        newlist.add(productModel)
                        Log.e("List size","${newlist.size}")
                    }

                    // Get the last visible document
                    lastVisible = documentSnapshots.documents[documentSnapshots.size() - 1]

                    // Update your data with the new items
                    list.addAll(newlist)

                    // Notify the adapter that new items have been inserted at the end of the list
                    val adapter = binding.recyclerView.adapter
                    adapter?.notifyItemRangeInserted(list.size - newlist.size, newlist.size)

                    // Restore the previous position and offset of the RecyclerView
                    layoutManager.scrollToPositionWithOffset(totalItemCount, lastVisibleItemPosition)
                    newlist.clear()
                }
            }
            // Set the flag to false to indicate that the loadMoreData() method has completed
            isLoading = false
        }
    }

}