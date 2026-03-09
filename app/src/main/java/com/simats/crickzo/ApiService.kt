package com.simats.crickzo

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth (Login/Signup/OTP are handled elsewhere as per request, 
    // but Profile/Reset are usually integrated)
    @GET("user/profile/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): Response<GenericResponse>

    @POST("user/favorite_team")
    suspend fun addFavoriteTeam(@Body request: Map<String, String>): Response<GenericResponse>

    // Matches
    @POST("match/create")
    suspend fun createMatch(@Body request: CreateMatchRequest): Response<GenericResponse>

    @GET("matches/live")
    suspend fun getLiveMatches(): Response<List<LiveMatchData>>

    @GET("matches/completed")
    suspend fun getCompletedMatches(): Response<List<CompletedMatchInfo>>

    @GET("match/details/{matchId}")
    suspend fun getMatchDetails(@Path("matchId") matchId: String): Response<MatchDetailsResponse>

    @POST("match/end")
    suspend fun endMatch(@Body request: Map<String, String>): Response<GenericResponse>

    // Players
    @POST("match/add_players")
    suspend fun addPlayers(@Body request: AddPlayersRequest): Response<GenericResponse>

    @POST("match/start")
    suspend fun startMatch(@Body request: StartMatchRequest): Response<GenericResponse>

    // Scoring
    @POST("match/ball")
    suspend fun submitBall(@Body request: BallInputRequest): Response<GenericResponse>

    @DELETE("match/undo/{matchId}")
    suspend fun undoBall(@Path("matchId") matchId: String): Response<GenericResponse>

    @GET("match/score/{matchId}")
    suspend fun getScoreboard(@Path("matchId") matchId: String): Response<ScoreResponse>

    @GET("match/batsmen/{matchId}")
    suspend fun getBatsmenStats(@Path("matchId") matchId: String): Response<List<BatsmanStat>>

    @GET("match/bowler/{matchId}")
    suspend fun getBowlerStats(@Path("matchId") matchId: String): Response<List<BowlerStat>>

    @GET("match/partnership/{matchId}")
    suspend fun getPartnership(@Path("matchId") matchId: String): Response<PartnershipResponse>

    @GET("match/last6/{matchId}")
    suspend fun getLastSixBalls(@Path("matchId") matchId: String): Response<List<String>>

    // Predictions
    @GET("match/predictions/{matchId}")
    suspend fun getMatchPredictions(@Path("matchId") matchId: String): Response<MatchPredictions>
}
