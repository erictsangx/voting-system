package com.hk.voting.models


/**
 * Wrap all json responses
 * WARNING: return ResponseWrapper directly from Controllers will not change HTTP STATUS CODE!
 */
data class ResponseWrapper(val result: Any?, val code: Int, val message: String)
