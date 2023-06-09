package project.structure.application.sample

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class SampleRouter {

    @Bean
    fun adminMemberRoute(handler: SampleHandler) = coRouter {
        GET("/", handler::test)
        POST("/sample", handler::create)
    }
}