package com.seunome.finlo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.seunome.finlo.core.designsystem.FinloTheme
import com.seunome.finlo.core.security.SecureStorage
import com.seunome.finlo.presentation.navigation.FinloNavGraph
import com.seunome.finlo.presentation.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var secureStorage: SecureStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themePreference by secureStorage.themeFlow.collectAsState(initial = "SYSTEM")
            val isPinEnabled by secureStorage.isPinEnabledFlow.collectAsState(initial = false)

            val darkTheme = when (themePreference) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            FinloTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val startDestination = if (isPinEnabled) Screen.PinLock.route else Screen.Dashboard.route

                FinloNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
