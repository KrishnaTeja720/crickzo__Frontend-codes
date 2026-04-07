package com.simats.crickzo

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Home

import androidx.compose.material.icons.outlined.Person
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
import com.simats.crickzo.ui.theme.GrayText
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch


@Composable
fun HomeScreen(
    userName: String,
    userEmail: String,
    userId: Int,
    onUpdateProfile: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("Home") }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // States for forgot password flow
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var resetOtp by remember { mutableStateOf("") }
    
    // States for match creation flow
    val currentMatchId = remember { mutableIntStateOf(0) }
    val teamAName = remember { mutableStateOf("") }
    val teamBName = remember { mutableStateOf("") }
    val tossWinnerState = remember { mutableStateOf("") }
    val tossDecisionState = remember { mutableStateOf("") }
    
    // State for MyMatches tab navigation
    var myMatchesInitialTab by remember { mutableStateOf("Live") }
    
    // API Data Lists
    val liveMatchesList = remember { mutableStateListOf<LiveMatchData>() }
    val createdMatches = remember { mutableStateListOf<CreatedMatch>() }
    val completedMatchesList = remember { mutableStateListOf<CompletedMatchInfo>() }
    
    var selectedMatchForScoring by remember { mutableStateOf<CreatedMatch?>(null) }
    var selectedLiveMatch by remember { mutableStateOf<LiveMatchData?>(null) }
    var selectedCompletedMatch by remember { mutableStateOf<CompletedMatchInfo?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshMatches() {
        if (isRefreshing || userId <= 0) return
        isRefreshing = true
        coroutineScope.launch {
            try {
                Log.d("HomeScreen", "Refreshing matches for userId: $userId")
                
                // 1. Fetch Live Matches
                val liveResponse = RetrofitClient.apiService.getLiveMatches(userId)
                val upcomingResponse = RetrofitClient.apiService.getUpcomingMatches(userId)
                
                if (liveResponse.isSuccessful || upcomingResponse.isSuccessful) {
                    val liveMatches = liveResponse.body() ?: emptyList()
                    val upcomingMatches = upcomingResponse.body() ?: emptyList()

                    Log.d("HomeScreen", "Fetched ${liveMatches.size} live and ${upcomingMatches.size} upcoming matches")

                    liveMatchesList.clear()
                    liveMatchesList.addAll(liveMatches)
                    
                    val newCreatedMatches = mutableListOf<CreatedMatch>()
                    
                    // Process Live Matches
                    liveMatches.forEach { liveMatch ->
                        newCreatedMatches.add(CreatedMatch(
                            id = liveMatch.matchId.toString(),
                            teamA = liveMatch.teamA ?: "Team A",
                            teamB = liveMatch.teamB ?: "Team B",
                            teamAPlayers = emptyList(), // Load on demand instead of here
                            teamBPlayers = emptyList(),
                            status = "LIVE",
                            location = liveMatch.venue ?: "Local Ground",
                            striker = "",
                            nonStriker = "",
                            bowler = "",
                            tossWinner = liveMatch.tossWinner ?: "",
                            tossDecision = liveMatch.tossDecision ?: "",
                            runs = liveMatch.runs,
                            wickets = liveMatch.wickets,
                            overs = liveMatch.overs.toFloat(),
                            crr = liveMatch.crr.toFloat(),
                            type = if (liveMatch.format?.all { it.isDigit() } == true) "${liveMatch.format} Overs" else (liveMatch.format ?: "T20"),
                            currentInnings = liveMatch.currentInnings
                        ))
                    }

                    // Process Upcoming Matches
                    upcomingMatches.forEach { upcomingMatch ->
                        newCreatedMatches.add(CreatedMatch(
                            id = upcomingMatch.matchId.toString(),
                            teamA = upcomingMatch.teamA ?: "Team A",
                            teamB = upcomingMatch.teamB ?: "Team B",
                            teamAPlayers = emptyList(),
                            teamBPlayers = emptyList(),
                            status = "UPCOMING",
                            location = upcomingMatch.venue ?: "Local Ground",
                            striker = "",
                            nonStriker = "",
                            bowler = "",
                            tossWinner = upcomingMatch.tossWinner ?: "",
                            tossDecision = upcomingMatch.tossDecision ?: "",
                            runs = 0,
                            wickets = 0,
                            overs = 0f,
                            crr = 0f,
                            type = if (upcomingMatch.format?.all { it.isDigit() } == true) "${upcomingMatch.format} Overs" else (upcomingMatch.format ?: "T20"),
                            currentInnings = 1
                        ))
                    }

                    createdMatches.clear()
                    createdMatches.addAll(newCreatedMatches)
                } else {
                    Log.e("HomeScreen", "Matches fetch failed: Live=${liveResponse.code()}, Upcoming=${upcomingResponse.code()}")
                    errorMessage = "Failed to load matches. Please try again."
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Server Error: ${liveResponse.code()}")
                    }
                }
                
                // 2. Fetch Completed Matches
                val compResponse = RetrofitClient.apiService.getCompletedMatches(userId)
                if (compResponse.isSuccessful) {
                    completedMatchesList.clear()
                    compResponse.body()?.let { completedMatchesList.addAll(it) }
                } else {
                    Log.e("HomeScreen", "Completed matches fetch failed: ${compResponse.code()}")
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error in refreshMatches", e)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Network Error: Check Server IP")
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(userId) {
        if (userId > 0) {
            refreshMatches()
        }
    }

    val noBottomBarScreens = remember {
        setOf(
            "ForgotPassword", "VerifyOtp", "ResetPassword", "CreateMatch", "AddPlayers",
            "Scoring", "MyMatches", "LiveMatches", "CompletedMatches", "Predictions", "CompletedMatchDetail"
        )
    }
    val hideBottomBar = currentScreen in noBottomBarScreens

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!hideBottomBar) {
                CrickzoBottomBar(
                    selectedItem = if (currentScreen == "AccountSettings") "Profile" else currentScreen,
                    onItemSelected = { screen ->
                        if (screen == "Add Match") {
                            currentScreen = "CreateMatch"
                        } else if (screen == "Home") {
                            refreshMatches()
                            currentScreen = "Home"
                        } else {
                            currentScreen = screen
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (hideBottomBar) PaddingValues(0.dp) else innerPadding)
        ) {
            if (isRefreshing && liveMatchesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1E40AF))
                }
            }
            
            when (currentScreen) {
                "Home" -> HomeContent(
                    userName = userName,
                    userId = userId,
                    liveMatches = liveMatchesList,
                    userCreatedMatchesCount = createdMatches.size,
                    onCreateMatchClick = { currentScreen = "CreateMatch" },
                    onMyMatchesClick = { 
                        myMatchesInitialTab = "Live"
                        currentScreen = "MyMatches" 
                    },
                    onLiveMatchesClick = { 
                        myMatchesInitialTab = "Live"
                        currentScreen = "MyMatches" 
                    },
                    onCompletedMatchesClick = { currentScreen = "CompletedMatches" },
                    onMenuAction = { action ->
                        when (action) {
                            "Profile" -> currentScreen = "Profile"
                            "AccountSettings" -> currentScreen = "AccountSettings"
                            "Logout" -> onLogout()
                        }
                    }
                )
                "ForgotPassword" -> {
                    ForgotPasswordScreen(
                        onBackToLogin = { currentScreen = "AccountSettings" },
                        onCodeSent = { email ->
                            forgotPasswordEmail = email
                            currentScreen = "VerifyOtp"
                        }
                    )
                }
                "VerifyOtp" -> {
                    VerifyOtpScreen(
                        email = forgotPasswordEmail,
                        subtitle = "Password Recovery",
                        onBack = { currentScreen = "ForgotPassword" },
                        onVerifySuccess = { otp ->
                            resetOtp = otp
                            currentScreen = "ResetPassword"
                        }
                    )
                }
                "ResetPassword" -> {
                    ResetPasswordScreen(
                        email = forgotPasswordEmail,
                        otp = resetOtp,
                        onBack = { currentScreen = "VerifyOtp" },
                        onResetSuccess = {
                            currentScreen = "AccountSettings"
                        }
                    )
                }
                "CompletedMatches" -> {
                    CompletedMatchesScreen(
                        userId = userId,
                        matches = completedMatchesList,
                        onBack = { 
                            refreshMatches()
                            currentScreen = "Home" 
                        }
                    )
                }
                "CompletedMatchDetail" -> {
                    selectedCompletedMatch?.let { match ->
                        CompletedMatchDetail(
                            userId = userId,
                            match = match,
                            onBack = { currentScreen = "MyMatches" }
                        )
                    }
                }
                "LiveMatches" -> {
                    LiveMatchesScreen(
                        userId = userId,
                        onBack = { 
                            refreshMatches()
                            currentScreen = "Home" 
                        },
                        onUpdateScore = { match ->
                            selectedMatchForScoring = null
                            selectedLiveMatch = match
                            currentScreen = "Scoring"
                        },
                        onViewPredictions = { match ->
                            selectedLiveMatch = match
                            currentScreen = "Predictions"
                        }
                    )
                }
                "Predictions" -> {
                    val match = selectedLiveMatch
                    PredictionsScreen(
                        match = match,
                        teamA = match?.teamA ?: "Team A",
                        teamB = match?.teamB ?: "Team B",
                        onBack = { currentScreen = "LiveMatches" }
                    )
                }
                "MyMatches" -> {
                    MyMatchesScreen(
                        userId = userId,
                        matches = createdMatches,
                        completedMatches = completedMatchesList,
                        initialTab = myMatchesInitialTab,
                        onBack = { 
                            refreshMatches()
                            currentScreen = "Home" 
                        },
                        onScoreMatch = { match ->
                            selectedMatchForScoring = match
                            selectedLiveMatch = null
                            currentScreen = "Scoring"
                        },
                        onCompletedMatchClick = { match ->
                            selectedCompletedMatch = match
                            currentScreen = "CompletedMatchDetail"
                        },
                        onAddMatch = { currentScreen = "CreateMatch" }
                    )
                }
                "CreateMatch" -> {
                    CreateMatchScreen(
                        onBack = { currentScreen = "Home" },
                        onContinue = { id, a, b, tw, td -> 
                            currentMatchId.intValue = id
                            teamAName.value = a
                            teamBName.value = b
                            tossWinnerState.value = tw
                            tossDecisionState.value = td
                            currentScreen = "AddPlayers" 
                        }
                    )
                }
                "AddPlayers" -> {
                    AddPlayersScreen(
                        matchId = currentMatchId.intValue,
                        teamAName = teamAName.value,
                        teamBName = teamBName.value,
                        tossWinner = tossWinnerState.value,
                        tossDecision = tossDecisionState.value,
                        onBack = { currentScreen = "CreateMatch" },
                        onStartMatch = { playersA, playersB, striker, nonStriker, bowler ->
                            val newMatch = CreatedMatch(
                                id = currentMatchId.intValue.toString(),
                                teamA = teamAName.value,
                                teamB = teamBName.value,
                                teamAPlayers = playersA,
                                teamBPlayers = playersB,
                                striker = striker,
                                nonStriker = nonStriker,
                                bowler = bowler,
                                status = "UPCOMING",
                                location = "Local Ground",
                                tossWinner = tossWinnerState.value,
                                tossDecision = tossDecisionState.value
                            )
                            createdMatches.add(newMatch)
                            selectedMatchForScoring = newMatch
                            selectedLiveMatch = null
                            currentScreen = "Scoring"
                        }
                    )
                }
                "Scoring" -> {
                    val scoringMatch = selectedMatchForScoring
                    val scoringLiveMatch = selectedLiveMatch
                    
                    val finalTeamAPlayers = remember(scoringMatch, scoringLiveMatch) {
                        mutableStateListOf<String>().apply {
                            if (scoringMatch != null) {
                                addAll(scoringMatch.teamAPlayers)
                            }
                        }
                    }
                    val finalTeamBPlayers = remember(scoringMatch, scoringLiveMatch) {
                        mutableStateListOf<String>().apply {
                            if (scoringMatch != null) {
                                addAll(scoringMatch.teamBPlayers)
                            }
                        }
                    }

                    LaunchedEffect(scoringLiveMatch, scoringMatch) {
                        val mId = scoringMatch?.id ?: scoringLiveMatch?.matchId?.toString()
                        if (mId != null && finalTeamAPlayers.isEmpty() && finalTeamBPlayers.isEmpty()) {
                            try {
                                val res = RetrofitClient.apiService.getMatchPlayers(mId)
                                if (res.isSuccessful) {
                                    res.body()?.let { responseBody ->
                                        finalTeamAPlayers.clear()
                                        finalTeamBPlayers.clear()
                                        responseBody.forEach { p ->
                                            if (p.teamName?.contains("B", ignoreCase = true) == true || p.teamName == (scoringMatch?.teamB ?: scoringLiveMatch?.teamB)) {
                                                finalTeamBPlayers.add(p.playerName)
                                            } else {
                                                finalTeamAPlayers.add(p.playerName)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    ScoringScreen(
                        matchId = scoringMatch?.id ?: scoringLiveMatch?.matchId?.toString() ?: "0",
                        userId = userId,
                        teamAName = scoringMatch?.teamA ?: scoringLiveMatch?.teamA ?: "Team A",
                        teamBName = scoringMatch?.teamB ?: scoringLiveMatch?.teamB ?: "Team B",
                        teamAPlayers = finalTeamAPlayers,
                        teamBPlayers = finalTeamBPlayers,
                        initialStriker = scoringMatch?.striker ?: "",
                        initialNonStriker = scoringMatch?.nonStriker ?: "",
                        initialBowler = scoringMatch?.bowler ?: "",
                        tossWinner = scoringMatch?.tossWinner ?: scoringLiveMatch?.tossWinner ?: "",
                        tossDecision = scoringMatch?.tossDecision ?: scoringLiveMatch?.tossDecision ?: "",
                        initialInnings = scoringMatch?.currentInnings ?: scoringLiveMatch?.currentInnings ?: 1,
                        onBack = { 
                            refreshMatches()
                            currentScreen = "Home" 
                        },
                        onMatchComplete = { completedInfo ->
                            completedMatchesList.add(completedInfo)
                            currentScreen = "Home"
                        }
                    )
                }
                "Profile" -> {
                    val context = LocalContext.current
                    ProfileScreen(
                        userName = userName,
                        userEmail = userEmail,
                        onAccountSettingsClick = { currentScreen = "AccountSettings" },
                        onPremiumClick = {
                            context.startActivity(Intent(context, SubscriptionActivity::class.java))
                        },
                        onLogout = onLogout
                    )
                }
                "AccountSettings" -> {

                    AccountSettingsScreen(
                        userId = userId,
                        initialName = userName,
                        initialEmail = userEmail,
                        onBack = { currentScreen = "Home" },
                        onSave = onUpdateProfile,
                        onLogout = onLogout,
                        onForgotPasswordClick = { currentScreen = "ForgotPassword" }
                    )
                }
            }
        }
    }
}

@Composable
fun CrickzoBottomBar(selectedItem: String, onItemSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(if (selectedItem == "Home") Icons.Filled.Home else Icons.Outlined.Home, "Home") },
            label = { Text("Home") },
            selected = selectedItem == "Home",
            onClick = { onItemSelected("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF1E40AF),
                unselectedIconColor = Color.Gray,
                selectedTextColor = Color(0xFF1E40AF),
                indicatorColor = Color(0xFFEFF6FF)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AddCircle, "Add Match") },
            label = { Text("Add Match") },
            selected = false,
            onClick = { onItemSelected("Add Match") },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color(0xFF1E40AF),
                unselectedTextColor = Color(0xFF1E40AF)
            )
        )
        NavigationBarItem(
            icon = { Icon(if (selectedItem == "Profile") Icons.Filled.Person else Icons.Outlined.Person, "Profile") },
            label = { Text("Profile") },
            selected = selectedItem == "Profile",
            onClick = { onItemSelected("Profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF1E40AF),
                unselectedIconColor = Color.Gray,
                selectedTextColor = Color(0xFF1E40AF),
                indicatorColor = Color(0xFFEFF6FF)
            )
        )
    }
}

@Composable
fun HomeContent(
    userName: String,
    userId: Int, // Added userId parameter
    liveMatches: List<LiveMatchData>,
    userCreatedMatchesCount: Int,
    onCreateMatchClick: () -> Unit,
    onMyMatchesClick: () -> Unit,
    onLiveMatchesClick: () -> Unit,
    onCompletedMatchesClick: () -> Unit,
    onMenuAction: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E40AF), Color(0xFF2563EB))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Welcome back,", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Text(userName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { onMenuAction("AccountSettings") },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color.White)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Live Matches Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Matches", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text(
                    "View All",
                    color = Color(0xFF3B82F6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onLiveMatchesClick() }
                )
            }

            if (liveMatches.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No live matches at the moment", color = GrayText, fontSize = 14.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    liveMatches.take(2).forEach { match ->
                        MatchMiniCard(match, Modifier.weight(1f))
                    }
                }
            }

            // Quick Actions
            Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    title = "New Match",
                    subtitle = "Start scoring",
                    icon = Icons.Default.Add,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f),
                    onClick = onCreateMatchClick
                )
                QuickActionCard(
                    title = "My Matches",
                    subtitle = "$userCreatedMatchesCount matches (User ID: $userId)",
                    icon = Icons.Default.History,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = onMyMatchesClick
                )
            }

            // Completed Matches Section
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onCompletedMatchesClick() },
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFF1F5F9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF64748B))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Completed Matches", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text("View match history and results", fontSize = 12.sp, color = GrayText)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun MatchMiniCard(match: LiveMatchData, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("LIVE", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("${match.teamA} vs ${match.teamB}", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${match.runs}/${match.wickets}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF))
            Text("${match.overs} Overs", fontSize = 10.sp, color = GrayText)
        }
    }
}

@Composable
fun QuickActionCard(title: String, subtitle: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color)
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Text(subtitle, fontSize = 12.sp, color = GrayText)
        }
    }
}
