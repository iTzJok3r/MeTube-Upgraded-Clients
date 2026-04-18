package com.itzjok3r.metubeapp.model

/**
 * Response wrapper for the GET /history endpoint.
 *
 * The MeTube backend returns a JSON object with three arrays:
 * - "queue"   → actively downloading items
 * - "done"    → completed items
 * - "pending" → queued but not yet started items
 *
 * This class is deserialized by Gson from that response.
 */
data class HistoryResponse(
    val queue: List<Map<String, Any?>> = emptyList(),
    val done: List<Map<String, Any?>> = emptyList(),
    val pending: List<Map<String, Any?>> = emptyList()
)
