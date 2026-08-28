package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.openapi.components.service
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.findProjectOrNull
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentsApi
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.nio.file.Path

class BackendEnvironmentsApi : EnvironmentsApi {
  override suspend fun getEnvironments(projectId: ProjectId): Flow<List<EnvironmentInfo>> {
    val project = projectId.findProjectOrNull() ?: return emptyFlow()
    return project.service<EnvironmentAggregatorService>().environments
  }

  override suspend fun setSelectedEnvironment(projectId: ProjectId, key: String?) {
    val project = projectId.findProjectOrNull() ?: return
    project.service<SelectedEnvironmentService>().selectedKey = key
  }

  override suspend fun isInjectionSupported(projectId: ProjectId): Flow<Boolean> {
    val project = projectId.findProjectOrNull() ?: return emptyFlow()
    return project.service<SelectedRunConfigurationSupportService>().isSupportedFlow
  }

  override suspend fun getMissingEnvironmentFiles(projectId: ProjectId): Flow<List<EnvironmentFileKind>> {
    val project = projectId.findProjectOrNull() ?: return emptyFlow()
    return project.service<EnvironmentFileService>().missingFiles
  }

  override suspend fun createEnvironmentFile(projectId: ProjectId, kind: EnvironmentFileKind) {
    val project = projectId.findProjectOrNull() ?: return
    project.service<EnvironmentFileService>().createFile(kind)
  }

  override suspend fun openEnvironmentFile(projectId: ProjectId, path: String) {
    val project = projectId.findProjectOrNull() ?: return
    project.service<EnvironmentFileService>().openFile(Path.of(path))
  }
}
