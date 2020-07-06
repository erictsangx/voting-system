package com.hk.voting.config

import com.hk.voting.utility._mapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2


@Configuration
@EnableScheduling
@EnableSwagger2
class WebConfig : WebMvcConfigurationSupport() {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(VoidResponseInterceptor())
    }

    //use customized JsonMapper
    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.add(object : AbstractJackson2HttpMessageConverter(_mapper, MediaType.APPLICATION_JSON) {})
    }

    //swagger
    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(false)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.hk.voting"))
                .paths(PathSelectors.any())
                .build()
    }

    // fix using WebMvcConfigurationSupport will override swagger2 config
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/")
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/")
    }


}
