package com.example.abb_demo2

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.*
import com.google.android.play.core.ktx.*
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException


class InstallCameraModule @Keep constructor(private val manager: SplitInstallManager) : ViewModel() {

    val cameraModuleState = getStatusLiveDataForModule(CAMERA_MODULE)

    private val _events: BroadcastChannel<Event> = BroadcastChannel(Channel.BUFFERED)
    val events: Flow<Event> = _events.asFlow()

    private fun getStatusLiveDataForModule(moduleName: String): LiveData<ModuleStatus> {
        return manager.requestProgressFlow()
            .filter { state ->
                state.moduleNames.contains(moduleName)
            }
            .map { state ->
                Log.d("STATE", state.toString())
                when (state.status) {
                    SplitInstallSessionStatus.CANCELED -> ModuleStatus.Available
                    SplitInstallSessionStatus.CANCELING -> ModuleStatus.Installing(0.0)
                    SplitInstallSessionStatus.DOWNLOADING -> ModuleStatus.Installing(
                        state.bytesDownloaded.toDouble() / state.totalBytesToDownload
                    )
                    SplitInstallSessionStatus.DOWNLOADED -> ModuleStatus.Installed
                    SplitInstallSessionStatus.FAILED -> {
                        _events.send(Event.InstallErrorEvent(state))
                        ModuleStatus.Available
                    }
                    SplitInstallSessionStatus.INSTALLED -> ModuleStatus.Installed
                    SplitInstallSessionStatus.INSTALLING -> ModuleStatus.Installing(1.0)
                    SplitInstallSessionStatus.PENDING -> ModuleStatus.Installing(0.0)
                    SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> ModuleStatus.NeedsConfirmation(
                        state
                    )
                    SplitInstallSessionStatus.UNKNOWN -> ModuleStatus.Unavailable
                    else -> ModuleStatus.Unavailable
                }
            }.catch {
                _events.send(
                    Event.ToastEvent(
                        "Something went wrong. No install progress will be reported."
                    )
                )
                emit(ModuleStatus.Unavailable)
            }.asLiveData()
    }

    fun invokeCameraPermission() {
        openActivityInOnDemandModule(
            CAMERA_MODULE,
            "com.example.dynamicfeature.CameraFragment"
        )
    }

    private fun openActivityInOnDemandModule(moduleName: String, fragmentName: String) {
        if (manager.installedModules.contains(moduleName)) {
            viewModelScope.launch {
                _events.send(Event.NavigationEvent(fragmentName))
            }
        } else {
            val status = when (moduleName) {
                CAMERA_MODULE -> cameraModuleState.value
                else -> throw IllegalArgumentException("State not implemented")
            }
            if (status is ModuleStatus.NeedsConfirmation) {
                viewModelScope.launch { _events.send(Event.InstallConfirmationEvent(status.state)) }
            } else {
                requestModuleInstallation(moduleName)
            }
        }
    }

    private fun requestModuleInstallation(moduleName: String) {
        viewModelScope.launch {
            try {
                manager.requestInstall(listOf(moduleName))
            } catch (e: SplitInstallException) {
                _events.send(Event.ToastEvent("Failed starting installation of $moduleName"))
            }
        }
    }
}

sealed class ModuleStatus {
    object Available : ModuleStatus()
    data class Installing(val progress: Double) : ModuleStatus()
    object Unavailable : ModuleStatus()
    object Installed : ModuleStatus()
    class NeedsConfirmation(val state: SplitInstallSessionState) : ModuleStatus()
}

class InstallViewModelProviderFactory(
    private val manager: SplitInstallManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(SplitInstallManager::class.java).newInstance(manager)
    }
}

const val CAMERA_MODULE = "dynamicfeature"