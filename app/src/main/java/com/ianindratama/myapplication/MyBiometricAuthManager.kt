package com.ianindratama.myapplication

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MyBiometricAuthManager(
    private val activity: FragmentActivity
) {

    private val resultChannel = Channel<BiometricResult>()
    val biometricResult = resultChannel.receiveAsFlow()

    private val authenticators = if (Build.VERSION.SDK_INT >= 30) {
        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    } else BIOMETRIC_STRONG

    private val biometricPrompt by lazy {
        BiometricPrompt(
            activity,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    resultChannel.trySend(BiometricResult.AuthenticationSucceeded)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    resultChannel.trySend(BiometricResult.AuthenticationFailed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    resultChannel.trySend(BiometricResult.AuthenticationError(errString.toString()))
                }

            }
        )
    }

    private fun checkIfBiometricAuthAvailable(): Boolean {
        val biometricManager = BiometricManager.from(activity)

        val authenticators = authenticators

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                resultChannel.trySend(BiometricResult.HardwareUnavailable)
                false
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                resultChannel.trySend(BiometricResult.HardwareCurrentlyUnavailable)
                false
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                resultChannel.trySend(BiometricResult.BiometricNotSet)
                false
            }

            BiometricManager.BIOMETRIC_SUCCESS -> {
                true
            }

            else -> false
        }
    }

    fun showBiometricAuth() {
        if (!checkIfBiometricAuthAvailable()){
            return
        }

        val promptInfo = if (Build.VERSION.SDK_INT >= 30) {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Sample prompt title")
                .setDescription("Sample prompt description")
                .setAllowedAuthenticators(authenticators)
        } else {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Sample prompt title")
                .setDescription("Sample prompt description")
                .setNegativeButtonText("Cancel")
        }

        biometricPrompt.authenticate(promptInfo.build())
    }

    sealed interface BiometricResult {
        data object HardwareUnavailable : BiometricResult
        data object HardwareCurrentlyUnavailable : BiometricResult
        data object BiometricNotSet : BiometricResult
        data object AuthenticationSucceeded: BiometricResult
        data object AuthenticationFailed: BiometricResult
        data class AuthenticationError(val error: String): BiometricResult
    }
}