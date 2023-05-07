package com.alpha.apradhsuchna

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
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
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alpha.apradhsuchna.Adapter.recordAdapter
import com.alpha.apradhsuchna.Form.form
import com.alpha.apradhsuchna.Location.LocationFetcher
import com.alpha.apradhsuchna.ViewModel.DataState
import com.alpha.apradhsuchna.ViewModel.MyViewModel
import com.alpha.apradhsuchna.databinding.ActivityDashboardBinding
import com.alpha.apradhsuchna.model.UsersData
import com.alpha.apradhsuchna.model.record_data
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.core.ImageTranscoderType
import com.facebook.imagepipeline.core.MemoryChunkType
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.michaelrocks.paranoid.Obfuscate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


@Obfuscate
class Dashboard() : AppCompatActivity(),LocationFetcher.OnLocationFetchedListener {

    lateinit var lastVisible:DocumentSnapshot
    lateinit var binding:ActivityDashboardBinding
    var list: MutableList<record_data> = ArrayList()
    var list_all: MutableList<record_data> = ArrayList()
    var newlist: MutableList<record_data> = ArrayList()
    var list_s: MutableList<String> = ArrayList()
    var mylist: MutableList<record_data> = ArrayList()
    lateinit var query: Query
    lateinit var rootRef:CollectionReference
    var scroll:Boolean=true
    lateinit var userref: DocumentReference
    var auth:FirebaseAuth?=null
    var user: FirebaseUser?=null
    private val LOCATION_PERMISSION_CODE = 123
    private val GPS_REQUEST_CODE = 456
    private lateinit var viewModel: MyViewModel
    var filter_select:String="all"
    // Declare a boolean flag to track if the loadMoreData() method is in progress or not
    private var isLoading = false
    private var fetched_address = ""
    private var total = 0
    var user_model: UsersData? = UsersData()
    lateinit var recordAdapter:recordAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        binding=ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val ref=FirebaseFirestore.getInstance()
        rootRef = ref.collection("records")
        query = rootRef.orderBy("uploaded_date", Query.Direction.ASCENDING).limit(35) // Always have this limit as 15, 25, 35, 45 so on . . .
        // item ID 0 was clicked

        val gridLayoutManager = GridLayoutManager(this,2)

        auth=FirebaseAuth.getInstance()
        user=auth!!.currentUser
        userref = ref.collection("users").document(user!!.uid)

        binding.recyclerView.setItemViewCacheSize(500)
        binding.recyclerView.setDrawingCacheEnabled(true)
        binding.recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
        binding.recyclerView.setItemViewCacheSize(500)
        binding.recyclerView.setLayoutManager(gridLayoutManager)

        setSupportActionBar(binding.toolbar)

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
                    binding.recyclerView.setAdapter(recordAdapter)
                    scroll=true
                    binding.textTotal.text="Total - "+total
                    Log.e("Scroll","TRUE")
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
        check_admin()
        binding.animationView.setOnClickListener{
            binding.animationView.playAnimation()
            query = rootRef.orderBy("uploaded_date", Query.Direction.ASCENDING).limit(35)
            get_data()
        }
        get_total()
        get_data()
        viewModel = ViewModelProvider(this).get(MyViewModel::class.java)

