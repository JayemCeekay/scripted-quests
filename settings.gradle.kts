rootProject.name = "parent"
include(":adapter_api")
include(":adapter_unsupported")
include(":adapter_v1_16_R3")
include(":adapter_v1_18_R1")
include(":adapter_v1_18_R2")
include(":ScriptedQuests")
project(":ScriptedQuests").projectDir = file("plugin")

pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://papermc.io/repo/repository/maven-public/")
  }
}
