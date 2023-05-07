package com.alpha.apradhsuchna.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpha.apradhsuchna.model.record_data
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MyViewModel : ViewModel() {
    private val rootRef: CollectionReference = FirebaseFirestore.getInstance().collection("records")
    private val _recordDataState = MutableStateFlow<DataState<List<record_data>>>(DataState.Loading)
    val recordDataState: StateFlow<DataState<List<record_data>>> = _recordDataState

    init {
        refreshData()
    }

    private suspend fun getAllDataList(): List<record_data> {
        return withContext(Dispatchers.IO) {
            try {
                val query = rootRef.orderBy("uploaded_date", Query.Direction.ASCENDING)
                val snapshot = query.get().await()
                snapshot.documents.mapNotNull { it.toObject(record_data::class.java) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun loadRecords() {
        _recordDataState.value = DataState.Loading
        try {
            val list = getAllDataList()
            _recordDataState.value = DataState.Success(list)
        } catch (e: Exception) {
            _recordDataState.value = DataState.Error(e.message ?: "Unknown error")
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            loadRecords()
        }
    }
}