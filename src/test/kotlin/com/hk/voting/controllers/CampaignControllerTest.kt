package com.hk.voting.controllers

import com.hk.voting.controllers.CampaignController.Companion.INVALID_CAMPAIGN
import com.hk.voting.controllers.CampaignController.Companion.INVALID_CANDIDATE
import com.hk.voting.controllers.CampaignController.Companion.INVALID_HK_ID
import com.hk.voting.exceptions.UnprocessableException
import com.hk.voting.models.*
import com.hk.voting.services.CampaignRepo
import com.hk.voting.services.JwtService
import com.hk.voting.services.RedisService
import com.hk.voting.test.*
import com.hk.voting.utility.CacheKey
import com.hk.voting.utility.CacheKey.CAMPAIGN_LIST_AVAILABLE
import com.hk.voting.utility.CacheKey.CAMPAIGN_LIST_EXPIRED
import com.hk.voting.utility.CacheKey.LIST_VOTE
import com.hk.voting.utility.nextHour
import com.hk.voting.utility.privacyHash
import com.hk.voting.utility.toJsonString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import java.time.Duration


@WebMvcTest(*[CampaignController::class, JwtService::class], properties = ["embedded.containers.enabled=false"])
@TestPropertySource(locations = ["classpath:application.test.properties"])
class CampaignControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @MockBean
    lateinit var repo: CampaignRepo

    @MockBean
    lateinit var redis: RedisService

