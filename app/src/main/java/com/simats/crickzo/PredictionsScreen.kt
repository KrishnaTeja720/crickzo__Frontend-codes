package com.simats.crickzo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PredictionsScreen(
    match: LiveMatchData? = null,
    teamA: String = "Mumbai Indians",
    teamB: String = "Chennai Super Kings",
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Predictions") }
    val coroutineScope = rememberCoroutineScope()
    
    // Local state to simulate live updates or hold API data
    var currentMatch by remember(match) {
        mutableStateOf(match ?: LiveMatchData(
            matchId = 0,
            team1 = teamA,
            score1 = "142/3",
            overs1 = "(15.2)",
            team2 = teamB,
            score2 = "--",
            overs2 = "",
            crr = 9.26,
            location = "Wankhede Stadium"
        ))
    }

    // Backend predictions state
    var apiPredictions by remember { mutableStateOf<MatchPredictions?>(null) }
    var isUsingFallback by remember { mutableStateOf(false) }

    // Fetch API predictions if available
    LaunchedEffect(currentMatch.matchId) {
        if (currentMatch.matchId != 0) {
            try {
                val response = RetrofitClient.apiService.getMatchPredictions(currentMatch.matchId.toString())
                if (response.isSuccessful) {
                    apiPredictions = response.body()
                    isUsingFallback = false
                } else {
                    isUsingFallback = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isUsingFallback = true
            }
        } else {
            isUsingFallback = true
        }
    }

    // Simulation effect to update score automatically if no real backend data
    LaunchedEffect(Unit) {
        if (currentMatch.matchId == 0) {
            while (true) {
                delay(4000)
                currentMatch = simulateNextBall(currentMatch)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E40AF), Color(0xFF3B82F6))
                    )
                )
                .padding(top = 40.dp, bottom = 20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currentMatch.team1} vs",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFEF4444),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timeline,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = currentMatch.team2,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentMatch.location} • T20",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Custom Segmented Switch
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                        .padding(4.dp)
                ) {
                    val tabs = listOf("Score Update", "Predictions")
                    tabs.forEach { tab ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    if (selectedTab == tab) Color.White else Color.Transparent,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp)
                                .clickableWithoutRipple { selectedTab = tab },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (tab == "Predictions") Icons.Default.QueryStats else Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = if (selectedTab == tab) Color(0xFF1E40AF) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = tab,
                                    color = if (selectedTab == tab) Color(0xFF1E40AF) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedTab == "Score Update") {
                ScoreUpdateView(match = currentMatch)
            } else {
                PredictionsContentIntegrated(match = currentMatch, apiPredictions = apiPredictions)
            }
        }
    }
}

