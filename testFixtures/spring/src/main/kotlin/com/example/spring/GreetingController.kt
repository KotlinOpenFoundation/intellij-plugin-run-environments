package com.example.spring

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/greeting")
class GreetingController(
  private val config: ApplicationProperties
) {
  @GetMapping("")
  fun greet(): ResponseEntity<Greeting> {
    return greetByName(config.defaultGreeting)
  }

  @GetMapping("/{name}")
  fun greetByName(
    @PathVariable name: String
  ): ResponseEntity<Greeting> {
    return ResponseEntity.ok(Greeting("Hello, $name!"))
  }
}
