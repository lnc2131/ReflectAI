package utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility class for handling runtime permissions
 */
object PermissionUtils {

    /**
     * Check if a permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request a permission directly (useful for ViewModel)
     */
    fun requestPermission(activity: Activity, permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    /**
     * Check and request microphone permission specifically
     */
    fun checkMicrophonePermission(activity: Activity): Boolean {
        val permission = Manifest.permission.RECORD_AUDIO
        
        if (isPermissionGranted(activity, permission)) {
            return true
        }
        
        // Request permission
        requestPermission(activity, permission, RECORD_AUDIO_PERMISSION_CODE)
        return false
    }
    
    /**
     * A Composable function that requests a permission and returns whether it's granted
     */
    @Composable
    fun RequestPermission(
        permission: String = Manifest.permission.RECORD_AUDIO,
        onPermissionResult: (Boolean) -> Unit
    ) {
        var permissionGranted by remember { mutableStateOf(false) }
        
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                permissionGranted = isGranted
                onPermissionResult(isGranted)
            }
        )
        
        LaunchedEffect(Unit) {
            launcher.launch(permission)
        }
        
        return
    }
    
    // Permission request codes
    const val RECORD_AUDIO_PERMISSION_CODE = 101
}