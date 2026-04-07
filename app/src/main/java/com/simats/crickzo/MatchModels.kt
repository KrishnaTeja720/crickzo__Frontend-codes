package com.simats.crickzo

import com.google.gson.annotations.SerializedName

// Auth Models
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("status") val status: String,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("message") val message: String?
)

data class SignupRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class SignupResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

data class UserProfileResponse(
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("matches_played") val matchesPlayed: Int = 0
)

data class ForgotPasswordRequest(
    @SerializedName("email") val email: String
)

data class ForgotPasswordResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

data class VerifyOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)

data class VerifyOtpResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("new_password") val newPassword: String
)

data class ResetPasswordResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

data class ResendOtpRequest(
    @SerializedName("email") val email: String
)

data class ResendOtpResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

data class ChangePasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)

// Match Models
data class CreateMatchRequest(
    @SerializedName("user_id") val user_id: Int,
    @SerializedName("team_a") val team_a: String,
    @SerializedName("team_b") val team_b: String,
    @SerializedName("format") val format: String,
    @SerializedName("venue") val venue: String,
    @SerializedName("toss") val toss: String,
    @SerializedName("toss_decision") val toss_decision: String,
    @SerializedName("pitch") val pitch: String,
    @SerializedName("weather") val weather: String
)

data class CreateMatchResponse(
    @SerializedName("status") val status: String,
    @SerializedName("match_id") val matchId: Int,
    @SerializedName("message") val message: String
)

data class PlayerListItem(
    @SerializedName("player_name") val playerName: String,
    @SerializedName("team_name") val teamName: String? = null,
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("balls") val balls: Int = 0
)

