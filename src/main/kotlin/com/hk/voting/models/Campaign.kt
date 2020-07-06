package com.hk.voting.models

import com.fasterxml.jackson.annotation.JsonIgnore
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class Campaign(
        val title: String,
        val startTime: Instant,
        val endTime: Instant,
        val candidates: List<Candidate>,
        val id: String? = null
) {
    val isAvailable: Boolean
        @JsonIgnore
        get() {
            val now = Instant.now()
            return (startTime <= now && now <= endTime)
        }
}

@Document
data class Candidate(
        val name: String,
        val id: String = ObjectId().toString()
)
