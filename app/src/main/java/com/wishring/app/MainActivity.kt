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
import com.wishring.app.core.util.BlePermissionManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the WISH RING app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private lateinit var blePermissionManager: BlePermissionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize BLE permission manager
        blePermissionManager = BlePermissionManager(this)
        blePermissionManager.initialize()
        
        setContent {
            WishRingTheme {
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
    
    /**
     * Request Bluetooth permissions
     * Called from HomeScreen when user taps "권한 허용"
     */
    fun requestBluetoothPermissions() {
        blePermissionManager.requestBluetoothSetup(
            onPermissionsGranted = {
                // Permissions granted - continue with bluetooth flow
            },
            onPermissionsDenied = { deniedPermissions ->
                // Handle denied permissions
            },
            onBluetoothEnabled = {
                // Bluetooth enabled - ready to use
            },
            onProgressUpdate = { message ->
                // Update progress message
            }
        )
    }
}