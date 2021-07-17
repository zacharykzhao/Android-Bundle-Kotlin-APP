package com.example.abb_demo2

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Camera
import android.graphics.drawable.AnimatedVectorDrawable
import android.hardware.Camera.open
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.Keep
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.example.abb_demo2.databinding.FragmentMainBinding
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import androidx.lifecycle.Observer
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.ktx.*
import com.google.android.play.core.review.ReviewManager
import com.google.android.samples.dynamicfeatures.state.ReviewViewModel
import com.google.android.samples.dynamicfeatures.state.ReviewViewModelProviderFactory
import com.google.android.samples.dynamicfeatures.state.UpdateViewModel
import com.google.android.samples.dynamicfeatures.state.UpdateViewModelProviderFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@Keep
class MainFragment : Fragment(R.layout.fragment_main) {
    val FINE_LOCATION_RQ = 5184

    private var bindings: FragmentMainBinding? = null

    private lateinit var splitInstallManager: SplitInstallManager
    private val installViewModel by viewModels<InstallCameraModule> {
        InstallViewModelProviderFactory(splitInstallManager)
    }

    private var startModuleWhenReady: Boolean = false

    private lateinit var appUpdateManager: AppUpdateManager
    private val updateViewModel by viewModels<UpdateViewModel> {
        UpdateViewModelProviderFactory(appUpdateManager)
    }

    private lateinit var reviewManager: ReviewManager
    private val reviewViewModel by activityViewModels<ReviewViewModel> {
        ReviewViewModelProviderFactory(reviewManager)
    }

    private lateinit var snackbar: Snackbar


