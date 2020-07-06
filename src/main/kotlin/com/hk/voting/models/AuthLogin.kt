package com.hk.voting.models

import io.swagger.annotations.ApiModelProperty

data class AuthLogin(
        @ApiModelProperty(example = "admin")
        val username: String,
        @ApiModelProperty(example = "adminPass")
        val password: String
)
