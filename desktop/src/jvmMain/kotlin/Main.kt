import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.github.tkuenneth.nativeparameterstoreaccess.MacOSDefaults
import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess.IS_MACOS
import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess.IS_WINDOWS
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import com.h0tk3y.flashcards.common.App
import com.h0tk3y.flashcards.common.KeyHandlers
import com.h0tk3y.flashcards.common.darkColors
import com.h0tk3y.flashcards.common.db.Database
import com.h0tk3y.flashcards.common.db.DatabaseDriverFactory
import com.h0tk3y.flashcards.common.lightColors
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import moe.tlaster.precompose.PreComposeWindow
import java.awt.Dimension
import javax.swing.JFrame
import kotlin.properties.Delegates

fun main() = application {
    val windowState = rememberWindowState(position = WindowPosition(Alignment.Center), size = DpSize(500.dp, 600.dp))

    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.launch {
        while (isActive) {
            val newMode = isSystemInDarkTheme()
            if (isInDarkMode != newMode) {
                isInDarkMode = newMode
            }
            delay(1000)
        }
    }

    PreComposeWindow(state = windowState, onCloseRequest = {}, onPreviewKeyEvent = {
        it.type == KeyEventType.KeyUp && KeyHandlers.handler().invoke(it)
    }) {
        window.title = "h0tk3y's Flashcards"
        window.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        window.minimumSize = Dimension(600, 700)

        var colors by remember { mutableStateOf(colors()) }
        onIsInDarkModeChanged = { _, _ ->
            colors = colors()
        }

        App(Database(DatabaseDriverFactory()), applyTheme = {
            MaterialTheme(colors) { it() }
        })
    }
}

private fun colors(): Colors = if (isInDarkMode) {
    darkColors()
} else {
    lightColors()
}

private var isInDarkMode by Delegates.observable(false) { _, oldValue, newValue ->
    onIsInDarkModeChanged?.let { it(oldValue, newValue) }
}
private var onIsInDarkModeChanged: ((Boolean, Boolean) -> Unit)? = null


fun isSystemInDarkTheme(): Boolean {
    return when {
        IS_WINDOWS -> {
            val result = WindowsRegistry.getWindowsRegistryEntry(
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "AppsUseLightTheme"
            )
            result == 0x0
        }

        IS_MACOS -> {
            val result = MacOSDefaults.getDefaultsEntry("AppleInterfaceStyle")
            result == "Dark"
        }

        else -> false
    }
}
