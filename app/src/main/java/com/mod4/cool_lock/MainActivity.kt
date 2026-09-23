package com.mod4.cool_lock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.mod4.cool_lock.data.CacheManager
import com.mod4.cool_lock.data.ModuleRepository
import com.mod4.cool_lock.ui.BadlockViewModel
import com.mod4.cool_lock.ui.screens.MainScreen
import com.mod4.cool_lock.ui.theme.CoolLockTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BadlockViewModel by viewModels {
        val cacheManager = CacheManager(applicationContext)
        val repository = ModuleRepository(applicationContext, cacheManager)
        BadlockViewModel.Factory(repository, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        installSplashScreen()
        setContent {
            CoolLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}
