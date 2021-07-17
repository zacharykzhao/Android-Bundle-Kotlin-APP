package com.example.dynamicfeature

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class CameraViewModel : ViewModel() {
    val CAMERA_RQ = 9451
    fun requestCameraPermission(){
        viewModelScope.launch {
//            checkForPermissions(
//                android.Manifest.permission.CAMERA,
//                "camera",
//                CAMERA_RQ
//            )
        }
    }

//    private fun checkForPermissions(permission: String, name: String, requestCode: Int) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            when {
//                ContextCompat.checkSelfPermission(
//                    this.requireContext(),
//                    permission
//                ) == PackageManager.PERMISSION_GRANTED -> {
//                    Toast.makeText(
//                        this.requireContext(),
//                        "$name Permission granted",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//                shouldShowRequestPermissionRationale(permission) -> showDialog(
//                    permission,
//                    name,
//                    requestCode
//                )
//
//                else -> ActivityCompat.requestPermissions(
//                    requireActivity(),
//                    arrayOf(permission),
//                    requestCode
//                )
//            }
//        }
//    }
//
//    private fun showDialog(permission: String, name: String, requestCode: Int) {
//        AlertDialog.Builder(this.requireContext())
//            .setMessage("\"Permission to access your $name is required to use this app\"")
//            .setTitle("Permission required")
//            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
//                ActivityCompat.requestPermissions(
//                    this.requireActivity(),
//                    arrayOf(permission),
//                    requestCode
//                )
//            })
//            .create()
//            .show()
//
//        val builder = AlertDialog.Builder(this.requireContext())
//
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        fun innerCheck(name: String) {
//            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this.requireContext(), "$name permission refused", Toast.LENGTH_SHORT)
//                    .show()
//            } else {
//                Toast.makeText(this.context, "$name permission granted", Toast.LENGTH_SHORT)
//                    .show()
//            }
//        }
//
//        when (requestCode) {
//            CAMERA_RQ -> innerCheck("camera")
//        }
//    }


}