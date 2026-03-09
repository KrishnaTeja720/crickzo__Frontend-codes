package com.simats.crickzo

import com.google.gson.annotations.SerializedName

// Match Creation & Details
data class CreateMatchRequest(
    @SerializedName("team_a") val teamA: String,
    @SerializedName("team_b") val teamB: String,
    val format: String,
    val venue: String,
    val toss: String,
    val pitch: String,
    val weather: String
)

data class MatchDetailsResponse(
    @SerializedName("team_a") val teamA: String,
    @SerializedName("team_b") val teamB: String,
    val venue: String,
    val format: String,
    val runs: Int,
    val wickets: Int,
    val overs: Double,
    val crr: Double,
    @SerializedName("team_a_players") val teamAPlayers: List<String>? = null,
    @SerializedName("team_b_players") val teamBPlayers: List<String>? = null
)

// Players
data class AddPlayersRequest(
    @SerializedName("match_id") val matchId: Int,
    val team: String,
    val players: List<String>
)

data class StartMatchRequest(
    @SerializedName("match_id") val matchId: Int,
    val striker: String,
    @SerializedName("non_striker") val nonStriker: String,
    val bowler: String
)

// Scoring
data class BallInputRequest(
    @SerializedName("match_id") val matchId: Int,
    val innings: Int,
    @SerializedName("over_number") val over: Int,
    @SerializedName("ball_number") val ball: Int,
    val batsman: String,
    val bowler: String,
    val runs: Int,
    val extras: Int,
    val wicket: Int // 0 or 1
)

data class ScoreResponse(
    val runs: Int,
    val wickets: Int,
    val overs: Double,
    val crr: Double
)

data class BatsmanStat(
    val batsman: String,
    val runs: Int,
    val balls: Int,
    val fours: Int,
    val sixes: Int,
    @SerializedName("strike_rate") val strikeRate: Double
)

data class BowlerStat(
    val bowler: String,
    val overs: String,
    val runs: Int,
    val wickets: Int,
    val economy: Double
)

data class PartnershipResponse(
    val runs: Int,
    val balls: Int
)

// Matches List
data class LiveMatchData(
    @SerializedName("match_id") val matchId: Int = 0,
    @SerializedName("team_a") val teamA: String = "",
    @SerializedName("team_b") val teamB: String = "",
    val venue: String = "",
    val runs: Int = 0,
    val wickets: Int = 0,
    val overs: Double = 0.0,
    val crr: Double = 0.0,
    @SerializedName("team_a_players") val teamAPlayers: List<String>? = null,
    @SerializedName("team_b_players") val teamBPlayers: List<String>? = null,
    // Fields for PredictionsScreen compatibility
    val team1: String = "",
    val team2: String = "",
    val score1: String = "0/0",
    val score2: String = "--",
    val overs1: String = "(0.0)",
    val overs2: String = "",
    val rrr: String? = null,
    val target: String? = null,
    val location: String = ""
)

data class CompletedMatchInfo(
    @SerializedName("match_id") val matchId: Int = 0,
    @SerializedName("team_a") val teamA: String = "",
    @SerializedName("team_b") val teamB: String = "",
    val venue: String = "",
    val winner: String? = null,
    val type: String = "T20",
    val date: String = "Today",
    val scoreA: String = "0/0",
    val scoreB: String = "0/0",
    val result: String = "Match Completed"
)

data class CreatedMatch(
    val id: String,
    val teamA: String,
    val teamB: String,
    val teamAPlayers: List<String>,
    val teamBPlayers: List<String>,
    val status: String = "LIVE",
    val type: String = "T20",
    val location: String = "Local Ground",
    val striker: String = "",
    val nonStriker: String = "",
    val bowler: String = ""
)

// Predictions
data class MatchPredictions(
    @SerializedName("winner_prediction") val winnerPrediction: WinnerPrediction,
    @SerializedName("projected_score") val projectedScore: ProjectedScore,
    @SerializedName("phase_analysis") val phaseAnalysis: PhaseAnalysis,
    @SerializedName("next_over") val nextOver: PredictionDetail,
    @SerializedName("next_5_overs") val next5Overs: PredictionDetail,
    @SerializedName("wicket_probability") val wicketProbability: Int,
    @SerializedName("partnership_forecast") val partnershipForecast: PartnershipForecast,
    @SerializedName("batsman_forecast") val batsmanForecast: List<BatsmanForecast>,
    @SerializedName("death_overs_score") val deathOversScore: PredictionDetail
)

data class WinnerPrediction(val teamA: Int, val teamB: Int)
data class ProjectedScore(val range: String, val confidence: Int)
data class PhaseAnalysis(val phase: String, @SerializedName("death_runs") val deathRuns: Int, val confidence: Int)
data class PredictionDetail(val runs: Int, val confidence: Int)
data class PartnershipForecast(val runs: Int, val chance: String)
data class BatsmanForecast(
    val name: String,
    @SerializedName("final_runs") val finalRuns: Int,
    @SerializedName("boundary_percent") val boundaryPercent: Int,
    @SerializedName("out_risk") val outRisk: Int
)

// Common
data class GenericResponse(
    val message: String?, 
    val error: String?, 
    val status: String?,
    @SerializedName("match_id") val matchId: Int? = null
)
