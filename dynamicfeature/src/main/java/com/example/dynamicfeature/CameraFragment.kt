package com.example.dynamicfeature

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.dynamicfeature.databinding.FragmentCameraBinding
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.samples.dynamicfeatures.state.ReviewViewModel
import com.google.android.samples.dynamicfeatures.state.ReviewViewModelProviderFactory
import android.net.Uri
import android.os.Environment
import java.io.File


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class CameraFragment : Fragment() {

    private var cameraBinding: FragmentCameraBinding? = null

    val CAMERA_RQ = 9451

    private val reviewViewModel by activityViewModels<ReviewViewModel> {
        ReviewViewModelProviderFactory(ReviewManagerFactory.create(requireContext()))
    }

    private var resultHandled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return FragmentCameraBinding.inflate(inflater, container, false).also {
            cameraBinding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ActivityCompat.requestPermissions(
            requireActivity(), arrayOf(Manifest.permission.CAMERA),
            CAMERA_RQ
        )

//        checkForPermissions(
//            android.Manifest.permission.CAMERA,
//            "READ_CONTACTS",
//            CAMERA_RQ
//        )
        cameraBinding = FragmentCameraBinding.bind(view).apply {
            btnModuleOpenCamera.setOnClickListener {
                try {
                    Camera.open()
                } catch (e: Exception){
                    Toast.makeText(
                        requireContext(),
                        "In module open Camera"+e,
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("zkf module", "failed to open Camera\n" +e.stackTrace)

                }

            }
        }

        super.onViewCreated(view, savedInstanceState)

    }


    override fun onResume() {
        super.onResume()
        reviewViewModel.preWarmReview()
    }

    override fun onDestroyView() {
        cameraBinding = null
        super.onDestroyView()
    }


    private fun checkForPermissions(permission: String, name: String, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(
                    this.requireContext(),
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Toast.makeText(
                        this.requireContext(),
                        "$name Permission granted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                shouldShowRequestPermissionRationale(permission) -> showDialog(
                    permission,
                    name,
                    requestCode
                )

                else -> ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(permission),
                    requestCode
                )
            }
        }
    }

    private fun showDialog(permission: String, name: String, requestCode: Int) {
        AlertDialog.Builder(this.requireContext())
            .setMessage("\"Permission to access your $name is required to use this app\"")
            .setTitle("Permission required")
            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                ActivityCompat.requestPermissions(
                    this.requireActivity(),
                    arrayOf(permission),
                    requestCode
                )
            })
            .create()
            .show()

        val builder = AlertDialog.Builder(this.requireContext())

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        fun innerCheck(name: String) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this.requireContext(),
                    "$name permission refused",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                Toast.makeText(this.context, "$name permission granted", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        when (requestCode) {
            CAMERA_RQ -> innerCheck("camera")
        }
    }

}