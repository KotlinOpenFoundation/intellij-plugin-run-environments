plugins {
  id("application")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
    vendor.set(JvmVendorSpec.ADOPTIUM)
  }
}

application {
  mainClass.set("com.example.orderservice.OrderService")
}
