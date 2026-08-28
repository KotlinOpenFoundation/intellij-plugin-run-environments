package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend

import com.intellij.platform.project.ProjectId
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentsApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory stand-in for the backend's [EnvironmentsApi], so [RunEnvironmentsService] can be
 * tested against it directly instead of the real split-mode RPC connection, which a plain unit
 * test can't form (confirmed by timing out against the real [com.intellij.platform.rpc.RemoteApiProviderService]).
 *
 * Pushed values (the three flows) are driven by mutating the corresponding [MutableStateFlow]
 * directly; calls the service makes (selection, file creation/opening) land on the corresponding
 * channel for the test to [kotlinx.coroutines.channels.ReceiveChannel.receive].
 */
class FakeEnvironmentsApi : EnvironmentsApi {
  val environments = MutableStateFlow<List<EnvironmentInfo>>(emptyList())
  val injectionSupported = MutableStateFlow(false)
  val missingFiles = MutableStateFlow<List<EnvironmentFileKind>>(emptyList())

  val selectedKeys: Channel<String?> = Channel(Channel.UNLIMITED)
  val createdFiles: Channel<EnvironmentFileKind> = Channel(Channel.UNLIMITED)
  val openedPaths: Channel<String> = Channel(Channel.UNLIMITED)

  override suspend fun getEnvironments(projectId: ProjectId): Flow<List<EnvironmentInfo>> = environments

  override suspend fun setSelectedEnvironment(projectId: ProjectId, key: String?) {
    selectedKeys.send(key)
  }

  override suspend fun isInjectionSupported(projectId: ProjectId): Flow<Boolean> = injectionSupported

  override suspend fun getMissingEnvironmentFiles(projectId: ProjectId): Flow<List<EnvironmentFileKind>> = missingFiles

  override suspend fun createEnvironmentFile(projectId: ProjectId, kind: EnvironmentFileKind) {
    createdFiles.send(kind)
  }

  override suspend fun openEnvironmentFile(projectId: ProjectId, path: String) {
    openedPaths.send(path)
  }
}
