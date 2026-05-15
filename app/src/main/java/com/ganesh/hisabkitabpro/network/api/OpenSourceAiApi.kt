package com.ganesh.hisabkitabpro.network.api

import retrofit2.http.GET
import retrofit2.http.Query

interface WikipediaApi {
    @GET("api.php")
    suspend fun searchKnowledge(
        @Query("action") action: String = "query",
        @Query("format") format: String = "json",
        @Query("prop") prop: String = "extracts",
        @Query("exintro") exintro: Boolean = true,
        @Query("explaintext") explaintext: Boolean = true,
        @Query("titles") titles: String
    ): WikipediaResponse
}

data class WikipediaResponse(val query: WikipediaQuery)
data class WikipediaQuery(val pages: Map<String, WikipediaPage>)
data class WikipediaPage(val extract: String?)
