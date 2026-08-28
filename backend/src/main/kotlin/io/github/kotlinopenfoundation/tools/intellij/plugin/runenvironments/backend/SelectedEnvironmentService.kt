package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.openapi.components.Service
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo

/**
 * Backend-side mirror of the frontend's selected environment ([EnvironmentInfo.key]), pushed over
 * RPC by [BackendEnvironmentsApi.setSelectedEnvironment].
 *
 * Kept in-memory only (not persisted) - the frontend's `RunEnvironmentsService` owns persistence
 * and re-pushes the selection to the backend on every reconnect, since run configurations execute
 * backend-side and need to know which environment's variables to inject.
 */
@Service(PROJECT)
class SelectedEnvironmentService {
  @Volatile
  var selectedKey: String? = null
}
