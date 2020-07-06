package com.hk.voting.utility


/**
 * Redis keys for caching
 */
object CacheKey {
    const val CAMPAIGN_LIST_AVAILABLE = "CAMPAIGN_LIST_AVAILABLE"
    const val CAMPAIGN_LIST_EXPIRED = "CAMPAIGN_LIST_EXPIRED"

    const val CANDIDATE_COUNT = "CANDIDATE_COUNT"

    const val CAMPAIGN_SINGLE = "CAMPAIGN_SINGLE:"


    const val LIST_VOTE = "LIST_VOTE:"

    const val MQ_TOPIC = "INSERT_VOTE"
}
