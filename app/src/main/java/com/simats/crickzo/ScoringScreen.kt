package com.simats.crickzo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ScoringScreen(
    matchId: String,
    teamAName: String,
    teamBName: String,
    teamAPlayers: List<String>,
    teamBPlayers: List<String>,
    initialStriker: String = "",
    initialNonStriker: String = "",
    initialBowler: String = "",
    onBack: () -> Unit,
    onMatchComplete: (CompletedMatchInfo) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val apiService = RetrofitClient.apiService
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf("Score Update") }
    var currentInnings by remember { mutableIntStateOf(1) }
    
    // UI Dialog States
    var showWhoOutDialog by remember { mutableStateOf(false) }
    var showSelectNextBatsmanDialog by remember { mutableStateOf(false) }
    var showSelectStrikerDialog by remember { mutableStateOf(false) }
    var showSelectNonStrikerDialog by remember { mutableStateOf(false) }
    var showChangeBowlerDialog by remember { mutableStateOf(false) }
    var showOverCompleteDialog by remember { mutableStateOf(false) }
    var showEndInningsDialog by remember { mutableStateOf(false) }
    var showEndMatchDialog by remember { mutableStateOf(false) }
    
    var outPosition by remember { mutableStateOf("") }
    
    val currentBattingTeam = if (currentInnings == 1) teamAName else teamBName
    val battingPlayers = if (currentInnings == 1) teamAPlayers else teamBPlayers
    val bowlingPlayers = if (currentInnings == 1) teamBPlayers else teamAPlayers

    // Scoreboard State
    var totalRuns by remember { mutableIntStateOf(0) }
    var totalWickets by remember { mutableIntStateOf(0) }
    var totalOvers by remember { mutableDoubleStateOf(0.0) }
    var currentCRR by remember { mutableDoubleStateOf(0.0) }

    // Stats
    var batsmenStats by remember { mutableStateOf(emptyList<BatsmanStat>()) }
    var bowlersStats by remember { mutableStateOf(emptyList<BowlerStat>()) }
    var partnershipData by remember { mutableStateOf(PartnershipResponse(0, 0)) }
    var lastSixBalls by remember { mutableStateOf(emptyList<String>()) }
    var apiPredictions by remember { mutableStateOf<MatchPredictions?>(null) }

    // Current Players State
    var striker by remember { mutableStateOf(initialStriker) }
    var nonStriker by remember { mutableStateOf(initialNonStriker) }
    var currentBowlerName by remember { mutableStateOf(initialBowler) }

    val dismissedPlayers = remember { mutableStateListOf<String>() }

    fun refreshMatchData() {
        coroutineScope.launch {
            try {
                val scoreRes = apiService.getScoreboard(matchId)
                if (scoreRes.isSuccessful) {
                    scoreRes.body()?.let {
                        totalRuns = it.runs
                        totalWickets = it.wickets
                        totalOvers = it.overs
                        currentCRR = it.crr
                    }
                }

                val batsmenRes = apiService.getBatsmenStats(matchId)
                if (batsmenRes.isSuccessful) {
                    val stats = batsmenRes.body() ?: emptyList()
                    batsmenStats = stats
                    if (stats.isNotEmpty()) {
                        if (striker.isEmpty()) striker = stats.getOrNull(0)?.batsman ?: ""
                        if (nonStriker.isEmpty()) nonStriker = stats.getOrNull(1)?.batsman ?: ""
                    }
                }

                val bowlerRes = apiService.getBowlerStats(matchId)
                if (bowlerRes.isSuccessful) {
                    val stats = bowlerRes.body() ?: emptyList()
                    bowlersStats = stats
                    if (stats.isNotEmpty() && currentBowlerName.isEmpty()) {
                        currentBowlerName = stats.lastOrNull()?.bowler ?: ""
                    }
                }

                val partRes = apiService.getPartnership(matchId)
                if (partRes.isSuccessful) {
                    partnershipData = partRes.body() ?: PartnershipResponse(0, 0)
                }

                val last6Res = apiService.getLastSixBalls(matchId)
                if (last6Res.isSuccessful) {
                    lastSixBalls = last6Res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Network error handling
            }
        }
    }

    LaunchedEffect(matchId, selectedTab) {
        refreshMatchData()
    }

    fun finalizeWicket(player: String) {
        dismissedPlayers.add(player)
        outPosition = if (player == striker) "striker" else "non-striker"
        showWhoOutDialog = false
        showSelectNextBatsmanDialog = true
    }

    fun submitBall(runs: Int, extras: Int, isWicket: Boolean, extraType: String = "") {
        if (striker.isEmpty() || currentBowlerName.isEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Select Striker and Bowler first!")
            }
            return
        }

        coroutineScope.launch {
            try {
                val overNum = totalOvers.toInt()
                val ballNum = ((totalOvers * 10).toInt() % 10) + 1
                
                val request = BallInputRequest(
                    matchId = matchId.toIntOrNull() ?: 0,
                    innings = currentInnings,
                    over = overNum,
                    ball = ballNum,
                    batsman = striker,
                    bowler = currentBowlerName,
                    runs = runs,
                    extras = extras,
                    wicket = if (isWicket) 1 else 0
                )
                
                val res = apiService.submitBall(request)
                if (res.isSuccessful) {
                    refreshMatchData()
                    if (runs % 2 != 0) {
                        val temp = striker; striker = nonStriker; nonStriker = temp
                    }
                    val updatedOvers = apiService.getScoreboard(matchId).body()?.overs ?: totalOvers
                    if (((updatedOvers * 10).toInt() % 10) == 0 && updatedOvers > totalOvers) {
                        showOverCompleteDialog = true
                        val temp = striker; striker = nonStriker; nonStriker = temp
                    }
                } else {
                    snackbarHostState.showSnackbar("Backend Error: ${res.message()}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Check backend connection!")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(padding)) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth().background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF1E40AF), Color(0xFF3B82F6)))).padding(top = 40.dp, bottom = 20.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$teamAName vs $teamBName", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Live Scoring • Innings $currentInnings", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                        IconButton(onClick = { showEndInningsDialog = true }) { Icon(Icons.Default.StopCircle, "End", tint = Color.White) }
                    }
                    
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(40.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)).padding(4.dp)) {
                        listOf("Score Update", "Predictions").forEach { tab ->
                            val isSelected = selectedTab == tab
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(18.dp)).clickable { selectedTab = tab }, contentAlignment = Alignment.Center) {
                                Text(tab, color = if (isSelected) Color(0xFF1E40AF) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (selectedTab == "Score Update") {
                    // Score Card
                    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFEFF6FF), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0xFFDBEAFE))) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(currentBattingTeam, color = Color(0xFF64748B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("$totalRuns/$totalWickets", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${String.format(Locale.US, "%.1f", totalOvers)} Overs", color = Color(0xFF64748B), fontSize = 16.sp)
                                Spacer(Modifier.width(16.dp))
                                Text("CRR: ${String.format(Locale.US, "%.2f", currentCRR)}", color = Color(0xFF1E40AF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Batsmen Section
                    ScoringSectionCard(title = "Batsmen", actionIcon = Icons.AutoMirrored.Filled.CompareArrows, onActionClick = { 
                        val t = striker; striker = nonStriker; nonStriker = t 
                    }) {
                        if (striker.isEmpty() && nonStriker.isEmpty()) {
                            Text("Tap to select batsmen", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().clickable { showSelectStrikerDialog = true }.padding(12.dp))
                        } else {
                            val sStat = batsmenStats.find { it.batsman == striker } ?: BatsmanStat(striker, 0, 0, 0, 0, 0.0)
                            val nsStat = batsmenStats.find { it.batsman == nonStriker } ?: BatsmanStat(nonStriker, 0, 0, 0, 0, 0.0)
                            
                            BatsmanRowItem(sStat.batsman, sStat.runs, sStat.balls, sStat.fours, sStat.sixes, true, onClick = { showSelectStrikerDialog = true })
                            BatsmanRowItem(nsStat.batsman, nsStat.runs, nsStat.balls, nsStat.fours, nsStat.sixes, false, onClick = { showSelectNonStrikerDialog = true })
                        }
                    }

                    // Bowler Section
                    ScoringSectionCard(title = "Bowler", actionIcon = Icons.Outlined.Sync, onActionClick = { showChangeBowlerDialog = true }) {
                        val bowler = bowlersStats.find { it.bowler == currentBowlerName }
                        if (currentBowlerName.isEmpty()) {
                            Text("Tap to select bowler", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().clickable { showChangeBowlerDialog = true }.padding(12.dp))
                        } else {
                            BowlerRowItem(currentBowlerName, bowler?.overs ?: "0.0", 0, bowler?.runs ?: 0, bowler?.wickets ?: 0, String.format(Locale.US, "%.2f", bowler?.economy ?: 0.0), onClick = { showChangeBowlerDialog = true })
                        }
                    }

                    // Run Buttons
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                listOf("0", "1", "2", "3", "4", "6").forEach { RunCircleButton(it) { submitBall(it.toInt(), 0, false) } }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionPillButton("Wide", Color(0xFFF97316), Modifier.weight(1f)) { submitBall(0, 1, false, "wd") }
                                ActionPillButton("No Ball", Color(0xFFF97316), Modifier.weight(1f)) { submitBall(0, 1, false, "nb") }
                                ActionPillButton("Wicket", Color(0xFFEF4444), Modifier.weight(1f)) { showWhoOutDialog = true }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showSelectStrikerDialog) {
        SelectPlayerDialog("Select Striker", battingPlayers.filter { it != nonStriker }, { showSelectStrikerDialog = false }) { striker = it; showSelectStrikerDialog = false }
    }
    if (showSelectNonStrikerDialog) {
        SelectPlayerDialog("Select Non-Striker", battingPlayers.filter { it != striker }, { showSelectNonStrikerDialog = false }) { nonStriker = it; showSelectNonStrikerDialog = false }
    }
    if (showChangeBowlerDialog || showOverCompleteDialog) {
        SelectPlayerDialog("Select Bowler", bowlingPlayers, { showChangeBowlerDialog = false; showOverCompleteDialog = false }) { currentBowlerName = it; showChangeBowlerDialog = false; showOverCompleteDialog = false }
    }
    if (showWhoOutDialog) {
        WhoGotOutDialog(striker, nonStriker, { showWhoOutDialog = false }, { finalizeWicket(it) })
    }
    if (showSelectNextBatsmanDialog) {
        val available = battingPlayers.filter { it !in dismissedPlayers && it != striker && it != nonStriker }
        SelectPlayerDialog("Select Next Batsman", available, { showSelectNextBatsmanDialog = false }) { if (outPosition == "striker") striker = it else nonStriker = it; showSelectNextBatsmanDialog = false }
    }
}

@Composable
fun StatMiniCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color(0xFF1E293B), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun SelectPlayerDialog(title: String, players: List<String>, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(players) { player ->
                Surface(modifier = Modifier.fillMaxWidth().clickable { onSelect(player) }.padding(vertical = 8.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
                    Text(player, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun ScoringSectionCard(title: String, actionIcon: ImageVector, onActionClick: () -> Unit, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                IconButton(onClick = onActionClick) { Icon(actionIcon, null, modifier = Modifier.size(20.dp), tint = Color(0xFF3B82F6)) }
            }
            content()
        }
    }
}

@Composable
fun BatsmanRowItem(name: String, runs: Int, balls: Int, fours: Int, sixes: Int, isOnStrike: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (isOnStrike) Icon(Icons.Default.Bolt, null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
            Text(name.ifBlank { "Select Batsman" }, fontWeight = if (isOnStrike) FontWeight.Bold else FontWeight.Normal, color = if(isOnStrike) Color(0xFF1E40AF) else Color(0xFF1E293B))
        }
        Text("$runs($balls)", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}

@Composable
fun BowlerRowItem(name: String, overs: String, maidens: Int, runs: Int, wickets: Int, econ: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name.ifBlank { "Select Bowler" }, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
        Text("$wickets-$runs ($overs)", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}

@Composable
fun RunCircleButton(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(42.dp), color = Color(0xFF1E40AF), shape = CircleShape) {
        Box(contentAlignment = Alignment.Center) { Text(text, color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun ActionPillButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(36.dp), colors = ButtonDefaults.buttonColors(containerColor = color), contentPadding = PaddingValues(horizontal = 8.dp), shape = RoundedCornerShape(8.dp)) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WhoGotOutDialog(striker: String, nonStriker: String, onDismiss: () -> Unit, onConfirmOut: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Wicket! Who is out?") }, text = {
        Column {
            Button(onClick = { onConfirmOut(striker) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) { Text(striker) }
            Button(onClick = { onConfirmOut(nonStriker) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) { Text(nonStriker) }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
