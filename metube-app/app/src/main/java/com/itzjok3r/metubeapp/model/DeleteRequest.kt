package com.itzjok3r.metubeapp.model

/** Payload for POST /delete endpoint */
data class DeleteRequest(
    val ids: List<String>,
    val where: String // "queue" or "done"
)
