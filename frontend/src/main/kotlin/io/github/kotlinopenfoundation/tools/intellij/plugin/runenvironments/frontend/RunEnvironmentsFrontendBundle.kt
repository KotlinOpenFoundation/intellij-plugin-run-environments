package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.RunEnvironmentsFrontendBundle"

object RunEnvironmentsFrontendBundle : DynamicBundle(BUNDLE) {
  @JvmStatic
  fun message(
    @PropertyKey(resourceBundle = BUNDLE) key: String,
    vararg params: Any
  ): String =
    getMessage(key, *params)
}
