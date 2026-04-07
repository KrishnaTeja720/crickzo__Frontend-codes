package com.simats.crickzo

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    // Local state to simulate live updates or hold API data
    var currentMatch by remember(match) {
        mutableStateOf(match ?: LiveMatchData(
            matchId = 0,
            teamA = teamA,
            teamB = teamB,
            venue = "Local Ground",
            runs = 0,
            wickets = 0,
            overs = 0.0,
            crr = 0.0
        ))
    }

    // Backend predictions state
    var apiPredictions by remember { mutableStateOf<MatchPredictions?>(null) }

    // Fetch API predictions and score with LIVE polling (Step 1)
    LaunchedEffect(currentMatch.matchId) {
        if (currentMatch.matchId != 0) {
            while (true) {
                try {
                    // 1. Sync Live Score (so top bar is up-to-date)
                    val stateRes = RetrofitClient.apiService.getMatchState(currentMatch.matchId.toString(), currentMatch.currentInnings)
                    if (stateRes.isSuccessful) {
                        stateRes.body()?.let { s ->
                            currentMatch = currentMatch.copy(
                                runs = s.runs,
                                wickets = s.wickets,
                                overs = s.overs?.toDoubleOrNull() ?: currentMatch.overs,
                                crr = s.crr,
                                striker = s.striker,
                                strikerRuns = s.strikerRuns,
                                strikerBalls = s.strikerBalls,
                                nonStriker = s.nonStriker,
                                nonStrikerRuns = s.nonStrikerRuns,
                                nonStrikerBalls = s.nonStrikerBalls,
                                currentInnings = s.currentInnings
                            )
                        }
                    }

                    // 2. Sync Predictions (Winner Probability, etc.)
                    val response = RetrofitClient.apiService.getMatchPredictions(currentMatch.matchId.toString())
                    if (response.isSuccessful) {
                        apiPredictions = response.body()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(2000) // Poll every 2 seconds for REAL-TIME feel
            }
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
                .background(Color(0xFF2563EB)) // Blue as per screenshot
                .padding(top = 40.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Match Predictor",
                color = Color.White,
                fontSize = 18.sp,
            )
        }
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Score Bar (Step 2)
            ScoreUpdateView(match = currentMatch)
            
            PredictionsContentIntegrated(match = currentMatch, apiPredictions = apiPredictions)
        }
    }
}

