package com.example.karttracker.components

import androidx.lifecycle.ViewModel
import com.example.karttracker.database.dao.RunSessionDao
import com.example.karttracker.database.entity.RunSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val runSessionDao: RunSessionDao
) : ViewModel() {

    val allRunSessions: Flow<List<RunSessionEntity>> =
        runSessionDao.getAllRunSessions()
            .flowOn(Dispatchers.IO)
}
