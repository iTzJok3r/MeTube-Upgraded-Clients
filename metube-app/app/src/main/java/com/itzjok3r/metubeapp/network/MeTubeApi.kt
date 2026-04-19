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

    @POST("delete")
    suspend fun deleteDownload(@Body request: com.itzjok3r.metubeapp.model.DeleteRequest): Response<Map<String, Any>>

    /**
     * Fetch available ytdl-dlp presets.
     */
    @GET("presets")
    suspend fun getPresets(): Response<Map<String, List<String>>>

    /**
     * Start pending downloads.
     */
    @POST("start")
    suspend fun startDownloads(@Body request: com.itzjok3r.metubeapp.model.BulkIdRequest): Response<Map<String, Any>>

    /**
     * Fetch all subscriptions.
     */
    @GET("subscriptions")
    suspend fun getSubscriptions(): Response<List<com.itzjok3r.metubeapp.model.SubscriptionItem>>

    /**
     * Delete subscriptions.
     */
    @POST("subscriptions/delete")
    suspend fun deleteSubscriptions(@Body request: com.itzjok3r.metubeapp.model.BulkIdRequest): Response<Map<String, Any>>

    /**
     * Trigger manual check for subscriptions.
     */
    @POST("subscriptions/check")
    suspend fun checkSubscriptions(@Body request: com.itzjok3r.metubeapp.model.BulkIdRequest): Response<Map<String, Any>>

    /**
     * Add a new subscription.
     */
    @POST("subscribe")
    suspend fun addSubscription(@Body request: AddRequest): Response<Map<String, Any>>

    /**
     * Update an existing subscription.
     */
    @POST("subscriptions/update")
    suspend fun updateSubscription(@Body request: com.itzjok3r.metubeapp.model.SubscriptionItem): Response<Map<String, Any>>

    /**
     * Fetch cookie status.
     */
    @GET("cookie-status")
    suspend fun getCookieStatus(): Response<Map<String, Any>>

    /**
     * Delete server-side cookies.
     */
    @POST("delete-cookies")
    suspend fun deleteCookies(): Response<Map<String, Any>>
}
