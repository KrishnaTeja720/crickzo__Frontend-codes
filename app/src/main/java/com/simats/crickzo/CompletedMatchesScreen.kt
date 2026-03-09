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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun CompletedMatchesScreen(
    matches: List<CompletedMatchInfo> = listOf(),
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var completedMatches by remember { mutableStateOf(matches) }
    var isLoading by remember { mutableStateOf(matches.isEmpty()) }
    var selectedMatch by remember { mutableStateOf<CompletedMatchInfo?>(null) }

    val mockCompletedMatches = listOf(
        CompletedMatchInfo(
            matchId = 1,
            teamA = "Gujarat Titans",
            teamB = "Lucknow Super Giants",
            scoreA = "172/5 (20.0)",
            scoreB = "175/4 (19.3)",
            result = "Lucknow Super Giants won by 6 wickets",
            venue = "Narendra Modi Stadium",
            date = "Yesterday"
        ),
        CompletedMatchInfo(
            matchId = 2,
            teamA = "Mumbai Indians",
            teamB = "Chennai Super Kings",
            scoreA = "156/8 (20.0)",
            scoreB = "157/3 (18.1)",
            result = "Chennai Super Kings won by 7 wickets",
            venue = "Wankhede Stadium",
            date = "2 days ago"
        )
    )

    LaunchedEffect(Unit) {
        if (matches.isEmpty()) {
            coroutineScope.launch {
                try {
                    val response = RetrofitClient.apiService.getCompletedMatches()
                    if (response.isSuccessful) {
                        completedMatches = response.body() ?: mockCompletedMatches
                    } else {
                        completedMatches = mockCompletedMatches
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    completedMatches = mockCompletedMatches
                } finally {
                    isLoading = false
                }
            }
        }
    }

    if (selectedMatch == null) {
        CompletedMatchesList(
            matches = if (completedMatches.isEmpty() && !isLoading) mockCompletedMatches else completedMatches,
            onBack = onBack,
            onMatchClick = { selectedMatch = it }
        )
    } else {
        CompletedMatchDetail(
            match = selectedMatch!!,
            onBack = { selectedMatch = null }
        )
    }
}

@Composable
fun CompletedMatchesList(
    matches: List<CompletedMatchInfo>,
    onBack: () -> Unit,
    onMatchClick: (CompletedMatchInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E40AF)
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 48.dp, bottom = 16.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    text = "Completed Matches",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(matches) { match ->
                CompletedMatchCard(match = match, onClick = { onMatchClick(match) })
            }
        }
    }
}

@Composable
fun CompletedMatchCard(match: CompletedMatchInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = match.date, color = Color(0xFF64748B), fontSize = 12.sp)
                Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(4.dp)) {
                    Text(
                        text = match.type, 
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), 
                        color = Color(0xFF1E40AF), 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = match.teamA, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = match.scoreA, color = Color(0xFF64748B), fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = match.teamB, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = match.scoreB, color = Color(0xFF64748B), fontSize = 13.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
            
            Text(text = match.result, color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun CompletedMatchDetail(match: CompletedMatchInfo, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf("Match Result") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // detail Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E40AF)
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${match.teamA} vs ${match.teamB}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${match.venue} • ${match.type}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "COMPLETED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    MatchTabButton(
                        text = "Match Result",
                        icon = Icons.Default.EmojiEvents,
                        isSelected = selectedTab == "Match Result",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = "Match Result" }
                    )
                    MatchTabButton(
                        text = "AI Predictions",
                        icon = Icons.Default.AutoAwesome,
                        isSelected = selectedTab == "AI Predictions",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = "AI Predictions" }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedTab == "Match Result") {
                // Winner Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            color = Color(0xFFFEF3C7),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFD97706), modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(text = "Winner", color = Color(0xFF92400E), fontSize = 14.sp)
                        Text(
                            text = if (match.result.contains(match.teamA, ignoreCase = true)) match.teamA else match.teamB,
                            color = Color(0xFF92400E),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(text = match.result, color = Color(0xFF92400E).copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }

                // Full Scorecard
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(4.dp).height(16.dp).background(Color(0xFF1E40AF), RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(8.dp))
                            Text(text = "Full Scorecard", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(20.dp))
                        InningsRow("1st Innings", match.teamA, match.scoreA, "", "172", "5", "20.0", Color(0xFF1E40AF))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color(0xFFF1F5F9))
                        InningsRow("2nd Innings", match.teamB, match.scoreB, "", "175", "4", "19.3", Color(0xFF3B82F6))
                    }
                }
            } else {
                AIPredictionsContent()
            }
        }
    }
}

@Composable
fun MatchTabButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .padding(2.dp)
            .clickable(onClick = onClick),
        color = if (isSelected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = if (isSelected) Color(0xFF1E40AF) else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = if (isSelected) Color(0xFF1E40AF) else Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun InningsRow(
    title: String,
    teamName: String,
    score: String,
    overs: String,
    runs: String,
    wickets: String,
    overNum: String,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(text = title, color = Color(0xFF64748B), fontSize = 12.sp)
                Text(text = teamName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
            }
            Text(text = score, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        
        Spacer(Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScoreDetailItem("Runs", runs)
            ScoreDetailItem("Wickets", wickets)
            ScoreDetailItem("Overs", overNum)
        }
    }
}

@Composable
fun ScoreDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 10.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun AIPredictionsContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = "AI Post-Match Analysis", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "The match was highly competitive. Lucknow's strategy to accelerate in the middle overs proved decisive against Gujarat's bowling attack.",
                    color = Color(0xFF475569),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