        recordAdapter = recordAdapter(list_all,this)
        binding.recyclerView.adapter = recordAdapter

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.recordDataState.collect { dataState ->
                when (dataState) {
                    is DataState.Loading -> {
                        // Show loading state
                    }
                    is DataState.Success -> {
                        val recordDataList = dataState.data
                        list_all = recordDataList.toMutableList()
                        Log.e("Now fetched","${list_all.size}")
                    }
                    is DataState.Error -> {
                        // Show error state
                    }
                }
            }
        }

        viewModel.refreshData()


        // Add a scroll listener to the RecyclerView
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // Check if the last visible item has been reached
                if (lastVisibleItemPosition == totalItemCount - 1 && scroll) {
                    // Trigger your method here
                    query = query
                        .startAfter(lastVisible)
                        .limit(10)
                    loadMoreData(layoutManager,totalItemCount,lastVisibleItemPosition)
                }
            }
        })

        // Check if the location permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the location permission if it has not been granted yet
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
        } else {
            // The location permission has already been granted
            // Check if GPS is enabled
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // GPS is disabled, show the location settings screen
                val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(settingsIntent, GPS_REQUEST_CODE)
            } else {
                // GPS is enabled, call the fetchLocation() method to fetch the user's location
                val locationFetcher = LocationFetcher(this)
                locationFetcher.setOnLocationFetchedListener(this)
                locationFetcher.fetchLocation()
            }
        }

        binding.navView.setNavigationItemSelectedListener(object :
            NavigationView.OnNavigationItemSelectedListener {
            var fragment: Fragment? = null
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.nav_about -> {

                    }
                    R.id.nav_share -> {
                        binding.navView.getMenu().getItem(1).setCheckable(false)
                        val title = """
                            *Warrant Book*
                            
                           In addition to managing records, this tool can help you streamline your workflows and improve collaboration among team members. By providing access to records across multiple devices and locations, team members can work together more efficiently and effectively. Why go for papers when we can view the details in just clicks!!!.. Tap on the below link to download
                            """.trimIndent() //Text to be shared

                        val sharingIntent = Intent(Intent.ACTION_SEND)
                        sharingIntent.type = "text/plain"
                        sharingIntent.putExtra(
                            Intent.EXTRA_TEXT, """
     $title     This is a playstore link to download.. https://play.google.com/store/apps/details?id=$packageName
     """.trimIndent()
                        )
                        startActivity(Intent.createChooser(sharingIntent, "Share using"))

                    }
                    R.id.nav_developer -> {
                    }
                    R.id.nav_privacy -> {

                    }
                    R.id.nav_terms -> {

                    }
                    R.id.nav_logout -> {
                        binding.navView.getMenu().getItem(5).setCheckable(false)
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
        menu.add(0, 8, 0, "Location")
        // OR inflate your menu from an XML:
        // OR inflate your menu from an XML:
        dropDownMenu.menuInflater.inflate(R.menu.menu_main, menu)
        dropDownMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                0 -> {
                    // item ID 0 was clicked
                    filter_select="all"
                    binding.filter.text = "All"
                    return@setOnMenuItemClickListener true
                }
                1 -> {
                    filter_select="name"
                    binding.filter.text="Name"
                    return@setOnMenuItemClickListener true
                }
                2 -> {
                    filter_select="co"
                    binding.filter.text="CO"
                    return@setOnMenuItemClickListener true
                }
                3 -> {
                    filter_select="ps"
                    binding.filter.text="Police Station"
                    return@setOnMenuItemClickListener true
                }
                4 -> {
                    filter_select="address"
                    binding.filter.text="Address"
                    return@setOnMenuItemClickListener true
                }
                5 -> {
                    filter_select="cn"
                    binding.filter.text="Court Name"
                    return@setOnMenuItemClickListener true
                }
                6 -> {
                    filter_select="age"
                    binding.filter.text="Age"
                    return@setOnMenuItemClickListener true
                }
                7 -> {
                    filter_select="section"
                    binding.filter.text="Section"
                    return@setOnMenuItemClickListener true
                }
                8 -> {
                    binding.filter.text="Location"
                    show_data_of_address(fetched_address)
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
    private fun check_admin() {
        userref.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document != null && document.exists()) {
                    user_model = document.toObject(UsersData::class.java)
                    if(user_model!!.role=="admin"){
                        binding.entry.visibility=View.VISIBLE
                    }
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
    // Handle the result of the location settings screen
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GPS_REQUEST_CODE) {
            // Check if GPS is enabled after the user has returned from the location settings screen
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // GPS is enabled, call the fetchLocation() method to fetch the user's location
                val locationFetcher = LocationFetcher(this)
                locationFetcher.fetchLocation()
            } else {
                // GPS is still disabled, handle this case as needed
            }
        }
    }

    // Handle the result of the location permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // The location permission has been granted
            // Call the fetchLocation() method to fetch the user's location
            val locationFetcher = LocationFetcher(this)
            locationFetcher.setOnLocationFetchedListener(this)
            locationFetcher.fetchLocation()
            Log.e("Address","${"longitude"}")
        } else {
            // The location permission has been denied
            // Handle this case as needed
        }
    }

    private fun get_total() {
        rootRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val count = task.result?.size() ?: 0
                binding.textTotal.text="Total - "+count
                total=count
                // Do something with the count here
            } else {
                // Handle the error here
            }
        }
    }

    private fun search(voice_text_Str: String) {
        if (voice_text_Str == "") {
            val recordAdapter= recordAdapter(list,this)
            binding.recyclerView.setAdapter(recordAdapter)
            scroll=true
            binding.textTotal.text="Total - "+total
            Log.e("Scroll","TRUE")
            recordAdapter.notifyDataSetChanged()
        } else {
            Log.e("Scroll","TRUE")
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
            binding.recyclerView.setAdapter(recordAdapter)
            scroll=false
            recordAdapter.notifyItemRangeInserted(0, mylist.size)
            binding.textTotal.text="Total - "+mylist.size
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
        query.get().addOnSuccessListener { documentSnapshots ->
            if (!documentSnapshots.isEmpty) {
                for (document in documentSnapshots) {
                    Log.e("test", document.id)
                    val productModel: record_data = document.toObject(record_data::class.java)
                    list.add(productModel)
                }
                scroll=true
                // Get the last visible document
                lastVisible = documentSnapshots.documents[documentSnapshots.size() - 1]
                list.shuffle()
                binding.animationView.pauseAnimation()
                recordAdapter= recordAdapter(list,this)
                binding.recyclerView.setAdapter(recordAdapter);
                recordAdapter.notifyDataSetChanged()
            }
        }.addOnFailureListener { exception ->
            // Handle exceptions here
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
        scroll=true
        // Set the flag to true to indicate that the loadMoreData() method is in progress
        isLoading = true

        // Implement your logic to load more data here
        if (lastVisible != null) {
            query.startAfter(lastVisible).get().addOnSuccessListener { documentSnapshots ->
                if (!documentSnapshots.isEmpty) {
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
                // Set the flag to false to indicate that the loadMoreData() method has completed
                isLoading = false
            }.addOnFailureListener { exception ->
                // Handle exceptions here
                isLoading = false
            }
        }
    }

    override fun onLocationFetched(address: String) {
        fetched_address=address
        Log.e("TAG", "Fetched address: $address")
    }
    private fun show_data_of_address(address: String){
        //val address="25X3+X58, तखतपुर, परसादा, छत्तीसगढ़ 495004, भारत,"
        mylist.clear()
        filter_select="address"
        var count = 0
        val c_list: MutableList<Int> = ArrayList()
        val str_Args: Array<String> = address.lowercase(Locale.getDefault()).split(", ").toTypedArray()
        for (objects in list_all) {
            convert_to_list_address(objects)
            for (s in list_s) {
                for (str_arg in str_Args) {
                    if (s.contains(str_arg)) {
                        println("{$s} = {$str_arg}")
                        count++
                    }
                }
                c_list.add(count)
                println(c_list.toString() + "")
                if (count > 0){
                    mylist.add(objects)
                }
                count = 0
            }
        }
        Log.e("size = ","{${mylist.size}}")
        val recordAdapter= recordAdapter(mylist,this)
        binding.recyclerView.setAdapter(recordAdapter)
        scroll=false
        recordAdapter.notifyItemRangeInserted(0, mylist.size)
        binding.textTotal.text="Total - "+mylist.size
    }
}