package io.mfj.uns.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Event(
    val id: String,
    val agencyId: String,
    val status: String,
    val payload: Map<String, Any>
)