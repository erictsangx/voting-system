package com.hk.voting.controllers

import com.hk.voting.exceptions.UnprocessableException
import com.hk.voting.models.Campaign
import com.hk.voting.models.VoteCount
import com.hk.voting.services.CampaignRepo
import io.swagger.annotations.*
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/api/v1/campaign")
class AdminCampaignController @Autowired constructor(val campaignRepo: CampaignRepo) {

    companion object {
        private const val INVALID_DATE = "startTime > endTime"
    }

    @ApiOperation("create a campaign")
    @ApiImplicitParams(*[ApiImplicitParam(name = "JWT", value = "token", paramType = "header")])
    @ApiResponses(value = [ApiResponse(code = 422, message = INVALID_DATE)])
    @Secured(*["ROLE_ADMIN", "ROLE_EDITOR"])
    @PostMapping("/create")
    fun create(@RequestBody campaign: Campaign): Campaign {

        if (campaign.startTime > campaign.endTime) {
            throw UnprocessableException(INVALID_DATE)
        }
        //sanitize id
        val sanitized = campaign.copy(
                id = null,
                candidates = campaign.candidates.map { it.copy(id = ObjectId().toString()) }
        )
        return campaignRepo.create(sanitized)
    }

    @ApiOperation("Count all votes of a campaign and update the corresponding vote count")
    @ApiImplicitParams(*[ApiImplicitParam(name = "JWT", value = "token", paramType = "header")])
    @GetMapping("/confirm-vote-count")
    @Secured(*["ROLE_ADMIN", "ROLE_EDITOR"])
    fun confirmVoteCount(@RequestParam campaignId: String) {
        val campaign = campaignRepo.singleOrNull(campaignId) ?: return
        campaign.candidates.map {
            VoteCount(it.id, campaignRepo.countVote(it.id))
        }.forEach {
            campaignRepo.upsertVoteCount(it)
        }
    }
}