    override fun onAttach(context: Context) {
        super.onAttach(context)
        splitInstallManager = SplitInstallManagerFactory.create(context)
        appUpdateManager = AppUpdateManagerFactory.create(context)
        reviewManager = ReviewManagerFactory.create(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindings = FragmentMainBinding.bind(view).apply {
            // base app click listener
            btnBasePermission.setOnClickListener {
                startModuleWhenReady = false
                checkForPermissions(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    "location",
                    FINE_LOCATION_RQ
                )
            }

            btnOpenCamera.setOnClickListener{
                // open camera in base
                //
                try {
                    android.hardware.Camera.open()
                } catch (e: Exception){
                    Toast.makeText(
                        requireContext(),
                        "In module open Camera"+e,
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("zkf base", "failed to open Camera\n" +e.stackTrace)

                }
            }

            // dynamic load
            btnDynamicPermission.setOnClickListener {
                startModuleWhenReady = true
                installViewModel.invokeCameraPermission()
            }
        }
        snackbar = Snackbar.make(view, R.string.update_available, Snackbar.LENGTH_INDEFINITE)

        addInstallViewModelObservers()
        addUpdateViewModelObservers()
    }


    private fun addInstallViewModelObservers() {
        with(installViewModel) {
            events.onEach { event ->
                when (event) {
                    is Event.ToastEvent -> toastAndLog(event.message)
                    is Event.NavigationEvent -> {
                        navigateToFragment(event.fragmentClass)
                    }
                    is Event.InstallConfirmationEvent -> splitInstallManager.startConfirmationDialogForResult(
                        event.status,
                        this@MainFragment,
                        INSTALL_CONFIRMATION_REQ_CODE
                    )
                    else -> throw IllegalStateException("Event type not handled: $event")
                }
            }.launchIn(viewLifecycleOwner.lifecycleScope)
            cameraModuleState.observe(viewLifecycleOwner, Observer { status ->
                bindings?.let {
                    updateModuleButton(status)
                }
            })
        }
    }

    private fun updateUpdateButton(updateResult: AppUpdateResult) {
        when (updateResult) {
            AppUpdateResult.NotAvailable -> {
                Log.d(TAG, "No update available")
                snackbar.dismiss()
            }
            is AppUpdateResult.Available -> with(snackbar) {
                setText(R.string.update_available_snackbar)
                setAction(R.string.update_now) {
                    updateViewModel.invokeUpdate()
                }
                show()
            }
            is AppUpdateResult.InProgress -> {
                with(snackbar) {
                    val updateProgress: Int = if (updateResult.installState.totalBytesToDownload == 0L) {
                        0
                    } else {
                        (updateResult.installState.bytesDownloaded * 100 /
                                updateResult.installState.totalBytesToDownload).toInt()
                    }
                    setText(context.getString(R.string.downloading_update, updateProgress))
                    setAction(null) {}
                    show()
                }
            }
            is AppUpdateResult.Downloaded -> {
                with(snackbar) {
                    setText(R.string.update_downloaded)
                    setAction(R.string.complete_update) {
                        updateViewModel.invokeUpdate()
                    }
                    show()
                }
            }
        }
    }


    private fun addUpdateViewModelObservers() {
        with(updateViewModel) {
            updateStatus.observe(
                viewLifecycleOwner,
                { updateResult: AppUpdateResult ->
                    updateUpdateButton(updateResult)

                    // If it's an immediate update, launch it immediately and finish Activity
                    // to prevent the user from using the app until they update.
                    if (updateResult is AppUpdateResult.Available) {
                        if (shouldLaunchImmediateUpdate(updateResult.updateInfo)) {
                            if (appUpdateManager.startUpdateFlowForResult(
                                    updateResult.updateInfo,
                                    AppUpdateType.IMMEDIATE,
                                    this@MainFragment,
                                    UPDATE_CONFIRMATION_REQ_CODE
                                )) {
                                // only exit if update flow really started
                                requireActivity().finish()
                            }
                        }
                    }
                }
            )
            events.onEach { event ->
                when (event) {
                    is Event.ToastEvent -> toastAndLog(event.message)
                    is Event.StartUpdateEvent -> {
                        val updateType = if (event.immediate) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
                        appUpdateManager.startUpdateFlowForResult(
                            event.updateInfo,
                            updateType,
                            this@MainFragment,
                            UPDATE_CONFIRMATION_REQ_CODE
                        )
                    }
                    else -> throw IllegalStateException("Event type not handled: $event")
                }
            }.launchIn(viewLifecycleOwner.lifecycleScope)
        }
    }

    private fun updateModuleButton(status: ModuleStatus) {
        bindings?.btnDynamicPermission?.apply {
            isEnabled = status !is ModuleStatus.Unavailable
            when (status) {
                ModuleStatus.Available -> {
                    text = getString(R.string.install)
//                    shrink()
                }
                is ModuleStatus.Installing -> {
                    text = getString(
                        R.string.installing,
                        (status.progress * 100).toInt()
                    )
//                    extend()
                }
                ModuleStatus.Unavailable -> {
                    text = getString(R.string.feature_not_available)
//                    shrink()
                }
                ModuleStatus.Installed -> {
                    SplitCompat.installActivity(requireActivity())
//                    shrink()
                    if (startModuleWhenReady) {
                        startModuleWhenReady = false
                        installViewModel.invokeCameraPermission()
                    }
                }
                is ModuleStatus.NeedsConfirmation -> {
                    splitInstallManager.startConfirmationDialogForResult(
                        status.state,
                        this@MainFragment,
                        UPDATE_CONFIRMATION_REQ_CODE
                    )
                }
            }
        }
    }

    private fun navigateToFragment(fragmentClass: String) {
        val fragment = parentFragmentManager.fragmentFactory.instantiate(
            ClassLoader.getSystemClassLoader(),
            fragmentClass
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.mycontainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun checkForPermissions(permission: String, name: String, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Toast.makeText(
                        requireContext(),
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        fun innerCheck(name: String) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "$name permission refused", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(requireContext(), "$name permission granted", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        when (requestCode) {
            FINE_LOCATION_RQ -> innerCheck("location")
        }
    }

    private fun showDialog(permission: String, name: String, requestCode: Int) {
        AlertDialog.Builder(requireContext())
            .setMessage("\"Permission to access your $name is required to use this app\"")
            .setTitle("Permission required")
            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(permission),
                    requestCode
                )
            })
            .create()
            .show()

        val builder = AlertDialog.Builder(requireContext())
    }


}

fun MainFragment.toastAndLog(text: String) {
    Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
    Log.d(TAG, text)
}

private const val TAG = "DynamicFeatures"
const val INSTALL_CONFIRMATION_REQ_CODE = 1
const val UPDATE_CONFIRMATION_REQ_CODE = 2