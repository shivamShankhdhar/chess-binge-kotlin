package bizora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// These screens have their own warm canvas; the rest of the app uses white.
internal object MpinStyle {
    val Canvas = Color(0xFFFBFBF4)
    val Slot = Color(0xFFF5F6EC)
    val Green = Color(0xFF3B550B)
    val Ink = Color(0xFF0B140A)
    val Secondary = Color(0xFF65666B)
    val Error = Color(0xFFC90000)
    val ErrorTint = Color(0xFFFFE5E3)
    val Amber = Color(0xFFF39800)
}

internal val LocalCompactMpin = staticCompositionLocalOf { false }

@Composable
internal fun MpinPage(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MpinStyle.Canvas, modifier = Modifier.fillMaxSize().testTag("mpin-page")) {
        BoxWithConstraints {
            CompositionLocalProvider(LocalCompactMpin provides (maxHeight < 720.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content
                )
            }
        }
    }
}

@Composable
internal fun MpinHeader(onBack: (() -> Unit)? = null, onSetupLater: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (LocalCompactMpin.current) 46.dp else 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("mpin-brand")
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MpinStyle.Green,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            BizoraSignInIcon(size = 28.dp)
            Text(
                "Bizora",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MpinStyle.Ink,
                letterSpacing = (-0.3).sp
            )
        }

        Spacer(Modifier.weight(1f))

        if (onSetupLater != null) {
            TextButton(
                onClick = onSetupLater,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("mpin-setup-later")
            ) {
                Text(
                    "Set up later",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MpinStyle.Green
                )
            }
        }
    }
}

@Composable
internal fun MpinHeading(title: String, subtitle: String, step: String? = null) {
    val compact = LocalCompactMpin.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (step != null) {
            Text(
                step,
                color = MpinStyle.Green,
                fontSize = if (compact) 13.sp else 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(if (compact) 4.dp else 6.dp))
        } else {
            Spacer(Modifier.height(if (compact) 2.dp else 4.dp))
        }
        Text(
            title,
            color = MpinStyle.Ink,
            fontSize = if (compact) 23.sp else 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(if (compact) 3.dp else 4.dp))
        Text(
            subtitle,
            color = MpinStyle.Secondary,
            fontSize = if (compact) 14.sp else 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** Container for the slots and action messages; kept transparent and cardless matching the reference design. */
@Composable
internal fun MpinInputCard(locked: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("mpin-card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
internal fun MpinSlots(
    filled: Int,
    error: Boolean = false,
    locked: Boolean = false,
    focus: Boolean = true,
    modifier: Modifier = Modifier
) {
    val compact = LocalCompactMpin.current
    val slotWidth = if (compact) 48.dp else 54.dp
    val slotHeight = if (compact) 52.dp else 58.dp
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp, Alignment.CenterHorizontally)
    ) {
        repeat(4) { index ->
            val isFilled = index < filled || error || locked
            val active = focus && index == filled && !error && !locked
            Box(
                modifier = Modifier
                    .size(width = slotWidth, height = slotHeight)
                    .clip(shape)
                    .background(
                        when {
                            error -> MpinStyle.ErrorTint
                            locked -> Color(0xFFF0EEE8)
                            active -> Color.White
                            else -> MpinStyle.Slot
                        }
                    )
                    .then(
                        when {
                            error -> Modifier.border(1.dp, Color(0xFFFCA5A5), shape)
                            active -> Modifier.border(1.5.dp, MpinStyle.Green, shape)
                            else -> Modifier
                        }
                    )
                    .semantics { contentDescription = "MPIN digit ${index + 1}, ${if (isFilled) "filled" else "empty"}" },
                contentAlignment = Alignment.Center
            ) {
                if (isFilled) {
                    Box(
                        Modifier
                            .size(if (compact) 12.dp else 14.dp)
                            .background(
                                when {
                                    error -> MpinStyle.Error
                                    locked -> Color(0xFFAEAEAC)
                                    else -> MpinStyle.Green
                                },
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
internal fun MpinLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = MpinStyle.Green,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = if (LocalCompactMpin.current) 4.dp else 6.dp)
    )
}

@Composable
internal fun MpinMessage(text: String, color: Color = MpinStyle.Green, icon: ImageVector? = null, loading: Boolean = false) {
    val compact = LocalCompactMpin.current
    val iconSize = if (compact) 16.dp else 18.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(iconSize), color = color, strokeWidth = 2.dp)
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
        }
        Text(
            text = text,
            color = color,
            fontSize = if (compact) 12.5.sp else 13.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun MpinButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val compact = LocalCompactMpin.current
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 42.dp else 48.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MpinStyle.Green)
    ) {
        Text(text, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun MpinKeypadPanel(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    disabled: Boolean,
    trustText: String,
    modifier: Modifier = Modifier,
    onBiometric: (() -> Unit)? = null,
    adContent: @Composable () -> Unit = { MpinAdSlot() }
) {
    val compact = LocalCompactMpin.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mpin-keypad-panel"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Numeric Keypad
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .testTag("mpin-keypad"),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 6.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )
            val keyHeight = if (compact) 48.dp else 56.dp
            val tint = if (disabled) Color(0xFFB3B3B3) else MpinStyle.Ink

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(keyHeight)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    enabled = !disabled && (key != "BIO" || onBiometric != null)
                                ) {
                                    when (key) {
                                        "DEL" -> onBackspace()
                                        "BIO" -> onBiometric?.invoke()
                                        else -> onDigit(key)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "DEL" -> Icon(
                                    Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = "Backspace",
                                    tint = tint,
                                    modifier = Modifier.size(24.dp)
                                )
                                "BIO" -> {
                                    if (onBiometric != null) {
                                        Icon(
                                            Icons.Default.Fingerprint,
                                            contentDescription = "Unlock with biometrics",
                                            tint = tint,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                else -> Text(
                                    text = key,
                                    fontSize = if (compact) 23.sp else 25.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = tint
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(if (compact) 12.dp else 18.dp))
        MpinTrustFooter(trustText, adContent)
    }
}

@Composable
internal fun MpinTrustFooter(text: String?, adContent: (@Composable () -> Unit)? = null) {
    val compact = LocalCompactMpin.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = if (compact) 10.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (text != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MpinStyle.Green,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = text,
                    color = MpinStyle.Secondary,
                    fontSize = if (compact) 11.5.sp else 12.5.sp
                )
            }
        }
        if (adContent != null) {
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).testTag("mpin-ad")) {
                adContent()
            }
        }
    }
}

@Composable
internal fun MpinAdSlot() {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        AdBanner(availableWidthDp = maxWidth.value.toInt(), showPlaceholder = false)
    }
}