//    @Autowired
//    lateinit var controller: CampaignController

    @Test
    fun `list available campaigns with caching`() {

        //setup query
        val expected = listOf(Campaign(title = randomStr, startTime = randomInstant, endTime = randomInstant, candidates = (1..3).map { Candidate(randomStr, randomId) }))
        method(repo.listAvailable()) {
            expected
        }

        val response = mvc.get("/api/v1/campaign/list/available")
                .andExpect(status.isOk)
                .andReturn().response.contentAsString

        assertEquals(expected.wrapper, response)


        //check cache
        Mockito.verify(redis).get(CAMPAIGN_LIST_AVAILABLE)
        Mockito.verify(redis).set(CAMPAIGN_LIST_AVAILABLE, expected.toJsonString(), nextHour)

    }


    @Test
    fun `list expired campaigns with caching`() {

        //setup query
        val date = randomInstant
        val expected = listOf(
                Campaign(title = randomStr, startTime = randomInstant, endTime = date + Duration.ofDays(1), candidates = emptyList()),
                Campaign(title = randomStr, startTime = randomInstant, endTime = date, candidates = (1..3).map { Candidate(randomStr, randomId) }),
                Campaign(title = randomStr, startTime = randomInstant, endTime = date + Duration.ofDays(3), candidates = emptyList())
        ).sortedByDescending { it.endTime }
        method(repo.listExpired()) {
            expected
        }

        val response = mvc.get("/api/v1/campaign/list/expired")
                .andExpect(status.isOk)
                .andReturn().response.contentAsString

        assertEquals(expected.wrapper, response)

        //check cache
        Mockito.verify(redis).get(CAMPAIGN_LIST_EXPIRED)
        Mockito.verify(redis).set(CAMPAIGN_LIST_EXPIRED, expected.toJsonString(), nextHour)
    }


    @Test
    fun `real-time counting votes with caching`() {
        //setup query
        val nonCached = VoteCount(randomId, randomLong)
        method(repo.getVoteCount(listOf(nonCached.candidateId))) {
            listOf(nonCached)
        }

        //setup cache
        val cached = VoteCount(randomId, randomLong)
        method(redis.hGet(CacheKey.CANDIDATE_COUNT, cached.candidateId)) {
            cached.count.toString()
        }

        val expected = listOf(cached, nonCached)
        val response = mvc.post("/api/v1/campaign/count", expected.map { it.candidateId })
                .andExpect(status.isOk)
                .andReturn().response.contentAsString

        assertEquals(expected.wrapper, response)

        //load cache
        expected.forEach {
            Mockito.verify(redis).hGet(CacheKey.CANDIDATE_COUNT, it.candidateId)
        }

        //save cache
        Mockito.verify(redis).inc(CacheKey.CANDIDATE_COUNT, nonCached.candidateId, nonCached.count)

    }

    @Test
    fun `list votes by HK ID`() {

        //invalid HK ID
        val invalid = mvc.get("/api/v1/campaign/list-vote", mapOf("hkId" to randomStr))
                .andExpect(status.is4xxClientError)
                .andReturn()

        assertTrue(invalid.resolvedException is UnprocessableException)


        val hkId = "M6884593"
        val expected = (1..10).map { Vote(randomId, randomId, privacyHash(hkId), randomInstant) }

        //setup query
        method(repo.listVote(privacyHash(hkId))) {
            expected
        }

        val response = mvc.get("/api/v1/campaign/list-vote", mapOf("hkId" to hkId))
                .andExpect(status.isOk)
                .andReturn().response.contentAsString

        assertEquals(expected.wrapper, response)

        //check caching
        Mockito.verify(redis).get(LIST_VOTE + privacyHash(hkId))
        Mockito.verify(redis).setex(LIST_VOTE + privacyHash(hkId), expected.toJsonString(), Duration.ofHours(1))


        //setup cache
        val expectedCache = expected.map { it.copy(candidateId = randomStr) }
        method(redis.get(LIST_VOTE + privacyHash(hkId))) {
            expectedCache.toJsonString()
        }

        val responseCache = mvc.get("/api/v1/campaign/list-vote", mapOf("hkId" to hkId))
                .andExpect(status.isOk)
                .andReturn().response.contentAsString

        assertEquals(expectedCache.wrapper, responseCache)

    }

    @Test
    fun `submit a vote`() {
        //validate HK ID
        val invalidHkId = mvc.post("/api/v1/campaign/vote", CampaignController.Voting(randomId, randomId, randomStr))
                .andExpect(status.is4xxClientError)
                .andReturn()

        assertTrue(invalidHkId.resolvedException is UnprocessableException)
        assertEquals(ResponseWrapper(null, 422, INVALID_HK_ID).toJsonString(), invalidHkId.response.contentAsString)


        val voting = CampaignController.Voting(randomId, randomId, "WO790393A")
        val campaign = Campaign(randomStr, randomPast, randomFuture, listOf(Candidate(randomStr, id = voting.candidateId)), id = voting.campaignId)

        Mockito.`when`(repo.singleOrNull(voting.campaignId))
                .thenReturn(
                        //null campaign
                        null,
                        //campaign exists but expired
                        campaign.copy(endTime = randomPast),
                        //campaign exists but wrong candidate
                        campaign.copy(candidates = emptyList()),
                        //valid campaign and candidate
                        campaign
                )

        val invalidCampaign = mvc.post("/api/v1/campaign/vote", voting)
                .andExpect(status.is4xxClientError)
                .andReturn()
        assertTrue(invalidCampaign.resolvedException is UnprocessableException)
        assertEquals(ResponseWrapper(null, 422, INVALID_CAMPAIGN).toJsonString(), invalidCampaign.response.contentAsString)

        val expiredCampaign = mvc.post("/api/v1/campaign/vote", voting)
                .andExpect(status.is4xxClientError)
                .andReturn()
        assertTrue(expiredCampaign.resolvedException is UnprocessableException)
        assertEquals(ResponseWrapper(null, 422, INVALID_CAMPAIGN).toJsonString(), expiredCampaign.response.contentAsString)

        val invalidCandidate = mvc.post("/api/v1/campaign/vote", voting)
                .andExpect(status.is4xxClientError)
                .andReturn()
        assertTrue(invalidCandidate.resolvedException is UnprocessableException)
        assertEquals(ResponseWrapper(null, 422, INVALID_CANDIDATE).toJsonString(), invalidCandidate.response.contentAsString)

        val result = mvc.post("/api/v1/campaign/vote", voting)
                .andExpect(status.isOk)
                .andReturn().response.contentAsString

        assertEquals(emptyResponse, result)

        //save campaign to cache
        Mockito.verify(redis).setex(CacheKey.CAMPAIGN_SINGLE + voting.campaignId, campaign.toJsonString(), Duration.ofHours(1))


        //check load campaign from cache
        method(redis.get(CacheKey.CAMPAIGN_SINGLE + voting.campaignId)) {
            Campaign(randomStr, randomPast, randomFuture, emptyList(), id = voting.campaignId).toJsonString()
        }
        mvc.post("/api/v1/campaign/vote", voting)
                .andExpect(status.is4xxClientError)
                .andReturn()


    }

}
