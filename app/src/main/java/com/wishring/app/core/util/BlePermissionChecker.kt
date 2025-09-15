package com.wishring.app.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for checking BLE permissions
 * Handles both legacy and modern Android permission models
 */
@Singleton
class BlePermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Check if all required BLE permissions are granted
     */
    fun hasAllBlePermissions(): Boolean {
        return hasBluetoothPermissions()
    }
    
    /**
     * Check if Bluetooth permissions are granted
     */
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires BLUETOOTH_SCAN and BLUETOOTH_CONNECT
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Legacy Android requires BLUETOOTH and BLUETOOTH_ADMIN
            hasPermission(Manifest.permission.BLUETOOTH) &&
            hasPermission(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }
    

    
    /**
     * Check if scan permission is granted
     */
    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            hasPermission(Manifest.permission.BLUETOOTH)
        }
    }
    
    /**
     * Check if connect permission is granted
     */
    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.BLUETOOTH)
        }
    }
    
    /**
     * Get list of missing BLE permissions
     */
    fun getMissingBlePermissions(): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (!hasPermission(Manifest.permission.BLUETOOTH)) {
                missingPermissions.add(Manifest.permission.BLUETOOTH)
            }
            if (!hasPermission(Manifest.permission.BLUETOOTH_ADMIN)) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
        
        return missingPermissions
    }
    
    /**
     * Get list of all required BLE permissions for current Android version
     */
    fun getRequiredBlePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }
    
    /**
     * Execute action only if permissions are granted
     * @param onPermissionGranted Action to execute if permissions are available
     * @param onPermissionDenied Action to execute if permissions are missing (optional)
     */
    inline fun executeWithPermission(
        onPermissionGranted: () -> Unit,
        noinline onPermissionDenied: ((List<String>) -> Unit)? = null
    ) {
        if (hasAllBlePermissions()) {
            onPermissionGranted()
        } else {
            onPermissionDenied?.invoke(getMissingBlePermissions())
        }
    }
    
    /**
     * Execute suspend action only if permissions are granted, returning a result
     */
    inline fun <T> executeWithPermissionResult(
        onPermissionGranted: () -> T,
        noinline onPermissionDenied: ((List<String>) -> T)? = null
    ): T? {
        return if (hasAllBlePermissions()) {
            onPermissionGranted()
        } else {
            onPermissionDenied?.invoke(getMissingBlePermissions())
        }
    }
    
    /**
     * Check if specific permission is granted
     */
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}