@Composable
fun PredictionsContentIntegrated(match: LiveMatchData, apiPredictions: MatchPredictions?) {
    // If API data is available, use it. Otherwise, use local calculation logic.
    val predictions = if (apiPredictions != null) {
        // Map API model to Local UI model
        LocalMatchPredictions(
            winProbA = apiPredictions.winnerPrediction.teamA,
            winProbB = apiPredictions.winnerPrediction.teamB,
            totalProjectedMin = apiPredictions.projectedScore.range.split("-").getOrNull(0)?.trim()?.toIntOrNull() ?: 160,
            totalProjectedMax = apiPredictions.projectedScore.range.split("-").getOrNull(1)?.trim()?.toIntOrNull() ?: 180,
            phaseAnalysisName = apiPredictions.phaseAnalysis.phase,
            phaseAnalysisScore = apiPredictions.phaseAnalysis.deathRuns,
            phaseAnalysisConfidence = "${apiPredictions.phaseAnalysis.confidence}%",
            nextOverRuns = apiPredictions.nextOver.runs,
            nextOverConfidence = "${apiPredictions.nextOver.confidence}% confidence",
            next5OversRuns = apiPredictions.next5Overs.runs,
            next5OversConfidence = "${apiPredictions.next5Overs.confidence}% confidence",
            wicketProb = apiPredictions.wicketProbability,
            partnershipForecast = apiPredictions.partnershipForecast.runs,
            partnershipLikely = apiPredictions.partnershipForecast.chance,
            strikerName = apiPredictions.batsmanForecast.getOrNull(0)?.name ?: "Batsman 1",
            strikerRuns = apiPredictions.batsmanForecast.getOrNull(0)?.finalRuns ?: 45,
            strikerBoundary = "${apiPredictions.batsmanForecast.getOrNull(0)?.boundaryPercent ?: 40}%",
            strikerRisk = "${apiPredictions.batsmanForecast.getOrNull(0)?.outRisk ?: 20}%",
            nonStrikerName = apiPredictions.batsmanForecast.getOrNull(1)?.name ?: "Batsman 2",
            nonStrikerRuns = apiPredictions.batsmanForecast.getOrNull(1)?.finalRuns ?: 32,
            nonStrikerBoundary = "${apiPredictions.batsmanForecast.getOrNull(1)?.boundaryPercent ?: 35}%",
            nonStrikerRisk = "${apiPredictions.batsmanForecast.getOrNull(1)?.outRisk ?: 15}%",
            deathOversScore = apiPredictions.deathOversScore.runs,
            deathOversConfidence = "${apiPredictions.deathOversScore.confidence}%"
        )
    } else {
        // Fallback to local logic if API fails
        calculatePredictions(match)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Match Winner Prediction
        DetailedPredictionCard(
            title = "Match Winner Prediction",
            icon = Icons.AutoMirrored.Filled.TrendingUp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WinnerProgress(name = match.team1, percentage = predictions.winProbA, color = Color(0xFF1E40AF))
                WinnerProgress(name = match.team2, percentage = predictions.winProbB, color = Color(0xFF3B82F6))
            }
        }

        // Total Innings Prediction
        DetailedPredictionCard(
            title = "Total Innings Prediction",
            icon = Icons.Default.Lightbulb,
            headerExtra = {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Confidence", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Text(predictions.phaseAnalysisConfidence, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        ) {
            Text(
                text = "${predictions.totalProjectedMin} – ${predictions.totalProjectedMax}",
                color = Color(0xFF1E40AF),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Phase Analysis
        DetailedPredictionCard(
            title = "Phase Analysis",
            icon = Icons.Default.WaterDrop
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PhaseItem(
                    name = predictions.phaseAnalysisName,
                    score = predictions.phaseAnalysisScore.toString(),
                    percentage = predictions.phaseAnalysisConfidence,
                    color = Color(0xFFFFE4E6)
                )
            }
        }

        // Next Over & Next 5 Overs
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniPredictionCard(
                modifier = Modifier.weight(1f),
                title = "Next Over",
                icon = Icons.Default.History,
                value = "${predictions.nextOverRuns} runs",
                confidence = predictions.nextOverConfidence
            )
            MiniPredictionCard(
                modifier = Modifier.weight(1f),
                title = "Next 5 Overs",
                icon = Icons.Default.History,
                value = "${predictions.next5OversRuns} runs",
                confidence = predictions.next5OversConfidence
            )
        }

        // Wicket Probability
        DetailedPredictionCard(
            title = "Wicket Probability",
            icon = Icons.Default.QueryStats
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(predictions.wicketProb / 100f)
                            .fillMaxHeight()
                            .background(Color(0xFF1E40AF), RoundedCornerShape(4.dp))
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text("${predictions.wicketProb}%", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // Partnership Forecast
        DetailedPredictionCard(
            title = "Partnership Forecast",
            icon = Icons.Default.Groups,
            headerExtra = {
                Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        predictions.partnershipLikely,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }
        ) {
            Text(
                text = "${predictions.partnershipForecast} runs",
                color = Color(0xFF1E293B),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Batsmen Forecast
        DetailedPredictionCard(
            title = "Batsmen Forecast",
            icon = Icons.Default.Person
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BatsmanForecastItem(
                    name = predictions.strikerName,
                    runs = predictions.strikerRuns.toString(),
                    boundary = predictions.strikerBoundary,
                    risk = predictions.strikerRisk,
                    isStriker = true
                )
                BatsmanForecastItem(
                    name = predictions.nonStrikerName,
                    runs = predictions.nonStrikerRuns.toString(),
                    boundary = predictions.nonStrikerBoundary,
                    risk = predictions.nonStrikerRisk,
                    isStriker = false
                )
            }
        }

        // Death Overs Score
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFE4E6),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Death Overs Score", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Confidence", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(predictions.deathOversConfidence, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Text(
                    text = predictions.deathOversScore.toString(),
                    color = Color(0xFFEF4444),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun ScoreUpdateView(match: LiveMatchData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDBEAFE))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${match.team1} vs ${match.team2}",
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(match.score1, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF))
                    Text(match.overs1, fontSize = 14.sp, color = Color(0xFF64748B))
                }
                Text("VS", fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val score2Disp = if (match.score2 == "--") "0/0" else match.score2
                    Text(score2Disp, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF))
                    Text(match.overs2, fontSize = 14.sp, color = Color(0xFF64748B))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CRR: ${String.format(Locale.US, "%.2f", match.crr)}", color = Color(0xFF1E40AF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (match.rrr != null) {
                    Spacer(Modifier.width(16.dp))
                    Text("RRR: ${match.rrr}", color = Color(0xFFEF4444), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailedPredictionCard(
    title: String,
    icon: ImageVector,
    headerExtra: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = title,
                        color = Color(0xFF1E293B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                headerExtra?.invoke()
            }
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun WinnerProgress(name: String, percentage: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Text(text = "$percentage%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(5.dp))
            )
        }
    }
}

@Composable
fun PhaseItem(name: String, score: String, percentage: String, color: Color) {
    Surface(
        modifier = Modifier.size(width = 120.dp, height = 110.dp),
        shape = RoundedCornerShape(12.dp),
        color = color,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = name, color = Color(0xFF64748B), fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text(text = score, color = Color(0xFFEF4444), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = percentage, color = Color(0xFF94A3B8).copy(alpha = 0.8f), fontSize = 10.sp)
        }
    }
}

@Composable
fun MiniPredictionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    value: String,
    confidence: String
) {
    Card(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = title, color = Color(0xFF64748B), fontSize = 12.sp)
            }
            Text(text = value, color = Color(0xFF1E40AF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = confidence, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
    }
}

@Composable
fun BatsmanForecastItem(
    name: String,
    runs: String,
    boundary: String,
    risk: String,
    isStriker: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF8FAFC).copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Text(
                        text = if (isStriker) "Striker" else "Non-Striker",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ForecastStat(label = "Final Runs", value = runs, color = Color(0xFF1E40AF))
                ForecastStat(label = "Boundary %", value = boundary, color = Color(0xFF1E293B))
                ForecastStat(label = "Out Risk", value = risk, color = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
fun ForecastStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

inline fun Modifier.clickableWithoutRipple(
    crossinline onClick: () -> Unit
): Modifier = composed {
    this.clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        onClick()
    }
}

// Prediction Logic Data Classes and Functions

data class LocalMatchPredictions(
    val winProbA: Int,
    val winProbB: Int,
    val totalProjectedMin: Int,
    val totalProjectedMax: Int,
    val phaseAnalysisName: String,
    val phaseAnalysisScore: Int,
    val phaseAnalysisConfidence: String,
    val nextOverRuns: Int,
    val nextOverConfidence: String,
    val next5OversRuns: Int,
    val next5OversConfidence: String,
    val wicketProb: Int,
    val partnershipForecast: Int,
    val partnershipLikely: String,
    val strikerName: String,
    val strikerRuns: Int,
    val strikerBoundary: String,
    val strikerRisk: String,
    val nonStrikerName: String,
    val nonStrikerRuns: Int,
    val nonStrikerBoundary: String,
    val nonStrikerRisk: String,
    val deathOversScore: Int,
    val deathOversConfidence: String
)

private fun parseScore(score: String): Pair<Int, Int> {
    if (score == "--" || score.isEmpty()) return 0 to 0
    val parts = score.split("/")
    val runs = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val wickets = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return runs to wickets
}

private fun parseOvers(overs: String): Int {
    if (overs.isEmpty() || overs == "--") return 0
    val clean = overs.replace("(", "").replace(")", "").trim()
    val parts = clean.split(".")
    val ov = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val balls = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return ov * 6 + balls
}

private fun simulateNextBall(match: LiveMatchData): LiveMatchData {
    val isSecondInnings = match.score2 != "--"
    val scoreStr = if (isSecondInnings) match.score2 else match.score1
    val oversStr = if (isSecondInnings) match.overs2 else match.overs1
    
    var (runs, wickets) = parseScore(scoreStr)
    var balls = parseOvers(oversStr)
    
    if (balls >= 120 || wickets >= 10) return match // Match ended
    
    // Simulate a ball
    balls += 1
    val outcome = (0..100).random()
    when {
        outcome < 4 -> wickets += 1 // ~4% chance of wicket
        outcome < 12 -> runs += 6   // ~8% chance of 6
        outcome < 28 -> runs += 4   // ~16% chance of 4
        outcome < 45 -> runs += 2   // ~17% chance of 2
        outcome < 80 -> runs += 1   // ~35% chance of 1
        else -> {}                  // ~20% dot ball
    }
    
    val newScore = "$runs/$wickets"
    val newOvers = "(${balls / 6}.${balls % 6})"
    val newCrr = if (balls > 0) (runs.toDouble() / (balls / 6.0)) else 0.0
    
    return if (isSecondInnings) {
        val target = match.target?.toIntOrNull() ?: 0
        val runsNeeded = target - runs
        val ballsLeft = 120 - balls
        val newRrr = if (ballsLeft > 0) String.format(Locale.US, "%.2f", (runsNeeded.toDouble() / (ballsLeft / 6.0))) else "0.00"
        match.copy(score2 = newScore, overs2 = newOvers, crr = newCrr, rrr = newRrr)
    } else {
        match.copy(score1 = newScore, overs1 = newOvers, crr = newCrr)
    }
}

private fun calculatePredictions(match: LiveMatchData): LocalMatchPredictions {
    val (runs1, wickets1) = parseScore(match.score1)
    val balls1 = parseOvers(match.overs1)
    
    val (runs2, wickets2) = if (match.score2 != "--") parseScore(match.score2) else 0 to 0
    val balls2 = if (match.overs2.isNotEmpty() && match.overs2 != "--") parseOvers(match.overs2) else 0

    val isSecondInnings = match.score2 != "--"
    
    val currentRuns = if (isSecondInnings) runs2 else runs1
    val currentWickets = if (isSecondInnings) wickets2 else wickets1
    val currentBalls = if (isSecondInnings) balls2 else balls1
    
    val totalBalls = 120
    val ballsLeft = totalBalls - currentBalls
    val crr = if (currentBalls > 0) (currentRuns.toDouble() / (currentBalls / 6.0)) else 7.0
    
    // Win Probability
    val winProbA = if (!isSecondInnings) {
        (50 + (crr - 7) * 4 - currentWickets * 3).toInt().coerceIn(10, 90)
    } else {
        val target = match.target?.toIntOrNull() ?: (runs1 + 1)
        val runsNeeded = target - runs2
        val rrr = if (ballsLeft > 0) (runsNeeded.toDouble() / (ballsLeft / 6.0)) else 0.0
        if (runsNeeded <= 0) 100 else (50 + (crr - rrr) * 8 - wickets2 * 5).toInt().coerceIn(5, 95)
    }
    val winProbB = 100 - winProbA
    
    // Total Innings
    val projected = (currentRuns + (crr * (ballsLeft / 6.0))).toInt()
    val minTotal = projected - 8
    val maxTotal = projected + 12
    
    // Phase Analysis
    val phase = when {
        currentBalls <= 36 -> "Powerplay"
        currentBalls <= 90 -> "Middle"
        else -> "Death"
    }
    val phaseScore = (crr * 6).toInt() + (10 - currentWickets) * 2
    
    // Next Over
    val nextOver = (crr + 1.2).toInt().coerceIn(4, 18)
    val next5Overs = (crr * 5 + (5 - currentWickets) * 1.5).toInt().coerceIn(25, 75)
    
    // Wicket Prob
    val wicketProb = (10 + (currentWickets * 4) + (currentBalls / 8)).coerceIn(5, 55)
    
    // Partnership
    val partnership = (currentRuns / (currentWickets + 1)) + 12
    
    // Death Score
    val deathScore = if (currentBalls > 90) (crr * 5 + 12).toInt() else 58 - (currentWickets * 2)

    return LocalMatchPredictions(
        winProbA = winProbA,
        winProbB = winProbB,
        totalProjectedMin = minTotal,
        totalProjectedMax = maxTotal,
        phaseAnalysisName = phase,
        phaseAnalysisScore = phaseScore,
        phaseAnalysisConfidence = "76%",
        nextOverRuns = nextOver,
        nextOverConfidence = "72% confidence",
        next5OversRuns = next5Overs,
        next5OversConfidence = "85% confidence",
        wicketProb = wicketProb,
        partnershipForecast = partnership,
        partnershipLikely = "35% likely",
        strikerName = "Rohit Sharma",
        strikerRuns = (crr * 6).toInt() + 12,
        strikerBoundary = "${(42 + crr * 1.5).toInt()}%",
        strikerRisk = "${(18 + currentWickets * 2.5).toInt()}%",
        nonStrikerName = "Ishan Kishan",
        nonStrikerRuns = (crr * 5).toInt() + 8,
        nonStrikerBoundary = "${(38 + crr * 1.5).toInt()}%",
        nonStrikerRisk = "${15 + currentWickets * 2}%",
        deathOversScore = deathScore,
        deathOversConfidence = "76%"
    )
}
