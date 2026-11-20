package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app.EueeMasteryApp
import com.example.core.model.UserPreferences
import com.example.navigation.EueeAppShell
import com.example.ui.theme.EueeMasteryTheme

class MainActivity : ComponentActivity() {

    private var shortcutRouteState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        shortcutRouteState = intent?.getStringExtra("route")

        val app = application as EueeMasteryApp
        val container = app.container

        setContent {
            val prefs by container.preferencesRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = UserPreferences())

            EueeMasteryTheme(
                themeMode = prefs.themeMode,
                accentPalette = prefs.accentPalette,
                fontScale = prefs.fontScale
            ) {
                EueeAppShell(
                    container = container,
                    initialShortcutRoute = shortcutRouteState
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        shortcutRouteState = intent.getStringExtra("route")
    }

    override fun onDestroy() {
        (application as? EueeMasteryApp)?.container?.speechHelper?.shutdown()
        super.onDestroy()
    }
}
