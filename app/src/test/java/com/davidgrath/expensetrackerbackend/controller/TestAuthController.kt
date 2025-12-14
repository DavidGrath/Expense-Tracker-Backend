package com.davidgrath.expensetrackerbackend.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestAuthController {

    @GetMapping("/test")
    fun tempEndpoint(): ResponseEntity<Unit> {
        return ResponseEntity.ok(Unit)
    }
}