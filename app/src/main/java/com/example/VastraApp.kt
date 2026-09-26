package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.VastraRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VastraApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { VastraRepository(database) }

    override fun onCreate() {
        super.onCreate()
        // Seed default clothing shop products if the database is newly initialized
        applicationScope.launch(Dispatchers.IO) {
            repository.seedSampleProductsIfEmpty(this@VastraApp)
        }
    }
}
