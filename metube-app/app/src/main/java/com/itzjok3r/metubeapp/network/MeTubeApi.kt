package com.itzjok3r.metubeapp.network

import com.itzjok3r.metubeapp.model.AddRequest
import com.itzjok3r.metubeapp.model.HistoryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface for the MeTube REST API.
 *
 * All paths are relative to the base URL configured in [RetrofitClient].
 * The base URL defaults to https://y.itzjok3r.qzz.io/ and can be changed
 * at runtime via the Settings screen.
 */
interface MeTubeApi {

    /**
     * Submit a new download to the MeTube queue.
     *
     * The request body uses the legacy API shape (url + quality + format)
     * which the backend auto-migrates. See [MeTubeRequestBuilder] for the
     * adapter that constructs this payload.
     *
     * @param request The download request payload.
     * @return Response with status information. HTTP 400 if url/quality is missing.
     */
    @POST("add")
    suspend fun addDownload(@Body request: AddRequest): Response<Map<String, Any>>

    /**
     * Fetch the full download history from the server.
     *
     * Returns three lists: "queue" (active), "done" (completed), "pending" (waiting).
     * Used on app startup to hydrate the initial state before Socket.IO takes over
     * for real-time updates.
     *
     * @return Response containing grouped download items.
     */
    @GET("history")
    suspend fun getHistory(): Response<HistoryResponse>
}
