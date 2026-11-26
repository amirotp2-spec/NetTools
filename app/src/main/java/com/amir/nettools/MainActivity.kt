package com.amir.nettools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.amir.nettools.ui.components.LocalIsFa
import com.amir.nettools.ui.navigation.AppRoot
import com.amir.nettools.ui.theme.NetToolsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as NetToolsApp).settings
        setContent {
            NetToolsTheme(themeMode = settings.themeMode) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (settings.isFa) LayoutDirection.Rtl else LayoutDirection.Ltr,
                    LocalIsFa provides settings.isFa
                ) {
                    AppRoot()
                }
            }
        }
    }
}
