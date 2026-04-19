package com.itzjok3r.metubeapp.model

/**
 * Request payload for bulk operations that only require a list of IDs.
 * Used for subscriptions/delete, subscriptions/check, and start endpoints.
 */
data class BulkIdRequest(
    val ids: List<String>
)
