package com.ianindratama.myapplication

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ianindratama.myapplication.ui.theme.MyApplicationTheme

class MainActivity : AppCompatActivity() {

    private val biometricAuthenticator by lazy {
        MyBiometricAuthManager(this@MainActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {

                val biometricResult by biometricAuthenticator.biometricResult.collectAsState(
                    initial = null
                )

                val enrollLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = {
                        biometricAuthenticator.showBiometricAuth()
                    }
                )

                LaunchedEffect(biometricResult) {
                    if (biometricResult is MyBiometricAuthManager.BiometricResult.BiometricNotSet) {
                        if (Build.VERSION.SDK_INT >= 30) {
                            val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                putExtra(
                                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                    BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                                )
                            }
                            enrollLauncher.launch(enrollIntent)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = {biometricAuthenticator.showBiometricAuth()}) {
                        Text(text = "Authenticate")
                    }
                    biometricResult?.let { result ->
                        Text(
                            text = when (result) {
                                MyBiometricAuthManager.BiometricResult.HardwareUnavailable -> {
                                    "Hardware unavailable"
                                }
                                MyBiometricAuthManager.BiometricResult.HardwareCurrentlyUnavailable -> {
                                    "Hardware temporarily unavailable"
                                }
                                MyBiometricAuthManager.BiometricResult.BiometricNotSet -> {
                                    "No biometric is set"
                                }
                                MyBiometricAuthManager.BiometricResult.AuthenticationSucceeded -> {
                                    "Authentication succeeded"
                                }
                                MyBiometricAuthManager.BiometricResult.AuthenticationFailed -> {
                                    "Authentication failed"
                                }
                                is MyBiometricAuthManager.BiometricResult.AuthenticationError -> {
                                    result.error
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}