@Composable
fun PredictionsContentIntegrated(match: LiveMatchData, apiPredictions: MatchPredictions?) {

    // ── Win Probability: prefer API, fall back to heuristic ─────────────────
    val fallback = calculatePredictions(match)

    val rawWinA: Float
    val rawWinB: Float
    if (apiPredictions?.winnerPrediction != null) {
        rawWinA = apiPredictions.winnerPrediction.teamA.toFloat()
        rawWinB = apiPredictions.winnerPrediction.teamB.toFloat()
    } else {
        // Use local heuristic — never "Calculating..."
        rawWinA = fallback.winProbA.toFloat()
        rawWinB = fallback.winProbB.toFloat()
    }

    // Normalize & clamp (identical to website: 2–98%, 1 decimal)
    val total = (rawWinA + rawWinB).takeIf { it > 0f } ?: 100f
    val aNorm = (rawWinA / total) * 100f
    val fWinA = (Math.round(aNorm.coerceIn(2f, 98f) * 10) / 10.0f)
    val fWinB = (Math.round((100f - fWinA) * 10) / 10.0f)

    val totalOvers = extractTotalOvers(match.format)

    // Detect innings from the label the backend returns
    val projLabel = apiPredictions?.projectedScore?.label ?: "Projected Total"
    val isSecondInnings = projLabel.startsWith("Target:", ignoreCase = true)

    val strikerForecast  = apiPredictions?.batsmanForecast?.find {
        it.name.equals(match.striker,    ignoreCase = true)
    }
    val nonStrikerForecast = apiPredictions?.batsmanForecast?.find {
        it.name.equals(match.nonStriker, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Team header (mirrors website: "TeamA vs TeamB" centered)
        Text(
            text = "${match.teamA} vs ${match.teamB}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // ── 1. MATCH WINNER PROBABILITY ─────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "MATCH WINNER PROBABILITY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF3B82F6).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "LIVE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Always show actual % (never "Calculating...")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${match.teamA}: ${fWinA}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${match.teamB}: ${fWinB}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Animated dual-color bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(fWinA.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1A56DB))))
                    )
                    Box(
                        modifier = Modifier
                            .weight(fWinB.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF34D399), Color(0xFF10B981))))
                    )
                }

                if (isSecondInnings) {
                    Spacer(Modifier.height(10.dp))
                    val runsNeededText = apiPredictions?.projectedScore?.range ?: ""
                    Text(
                        text = "Chasing — $runsNeededText needed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        // ── 2. NEXT OVER & NEXT 5 OVERS mini cards (side-by-side) ───────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val nextOverRuns = apiPredictions?.nextOver?.runs ?: fallback.nextOverRuns
            val next5Runs = apiPredictions?.next5Overs?.runs ?: fallback.next5OversRuns
            MiniPredictionCard(Modifier.weight(1f), "Next Over", "$nextOverRuns runs")
            MiniPredictionCard(Modifier.weight(1f), "Next 5 Overs", "$next5Runs runs")
        }

        // ── 3. PROJECTED SCORE / TARGET (gradient card) ─────────────────────
        val projValue = apiPredictions?.projectedScore?.range
            ?: "${fallback.totalProjectedMin}-${fallback.totalProjectedMax}"

        val cardBrush = if (isSecondInnings)
            Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))) // Purplish/pink for chase
        else
            Brush.linearGradient(listOf(Color(0xFF1E40AF), Color(0xFF3B82F6)))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(projLabel, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = projValue,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (isSecondInnings) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Needed to win",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ── 4. WICKET PROBABILITY ─────────────────────────────────────────────
        val wicketProb = apiPredictions?.wicketProbability ?: fallback.wicketProb
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "WICKET PROBABILITY (NEXT 12 BALLS)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "$wicketProb%",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFFFEE2E2), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((wicketProb / 100f).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // ── 5. BATSMAN FORECAST ───────────────────────────────────────────────
        Text(
            "BATSMAN FORECAST",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        // Striker card
        val striker1Name = match.striker ?: "Striker"
        val striker1Runs = strikerForecast?.finalRuns
            ?: (match.strikerRuns + (totalOvers * 1.5).toInt()).toString()
        val striker1Bound = "${strikerForecast?.boundaryPercent ?: fallback.strikerBoundary.filter { it.isDigit() }.toIntOrNull() ?: 0}%"
        val striker1Risk = "${strikerForecast?.outRisk ?: fallback.strikerRisk.filter { it.isDigit() }.toIntOrNull() ?: 0}%"

        WebStyleBatsmanForecastCard(striker1Name, striker1Runs, striker1Bound, striker1Risk)

        // Non-striker card
        val striker2Name = match.nonStriker ?: "Non-Striker"
        val striker2Runs = nonStrikerForecast?.finalRuns
            ?: (match.nonStrikerRuns + totalOvers).toString()
        val striker2Bound = "${nonStrikerForecast?.boundaryPercent ?: fallback.nonStrikerBoundary.filter { it.isDigit() }.toIntOrNull() ?: 0}%"
        val striker2Risk = "${nonStrikerForecast?.outRisk ?: fallback.nonStrikerRisk.filter { it.isDigit() }.toIntOrNull() ?: 0}%"

        WebStyleBatsmanForecastCard(striker2Name, striker2Runs, striker2Bound, striker2Risk)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun WebStyleBatsmanForecastCard(
    name: String,
    predictedScore: String,
    boundary: String,
    risk: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Player name with bottom divider (exact website style)
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFF1F5F9)
            )
            // Three-column stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PREDICTED SCORE", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(predictedScore, color = Color(0xFF1E293B), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BOUNDARY %", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(boundary, color = Color(0xFF1E293B), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("DISMISSAL RISK", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(risk, color = Color(0xFFEF4444), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
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
                text = "${match.teamA ?: "Team A"} vs ${match.teamB ?: "Team B"}",
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
                    Text("${match.runs}/${match.wickets}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF))
                    Text("(${match.overs})", fontSize = 14.sp, color = Color(0xFF64748B))
                }
                Text("VS", fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Displaying actual score if available, or placeholder that's not hardcoded 0/0 if possible.
                    // Since LiveMatchData only has one set of runs/wickets/overs, we use them for the current innings.
                    Text("${match.runs}/${match.wickets}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF))
                    Text("(${match.overs})", fontSize = 14.sp, color = Color(0xFF64748B))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CRR: ${String.format(Locale.US, "%.2f", match.crr)}", color = Color(0xFF1E40AF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DetailedPredictionCard(
    title: String,
    icon: ImageVector?,
    headerExtra: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = title,
                        color = Color(30, 41, 59),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                headerExtra?.invoke()
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun WinnerProgress(name: String, percentage: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(30, 41, 59))
            Text(text = "$percentage%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(241, 245, 249), RoundedCornerShape(5.dp))
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

// PhaseItem removed as it's no longer used

@Composable
fun MiniPredictionCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = Color(148, 163, 184), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(text = value, color = Color(0xFF1E293B), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun BatsmanForecastCard(
    name: String,
    predictedScore: String,
    boundary: String,
    risk: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B))
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("PREDICTED SCORE", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(predictedScore, color = Color(0xFF1E293B), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BOUNDARY %", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(boundary, color = Color(30, 41, 59), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("DISMISSAL RISK", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(risk, color = Color(239, 68, 68), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ForecastStat and BatsmanForecastItem removed as BatsmanForecastCard is used instead



private fun extractTotalOvers(format: String?): Int {
    if (format == null || format.isBlank()) return 20
    val f = format.lowercase().trim()
    return try {
        // Handle "2", "2 Overs", "2 Overs per innings", "T10", "ODI"
        if (f.contains("t10")) 10
        else if (f.contains("odi")) 50
        else if (f.contains("t20")) 20
        else {
            val numericPart = f.split(" ")[0].filter { it.isDigit() }
            if (numericPart.isNotEmpty()) numericPart.toInt() else 20
        }
    } catch (e: Exception) {
        20
    }
}

// Prediction Logic Data Classes and Functions

data class LocalMatchPredictions(
    val winProbA: Int,
    val winProbB: Int,
    val totalProjectedMin: Int,
    val totalProjectedMax: Int,
    val nextOverRuns: Int,
    val next5OversRuns: Int,
    val wicketProb: Int,
    val strikerName: String,
    val strikerRuns: String,
    val strikerBoundary: String,
    val strikerRisk: String,
    val nonStrikerName: String,
    val nonStrikerRuns: String,
    val nonStrikerBoundary: String,
    val nonStrikerRisk: String
)

internal fun simulateNextBall(match: LiveMatchData): LiveMatchData {
    var runs = match.runs
    var wickets = match.wickets
    var overs = match.overs
    
    val totalOvers = extractTotalOvers(match.format)
    val maxBalls = totalOvers * 6
    val currentBalls = (overs.toInt() * 6) + ((overs * 10).toInt() % 10)
    
    if (currentBalls >= maxBalls || wickets >= 10) return match // Match ended
    
    val newTotalBalls = currentBalls + 1
    val outcome = (0..100).random()
    when {
        outcome < 4 -> wickets += 1 // ~4% chance of wicket
        outcome < 12 -> runs += 6   // ~8% chance of 6
        outcome < 28 -> runs += 4   // ~16% chance of 4
        outcome < 45 -> runs += 2   // ~17% chance of 2
        outcome < 80 -> runs += 1   // ~35% chance of 1
        else -> {}                  // ~20% dot ball
    }
    
    val newOvers = (newTotalBalls / 6).toDouble() + (newTotalBalls % 6) / 10.0
    val newCrr = if (newTotalBalls > 0) (runs.toDouble() / (newTotalBalls / 6.0)) else 0.0
    
    return match.copy(runs = runs, wickets = wickets, overs = newOvers, crr = newCrr)
}

internal fun calculatePredictions(match: LiveMatchData): LocalMatchPredictions {
    val runs = match.runs
    val wickets = match.wickets
    val overs = match.overs
    val currentBalls = (overs.toInt() * 6) + ((overs * 10).toInt() % 10)
    
    val totalOvers = extractTotalOvers(match.format)
    val totalBalls = totalOvers * 6
    val ballsLeft = totalBalls - currentBalls
    
    val parRR = if (totalOvers <= 10) 9.5 else 8.5
    val crr = if (currentBalls > 0) (runs.toDouble() / (currentBalls / 6.0)) else parRR
    
    // Win Probability
    val winProbA = (50 + (crr - 7) * 4 - wickets * 3).toInt().coerceIn(10, 90)
    val winProbB = 100 - winProbA
    
    // Total Innings
    val projected = (runs + (crr * (ballsLeft / 6.0))).toInt()
    val minTotal = projected - 8
    val maxTotal = projected + 12
    
    // Phase Analysis
    val phase = when {
        currentBalls <= 36 -> "Powerplay"
        currentBalls <= 90 -> "Middle"
        else -> "Death"
    }
    val phaseScore = (crr * 6).toInt() + (10 - wickets) * 2
    
    // Next Over
    val nextOver = (crr + 1.2).toInt().coerceIn(4, 18)
    val next5Overs = (crr * 5 + (5 - wickets) * 1.5).toInt().coerceIn(25, 75)
    
    // Wicket Prob
    val wicketProb = (10 + (wickets * 4) + (currentBalls / 8)).coerceIn(5, 55)
    
    // Partnership
    val partnership = (runs / (wickets + 1)) + 12
    
    val currentStrikerRuns = match.strikerRuns
    val currentNonStrikerRuns = match.nonStrikerRuns
    
    val predictedStrikerMore = (crr * 1.5 + (totalBalls - currentBalls) / 20.0).toInt().coerceIn(2, 45)
    val predictedNonStrikerMore = (crr * 1.0 + (totalBalls - currentBalls) / 30.0).toInt().coerceIn(1, 35)

    return LocalMatchPredictions(
        winProbA = winProbA,
        winProbB = winProbB,
        totalProjectedMin = minTotal,
        totalProjectedMax = maxTotal,
        nextOverRuns = (crr * 1.1).toInt().coerceAtMost(36),
        next5OversRuns = (crr * 5 * 1.05).toInt().coerceAtMost(maxTotal - runs),
        wicketProb = wicketProb,
        strikerName = match.striker ?: "Striker",
        strikerRuns = (currentStrikerRuns + predictedStrikerMore).toString(),
        strikerBoundary = "${(42 + crr * 1.5).toInt().coerceAtMost(100)}%",
        strikerRisk = "${(18 + wickets * 2.5).toInt().coerceAtMost(100)}%",
        nonStrikerName = match.nonStriker ?: "Non-Striker",
        nonStrikerRuns = (currentNonStrikerRuns + predictedNonStrikerMore).toString(),
        nonStrikerBoundary = "${(38 + crr * 1.5).toInt().coerceAtMost(100)}%",
        nonStrikerRisk = "${(15 + wickets * 2).toInt().coerceAtMost(100)}%"
    )
}
