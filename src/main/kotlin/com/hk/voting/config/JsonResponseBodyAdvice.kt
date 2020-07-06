package com.hk.voting.config

import com.hk.voting.models.ResponseWrapper
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import java.io.File

/**
 * Standardize all responses
 * Only Wrapper com.hk.voting.*
 * Avoid conflicting swagger2
 */
@ControllerAdvice(basePackages = ["com.hk.voting"])
class JsonResponseBodyAdvice : ResponseBodyAdvice<Any> {
    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
        return true
    }

    override fun beforeBodyWrite(body: Any?, returnType: MethodParameter,
                                 selectedContentType: MediaType,
                                 selectedConverterType: Class<out HttpMessageConverter<*>>,
                                 request: ServerHttpRequest, response: ServerHttpResponse): Any? {
        return when (body) {
            is Byte, is File -> body
            is ResponseWrapper -> {
                body
            }
            else -> {
                ResponseWrapper(body, 200, "")
            }
        }
    }

}
