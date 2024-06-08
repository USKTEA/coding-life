package com.security.template.filter

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.security.web.context.DelegatingSecurityContextRepository
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

class CustomAuthenticateFilter(httpSecurity: HttpSecurity, manager: AuthenticationManager) :
    AbstractAuthenticationProcessingFilter(
        AntPathRequestMatcher("/api/login")
    ) {
    init {
        setSecurityContextRepository(
            getSecurityContextRepository(httpSecurity)
        )
        authenticationManager = manager
    }

    private fun getSecurityContextRepository(httpSecurity: HttpSecurity): SecurityContextRepository {
        val securityContextRepository = httpSecurity.getSharedObject(SecurityContextRepository::class.java)

        if (securityContextRepository == null) {
            return DelegatingSecurityContextRepository(
                HttpSessionSecurityContextRepository(), RequestAttributeSecurityContextRepository()
            )
        }

        return securityContextRepository
    }

    override fun attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse): Authentication {
        val username = request.getParameter("username")
        val password = request.getParameter("password")

        return authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
    }
}