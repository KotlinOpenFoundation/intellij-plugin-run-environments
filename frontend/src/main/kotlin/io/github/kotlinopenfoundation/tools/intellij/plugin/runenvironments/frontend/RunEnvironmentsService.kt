package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.platform.project.projectId
import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.client.durable
import fleet.rpc.remoteApiDescriptor
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileLink
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentsApi
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the currently selected run environment for a project, and the live environment list
 * pushed by the backend's [EnvironmentsApi.getEnvironments] over RPC.
 *
 * No polling: [environmentsFlow] just stays subscribed and updates the moment the backend's
 * environment set changes (a `run.env.json` edit, a module added/removed, ...). [durable] retries
 * the subscription automatically if the split-mode connection drops and comes back.
 */
@Service(PROJECT)
@State(name = "RunEnvironmentsState", storages = [Storage("runEnvironments.xml")])
class RunEnvironmentsService @JvmOverloads constructor(
  private val project: Project,
  private val coroutineScope: CoroutineScope,
  /**
   * How the backend's [EnvironmentsApi] is obtained - the real split-mode RPC resolution by
   * default, swappable for a fake in tests, so this service's own coordination logic can be
   * verified deterministically instead of depending on the real backend's file-system state and
   * timing.
   *
   * [JvmOverloads] keeps a plain (project, coroutineScope) constructor for the platform's service
   * instantiation, which doesn't know how to supply this parameter.
   */
  private val resolveApi: suspend () -> EnvironmentsApi = { RemoteApiProviderService.resolve(remoteApiDescriptor<EnvironmentsApi>()) }
) : PersistentStateComponent<RunEnvironmentsState> {
  private var state = RunEnvironmentsState()

  /** Resolved once, lazily, the first time it's needed - an async replacement for `by lazy`. */
  private val api: Deferred<EnvironmentsApi> = coroutineScope.async(start = LAZY) { resolveApi() }

  val environmentsFlow: StateFlow<List<EnvironmentInfo>> = flow {
    durable {
      val resolvedApi = api.await()
      // Re-pushed on every (re)connect, since the backend only keeps the selection in memory and
      // otherwise wouldn't learn it after a backend restart or a split-mode reconnect.
      resolvedApi.setSelectedEnvironment(project.projectId(), state.selectedEnvironment)
      resolvedApi.getEnvironments(project.projectId()).collect { emit(it) }
    }
  }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

  val environments by environmentsFlow::value

  /** Whether the toolbar's currently selected run configuration supports environment injection. */
  val injectionSupportedFlow: StateFlow<Boolean> = flow {
    durable {
      api.await().isInjectionSupported(project.projectId()).collect { emit(it) }
    }
  }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

  val injectionSupported by injectionSupportedFlow::value

  /** The environment file kinds the project doesn't have yet, offered for creation in the combo box. */
  val missingEnvironmentFilesFlow: StateFlow<List<EnvironmentFileKind>> = flow {
    durable {
      api.await().getMissingEnvironmentFiles(project.projectId()).collect { emit(it) }
    }
  }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

  val missingEnvironmentFiles by missingEnvironmentFilesFlow::value

  /** Creates [kind]'s file at the project root and opens it in the editor; backend-side work. */
  fun createEnvironmentFile(kind: EnvironmentFileKind) {
    coroutineScope.launch { api.await().createEnvironmentFile(project.projectId(), kind) }
  }

  /** Opens one of an environment's own files, creating it first when [link] points at a missing one. */
  fun openEnvironmentFile(link: EnvironmentFileLink) {
    coroutineScope.launch { api.await().openEnvironmentFile(project.projectId(), link.path) }
  }

  var selectedEnvironment: String?
    get() = state.selectedEnvironment
    set(value) {
      state.selectedEnvironment = value
      coroutineScope.launch { api.await().setSelectedEnvironment(project.projectId(), value) }
    }

  override fun getState(): RunEnvironmentsState {
    return state
  }

  override fun loadState(state: RunEnvironmentsState) {
    this.state = state
  }
}
