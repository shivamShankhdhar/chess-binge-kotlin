package com.example.chessbinge.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessbinge.audio.SoundManager
import com.example.chessbinge.engine.PuzzleRepository
import com.example.chessbinge.model.AIDifficulty
import com.example.chessbinge.model.GameMode
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.model.TimeControl
import com.example.chessbinge.ui.theme.BgCard
import com.example.chessbinge.ui.theme.BgCardBorder
import com.example.chessbinge.ui.theme.BgDark
import com.example.chessbinge.ui.theme.CrimsonDark
import com.example.chessbinge.ui.theme.CrimsonGlow
import com.example.chessbinge.ui.theme.CrimsonPrimary
import com.example.chessbinge.ui.theme.GoldAccent
import com.example.chessbinge.ui.theme.TextPrimary
import com.example.chessbinge.ui.theme.TextSecondary
import com.example.chessbinge.ui.theme.TitanOneFontFamily
import com.example.chessbinge.viewmodel.ChessViewModel
import kotlin.random.Random

@Composable
fun HomeScreen(
    viewModel: ChessViewModel,
    onStartGame: () -> Unit,
    onStartPuzzle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsState()

    var selectedDifficulty by remember { mutableStateOf(AIDifficulty.MEDIUM) }
    var selectedPlayerColor by remember { mutableStateOf<PieceColor?>(PieceColor.WHITE) } // null = random
    var selectedTimeControl by remember { mutableStateOf(TimeControl.BLITZ_5) }

    var isSoundMuted by remember { mutableStateOf(!SoundManager.isSoundEnabled.value) }
    var isVibrationMuted by remember { mutableStateOf(!SoundManager.isVibrationEnabled.value) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Audio Toggles
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CrimsonDark)
                            .border(1.dp, CrimsonGlow, CircleShape)
                    ) {
                        Text(
                            text = "♔",
                            fontSize = 22.sp,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHESS BINGE",
                        fontFamily = TitanOneFontFamily,
                        fontSize = 18.sp,
                        color = CrimsonPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                Row {
                    IconButton(onClick = {
                        isSoundMuted = !SoundManager.toggleSound()
                    }) {
                        Icon(
                            imageVector = if (isSoundMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Sound",
                            tint = if (isSoundMuted) TextSecondary else CrimsonGlow
                        )
                    }

                    IconButton(onClick = {
                        isVibrationMuted = !SoundManager.toggleVibration()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Vibration",
                            tint = if (isVibrationMuted) TextSecondary else CrimsonGlow
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2C1014),
                                Color(0xFF1D1216),
                                BgCard
                            )
                        )
                    )
                    .border(1.5.dp, CrimsonPrimary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "RED & BLACK",
                                fontFamily = TitanOneFontFamily,
                                fontSize = 12.sp,
                                color = GoldAccent,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "MASTER THE BOARD",
                                fontFamily = TitanOneFontFamily,
                                fontSize = 22.sp,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "♞",
                            fontSize = 42.sp,
                            color = CrimsonGlow
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats Bar
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF14151B))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        StatItem(label = "PLAYED", value = "${stats.gamesPlayed}")
                        StatItem(label = "WINS", value = "${stats.wins}", valueColor = GoldAccent)
                        StatItem(label = "LOSSES", value = "${stats.losses}")
                        StatItem(label = "PUZZLES", value = "${stats.puzzlesSolved}", valueColor = CrimsonGlow)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mode 1: Play vs AI Card
            ModeCard(
                title = "PLAY VS COMPUTER",
                subtitle = "Customizable engine with tactical evaluation",
                icon = Icons.Default.SmartToy,
                badge = "SINGLE PLAYER"
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Difficulty Selector
                    Text(text = "DIFFICULTY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AIDifficulty.entries.forEach { diff ->
                            val isSelected = selectedDifficulty == diff
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CrimsonPrimary else Color(0xFF1C1D26))
                                    .clickable { selectedDifficulty = diff }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = diff.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Player Color Selector
                    Text(text = "PLAY AS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val colorOptions = listOf(
                            PieceColor.WHITE to "White ♔",
                            null to "Random ☯",
                            PieceColor.BLACK to "Black ♚"
                        )
                        for ((optColor, label) in colorOptions) {
                            val isSelected = selectedPlayerColor == optColor
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CrimsonDark else Color(0xFF1C1D26))
                                    .border(if (isSelected) 1.dp else 0.dp, if (isSelected) CrimsonGlow else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedPlayerColor = optColor }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time Control Selector
                    Text(text = "TIME CONTROL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            TimeControl.UNTIMED to "Untimed",
                            TimeControl.BLITZ_3 to "3 min",
                            TimeControl.BLITZ_5 to "5 min",
                            TimeControl.RAPID_10 to "10 min"
                        ).forEach { (tc, label) ->
                            val isSelected = selectedTimeControl == tc
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CrimsonPrimary else Color(0xFF1C1D26))
                                    .clickable { selectedTimeControl = tc }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Start Match Button
                    Button(
                        onClick = {
                            val color = selectedPlayerColor ?: if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
                            viewModel.startNewGame(
                                mode = GameMode.VS_AI,
                                difficulty = selectedDifficulty,
                                time = selectedTimeControl,
                                color = color
                            )
                            onStartGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START VS AI",
                            fontFamily = TitanOneFontFamily,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode 2: Pass & Play Card
            ModeCard(
                title = "PASS & PLAY",
                subtitle = "Challenge a friend on the same screen",
                icon = Icons.Default.Group,
                badge = "2 PLAYERS"
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.startNewGame(
                            mode = GameMode.PASS_AND_PLAY,
                            time = selectedTimeControl,
                            color = PieceColor.WHITE
                        )
                        onStartGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222430)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "PLAY LOCAL MATCH",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode 3: Tactics & Puzzles Card
            ModeCard(
                title = "TACTICS & PUZZLES",
                subtitle = "Master checkmate patterns and tactical forks",
                icon = Icons.Default.Extension,
                badge = "${PuzzleRepository.puzzles.size} PUZZLES"
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.loadPuzzle(PuzzleRepository.puzzles.first())
                        onStartPuzzle()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E151A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonGlow.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "SOLVE PUZZLES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CrimsonGlow
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CrimsonGlow, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF241519))
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = CrimsonGlow, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = title, fontFamily = TitanOneFontFamily, fontSize = 15.sp, color = TextPrimary)
                        Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1D1F28))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            content()
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontFamily = TitanOneFontFamily, fontSize = 16.sp, color = valueColor)
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
    }
}
