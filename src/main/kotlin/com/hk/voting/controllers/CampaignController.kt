package com.hk.voting.controllers

import com.hk.voting.exceptions.UnprocessableException
import com.hk.voting.models.Campaign
import com.hk.voting.models.Vote
import com.hk.voting.models.VoteCount
import com.hk.voting.services.CampaignRepo
import com.hk.voting.services.RedisService
import com.hk.voting.utility.*
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.time.Duration

@RestController
@RequestMapping("/api/v1/campaign")
class CampaignController @Autowired constructor(
        val campaignRepo: CampaignRepo,
        val redisService: RedisService
) {

    companion object {
        const val INVALID_HK_ID = "Invalid HK ID"
        const val INVALID_CAMPAIGN = "Campaign not existed or expired"
        const val INVALID_CANDIDATE = "Wrong candidate"
    }

    data class Voting(
            @ApiModelProperty(example = "5efe07b74b294a156099d0e2")
            val candidateId: String,
            @ApiModelProperty(example = "5efe07b74b294a156099d0e2")
            val campaignId: String,
            @ApiModelProperty(value = "will convert to upper case", example = "A1234567")
            val hkId: String
    ) {
        /**
         * Convert to [Vote] with hashed uppercase [hkId]
         */
        fun hashed() = Vote(candidateId, campaignId, privacyHash(hkId.toUpperCase()))
    }


    @ApiOperation("available campaigns (startTime<=now<=endTime, sort by total votes DESC)", notes = "Cached, expire at next hour XX:00 ")
    @GetMapping("/list/available")
    fun listAvailable(): List<Campaign> {
        val key = CacheKey.CAMPAIGN_LIST_AVAILABLE

        //check cached?
        redisService.get(key)?.run { return this.toData() }

        val campaigns = campaignRepo.listAvailable()
        val sorted = campaigns.asSequence()
                .map { campaign ->
                    campaign to this.count(campaign.candidates.map { it.id }).map { it.count }.sum()
                }
                .sortedByDescending { it.second }
                .map { it.first }
                .toList()
        redisService.set(key, sorted.toJsonString(), nextHour)
        return sorted
    }


    @ApiOperation("expired campaigns (endTime<now, order by endTime, sort by endTime DESC)", notes = "Cached, expire at next hour XX:00")
    @ApiResponses(ApiResponse(code = 200, responseContainer = "List", response = Campaign::class, message = ""))
    @GetMapping("/list/expired")
    fun listExpired(): List<Campaign> {
        val key = CacheKey.CAMPAIGN_LIST_EXPIRED

        //check cached?
        redisService.get(key)?.run { return this.toData() }

        val campaigns = campaignRepo.listExpired()
        redisService.set(key, campaigns.toJsonString(), nextHour)
        return campaigns
    }


    @ApiOperation("Real-time vote counting of candidates")
    @PostMapping("/count")
    fun count(@RequestBody candidateIds: List<String>): List<VoteCount> {

        //check if  [VoteCount] are cached
        val (cached, nonCached) = candidateIds.asSequence()
                .map {
                    it to redisService.hGet(CacheKey.CANDIDATE_COUNT, it)?.toLong()
                }
                .partition { it.second != null }

        //query non-cached
        val nonCachedVoteCount = if (nonCached.isNotEmpty()) {
            campaignRepo.getVoteCount(nonCached.map { it.first })
        } else emptyList()

        //save non-cached to redis
        nonCachedVoteCount.forEach {
            redisService.inc(CacheKey.CANDIDATE_COUNT, it.candidateId, it.count)
        }

        return cached.map { VoteCount(it.first, it.second ?: 0) } + nonCachedVoteCount
    }

    @ApiOperation("list votes by HK ID")
    @ApiResponses(value = [ApiResponse(code = 422, message = INVALID_HK_ID)])
    @GetMapping(value = ["/list-vote"])
    fun listVote(@RequestParam hkId: String): List<Vote> {
        //validate HK ID
        hkId.takeIf { validateHKID(it) } ?: throw UnprocessableException(INVALID_HK_ID)

        return cacheListVote(privacyHash(hkId))
    }

    @ApiOperation("Vote a candidate", notes = "ONE HK ID can only vote ONE candidate for each campaign, duplicated votes will be ignored")
    @ApiResponses(value = [ApiResponse(code = 422, message = "$INVALID_HK_ID<br/>$INVALID_CAMPAIGN<br/>$INVALID_CANDIDATE")])
    @PostMapping(value = ["/vote"])
    fun vote(@RequestBody voting: Voting) {

        //validate HK ID
        voting.hkId.takeIf { validateHKID(it) } ?: throw UnprocessableException(INVALID_HK_ID)

        //check campaign existed and available
        val campaign = cacheCampaign(voting.campaignId)?.takeIf { it.isAvailable }
                ?: throw UnprocessableException(INVALID_CAMPAIGN)

        //check campaign owns the candidate
        campaign.candidates.find { it.id == voting.candidateId }
                ?: throw UnprocessableException(INVALID_CANDIDATE)

        //produce a message into MQ
        redisService.produce(voting.hashed().toJsonString())
    }


    private fun cacheCampaign(campaignId: String): Campaign? {
        val key = CacheKey.CAMPAIGN_SINGLE + campaignId
        val cached = redisService.get(key)
        if (cached != null) {
            return cached.toData()
        }

        val campaign = campaignRepo.singleOrNull(campaignId)
        if (campaign != null) {
            redisService.setex(key, campaign.toJsonString(), Duration.ofHours(1))
        }
        return campaign
    }

    private fun cacheListVote(hkId: String): List<Vote> {
        val key = CacheKey.LIST_VOTE + hkId
        val cached = redisService.get(key)
        if (cached != null) {
            return cached.toData()
        }

        val votes = campaignRepo.listVote(hkId)
        redisService.setex(key, votes.toJsonString(), Duration.ofHours(1))
        return votes
    }
}
