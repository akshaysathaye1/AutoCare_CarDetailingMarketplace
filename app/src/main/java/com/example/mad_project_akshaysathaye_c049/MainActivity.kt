package com.example.mad_project_akshaysathaye_c049

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.mad_project_akshaysathaye_c049.navigation.AutoCareNavGraph
import com.example.mad_project_akshaysathaye_c049.ui.theme.Mad_project_akshaysathaye_c049Theme

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.example.mad_project_akshaysathaye_c049.BuildConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        // Initialize AutoCare notification channel
        com.example.mad_project_akshaysathaye_c049.data.notifications.NotificationHelper.createNotificationChannel(this)

        // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Synchronize FCM token for the active session
        com.example.mad_project_akshaysathaye_c049.data.notifications.AutoCareMessagingService.syncDeviceToken(this)

        enableEdgeToEdge()
        setContent {
            Mad_project_akshaysathaye_c049Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        val injector = com.example.mad_project_akshaysathaye_c049.data.mock.MockDataInjector()
                        injector.injectGaragesWithPredefinedIds()
                        injector.injectMember3DataIfNeeded()
                    }
                    val navController = rememberNavController()
                    AutoCareNavGraph(navController = navController)
                }
            }
        }
    }
}