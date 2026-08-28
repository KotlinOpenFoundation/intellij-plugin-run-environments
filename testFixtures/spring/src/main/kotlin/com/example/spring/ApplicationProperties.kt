package com.example.spring

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = ApplicationProperties.PREFIX)
data class ApplicationProperties(
  val defaultGreeting: String = "stranger"
) {
  companion object {
    const val PREFIX = "app"
  }
}
