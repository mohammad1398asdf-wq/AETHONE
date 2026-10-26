package com.aethone.client

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aethone.client.ui.AppViewModel
import com.aethone.client.ui.navigation.AethoneNavHost
import com.aethone.client.ui.theme.AethoneTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : AppCompatActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            AethoneTheme(themeMode = settings.themeMode) {
                AethoneNavHost()
            }
        }
    }
}
