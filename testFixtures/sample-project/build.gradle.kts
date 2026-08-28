plugins {
  id("application")
}

// Auto-provisions a matching JDK if none is installed locally - no manual SDK setup needed.
java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
    vendor.set(JvmVendorSpec.ADOPTIUM)
  }
}

application {
  mainClass.set("com.example.app.App")
}
