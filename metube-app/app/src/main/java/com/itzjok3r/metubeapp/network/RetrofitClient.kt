package com.itzjok3r.metubeapp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton that provides the [MeTubeApi] Retrofit instance.
 *
 * The base URL can be changed at runtime (e.g. from the Settings screen).
 * Changing the URL replaces the internal Retrofit instance on next access.
 * Thread-safe via @Synchronized.
 */
object RetrofitClient {

    /** Default MeTube server URL. */
    private const val DEFAULT_BASE_URL = "http://localhost:8081/"

    /** Currently configured base URL. */
    @Volatile
    private var baseUrl: String = DEFAULT_BASE_URL

    /** Cached Retrofit instance; invalidated when base URL changes. */
    @Volatile
    private var retrofit: Retrofit? = null

    /** Cached API interface; invalidated when base URL changes. */
    @Volatile
    private var api: MeTubeApi? = null

    /**
     * OkHttp client with logging and sensible timeouts.
     *
     * The logging interceptor is set to BODY level for debug builds.
     * In production, consider reducing to BASIC or NONE.
     */
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Get the [MeTubeApi] instance for the current base URL.
     *
     * @return A ready-to-use Retrofit API interface.
     */
    @Synchronized
    fun getApi(): MeTubeApi {
        // Return cached instance if the URL hasn't changed
        api?.let { return it }

        // Ensure the base URL ends with a trailing slash (Retrofit requirement)
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit!!.create(MeTubeApi::class.java).also { api = it }
    }

    /**
     * Update the base URL for all subsequent API calls.
     *
     * Invalidates the cached Retrofit and API instances so the next call
     * to [getApi] builds a fresh instance with the new URL.
     *
     * @param url The new MeTube server URL.
     */
    @Synchronized
    fun setBaseUrl(url: String) {
        val normalizedUrl = if (url.endsWith("/")) url else "$url/"
        if (normalizedUrl != baseUrl) {
            baseUrl = normalizedUrl
            // Invalidate cached instances
            retrofit = null
            api = null
        }
    }

    /**
     * Get the currently configured base URL.
     *
     * @return The base URL string.
     */
    fun getBaseUrl(): String = baseUrl
}
