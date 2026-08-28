package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class MergeEnvironmentContributionsTest {
  private val rootQualifier = "<root>"

  @Test
  fun `keeps a single-source environment's variables while clearing its qualifier`() {
    val contributions =
      listOf(EnvironmentInfo("prod", qualifier = null, variables = mapOf("HOST" to "prod.example.com")))

    assertEquals(
      listOf(EnvironmentInfo("prod", variables = mapOf("HOST" to "prod.example.com"))),
      EnvironmentAggregatorService.mergeEnvironmentContributions(contributions, rootQualifier)
    )
  }

  @Test
  fun `keeps distinct qualifiers, and their variables, when two sources disagree on the same name`() {
    val contributions = listOf(
      EnvironmentInfo("dev", qualifier = "app", variables = mapOf("HOST" to "app.example.com")),
      EnvironmentInfo("dev", qualifier = "order-service", variables = mapOf("HOST" to "order.example.com"))
    )

    assertEquals(
      contributions.toSet(),
      EnvironmentAggregatorService.mergeEnvironmentContributions(contributions, rootQualifier).toSet()
    )
  }

  @Test
  fun `deduplicates identical contributions before merging`() {
    val contribution = EnvironmentInfo("staging", variables = mapOf("HOST" to "staging.example.com"))

    assertEquals(
      listOf(contribution),
      EnvironmentAggregatorService.mergeEnvironmentContributions(listOf(contribution, contribution), rootQualifier)
    )
  }

  @Test
  fun `falls back to the root qualifier for an unqualified contribution that collides with a qualified one`() {
    val contributions = listOf(
      EnvironmentInfo("dev", qualifier = null, variables = mapOf("HOST" to "root.example.com")),
      EnvironmentInfo("dev", qualifier = "order-service", variables = mapOf("HOST" to "order.example.com"))
    )

    assertEquals(
      setOf(
        EnvironmentInfo("dev", qualifier = rootQualifier, variables = mapOf("HOST" to "root.example.com")),
        EnvironmentInfo("dev", qualifier = "order-service", variables = mapOf("HOST" to "order.example.com"))
      ),
      EnvironmentAggregatorService.mergeEnvironmentContributions(contributions, rootQualifier).toSet()
    )
  }
}
