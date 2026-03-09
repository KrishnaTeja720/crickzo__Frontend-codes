package com.simats.crickzo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.crickzo.ui.theme.GrayText

@Composable
fun HomeScreen(
    userName: String,
    userEmail: String,
    onUpdateProfile: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("Home") }
    val forgotPasswordEmail = remember { mutableStateOf("") }
    
    // States for match creation flow
    val currentMatchId = remember { mutableIntStateOf(0) }
    val teamAName = remember { mutableStateOf("") }
    val teamBName = remember { mutableStateOf("") }
    val teamAPlayers = remember { mutableStateOf(listOf<String>()) }
    val teamBPlayers = remember { mutableStateOf(listOf<String>()) }
    
    // User created matches list
    val createdMatches = remember { mutableStateListOf<CreatedMatch>() }
    val completedMatchesList = remember { mutableStateListOf<CompletedMatchInfo>() }
    val selectedMatchForScoring = remember { mutableStateOf<CreatedMatch?>(null) }
    
    // For live matches navigation
    val selectedLiveMatch = remember { mutableStateOf<LiveMatchData?>(null) }

    Scaffold(
        bottomBar = {
            if (currentScreen != "ForgotPassword" && currentScreen != "VerifyOtp" && 
                currentScreen != "CreateMatch" && currentScreen != "AddPlayers" &&
                currentScreen != "Scoring" && currentScreen != "MyMatches" &&
                currentScreen != "LiveMatches" && currentScreen != "CompletedMatches" &&
                currentScreen != "Predictions") {
                CrickzoBottomBar(
                    selectedItem = if (currentScreen == "AccountSettings") "Profile" else currentScreen,
                    onItemSelected = { 
                        if (it == "Add Match") {
                            currentScreen = "CreateMatch"
                        } else {
                            currentScreen = it
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (currentScreen == "ForgotPassword" || currentScreen == "VerifyOtp" || 
                    currentScreen == "CreateMatch" || currentScreen == "AddPlayers" ||
                    currentScreen == "Scoring" || currentScreen == "MyMatches" ||
                    currentScreen == "LiveMatches" || currentScreen == "CompletedMatches" ||
                    currentScreen == "Predictions") PaddingValues(0.dp) else innerPadding)
        ) {
            // Main content
            when (currentScreen) {
                "Home" -> HomeContent(
                    userName = userName,
                    userCreatedMatchesCount = createdMatches.size,
                    onCreateMatchClick = { currentScreen = "CreateMatch" },
                    onMyMatchesClick = { currentScreen = "MyMatches" },
                    onLiveMatchesClick = { currentScreen = "LiveMatches" },
                    onCompletedMatchesClick = { currentScreen = "CompletedMatches" },
                    onMenuAction = { action ->
                        when (action) {
                            "Profile" -> currentScreen = "Profile"
                            "AccountSettings" -> currentScreen = "AccountSettings"
                            "Logout" -> onLogout()
                        }
                    }
                )
                "CompletedMatches" -> {
                    CompletedMatchesScreen(
                        matches = completedMatchesList,
                        onBack = { currentScreen = "Home" }
                    )
                }
                "LiveMatches" -> {
                    LiveMatchesScreen(
                        onBack = { currentScreen = "Home" },
                        onUpdateScore = { match ->
                            selectedMatchForScoring.value = null
                            selectedLiveMatch.value = match
                            currentScreen = "Scoring"
                        }
                    )
                }
                "Predictions" -> {
                    val match = selectedLiveMatch.value
                    PredictionsScreen(
                        match = match,
                        teamA = match?.team1 ?: "Mumbai Indians",
                        teamB = match?.team2 ?: "Chennai Super Kings",
                        onBack = { currentScreen = "LiveMatches" }
                    )
                }
                "MyMatches" -> {
                    MyMatchesScreen(
                        matches = createdMatches,
                        completedMatches = completedMatchesList,
                        onBack = { currentScreen = "Home" },
                        onScoreMatch = { match ->
                            selectedMatchForScoring.value = match
                            selectedLiveMatch.value = null
                            currentScreen = "Scoring"
                        },
                        onAddMatch = { currentScreen = "CreateMatch" }
                    )
                }
                "CreateMatch" -> {
                    CreateMatchScreen(
                        onBack = { currentScreen = "Home" },
                        onContinue = { id, a, b -> 
                            currentMatchId.intValue = id
                            teamAName.value = a
                            teamBName.value = b
                            currentScreen = "AddPlayers" 
                        }
                    )
                }
                "AddPlayers" -> {
                    AddPlayersScreen(
                        matchId = currentMatchId.intValue,
                        teamAName = teamAName.value,
                        teamBName = teamBName.value,
                        onBack = { currentScreen = "CreateMatch" },
                        onStartMatch = { playersA, playersB, striker, nonStriker, bowler ->
                            teamAPlayers.value = playersA
                            teamBPlayers.value = playersB
                            
                            val newMatch = CreatedMatch(
                                id = currentMatchId.intValue.toString(),
                                teamA = teamAName.value,
                                teamB = teamBName.value,
                                teamAPlayers = playersA,
                                teamBPlayers = playersB,
                                striker = striker,
                                nonStriker = nonStriker,
                                bowler = bowler
                            )
                            createdMatches.add(newMatch)
                            selectedMatchForScoring.value = newMatch
                            selectedLiveMatch.value = null
                            currentScreen = "Scoring"
                        }
                    )
                }
                "Scoring" -> {
                    val match = selectedMatchForScoring.value
                    val liveMatch = selectedLiveMatch.value
                    
                    ScoringScreen(
                        matchId = match?.id ?: liveMatch?.matchId?.toString() ?: "0",
                        teamAName = match?.teamA ?: liveMatch?.teamA ?: "Team A",
                        teamBName = match?.teamB ?: liveMatch?.teamB ?: "Team B",
                        teamAPlayers = match?.teamAPlayers ?: listOf(),
                        teamBPlayers = match?.teamBPlayers ?: listOf(),
                        initialStriker = match?.striker ?: "",
                        initialNonStriker = match?.nonStriker ?: "",
                        initialBowler = match?.bowler ?: "",
                        onBack = { currentScreen = "Home" },
                        onMatchComplete = { completedInfo ->
                            completedMatchesList.add(completedInfo)
                            match?.let { createdMatches.remove(it) }
                        }
                    )
                }
                "Profile", "AccountSettings" -> {
                    ProfileScreen(
                        userName = userName,
                        userEmail = userEmail,
                        onAccountSettingsClick = { currentScreen = "AccountSettings" },
                        onLogout = onLogout
                    )
                }
                "ForgotPassword" -> {
                    ForgotPasswordScreen(
                        onBackToLogin = { currentScreen = "AccountSettings" },
                        onSendCode = { email ->
                            forgotPasswordEmail.value = email
                            currentScreen = "VerifyOtp"
                        }
                    )
                }
                "VerifyOtp" -> {
                    VerifyOtpScreen(
                        email = forgotPasswordEmail.value,
                        subtitle = "Recover Your Account",
                        onBack = { currentScreen = "ForgotPassword" },
                        onVerify = {
                            currentScreen = "AccountSettings"
                        }
                    )
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Coming Soon")
                    }
                }
            }

            // Overlay for Account Settings
            if (currentScreen == "AccountSettings") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    AccountSettingsScreen(
                        initialName = userName,
                        initialEmail = userEmail,
                        onBack = { currentScreen = "Profile" },
                        onSave = { newName, newEmail ->
                            onUpdateProfile(newName, newEmail)
                            currentScreen = "Profile"
                        },
                        onForgotPasswordClick = {
                            currentScreen = "ForgotPassword"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    userName: String, 
    userCreatedMatchesCount: Int,
    onCreateMatchClick: () -> Unit, 
    onMyMatchesClick: () -> Unit,
    onLiveMatchesClick: () -> Unit,
    onCompletedMatchesClick: () -> Unit,
    onMenuAction: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
    ) {
        // Top Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E40AF))
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (userName.length >= 2) userName.take(2).uppercase() else "CF",
                                    color = Color(0xFF1E40AF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = userName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cricket Fan",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .width(200.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Profile", color = Color(0xFF1E293B)) },
                                onClick = {
                                    showMenu = false
                                    onMenuAction("Profile")
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(20.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Account Settings", color = Color(0xFF1E293B)) },
                                onClick = {
                                    showMenu = false
                                    onMenuAction("AccountSettings")
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Settings, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(20.dp))
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            DropdownMenuItem(
                                text = { Text("Logout", color = Color(0xFFEF4444)) },
                                onClick = {
                                    showMenu = false
                                    onMenuAction("Logout")
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Live match notification banner
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onLiveMatchesClick() },
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RadioButtonChecked,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2 live matches in progress",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Button(
                            onClick = onLiveMatchesClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Watch", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-20).dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Create New Match Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreateMatchClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E40AF))
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Create New Match",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Score & track your own matches",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f).clickable { onLiveMatchesClick() },
                    title = "Live",
                    subtitle = "2 matches",
                    icon = Icons.Default.RadioButtonChecked,
                    iconColor = Color(0xFFEF4444),
                    badge = "2"
                )
                StatCard(
                    modifier = Modifier.weight(1f).clickable { onMyMatchesClick() },
                    title = "My Matches",
                    subtitle = "$userCreatedMatchesCount created",
                    icon = Icons.Default.Security,
                    iconColor = Color(0xFF1E40AF)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Completed Matches Card
            Card(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onCompletedMatchesClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color(0xFFDBEAFE)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Color(0xFF1E40AF),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Completed Matches",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Track finished match records",
                                color = GrayText,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = GrayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Live Now Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                TextButton(onClick = onLiveMatchesClick) {
                    Text(text = "View All", color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Match Cards
            MatchCard(
                modifier = Modifier.clickable { onLiveMatchesClick() },
                team1 = "Mumbai Indians",
                score1 = "142/3",
                overs1 = "(15.2)",
                team2 = "Chennai Super Kings",
                score2 = "--",
                overs2 = "",
                crr = "9.26",
                location = "Wankhede Stad"
            )

            Spacer(modifier = Modifier.height(16.dp))

            MatchCard(
                modifier = Modifier.clickable { onLiveMatchesClick() },
                team1 = "Royal Challengers",
                score1 = "185/6",
                overs1 = "(20.0)",
                team2 = "Kolkata Knight Riders",
                score2 = "98/2",
                overs2 = "(10.4)",
                crr = "9.19",
                rrr = "9.36",
                target = "186",
                location = "M. Chinnaswam"
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CrickzoBottomBar(selectedItem: String, onItemSelected: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "Home",
                icon = Icons.Outlined.Home,
                isSelected = selectedItem == "Home",
                onClick = { onItemSelected("Home") }
            )
            BottomNavItem(
                label = "Add Match",
                icon = Icons.Default.Add,
                isSelected = selectedItem == "Add Match",
                onClick = { onItemSelected("Add Match") }
            )
            BottomNavItem(
                label = "Profile",
                icon = Icons.Outlined.Person,
                isSelected = selectedItem == "Profile",
                onClick = { onItemSelected("Profile") }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) Color(0xFF1E40AF) else Color(0xFF94A3B8)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .background(contentColor, CircleShape)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    badge: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (badge != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp),
                    shape = CircleShape,
                    color = Color(0xFFEF4444)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = subtitle, color = GrayText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MatchCard(
    modifier: Modifier = Modifier,
    team1: String,
    score1: String,
    overs1: String,
    team2: String,
    score2: String,
    overs2: String,
    crr: String,
    rrr: String? = null,
    target: String? = null,
    location: String
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFFFE4E6),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(4.dp).background(Color(0xFFEF4444), CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LIVE", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "T20",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color(0xFF1E40AF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = GrayText)
                    Text(location, color = GrayText, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = team1, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = team2, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = score1, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = overs1, color = GrayText, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = score2, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = overs2, color = GrayText, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Text(text = "CRR: ", color = GrayText, fontSize = 12.sp)
                    Text(text = crr, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (rrr != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "RRR: ", color = GrayText, fontSize = 12.sp)
                        Text(text = rrr, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                if (target != null) {
                    Text(text = "Target: $target", color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
