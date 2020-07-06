package com.hk.voting.config

import org.springframework.http.MediaType
import org.springframework.lang.Nullable
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * Standardize null response
 */
class VoidResponseInterceptor : HandlerInterceptorAdapter() {

    companion object {
        private const val voidResponse = """{"result":{},"code":200,"message":""}"""
        private const val contentType = MediaType.APPLICATION_JSON_VALUE
        private const val characterEncoding = "UTF-8"
    }

    override fun postHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any, @Nullable modelAndView: ModelAndView?) {
        // Returned void?
        if (!response.isCommitted) {
            // Used ModelAndView?
            if (modelAndView != null) {
                return
            }
            // Access static resource?
            if (ResourceHttpRequestHandler::class.java == handler.javaClass) {
                return
            }
            response.status = 200
            response.characterEncoding = characterEncoding
            response.contentType = contentType
            response.writer.use { writer -> writer.write(voidResponse) }
            response.flushBuffer()
        }
    }

}
