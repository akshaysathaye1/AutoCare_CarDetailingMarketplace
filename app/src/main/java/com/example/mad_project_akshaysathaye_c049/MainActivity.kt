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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Mad_project_akshaysathaye_c049Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AutoCareNavGraph(navController = navController)
                }
            }
        }
    }
}