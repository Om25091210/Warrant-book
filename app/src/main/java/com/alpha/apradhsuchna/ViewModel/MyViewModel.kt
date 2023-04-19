package com.alpha.apradhsuchna.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpha.apradhsuchna.model.record_data
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MyViewModel : ViewModel() {
    var query_all: Query
    var rootRef: CollectionReference = FirebaseFirestore.getInstance().collection("records")
    private val list = mutableListOf<record_data>()
    private val _recordDataList = MutableLiveData<List<record_data>>()
    val recordDataList: LiveData<List<record_data>>
        get() = _recordDataList


    init {
        query_all = rootRef.orderBy("uploaded_date", Query.Direction.ASCENDING)
    }

    suspend fun getAllDataList(): List<record_data> = suspendCoroutine { continuation ->
        list.clear()
        query_all.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documentSnapshots = task.result
                if (documentSnapshots != null) {
                    for (document in documentSnapshots) {
                        val productModel: record_data = document.toObject(record_data::class.java)
                        list.add(productModel)
                    }
                }
                continuation.resume(list) // Resume the coroutine with the result
            } else {
                continuation.resumeWithException(task.exception ?: Exception("Unknown exception")) // Resume the coroutine with an exception
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val list = getAllDataList()
                _recordDataList.postValue(list)
            } catch (e: Exception) {
                // Handle the exception here
            }
        }
    }
}
