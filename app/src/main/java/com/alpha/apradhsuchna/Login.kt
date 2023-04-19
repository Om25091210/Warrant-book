package com.alpha.apradhsuchna

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.Html
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alpha.apradhsuchna.databinding.ActivityLoginBinding
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import io.michaelrocks.paranoid.Obfuscate
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Obfuscate
class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    var DeviceToken: String? = null
    var downspeed = 0
    var role:String="user"
    var upspeed = 0
    var countDownTimer: CountDownTimer? = null
    private val resendOTPtoken: ForceResendingToken? = null
    private var verificationId: String? = null
    // variable for FirebaseAuth class
    private var mAuth: FirebaseAuth? = null
    var phone_ref: DatabaseReference? = null
    var admin_ref: DatabaseReference? = null
    var db = FirebaseFirestore.getInstance().collection("users")
    var emptylist = ArrayList<String>()
    var user: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        getSharedPreferences("authorized_entry", MODE_PRIVATE).edit()
            .putBoolean("entry_done", false).apply()

        getSharedPreferences("isAdmin_or_not", MODE_PRIVATE).edit()
            .putBoolean("authorizing_admin", false).apply()


        getSharedPreferences("Storing_name", MODE_PRIVATE).edit()
            .putString("user_name","").apply()

        mAuth = FirebaseAuth.getInstance()
        FirebaseMessaging.getInstance().subscribeToTopic("/topics/warrant")
            .addOnCompleteListener { task: Task<Void?> ->
                var msg = "Done"
                if (!task.isSuccessful) {
                    msg = "Failed"
                }
                Log.d("topic_log", msg)
            }
        phone_ref = FirebaseDatabase.getInstance().reference.child("Phone numbers")
        admin_ref = FirebaseDatabase.getInstance().reference.child("admin")
        getting_device_token()
        upAnimate(binding.logoLayout)

        binding.textView12.setText(Html.fromHtml(getString(R.string.sampleText)))
        binding.textView12.setMovementMethod(LinkMovementMethod.getInstance())

        binding.signIn.setOnClickListener(View.OnClickListener { v: View? ->
            if (binding.textView23.getText().toString().trim { it <= ' ' } != "Verify") {
                if (binding.edtEmail.getTextValue.trim { it <= ' ' }.length == 10) {
                    if(binding.edtName.getTextValue.trim()!="") {
                        check_user(binding.edtEmail.getTextValue.trim()+"")
                    }
                    else{
                        Toast.makeText(this@Login, "Enter your name please.", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(this@Login, "Enter 10 digit mobile number.", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                if (binding.pinView.getText().toString().trim { it <= ' ' }.length == 6) {
                    val otp_text =
                        Objects.requireNonNull<Editable>(binding.pinView.getText()).toString()
                            .trim { it <= ' ' }
                    Log.e("pinView", "==========")
                    verifyCode(otp_text)
                } else {
                    Toast.makeText(this, "Please enter a valid OTP.", Toast.LENGTH_SHORT).show()
                }
            }
        })

        binding.textView14.setOnClickListener(View.OnClickListener { v: View? ->
            if (binding.textView14.getText().toString() == "RESEND NEW CODE") {
                val phone = "+91" + binding.edtEmail.getTextValue
                if (resendOTPtoken != null) {
                    resendVerificationCode(phone, resendOTPtoken)
                }
                countTimer()
            }
        })

        binding.textView12.setText(Html.fromHtml(getString(R.string.sampleText)))
        binding.textView12.setMovementMethod(LinkMovementMethod.getInstance())

        binding.pBack.setOnClickListener(View.OnClickListener { v: View? ->
            binding.textView12.setVisibility(View.VISIBLE)
            binding.textView13.setVisibility(View.GONE)
            binding.textView14.setVisibility(View.GONE)
            onAnimate(binding.edtEmail)
            onAnimate(binding.edtName)
            binding.pinView.setText("")
            binding.pBack.setVisibility(View.GONE)
            binding.textView23.setText("Send OTP")
            offanimate(binding.pinView)
            countDownTimer!!.cancel()
        })

        binding.pinView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val ch = s.toString() + ""
                if (ch.length == 6) {
                    val otp_text = Objects.requireNonNull<Editable>(binding.pinView.getText()).toString()
                        .trim { it <= ' ' }
                    Log.e("pinView", "==========")
                    verifyCode(otp_text)
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    // callback method is called on Phone auth provider.
    // initializing our callbacks for on // verification callback method.
    private val mCallBack: OnVerificationStateChangedCallbacks =
        object : OnVerificationStateChangedCallbacks() {
            // below method is used when
            // OTP is sent from Firebase
            override fun onCodeSent(s: String, forceResendingToken: ForceResendingToken) {
                super.onCodeSent(s, forceResendingToken)
                // when we receive the OTP it
                // contains a unique id which
                // we are storing in our string
                // which we have already created.
                verificationId = s
                Log.e("The code","$s")
            }
            // this method is called when user
            // receive OTP from Firebase.
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                // below line is used for getting OTP code
                // which is sent in phone auth credentials.
                val code = p0.smsCode
                Log.e("The code","$code")
                // checking if the code
                // is null or not.
                if (code != null) {
                    // if the code is not null then
                    // we are setting that code to
                    // our OTP edittext field.
                    binding.pinView.setText(code)
                    Log.e("inside code block", "==========")
                    // after setting this code
                    // to OTP edittext field we
                    // are calling our verifycode method.
                    verifyCode(code)
                }
            }

            // this method is called when firebase doesn't
            // sends our OTP code due to any error or issue.
            override fun onVerificationFailed(e: FirebaseException) {
                // displaying error message with firebase exception.
                Log.e("errorMe", e.toString() + "")
            }
        }

    // below method is use to verify code from Firebase.
    private fun verifyCode(code: String) {
        // below line is used for getting getting
        // credentials from our verification id and code.
        Log.e("ok","ok")
        try {
            val credential = verificationId?.let { PhoneAuthProvider.getCredential(it, code) }

            // after getting credential we are
            // calling sign in method.
            credential?.let { signInWithCredential(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        // inside this method we are checking if
        // the code entered is correct or not.
        mAuth!!.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // if the code is correct and the task is successful
                    // we are sending our user to new activity.
                    Log.e("task successfull", "Success")
                    update_ui()
                } else {
                    // if the code is not correct then we are
                    // displaying an error message to the user.
                    Log.e("task result", task.exception!!.message!!)
                    binding.pinView.setError("Wrong Pin")
                }
            }
    }

    private fun update_ui() {
        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser

        val map= HashMap<String,String>()
        map["phone"]=user!!.phoneNumber+""
        map["name"]=binding.edtName.getTextValue+""
        map["role"]=role
        map["token"]=DeviceToken!!
        db.document(user!!.uid).set(map)

        getSharedPreferences("Storing_name", MODE_PRIVATE).edit()
            .putString("user_name",binding.edtName.getTextValue).apply()
        val i = Intent(this@Login, Dashboard::class.java)
        startActivity(i)
        finish()

        countDownTimer!!.cancel()
    }

    private fun check_user(phoneNumber: String) {
        phone_ref!!.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var check=0
                snapshot.children.forEach {
                    it.children.forEach{ it1->
                        Log.e("number","${snapshot.child(it.key+"").child(it1.key+"").value}")
                        Log.e("number","${phoneNumber.substring(3)}")

                        if(snapshot.child(it.key+"").child(it1.key+"").value == phoneNumber){
                            check=1
                            offanimate(binding.edtEmail)
                            offanimate(binding.edtName)
                            binding.textView12.setVisibility(View.GONE)
                            binding.textView13.setVisibility(View.VISIBLE)
                            binding.textView14.setVisibility(View.VISIBLE)
                            binding.pBack.setVisibility(View.VISIBLE)
                            binding.textView23.setText("Verify")
                            onAnimate(binding.pinView)
                            binding.pinView.setVisibility(View.VISIBLE)
                            val phone = "+91" + binding.edtEmail.getTextValue.trim()
                            sendVerificationCode(phone)
                            countTimer()
                            return@forEach
                        }
                    }
                }
                if(check!=1){
                    check_admin(phoneNumber)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Failed","Connection")
            }

        })
    }

    private fun check_admin(phoneNumber: String) {
        Log.e("Entered count", "Admin body")
        admin_ref!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var check=0
                snapshot.children.forEach {
                    if(phoneNumber.equals(it.key)){
                        Log.e("admin","${it.key}")
                        role="admin"
                        check=1
                        offanimate(binding.edtEmail)
                        offanimate(binding.edtName)
                        binding.textView12.setVisibility(View.GONE)
                        binding.textView13.setVisibility(View.VISIBLE)
                        binding.textView14.setVisibility(View.VISIBLE)
                        binding.pBack.setVisibility(View.VISIBLE)
                        binding.textView23.setText("Verify")
                        onAnimate(binding.pinView)
                        binding.pinView.setVisibility(View.VISIBLE)
                        val phone = "+91" + binding.edtEmail.getTextValue.trim()
                        sendVerificationCode(phone)
                        countTimer()
                        return@forEach
                    }
                }
                if(check!=1){
                    Snackbar.make(binding.signIn, "Phone no. is not in our database", Snackbar.LENGTH_LONG)
                        .setActionTextColor(Color.parseColor("#000000"))
                        .setTextColor(Color.parseColor("#D99D1c"))
                        .setBackgroundTint(Color.parseColor("#000000"))
                        .show()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun countTimer() {
        countDownTimer = object : CountDownTimer(25000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val f: NumberFormat = DecimalFormat("00")
                val min = millisUntilFinished / 60000 % 60
                val sec = millisUntilFinished / 1000 % 60
                binding.textView14.setText("Retry after - " + f.format(min) + ":" + f.format(sec))
            }

            // When the task is over it will print 00:00:00 there
            override fun onFinish() {
                binding.textView14.setText("RESEND NEW CODE")
                binding.textView14.setVisibility(View.VISIBLE)
                // btnVerify.setEnabled(true);
            }
        }
        (countDownTimer as CountDownTimer).start()
    }

    private fun resendVerificationCode(
        phoneNumber: String,
        token: ForceResendingToken
    ) {
        val options = PhoneAuthOptions.newBuilder(mAuth!!)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallBack)
            .setForceResendingToken(token)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun sendVerificationCode(number: String) {
        // this method is used for getting
        // OTP on user phone number.
        val options = PhoneAuthOptions.newBuilder(mAuth!!)
            .setPhoneNumber(number) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(mCallBack) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        Log.e("Logging","${options.zze()}")
    }

    fun offanimate(view: View?) {
        val move = ObjectAnimator.ofFloat(view, "translationX", -800f)
        move.duration = 1000
        val alpha2 = ObjectAnimator.ofFloat(view, "alpha", 0f)
        alpha2.duration = 500
        val animset = AnimatorSet()
        animset.play(alpha2).with(move)
        animset.start()
    }

    fun onAnimate(view: View?) {
        val move = ObjectAnimator.ofFloat(view, "translationX", 0f)
        move.duration = 1000
        val alpha2 = ObjectAnimator.ofFloat(view, "alpha", 100f)
        alpha2.duration = 500
        val animset = AnimatorSet()
        animset.play(alpha2).with(move)
        animset.start()
    }

    fun upAnimate(view: View?) {
        val move = ObjectAnimator.ofFloat(view, "translationY", -180f)
        move.duration = 500
        val alpha2 = ObjectAnimator.ofFloat(view, "alpha", 100f)
        alpha2.duration = 500
        val animset = AnimatorSet()
        animset.play(alpha2).with(move)
        animset.start()
    }

    private fun getting_device_token() {
        val connectivityManager = this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val nc = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (nc != null) {
            downspeed = nc.linkDownstreamBandwidthKbps / 1000
            upspeed = nc.linkUpstreamBandwidthKbps / 1000
        } else {
            downspeed = 0
            upspeed = 0
        }
        if (upspeed != 0 && downspeed != 0 || getWifiLevel() != 0) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token: String ->
                if (!TextUtils.isEmpty(token)) {
                    Log.d("token", "retrieve token successful : $token")
                } else {
                    Log.w("token121", "token should not be null...")
                }
            }.addOnFailureListener { e: java.lang.Exception? -> }.addOnCanceledListener {}
                .addOnCompleteListener { task: Task<String> ->
                    try {
                        DeviceToken = task.result
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
        }
    }

    fun getWifiLevel(): Int {
        return try {
            val wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val linkSpeed = wifiManager.connectionInfo.rssi
            WifiManager.calculateSignalLevel(linkSpeed, 5)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            0
        }
    }

    override fun onStart() {
        super.onStart()
        mAuth = FirebaseAuth.getInstance()
        user=mAuth!!.currentUser
        if(user!=null){
            val i = Intent(this@Login, Dashboard::class.java)
            startActivity(i)
            finish()
        }
    }
}