package dev.belyaevad.texttools.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.belyaevad.texttools.domain.Factory

/** TextTools desktop entry for 1.0.0-alpha. */
fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "TextTools") {
            MaterialTheme {
                menuScreen()
            }
        }
    }

/** The action grid. Each button names a factory action. */
@Composable
fun menuScreen() {
    var chosen by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        Text("TextTools 1.0.0-alpha")
        Factory.actions.forEach { action ->
            Button(onClick = { chosen = action.name }) {
                Text(action.name)
            }
        }
        if (chosen.isNotEmpty()) {
            Text("Выбрано: $chosen")
        }
    }
}
