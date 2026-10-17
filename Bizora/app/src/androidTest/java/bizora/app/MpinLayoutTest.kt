package bizora.app

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import bizora.app.ui.components.MpinStyle
import bizora.app.ui.screens.*
import bizora.app.ui.theme.BizoraTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before

class MpinLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun edgeToEdge() {
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
    }

    @Composable
    private fun PreviewAd() {
        Box(Modifier.fillMaxWidth().height(56.dp).background(Color(0xFFF0F1F1)), contentAlignment = Alignment.Center) {
            Text("Advertisement", color = Color(0xFF828587), fontSize = 11.sp)
        }
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val folder = File(compose.activity.cacheDir, "mpin-review")
        folder.mkdirs()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun checkPanelPlacement() {
        val card = compose.onNodeWithTag("mpin-card").fetchSemanticsNode().boundsInRoot
        val panel = compose.onNodeWithTag("mpin-keypad-panel").fetchSemanticsNode().boundsInRoot
        val keypad = compose.onNodeWithTag("mpin-keypad").fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Panel must be placed below fields", panel.top >= card.bottom)
        assertTrue("Keypad must be visible within viewport", keypad.bottom <= root.bottom)
        compose.onNodeWithTag("mpin-ad").assertIsDisplayed()
    }

    @Test fun setupReferenceStatesAndHeaderAction() {
        val state = mutableStateOf(SetupLockUiState.CREATE_EMPTY)
        val later = mutableStateOf(true)
        var skipped = false
        compose.setContent {
            BizoraTheme {
                val pinLength = when (state.value) {
                    SetupLockUiState.CREATE_EMPTY, SetupLockUiState.CONFIRM_EMPTY -> 0
                    SetupLockUiState.CREATE_ENTERING -> 2
                    SetupLockUiState.CONFIRM_ENTERING -> 3
                    else -> 4
                }
                MpinSetupContent(
                    uiState = state.value, pinLength = pinLength, showSetupLater = later.value,
                    onBack = {}, onSetupLater = { skipped = true }, onDigit = {}, onBackspace = {},
                    onChangeMpin = {}, onRetry = {}, onDone = {}, adContent = { PreviewAd() }
                )
            }
        }
        compose.onNodeWithText("Set up later").performClick()
        compose.runOnIdle { assertTrue(skipped) }
        val image = compose.onRoot().captureToImage().toPixelMap()
        assertEquals("Screen canvas must be warm ivory", MpinStyle.Canvas, image[2, 2])
        val brand = compose.onNodeWithTag("mpin-brand").fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Brand must be on the left", brand.left < root.center.x)
        capture("setup_after_sign_in")
        compose.runOnIdle { later.value = false }
        compose.onNodeWithText("Set up later").assertDoesNotExist()
        SetupLockUiState.entries.forEach { next ->
            compose.runOnIdle { state.value = next }
            if (next != SetupLockUiState.ENABLED_SUCCESS) {
                checkPanelPlacement()
                if (next == SetupLockUiState.SAVING || next == SetupLockUiState.SAVE_ERROR) {
                    compose.onNodeWithText("1").assertIsNotEnabled()
                }
            } else {
                compose.onNodeWithText("Done").assertIsDisplayed()
                compose.onNodeWithTag("mpin-ad").assertIsDisplayed()
            }
            capture("setup_${next.name.lowercase()}")
        }
    }

    @Test fun unlockReferenceStates() {
        val state = mutableStateOf(LockUiState.READY)
        compose.setContent {
            BizoraTheme {
                AppLockContent(
                    uiState = state.value,
                    pinLength = when (state.value) { LockUiState.READY -> 0; LockUiState.ENTERING -> 2; else -> 4 },
                    firstName = "Shivam", cooldownSeconds = if (state.value == LockUiState.TEMPORARILY_LOCKED) 30 else 0,
                    onDigit = {}, onBackspace = {}, onForgotMpin = {}, onRetry = {}, onCancelReset = {},
                    adContent = { PreviewAd() }
                )
            }
        }
        LockUiState.entries.forEach { next ->
            compose.runOnIdle { state.value = next }
            checkPanelPlacement()
            if (next in listOf(LockUiState.CHECKING, LockUiState.UNLOCKED, LockUiState.TEMPORARILY_LOCKED, LockUiState.CONNECTION_ERROR)) {
                compose.onNodeWithText("1").assertIsNotEnabled()
            }
            capture("unlock_${next.name.lowercase()}")
        }
    }

    @Test fun compactScreenKeepsErrorControlsAndAdReachable() {
        val setup = mutableStateOf(true)
        compose.setContent {
            BizoraTheme {
                Box(Modifier.size(width = 360.dp, height = 640.dp)) {
                    if (setup.value) {
                        MpinSetupContent(
                            uiState = SetupLockUiState.SAVE_ERROR, pinLength = 4,
                            onBack = {}, onSetupLater = {}, onDigit = {}, onBackspace = {},
                            onChangeMpin = {}, onRetry = {}, onDone = {}, adContent = { PreviewAd() }
                        )
                    } else {
                        AppLockContent(
                            uiState = LockUiState.CONNECTION_ERROR, pinLength = 4, firstName = "Shivam",
                            onDigit = {}, onBackspace = {}, onForgotMpin = {}, onRetry = {}, onCancelReset = {},
                            adContent = { PreviewAd() }
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Retry").assertIsDisplayed()
        compose.onNodeWithContentDescription("Backspace").assertIsDisplayed()
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithTag("mpin-ad").assertIsDisplayed()
        capture("compact_setup_error")
        compose.runOnIdle { setup.value = false }
        compose.onNodeWithText("Retry").assertIsDisplayed()
        compose.onNodeWithContentDescription("Backspace").assertIsDisplayed()
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithTag("mpin-ad").assertIsDisplayed()
        capture("compact_unlock_error")
    }
}
