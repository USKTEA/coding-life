package com.jibpractice.jibpractice

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/")
class WelcomeController {

    @GetMapping
    fun welcome(): String {
        return "welcome"
    }
}
