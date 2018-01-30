package com.androidkotlincore.mvp.impl.permissions

import android.app.Activity
import android.app.Fragment
import android.content.pm.PackageManager
import android.os.Build
import android.support.annotation.UiThread
import io.reactivex.Observable
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.awaitFirst
import kotlinx.coroutines.experimental.sync.Mutex
import java.util.concurrent.atomic.AtomicInteger

/**
* Created by Peter on 19.01.18.
*/
typealias SupportFragment = android.support.v4.app.Fragment

class PermissionsManagerDelegate(
        private val permissionsObservable: Observable<OnRequestPermissionsResultEvent>,
        private val view: suspend () -> Any) : PermissionsManager {

    override suspend fun requestPermissions(permissions: List<String>): RequestPermissionsResult {
        //we always use UI thread to work with permissions
        val result = async(UI) { requestPermissionsImpl(permissions) }.await()
        return result
    }

    override suspend fun requestPermissionsOrThrow(permissions: List<String>): RequestPermissionsResult {
        val result = requestPermissions(permissions)
        if (!result.isAllGranted) {
            throw PermissionException("Some permissions were denied: ${result.deniedPermissions.joinToString()}", result)
        }
        return result
    }

    override suspend fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        //we always use UI thread to work with permissions
        val result = async(UI) { shouldShowRequestPermissionRationaleImpl(permission) }.await()
        return result
    }

    @UiThread
    private suspend fun requestPermissionsImpl(permissions: List<String>): RequestPermissionsResult {
        if (!isMarshmallow() || permissions.all { isPermissionGranted(it) }) {
            return RequestPermissionsResult(grantedPermissions = permissions, deniedPermissions = emptyList())
        }

        // You can request only one permissions set at once;
        // otherwise second permissions set will be denied.
        // So, to prevent it, we use static mutex here
        REQUEST_PERMISSION_MUTEX.lock()
        try {
            val requestCode = generateRequestCode()

            val v = view()
            when (v) {
                is Activity -> v.requestPermissions(permissions.toTypedArray(), requestCode)
                is Fragment -> v.requestPermissions(permissions.toTypedArray(), requestCode)
                is SupportFragment -> v.requestPermissions(permissions.toTypedArray(), requestCode)
                else -> throw IllegalStateException("View $v must be ${Activity::class.java.name} " +
                        "or ${Fragment::class.java.name} " +
                        "or ${SupportFragment::class.java.name}!")
            }

            //wait for permissions request result
            val event = permissionsObservable.filter { it.requestCode == requestCode }.awaitFirst()
            return RequestPermissionsResult(event)
        } finally {
            REQUEST_PERMISSION_MUTEX.unlock()
        }
    }

    @UiThread
    private suspend fun shouldShowRequestPermissionRationaleImpl(permission: String): Boolean {
        if (!isMarshmallow()) return false

        val v = view()
        return when (v) {
            is Activity -> v.shouldShowRequestPermissionRationale(permission)
            is Fragment -> v.shouldShowRequestPermissionRationale(permission)
            is SupportFragment -> v.shouldShowRequestPermissionRationale(permission)
            else -> throw IllegalStateException("View $v must be ${Activity::class.java.name} " +
                    "or ${Fragment::class.java.name} " +
                    "or ${SupportFragment::class.java.name}!")
        }
    }

    @UiThread
    private suspend fun isPermissionGranted(permission: String): Boolean {
        if (!isMarshmallow()) return true

        val v = view()
        return when (v) {
            is Activity -> v.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            is Fragment -> v.activity!!.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            is SupportFragment -> v.activity!!.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            else -> throw IllegalStateException("View $v must be ${Activity::class.java.name} " +
                    "or ${Fragment::class.java.name} " +
                    "or ${SupportFragment::class.java.name}!")
        }
    }

    private suspend fun isPermissionRevoked(permission: String): Boolean {
        if (!isMarshmallow()) return false

        val v = view()
        val activity: Activity = when (v) {
            is Activity -> v
            is Fragment -> v.activity!!
            is SupportFragment -> v.activity!!
            else -> throw IllegalStateException("View $v must be ${Activity::class.java.name} or " +
                    "${Fragment::class.java.name} or " +
                    "${SupportFragment::class.java.name}!")
        }

        return activity.packageManager.isPermissionRevokedByPolicy(permission, activity.packageName)
    }

    private fun isMarshmallow(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    /**
     * Generates request code between 1 and 65530
     */
    private fun generateRequestCode(): Int {
        val result = REQUEST_CODE.getAndIncrement()
        if (result >= 65530) {
            REQUEST_CODE.set(1)
        }

        return result
    }

    private companion object {
        private val REQUEST_CODE = AtomicInteger(1)
        private val REQUEST_PERMISSION_MUTEX = Mutex()
    }

}