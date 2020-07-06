package com.hk.voting.models

import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class Vote(
        val candidateId: String,
        val campaignId: String,
        val hkId: String,
        val addedAt: Instant = Instant.now()
)