data class Player(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class MatchSetupRequest(
    @SerializedName("match_id") val matchId: Int,
    @SerializedName("team_a_players") val teamAPlayers: List<String>,
    @SerializedName("team_b_players") val teamBPlayers: List<String>
)

data class StartMatchRequest(
    @SerializedName("match_id") val matchId: Int,
    @SerializedName("striker") val striker: String,
    @SerializedName("non_striker") val nonStriker: String,
    @SerializedName("bowler") val bowler: String
)

data class ChangeBowlerRequest(
    @SerializedName("match_id") val matchId: Int,
    @SerializedName("bowler") val bowler: String
)

data class BallInputRequest(
    @SerializedName("match_id") val matchId: Int,
    @SerializedName("innings") val innings: Int,
    @SerializedName("batsman") val batsman: String,
    @SerializedName("bowler") val bowler: String,
    @SerializedName("runs") val runs: Int,
    @SerializedName("extras") val extras: Int,
    @SerializedName("extra_runs") val extraRunsAlias: Int = extras,
    @SerializedName("wicket") val wicket: Int,
    @SerializedName("is_wicket") val isWicket: Int = wicket,
    @SerializedName("wickets") val wickets: Int = wicket,
    @SerializedName("extras_type") val extrasType: String = "NONE",
    @SerializedName("extra_type") val extraTypeAlias: String = extrasType,
    @SerializedName("is_wide") val isWide: Int = if (extrasType.lowercase().startsWith("w") && extrasType.lowercase() != "wicket") 1 else 0,
    @SerializedName("is_noball") val isNoball: Int = if (extrasType.lowercase().startsWith("n")) 1 else 0,
    @SerializedName("is_no_ball") val isNoBallAlias: Int = if (extrasType.lowercase().startsWith("n")) 1 else 0
)

data class ScoreResponse(
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("wickets") val wickets: Int = 0,
    @SerializedName("balls") val balls: Int = 0,
    @SerializedName("overs") val overs: String? = "0.0",
    @SerializedName("crr") val crr: Double = 0.0,
    @SerializedName("striker") val striker: String? = null,
    @SerializedName("striker_runs") val strikerRuns: Int = 0,
    @SerializedName("striker_balls") val strikerBalls: Int = 0,
    @SerializedName("non_striker") val nonStriker: String? = null,
    @SerializedName("non_striker_runs") val nonStrikerRuns: Int = 0,
    @SerializedName("non_striker_balls") val nonStrikerBalls: Int = 0,
    @SerializedName("bowler") val bowler: String? = null,
    @SerializedName("bowler_runs") val bowlerRuns: Int = 0,
    @SerializedName("bowler_wickets") val bowlerWickets: Int = 0,
    @SerializedName("bowler_overs") val bowlerOvers: String? = "0.0",
    @SerializedName("current_innings") val currentInnings: Int = 1,
    @SerializedName("inn1_runs") val inn1Runs: Int = 0,
    @SerializedName("inn1_wickets") val inn1Wickets: Int = 0
)

data class BatsmanStat(
    @SerializedName("batsman") val batsman: String?,
    @SerializedName("player_name") val playerNameAlias: String? = batsman,
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("balls") val balls: Int = 0,
    @SerializedName("fours") val fours: Int = 0,
    @SerializedName("sixes") val sixes: Int = 0,
    @SerializedName("strike_rate") val strikeRate: Double = 0.0
)

data class BowlerStat(
    @SerializedName("bowler") val bowler: String?,
    @SerializedName("overs") val overs: String?,
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("wickets") val wickets: Int = 0,
    @SerializedName("economy") val economy: Double = 0.0
)

data class PartnershipResponse(
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("balls") val balls: Int = 0
)

data class LiveMatchData(
    @SerializedName("match_id") val matchId: Int,
    @SerializedName("team_a") val teamA: String? = "Team A",
    @SerializedName("team_b") val teamB: String? = "Team B",
    @SerializedName("venue") val venue: String? = "Local Ground",
    @SerializedName("inn1_runs") val inn1_runs: Int = 0,
    @SerializedName("inn1_wickets") val inn1_wickets: Int = 0,
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("wickets") val wickets: Int = 0,
    @SerializedName("overs") val overs: Double = 0.0,
    @SerializedName("crr") val crr: Double = 0.0,
    @SerializedName("current_innings") val currentInnings: Int = 1,
    @SerializedName("toss_winner") val tossWinner: String? = null,
    @SerializedName("toss_decision") val tossDecision: String? = null,
    @SerializedName("format") val format: String? = "T20",
    @SerializedName("striker") val striker: String? = null,
    @SerializedName("striker_runs") val strikerRuns: Int = 0,
    @SerializedName("striker_balls") val strikerBalls: Int = 0,
    @SerializedName("non_striker") val nonStriker: String? = null,
    @SerializedName("non_striker_runs") val nonStrikerRuns: Int = 0,
    @SerializedName("non_striker_balls") val nonStrikerBalls: Int = 0,
    @SerializedName("status") val status: String? = "live"
)

data class CompletedMatchInfo(
    @SerializedName("match_id") val matchId: Int,
    @SerializedName("team_a") val teamA: String?,
    @SerializedName("team_b") val teamB: String?,
    @SerializedName("venue") val venue: String?,
    @SerializedName("winner") val winner: String?,
    @SerializedName("result") val result: String? = null,
    @SerializedName("format") val format: String? = "T20",
    @SerializedName("team_a_runs") val teamARuns: Int = 0,
    @SerializedName("team_a_wickets") val teamAWickets: Int = 0,
    @SerializedName("team_a_overs") val teamAOvers: Float = 0f,
    @SerializedName("team_b_runs") val teamBRuns: Int = 0,
    @SerializedName("team_b_wickets") val teamBWickets: Int = 0,
    @SerializedName("team_b_overs") val teamBOvers: Float = 0f
)

data class MatchDetailsResponse(
    @SerializedName("team_a") val teamA: String,
    @SerializedName("team_b") val teamB: String,
    @SerializedName("venue") val venue: String,
    @SerializedName("format") val format: String,
    @SerializedName("toss_winner") val tossWinner: String?,
    @SerializedName("toss_decision") val tossDecision: String?,
    @SerializedName("status") val status: String,
    @SerializedName("runs") val runs: Int?,
    @SerializedName("wickets") val wickets: Int?,
    @SerializedName("overs") val overs: String?,
    @SerializedName("crr") val crr: Double?,
    @SerializedName("current_innings") val currentInnings: Int?,
    @SerializedName("striker") val striker: String?,
    @SerializedName("nonStriker") val nonStriker: String?,
    @SerializedName("bowler") val bowler: String?,
    @SerializedName("teamAScore") val teamAScore: ScoreResponse?,
    @SerializedName("teamBScore") val teamBScore: ScoreResponse?
)

data class MatchPredictions(
    @SerializedName("winnerPrediction") val winnerPrediction: WinnerPrediction?,
    @SerializedName("projectedScore") val projectedScore: ProjectedScore?,
    @SerializedName("nextOver") val nextOver: PredictionDetail?,
    @SerializedName("next5Overs") val next5Overs: PredictionDetail?,
    @SerializedName("wicketProbability") val wicketProbability: Int = 0,
    @SerializedName("partnershipForecast") val partnershipForecast: PartnershipForecast?,
    @SerializedName("batsmanForecast") val batsmanForecast: List<BatsmanForecast>? = null
)

data class WinnerPrediction(
    @SerializedName("teamA") val teamA: Int = 50,
    @SerializedName("teamB") val teamB: Int = 50
)

data class ProjectedScore(
    @SerializedName("range") val range: String? = "150-170",
    @SerializedName("label") val label: String? = "Projected Total",
    @SerializedName("confidence") val confidence: Int = 0
)

data class PredictionDetail(
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("confidence") val confidence: Int = 0
)

data class PartnershipForecast(
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("chance") val chance: String? = "low"
)

data class BatsmanForecast(
    @SerializedName("name") val name: String?,
    @SerializedName("final_runs") val finalRuns: String? = "0+",
    @SerializedName("boundary_percent") val boundaryPercent: Int = 0,
    @SerializedName("out_risk") val outRisk: Int = 0
)

data class ScorecardResponse(
    @SerializedName("teamA") val teamA: TeamScorecard?,
    @SerializedName("teamB") val teamB: TeamScorecard?
)

data class TeamScorecard(
    @SerializedName("batting") val batting: List<BatsmanScoreCard>,
    @SerializedName("bowling") val bowling: List<BowlerScoreCard>
)

data class BatsmanScoreCard(
    @SerializedName("playerName") val playerName: String,
    @SerializedName("runs") val runs: Int,
    @SerializedName("balls") val balls: Int,
    @SerializedName("fours") val fours: Int,
    @SerializedName("sixes") val sixes: Int,
    @SerializedName("status") val status: String = "DNB"
)

data class BowlerScoreCard(
    @SerializedName("playerName") val playerName: String,
    @SerializedName("overs") val overs: String,
    @SerializedName("maidens") val maidens: Int,
    @SerializedName("runs") val runs: Int,
    @SerializedName("wickets") val wickets: Int
)

data class EndMatchRequest(
    @SerializedName("match_id") val matchId: String,
    @SerializedName("winner") val winner: String
)

data class GenericMatchRequest(
    val match_id: String
)

data class EditScoreRequest(
    @SerializedName("match_id") val match_id: String,
    @SerializedName("innings") val innings: Int,
    @SerializedName("runs") val runs: Int,
    @SerializedName("wickets") val wickets: Int,
    @SerializedName("overs") val overs: String
)

data class RefreshScoreRequest(
    @SerializedName("match_id") val match_id: String,
    @SerializedName("innings") val innings: Int
)
