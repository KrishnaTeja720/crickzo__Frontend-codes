package com.simats.crickzo

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ScoringScreen(
    matchId: String,
    userId: Int,
    teamAName: String,
    teamBName: String,
    teamAPlayers: List<String>,
    teamBPlayers: List<String>,
    initialStriker: String = "",
    initialNonStriker: String = "",
    initialBowler: String = "",
    tossWinner: String = "",
    tossDecision: String = "",
    onBack: () -> Unit = {},
    onMatchComplete: (CompletedMatchInfo) -> Unit = {},
    initialInnings: Int = 1
) {
    val coroutineScope = rememberCoroutineScope()
    val apiService = RetrofitClient.apiService
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf("Score Update") }
    var currentInnings by remember { mutableIntStateOf(initialInnings) }
    var isLoadingInnings by remember { mutableStateOf(true) }
    
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
    
    // Local state for players
    val localTeamAPlayers = remember { mutableStateListOf<String>() }
    val localTeamBPlayers = remember { mutableStateListOf<String>() }
    var bowlersFromApi by remember { mutableStateOf(emptyList<Player>()) }
    var batsmenFromApi by remember { mutableStateOf(emptyList<Player>()) }

    // Sync parameters to local state
    LaunchedEffect(teamAPlayers, teamBPlayers) {
        if (teamAPlayers.isNotEmpty()) {
            localTeamAPlayers.clear()
            localTeamAPlayers.addAll(teamAPlayers)
        }
        if (teamBPlayers.isNotEmpty()) {
            localTeamBPlayers.clear()
            localTeamBPlayers.addAll(teamBPlayers)
        }
    }

    val effectiveTeamAPlayers = if (teamAPlayers.isNotEmpty()) teamAPlayers else localTeamAPlayers
    val effectiveTeamBPlayers = if (teamBPlayers.isNotEmpty()) teamBPlayers else localTeamBPlayers

    // Toss-based team assignment
    val isTeamABattingFirst = if (tossWinner.isEmpty() || tossDecision.isEmpty()) true 
                             else (tossWinner == teamAName && tossDecision == "Batting") || 
                                  (tossWinner == teamBName && tossDecision == "Bowling")
    
    val isTeamABattingNow = if (currentInnings == 1) isTeamABattingFirst else !isTeamABattingFirst

    val currentBattingTeam = if (isTeamABattingNow) teamAName else teamBName
    val battingPlayers = if (isTeamABattingNow) effectiveTeamAPlayers else effectiveTeamBPlayers
    val bowlingPlayers = if (isTeamABattingNow) effectiveTeamBPlayers else effectiveTeamAPlayers

    // Scoreboard State
    var totalRuns by remember { mutableIntStateOf(0) }
    var totalWickets by remember { mutableIntStateOf(0) }
    var totalBalls by remember { mutableIntStateOf(0) }
    var currentCRR by remember { mutableDoubleStateOf(0.0) }
    var firstInningsScore by remember { mutableStateOf<ScoreResponse?>(null) }
    
    // Manual Edit State
    var isManualScore by remember { mutableStateOf(false) }
    var editableRuns by remember(totalRuns) { mutableStateOf(totalRuns.toString()) }
    var editableWickets by remember(totalWickets) { mutableStateOf(totalWickets.toString()) }
    var editableOvers by remember(totalBalls) { mutableStateOf(String.format(Locale.US, "%d.%d", totalBalls / 6, totalBalls % 6)) }

    // Stats
    var batsmenStats by remember { mutableStateOf(emptyList<BatsmanStat>()) }
    var bowlersStats by remember { mutableStateOf(emptyList<BowlerStat>()) }
    var partnershipData by remember { mutableStateOf(PartnershipResponse(0, 0)) }
    var lastSixBalls by remember { mutableStateOf(emptyList<String>()) }

    // Current Players State from match_state
    var striker by remember { mutableStateOf(initialStriker) }
    var strikerRuns by remember { mutableIntStateOf(0) }
    var strikerBalls by remember { mutableIntStateOf(0) }
    
    var nonStriker by remember { mutableStateOf(initialNonStriker) }
    var nonStrikerRuns by remember { mutableIntStateOf(0) }
    var nonStrikerBalls by remember { mutableIntStateOf(0) }
    
    var currentBowlerName by remember { mutableStateOf(initialBowler) }
    var currentBowlerRuns by remember { mutableIntStateOf(0) }
    var currentBowlerWickets by remember { mutableIntStateOf(0) }
    var currentBowlerOvers by remember { mutableStateOf("0.0") }

    val dismissedPlayers = remember { mutableStateListOf<String>() }
    
    // Predictions State
    var apiPredictions by remember { mutableStateOf<MatchPredictions?>(null) }

    // Extras support state
    var selectedExtraType by remember { mutableStateOf("") }
    
    // Scorecard State
    var scorecardData by remember { mutableStateOf<ScorecardResponse?>(null) }

    fun refreshMatchData(isBallInput: Boolean = false) {
        if (!isManualScore) {
            coroutineScope.launch {
                try {
                    // Force refresh & fetch GROUND TRUTH in one call
                    val response = apiService.getMatchDetails(matchId)
                    if (response.isSuccessful) {
                        val details = response.body()
                        if (details != null) {
                            val prevBalls = totalBalls
                            
                            // MASTER SYNC - Use top-level Ground Truth fields
                            totalRuns = details.runs ?: totalRuns
                            totalWickets = details.wickets ?: totalWickets
                            val oStr = details.overs ?: "0.0"
                            val parts = oStr.split(".")
                            val ovVal = parts[0].toIntOrNull() ?: 0
                            val baVal = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
                            totalBalls = ovVal * 6 + baVal
                            currentCRR = details.crr ?: currentCRR
                            currentInnings = details.currentInnings ?: currentInnings
                            
                            // Players Sync
                            if (!details.striker.isNullOrEmpty()) striker = details.striker
                            if (!details.nonStriker.isNullOrEmpty()) nonStriker = details.nonStriker
                            if (!details.bowler.isNullOrEmpty()) currentBowlerName = details.bowler
                            
                            // Secondary Data
                            val teamScore = if (currentInnings == 1) details.teamAScore else details.teamBScore
                            if (teamScore != null) {
                                strikerRuns = teamScore.strikerRuns
                                strikerBalls = teamScore.strikerBalls
                                nonStrikerRuns = teamScore.nonStrikerRuns
                                nonStrikerBalls = teamScore.nonStrikerBalls
                                currentBowlerRuns = teamScore.bowlerRuns
                                currentBowlerWickets = teamScore.bowlerWickets
                                currentBowlerOvers = teamScore.bowlerOvers ?: "0.0"
                            }
                            
                            if (currentInnings == 2 && details.teamAScore != null) {
                                firstInningsScore = details.teamAScore
                            }

                            // Dynamic UI Binding
                            editableRuns = totalRuns.toString()
                            editableWickets = totalWickets.toString()
                            editableOvers = String.format(Locale.US, "%d.%d", totalBalls / 6, totalBalls % 6)

                            if (isBallInput && totalBalls / 6 > prevBalls / 6 && totalBalls > 0) {
                                showOverCompleteDialog = true
                            }
                        }
                    }

                    // Scorecard/Stats Refresh
                    val batsRes = apiService.getBatsmenStats(matchId, currentInnings)
                    if (batsRes.isSuccessful) batsmenStats = batsRes.body() ?: emptyList()

                    val bowlRes = apiService.getBowlerStats(matchId, currentInnings)
                    if (bowlRes.isSuccessful) bowlersStats = bowlRes.body() ?: emptyList()

                    val last6 = apiService.getLast6Balls(matchId, currentInnings)
                    if (last6.isSuccessful) lastSixBalls = last6.body() ?: emptyList()

                    val part = apiService.getPartnership(matchId, currentInnings)
                    if (part.isSuccessful) partnershipData = part.body() ?: PartnershipResponse(0,0)

                    if (selectedTab == "Scorecard") {
                        val scRes = apiService.getScorecard(matchId)
                        if (scRes.isSuccessful) scorecardData = scRes.body()
                    }
                    if (selectedTab == "Predictions") {
                        val pred = apiService.getMatchPredictions(matchId)
                        if (pred.isSuccessful) apiPredictions = pred.body()
                    }

                } catch (e: Exception) {
                    Log.e("Sync", "Refesh error: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(matchId) {
        if (matchId != "0") {
            try {
                if (localTeamAPlayers.isEmpty() || localTeamBPlayers.isEmpty()) {
                    val res = apiService.getMatchPlayers(matchId)
                    if (res.isSuccessful) {
                        res.body()?.let { list ->
                            localTeamAPlayers.clear(); localTeamBPlayers.clear()
                            list.forEach { p ->
                                if (p.teamName?.lowercase() == teamBName.lowercase()) 
                                    localTeamBPlayers.add(p.playerName)
                                else localTeamAPlayers.add(p.playerName)
                            }
                        }
                    }
                }
                refreshMatchData()
            } catch (e: Exception) {
                Log.e("Init", "Match init failed")
            } finally {
                isLoadingInnings = false
            }
        }
    }

    // Fetch bowlers from backend when needed with Debug Tracing (STEP 3 & 4)
    LaunchedEffect(showChangeBowlerDialog, showOverCompleteDialog) {
        if (showChangeBowlerDialog || showOverCompleteDialog) {
            try {
                Log.d("BOWLER", "Calling API for match: $matchId")
                val mId = matchId.toIntOrNull() ?: 0
                val response = apiService.getBowlers(mId)

                if (response.isSuccessful && response.body() != null) {
                    bowlersFromApi = response.body()!!
                }
            } catch (e: Exception) {
                Log.e("BOWLER", "Network error fetching bowlers", e)
            }
        }
    }

    // Fetch batsmen from backend when needed
    LaunchedEffect(showSelectNextBatsmanDialog, showSelectStrikerDialog, showSelectNonStrikerDialog) {
        if (showSelectNextBatsmanDialog || showSelectStrikerDialog || showSelectNonStrikerDialog) {
            try {
                val mId = matchId.toIntOrNull() ?: 0
                val response = apiService.getBatsmen(mId)
                if (response.isSuccessful && response.body() != null) {
                    batsmenFromApi = response.body()!!
                }
            } catch (e: Exception) {
                Log.e("BATSMAN", "Network error fetching batsmen", e)
            }
        }
    }

    LaunchedEffect(matchId, selectedTab, currentInnings, isLoadingInnings) {
        if (!isLoadingInnings) {
            refreshMatchData()
        }
    }

    // Live-poll predictions every 2500ms on Predictions tab (mirrors website interval)
    LaunchedEffect(selectedTab, matchId) {
        if (selectedTab == "Predictions") {
            while (true) {
                try {
                    val pred = apiService.getMatchPredictions(matchId)
                    if (pred.isSuccessful) apiPredictions = pred.body()
                } catch (e: Exception) {
                    Log.e("PredPoll", "Prediction poll failed")
                }
                kotlinx.coroutines.delay(2500L)
            }
        }
    }

    fun updateFromEditable() {
        isManualScore = true
        val r = editableRuns.toIntOrNull() ?: totalRuns
        val w = editableWickets.toIntOrNull() ?: totalWickets
        val oParts = editableOvers.split(".")
        val oBalls = if (oParts.size == 2) {
            (oParts[0].toIntOrNull() ?: 0) * 6 + (oParts[1].toIntOrNull() ?: 0)
        } else {
            (oParts[0].toIntOrNull() ?: 0) * 6
        }
        
        totalRuns = r
        totalWickets = w
        totalBalls = oBalls
        
        currentCRR = if (oBalls > 0) (r.toDouble() / (oBalls / 6.0)) else 0.0

        coroutineScope.launch {
            try {
                val ov = oBalls / 6
                val rm = oBalls % 6
                val oStr = "$ov.$rm"
                apiService.editScore(EditScoreRequest(matchId, currentInnings, r, w, oStr))
            } catch (e: Exception) {
                Log.e("ScoreEdit", "Failed to sync edit", e)
            }
        }
    }

    var isUndoing by remember { mutableStateOf(false) }

    fun undoLastBall() {
        if (isUndoing) return
        isUndoing = true
        coroutineScope.launch {
            try {
                val res = apiService.undoBall(matchId)
                if (res.isSuccessful) {
                    isManualScore = false
                    refreshMatchData()
                    snackbarHostState.showSnackbar("Last ball undone")
                } else {
                    snackbarHostState.showSnackbar("Undo failed")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Network error")
            } finally {
                isUndoing = false
            }
        }
    }

    fun submitBall(
        runs: Int,
        extrasType: String = "",
        extrasRuns: Int = 0,
        wicket: Int = 0
    ) {
        if (striker.isEmpty() || currentBowlerName.isEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Select Striker and Bowler first!")
            }
            return
        }

        coroutineScope.launch {
            try {
                val request = BallInputRequest(
                    matchId = matchId.toInt(),
                    innings = currentInnings,
                    batsman = striker,
                    bowler = currentBowlerName,
                    runs = runs,
                    extras = extrasRuns,
                    wicket = wicket,
                    extrasType = extrasType
                )

                val res = apiService.submitBall(request)

                if (res.isSuccessful) {
                    refreshMatchData(isBallInput = true)
                } else {
                    snackbarHostState.showSnackbar("Error: ${res.message()}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Check connection!")
            }
        }
    }

    fun updateBowler(newBowler: String) {
        if (newBowler.isEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Please select a bowler")
            }
            return
        }

        coroutineScope.launch {
            try {
                val mId = matchId.toIntOrNull() ?: 0
                val response = apiService.changeBowler(ChangeBowlerRequest(mId, newBowler))
                if (response.isSuccessful) {
                    currentBowlerName = newBowler
                    refreshMatchData()
                    snackbarHostState.showSnackbar("Bowler changed to $newBowler")
                } else {
                    snackbarHostState.showSnackbar("Failed to update bowler")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Network error: ${e.message}")
            }
        }
    }

    fun finalizeWicket(player: String) {
        dismissedPlayers.add(player)
        outPosition = if (player == striker) "striker" else "non-striker"
        showWhoOutDialog = false
        showSelectNextBatsmanDialog = true

        val exType = selectedExtraType
        val exRuns = if (exType == "wide" || exType == "no ball") 1 else 0
        val finalExtrasType = if (exType == "") "wicket" else exType

        submitBall(
            runs = 0,
            extrasType = finalExtrasType,
            extrasRuns = exRuns,
            wicket = 1
        )
        selectedExtraType = ""
    }

    fun handleMatchCompletion(winnerName: String) {
        coroutineScope.launch {
            try {
                val res = apiService.endMatch(EndMatchRequest(matchId, winnerName))
                if (res.isSuccessful) {
                    val completedInfo = CompletedMatchInfo(
                        matchId = matchId.toIntOrNull() ?: 0,
                        teamA = teamAName,
                        teamB = teamBName,
                        venue = "Local Ground",
                        winner = winnerName
                    )
                    onMatchComplete(completedInfo)
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to end match")
            }
        }
    }

    if (isLoadingInnings) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF1E40AF))
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (selectedTab == "Score Update") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { undoLastBall() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF64748B))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Undo Ball", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (currentInnings == 1) showEndInningsDialog = true else showEndMatchDialog = true
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentInnings == 1) Color(0xFF3B82F6) else Color(0xFFEF4444)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(if (currentInnings == 1) Icons.Default.SkipNext else Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (currentInnings == 1) "End Innings" else "End Match", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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
                            Text("$teamAName vs $teamBName (ID: $matchId)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Innings $currentInnings", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                        IconButton(onClick = { 
                            isManualScore = false
                            refreshMatchData() 
                        }) { Icon(Icons.Default.Refresh, "Refresh", tint = Color.White) }
                    }
                    
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(40.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)).padding(4.dp)) {
                        listOf("Score Update", "Predictions", "Scorecard").forEach { tab ->
                            val isSelected = selectedTab == tab
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(18.dp)).clickable { selectedTab = tab }, contentAlignment = Alignment.Center) {
                                Text(tab, color = if (isSelected) Color(0xFF1E40AF) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Score Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(currentBattingTeam, color = Color(0xFF64748B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicTextField(
                                value = editableRuns,
                                onValueChange = { 
                                    editableRuns = it
                                    updateFromEditable()
                                },
                                textStyle = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF), textAlign = TextAlign.Center),
                                modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(minWidth = 40.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Text("/", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF))
                            BasicTextField(
                                value = editableWickets,
                                onValueChange = { 
                                    editableWickets = it
                                    updateFromEditable()
                                },
                                textStyle = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF), textAlign = TextAlign.Center),
                                modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(minWidth = 20.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicTextField(
                                value = editableOvers,
                                onValueChange = { 
                                    editableOvers = it
                                    updateFromEditable()
                                },
                                textStyle = TextStyle(color = Color(0xFF64748B), fontSize = 16.sp, textAlign = TextAlign.End),
                                modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(minWidth = 30.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            Text(" Overs", color = Color(0xFF64748B), fontSize = 16.sp)
                            Spacer(Modifier.width(16.dp))
                            Text("CRR: ${String.format(Locale.US, "%.2f", currentCRR)}", color = Color(0xFF1E40AF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        if (currentInnings == 2 && firstInningsScore != null) {
                            Spacer(Modifier.height(8.dp))
                            val target = firstInningsScore!!.runs + 1
                            val needed = target - totalRuns
                            Text("Target: $target (Need $needed runs)", color = Color(0xFF1E40AF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (selectedTab == "Score Update") {
                    // Partnership & Last Balls
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(modifier = Modifier.weight(0.7f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Partnership", fontSize = 11.sp, color = Color.Gray)
                                Text("${partnershipData.runs} (${partnershipData.balls})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        Card(modifier = Modifier.weight(1.3f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Last 6 Balls", fontSize = 11.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    lastSixBalls.takeLast(6).forEach { ball ->
                                        val bLower = ball.lowercase()
                                        Surface(
                                            modifier = Modifier.size(22.dp),
                                            shape = CircleShape,
                                            color = when {
                                                bLower == "w" || bLower == "wicket" -> Color(0xFFEF4444)
                                                ball == "4" || ball == "6" -> Color(0xFF10B981)
                                                bLower == "wd" || bLower == "nb" || bLower == "wide" || bLower == "no ball" || 
                                                bLower.matches(Regex("^[0-9]+w(d)?$")) || bLower.matches(Regex("^[0-9]+nb$")) ||
                                                bLower.matches(Regex("^n[0-9]+$")) || bLower == "nw" -> Color(0xFFF97316)
                                                else -> Color(0xFFF1F5F9)
                                            }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                val bText = when {
                                                    bLower == "wicket" || bLower == "w" -> "W"
                                                    bLower == "wide" || bLower == "wd" || bLower == "1w" -> "Wd"
                                                    bLower == "no ball" || bLower == "nb" || bLower == "1nb" || bLower == "n1" -> "Nb"
                                                    bLower.matches(Regex("^[0-9]+w(d)?$")) -> {
                                                        val totalRuns = bLower.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1
                                                        if (totalRuns > 1) "Wd+${totalRuns - 1}" else "Wd"
                                                    }
                                                    bLower.matches(Regex("^n[0-9]+$")) || bLower.matches(Regex("^[0-9]+nb$")) -> {
                                                        val totalRuns = bLower.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1
                                                        if (totalRuns > 1) "Nb+${totalRuns - 1}" else "Nb"
                                                    }
                                                    bLower == "nw" -> "Nw"
                                                    else -> ball
                                                }
                                                Text(bText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if(bText != "0" && bText != "1" && bText != "2" && bText != "3") Color.White else Color(0xFF64748B))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Batsmen Section
                    ScoringSectionCard(title = "Batsmen", actionIcon = Icons.AutoMirrored.Filled.CompareArrows, onActionClick = { 
                        coroutineScope.launch {
                            apiService.swapStrikers(GenericMatchRequest(matchId))
                            refreshMatchData()
                        }
                    }) {
                        if (striker.isEmpty() && nonStriker.isEmpty() && battingPlayers.isEmpty()) {
                            Text("Loading players...", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(12.dp))
                        } else {
                            val sStat = batsmenStats.find { it.batsman == striker } ?: BatsmanStat(batsman = striker, runs = strikerRuns, balls = strikerBalls, fours = 0, sixes = 0, strikeRate = 0.0)
                            val nsStat = batsmenStats.find { it.batsman == nonStriker } ?: BatsmanStat(batsman = nonStriker, runs = nonStrikerRuns, balls = nonStrikerBalls, fours = 0, sixes = 0, strikeRate = 0.0)
                            
                            BatsmanRowItem(sStat.batsman?.ifEmpty { "Select Striker" } ?: "Select Striker", sStat.runs, sStat.balls, sStat.fours, sStat.sixes, true, onClick = { showSelectStrikerDialog = true })
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                            BatsmanRowItem(nsStat.batsman?.ifEmpty { "Select Non-Striker" } ?: "Select Non-Striker", nsStat.runs, nsStat.balls, nsStat.fours, nsStat.sixes, false, onClick = { showSelectNonStrikerDialog = true })
                        }
                    }

                    // Bowler Section
                    ScoringSectionCard(title = "Bowler", actionIcon = Icons.Outlined.Sync, onActionClick = { showChangeBowlerDialog = true }) {
                        val bowler = bowlersStats.find { it.bowler == currentBowlerName }
                        if (currentBowlerName.isEmpty() && bowlingPlayers.isEmpty()) {
                            Text("Loading players...", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(12.dp))
                        } else {
                            val bName = currentBowlerName.ifEmpty { "Select Bowler" }
                            val bOvers = bowler?.overs ?: currentBowlerOvers
                            val bRuns = bowler?.runs ?: currentBowlerRuns
                            val bWickets = bowler?.wickets ?: currentBowlerWickets
                            
                            BowlerRowItem(bName, bOvers, bRuns, bWickets, String.format(Locale.US, "%.2f", bowler?.economy ?: 0.0), onClick = { showChangeBowlerDialog = true })
                        }
                    }

                    // Input Section
                    ScoringSectionCard("Input Runs", Icons.Default.Add, {}) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("0", "1", "2", "3", "4", "6").forEach { runValue ->
                                    RunCircleButton(runValue) {
                                        val r = runValue.toInt()
                                        when (selectedExtraType) {
                                            "wide" -> submitBall(runs = r, extrasType = "wide", extrasRuns = r + 1)
                                            "no ball" -> submitBall(runs = r, extrasType = "no_ball", extrasRuns = 1)
                                            "bye" -> submitBall(runs = r, extrasType = "bye", extrasRuns = r)
                                            "legbye" -> submitBall(runs = r, extrasType = "legbye", extrasRuns = r)
                                            else -> submitBall(runs = r)
                                        }
                                        selectedExtraType = ""
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionPillButton(text = if (selectedExtraType == "wide") "WIDE [X]" else "Wide", color = Color(0xFFF97316), modifier = Modifier.weight(1f)) {
                                    selectedExtraType = if (selectedExtraType == "wide") "" else "wide"
                                }
                                ActionPillButton(text = if (selectedExtraType == "no ball") "NO BALL [X]" else "No Ball", color = Color(0xFFF97316), modifier = Modifier.weight(1f)) {
                                    selectedExtraType = if (selectedExtraType == "no ball") "" else "no ball"
                                }
                                ActionPillButton("Wicket", Color(0xFFEF4444), Modifier.weight(1f)) {
                                    showWhoOutDialog = true
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionPillButton(text = if (selectedExtraType == "bye") "BYE [X]" else "Bye", color = Color(0xFF64748B), modifier = Modifier.weight(1f)) {
                                    selectedExtraType = if (selectedExtraType == "bye") "" else "bye"
                                }
                                ActionPillButton(text = if (selectedExtraType == "legbye") "LEGBYE [X]" else "Leg-Bye", color = Color(0xFF64748B), modifier = Modifier.weight(1f)) {
                                    selectedExtraType = if (selectedExtraType == "legbye") "" else "legbye"
                                }
                                ActionPillButton(text = "Penalty", color = Color(0xFF1E293B), modifier = Modifier.weight(1f)) {
                                    submitBall(runs = 5, extrasType = "penalty", extrasRuns = 5)
                                }
                            }
                        }
                    }
                } else if (selectedTab == "Predictions") {
                    val currentMatchData = LiveMatchData(
                        matchId = matchId.toIntOrNull() ?: 0,
                        teamA = teamAName,
                        teamB = teamBName,
                        venue = "Local Ground",
                        runs = totalRuns,
                        wickets = totalWickets,
                        overs = (totalBalls / 6).toDouble() + (totalBalls % 6) / 10.0,
                        crr = currentCRR,
                        striker = striker,
                        strikerRuns = strikerRuns,
                        nonStriker = nonStriker,
                        nonStrikerRuns = nonStrikerRuns
                    )
                    PredictionsContentIntegrated(match = currentMatchData, apiPredictions = apiPredictions)
                } else if (selectedTab == "Scorecard") {
                    if (scorecardData != null) {
                        ScorecardView(scorecardData!!, teamAName, teamBName)
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF1E40AF))
                                Spacer(Modifier.height(16.dp))
                                Text("Fetching perfect scorecard for ID: $matchId...", color = Color.Gray)
                                Button(
                                    onClick = { refreshMatchData() },
                                    modifier = Modifier.padding(top = 16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showSelectStrikerDialog) {
        val availableNames = batsmenFromApi.map { it.name }
        SelectPlayerDialog("Select Striker", availableNames.filter { it != nonStriker }, { showSelectStrikerDialog = false }) { striker = it; showSelectStrikerDialog = false }
    }
    if (showSelectNonStrikerDialog) {
        val availableNames = batsmenFromApi.map { it.name }
        SelectPlayerDialog("Select Non-Striker", availableNames.filter { it != striker }, { showSelectNonStrikerDialog = false }) { nonStriker = it; showSelectNonStrikerDialog = false }
    }
    
    if (showChangeBowlerDialog || showOverCompleteDialog) {
        val safeApiBowlers = bowlersFromApi.filterNotNull()
        val safeLocalBowlers = bowlingPlayers.filterNotNull().filter { it.isNotEmpty() }

        AlertDialog(
            onDismissRequest = { 
                showChangeBowlerDialog = false
                showOverCompleteDialog = false 
            },
            title = { Text("Select Bowler") },
            text = {
                if (safeApiBowlers.isEmpty() && safeLocalBowlers.isEmpty()) {
                    Text("No bowlers found in the list.", color = Color.Red, modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                        if (safeApiBowlers.isNotEmpty()) {
                            items(safeApiBowlers) { player ->
                                val pName = player.name ?: "Unknown"
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            updateBowler(pName)
                                            showChangeBowlerDialog = false
                                            showOverCompleteDialog = false
                                        }
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(pName, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            items(safeLocalBowlers) { playerName ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            updateBowler(playerName)
                                            showChangeBowlerDialog = false
                                            showOverCompleteDialog = false
                                        }
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(playerName, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showChangeBowlerDialog = false
                    showOverCompleteDialog = false 
                }) { Text("Cancel") }
            }
        )
    }
    
    if (showWhoOutDialog) {
        WhoGotOutDialog(striker, nonStriker, { showWhoOutDialog = false }, { finalizeWicket(it) })
    }
    if (showSelectNextBatsmanDialog) {
        val availableNames = batsmenFromApi.map { it.name }
        val available = availableNames.filter { it !in dismissedPlayers && it != striker && it != nonStriker }
        SelectPlayerDialog("Select Next Batsman", available, { showSelectNextBatsmanDialog = false }) { if (outPosition == "striker") striker = it else nonStriker = it; showSelectNextBatsmanDialog = false }
    }
    
    if (showEndInningsDialog) {
        AlertDialog(
            onDismissRequest = { showEndInningsDialog = false },
            title = { Text("End Innings") },
            text = { Text("Are you sure you want to end this innings and start the next one?") },
            confirmButton = {
                TextButton(onClick = {
                    currentInnings = 2
                    striker = ""; nonStriker = ""; currentBowlerName = ""
                    dismissedPlayers.clear()
                    isManualScore = false
                    refreshMatchData()
                    showEndInningsDialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showEndInningsDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEndMatchDialog) {
        AlertDialog(
            onDismissRequest = { showEndMatchDialog = false },
            title = { Text("End Match") },
            text = { 
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Select the winner of this match:", modifier = Modifier.padding(bottom = 12.dp))
                    Button(
                        onClick = { 
                            handleMatchCompletion(teamAName)
                            showEndMatchDialog = false 
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF))
                    ) { Text(teamAName) }
                    
                    Button(
                        onClick = { 
                            handleMatchCompletion(teamBName)
                            showEndMatchDialog = false 
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) { Text(teamBName) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEndMatchDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SelectPlayerDialog(title: String, players: List<String>, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text(title) }, 
        text = {
            if (players.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Fetching players...", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(players) { player ->
                        Surface(modifier = Modifier.fillMaxWidth().clickable { onSelect(player) }.padding(vertical = 4.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
                            Text(player, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }, 
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
            Text(name, fontWeight = if (isOnStrike) FontWeight.Bold else FontWeight.Normal, color = if(isOnStrike) Color(0xFF1E40AF) else Color(0xFF1E293B))
        }
        Text("$runs($balls)", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}

@Composable
fun BowlerRowItem(name: String, overs: String, runs: Int, wickets: Int, econ: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
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
fun ScorecardView(data: ScorecardResponse, teamA: String, teamB: String) {
    var scorecardTab by remember { mutableStateOf(teamA) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)).padding(4.dp)) {
                listOf(teamA, teamB).forEach { team ->
                    val isSelected = scorecardTab.equals(team, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { scorecardTab = team }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = team,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF1E40AF) else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            val teamData = if (scorecardTab.equals(teamA, ignoreCase = true)) data.teamA else data.teamB
            
            if (teamData == null) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Team statistics not available")
                }
            } else {
            
            Text("Batting", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(8.dp)) {
                Text("Batsman", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                Text("R", modifier = Modifier.width(30.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                Text("B", modifier = Modifier.width(30.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                Text("4s", modifier = Modifier.width(25.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                Text("6s", modifier = Modifier.width(25.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
            }
            
                teamData.batting.forEach { b ->
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(b.playerName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Text(b.status, fontSize = 10.sp, color = Color.Gray)
                        }
                        Text(b.runs.toString(), modifier = Modifier.width(30.dp), textAlign = TextAlign.End, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(b.balls.toString(), modifier = Modifier.width(30.dp), textAlign = TextAlign.End, fontSize = 13.sp)
                        Text(b.fours.toString(), modifier = Modifier.width(25.dp), textAlign = TextAlign.End, fontSize = 13.sp)
                        Text(b.sixes.toString(), modifier = Modifier.width(25.dp), textAlign = TextAlign.End, fontSize = 13.sp)
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }

            Spacer(Modifier.height(24.dp))

            Text("Bowling", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))

            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(8.dp)) {
                Text("Bowler", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                Text("O", modifier = Modifier.width(35.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                Text("M", modifier = Modifier.width(25.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                Text("R", modifier = Modifier.width(30.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                Text("W", modifier = Modifier.width(30.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
            }
            
            if (teamData.bowling.isEmpty()) {
                Text("No bowling stats yet", modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Gray, fontSize = 12.sp)
            }

                teamData.bowling.forEach { bo ->
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(bo.playerName, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text(bo.overs, modifier = Modifier.width(35.dp), textAlign = TextAlign.End, fontSize = 13.sp)
                        Text(bo.maidens.toString(), modifier = Modifier.width(25.dp), textAlign = TextAlign.End, fontSize = 13.sp)
                        Text(bo.runs.toString(), modifier = Modifier.width(30.dp), textAlign = TextAlign.End, fontSize = 13.sp)
                        Text(bo.wickets.toString(), modifier = Modifier.width(30.dp), textAlign = TextAlign.End, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

@Composable
fun WhoGotOutDialog(striker: String, nonStriker: String, onDismiss: () -> Unit, onConfirmOut: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Wicket! Who is out?") }, 
        text = {
            Column {
                Button(onClick = { onConfirmOut(striker) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) { Text(striker) }
                Button(onClick = { onConfirmOut(nonStriker) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) { Text(nonStriker) }
            }
        }, 
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
