package bizora.app.ui.screens

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.data.model.Team
import bizora.app.ui.theme.LocalBizoraColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InviteMemberScreen(
    workplaceName: String,
    teams: List<Team>,
    initialTeamId: String?,
    onBack: () -> Unit,
    onCreateTeam: () -> Unit,
    onInvite: suspend (name: String, email: String, teamId: String) -> Result<Unit>
) {
    val colors = LocalBizoraColors.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedTeamId by remember(initialTeamId, teams) {
        mutableStateOf(initialTeamId?.takeIf { id -> teams.any { it.id == id } } ?: teams.firstOrNull()?.id)
    }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var invited by remember { mutableStateOf(false) }
    val selectedTeam = teams.find { it.id == selectedTeamId }

    BackHandler(enabled = !submitting) { onBack() }

    Scaffold(
        containerColor = colors.canvas,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, enabled = !submitting) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text("Invite member", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        },
        bottomBar = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanEmail = email.trim()
                    when {
                        cleanName.isEmpty() -> error = "Enter the member's name"
                        cleanEmail.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> error = "Enter a valid email address"
                        selectedTeamId == null -> error = "Create or select a team first"
                        else -> {
                            error = null
                            submitting = true
                            scope.launch {
                                val result = onInvite(cleanName, cleanEmail, selectedTeamId!!)
                                submitting = false
                                if (result.isSuccess) {
                                    invited = true
                                    delay(800)
                                    onBack()
                                } else {
                                    error = result.exceptionOrNull()?.message ?: "Could not invite member"
                                }
                            }
                        }
                    }
                },
                enabled = !submitting && !invited,
                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp).height(56.dp)
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                else Text(if (invited) "Invited" else "Invite", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(30.dp))
                    Text(selectedTeam?.name ?: "Your team", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text(workplaceName, fontSize = 14.sp, color = colors.textSecondary)
                    if (selectedTeam != null) {
                        Text("Team code: ${selectedTeam.displayCode}", fontSize = 13.sp, color = colors.brandPrimary)
                        val count = selectedTeam.members.count { it.role.equals("EMPLOYEE", ignoreCase = true) }
                        Text("$count ${if (count == 1) "member" else "members"}", fontSize = 13.sp, color = colors.textSecondary)
                    } else {
                        Text("Create a team before inviting your first member.", fontSize = 14.sp, color = colors.textSecondary)
                    }
                }
            }

            if (teams.size > 1) {
                Text("Choose team", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                teams.forEach { team ->
                    val selected = selectedTeamId == team.id
                    Surface(
                        color = if (selected) colors.brandTint else colors.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (selected) colors.brandPrimary else colors.border),
                        modifier = Modifier.fillMaxWidth().clickable { selectedTeamId = team.id }
                    ) {
                        Text(team.name, modifier = Modifier.padding(14.dp), color = colors.textPrimary)
                    }
                }
            }

            TextButton(onClick = onCreateTeam) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Create a new team")
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("Email (optional)") },
                supportingText = { Text("An email lets this member join directly. Their employee code is generated automatically.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            if (error != null) Text(error!!, color = colors.danger, fontSize = 14.sp)
            if (invited) Text("Member invited successfully", color = colors.brandPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
        }
    }
}
