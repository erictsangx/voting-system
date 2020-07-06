package com.hk.voting.models

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant


@Document
data class VoteCount(
        val candidateId: String,
        val count: Long = 0,
        @JsonIgnore
        val updatedAt: Instant = Instant.now()
)
