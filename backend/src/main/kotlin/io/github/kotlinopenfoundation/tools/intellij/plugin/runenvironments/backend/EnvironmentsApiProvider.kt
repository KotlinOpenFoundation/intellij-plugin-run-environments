package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.platform.rpc.backend.RemoteApiProvider
import fleet.rpc.remoteApiDescriptor
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentsApi

internal class EnvironmentsApiProvider : RemoteApiProvider {
  override fun RemoteApiProvider.Sink.remoteApis() {
    remoteApi(remoteApiDescriptor<EnvironmentsApi>()) {
      BackendEnvironmentsApi()
    }
  }
}
