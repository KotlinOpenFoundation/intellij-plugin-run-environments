package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

val Project.runEnvironments: RunEnvironmentsService
  get() = service()
