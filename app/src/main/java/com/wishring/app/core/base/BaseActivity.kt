package com.wishring.app.core.base

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import android.util.Log

/**
 * Base Activity class providing common functionality
 * All activities should extend this class
 * 
 * @param VM ViewModel type
 */
abstract class BaseActivity<VM : ViewModel> : ComponentActivity() {
    
    /**
     * ViewModel instance
     * Should be initialized using Hilt's viewModels() delegate
     */
    protected abstract val viewModel: VM
    
    /**
     * List of required permissions for this activity
     * Override to specify permissions
     */
    protected open val requiredPermissions: Array<String> = emptyArray()
    
    /**
     * Permission request code
     */
    private val permissionRequestCode = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup system UI
        setupSystemUI()
        
        // Check and request permissions
        checkAndRequestPermissions()
        
        // Initialize common components
        initializeCommon()
        
        // Set Compose content
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Content()
                }
            }
        }
        
        // Log activity creation
        Log.d("BaseActivity", "${this::class.simpleName} created")
    }
    
    /**
     * Compose content to be displayed
     * Must be implemented by child classes
     */
    @Composable
    protected abstract fun Content()
    
    /**
     * Setup system UI (status bar, navigation bar, etc.)
     */
    private fun setupSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Make status bar transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        // Make navigation bar transparent
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }
    
    /**
     * Check and request required permissions
     */
    private fun checkAndRequestPermissions() {
        if (requiredPermissions.isEmpty()) return
        
        val permissionsToRequest = mutableListOf<String>()
        
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                permissionRequestCode
            )
        } else {
            onAllPermissionsGranted()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == permissionRequestCode) {
            val deniedPermissions = mutableListOf<String>()
            
            permissions.forEachIndexed { index, permission ->
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permission)
                }
            }
            
            if (deniedPermissions.isEmpty()) {
                onAllPermissionsGranted()
            } else {
                onPermissionsDenied(deniedPermissions)
            }
        }
    }
    
    /**
     * Called when all permissions are granted
     * Override to handle permission grant
     */
    protected open fun onAllPermissionsGranted() {
        Log.d("BaseActivity", "All permissions granted")
    }
    
    /**
     * Called when some permissions are denied
     * Override to handle permission denial
     * 
     * @param deniedPermissions List of denied permissions
     */
    protected open fun onPermissionsDenied(deniedPermissions: List<String>) {
        Log.w("BaseActivity", "Permissions denied: $deniedPermissions")
    }
    
    /**
     * Initialize common components
     * Override to add custom initialization
     */
    protected open fun initializeCommon() {
        // Can be overridden by child classes
    }
    
    /**
     * App theme wrapper
     * Can be customized per activity
     */
    @Composable
    protected open fun AppTheme(content: @Composable () -> Unit) {
        // This will be replaced with actual app theme
        MaterialTheme {
            content()
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("BaseActivity", "${this::class.simpleName} resumed")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("BaseActivity", "${this::class.simpleName} paused")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("BaseActivity", "${this::class.simpleName} destroyed")
    }
}