package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebase.configureAppCheck
import com.example.ui.screens.VaaniCompanionApp
import com.example.ui.theme.VaaniTheme
import com.example.viewmodel.VaaniViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure Firebase App Check with launch intent before any UI, ViewModel, or AI calls
    configureAppCheck(this, intent)

    enableEdgeToEdge()
    setContent {
      VaaniTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val viewModel: VaaniViewModel = viewModel()
          VaaniCompanionApp(viewModel = viewModel)
        }
      }
    }
  }
}
