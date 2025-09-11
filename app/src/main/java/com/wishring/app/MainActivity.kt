package com.wishring.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.wishring.app.presentation.navigation.WishRingNavGraph
import com.wishring.app.ui.theme.WishRingTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the WISH RING app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            WishRingTheme {
                // Status bar will be handled by system default
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    WishRingNavGraph(
                        navController = navController
                    )
                }
            }
        }
    }
}