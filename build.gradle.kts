plugins {
  id("org.jetbrains.intellij.platform") version "2.2.0"
  kotlin("jvm") version "2.1.0"
}

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    phpstorm("2024.3.1")
    bundledPlugin("com.jetbrains.php")
  }
}

intellijPlatform {
  pluginConfiguration {
    version = "1.0"
    description = "Small plugin to help with GQL Loader"
    vendor {
      name = "Mikify"
      email = "contact@mikify.dev"
      url = "https://mikify.dev"
    }

    ideaVersion {
      sinceBuild = "243"
      untilBuild = "243.*"
    }
  }

  signing {
    privateKey = System.getenv("PRIVATE_KEY")
    password = System.getenv("PRIVATE_KEY_PASSWORD")
    certificateChain = System.getenv("CERTIFICATE_CHAIN")
  }

  publishing {
    token = System.getenv("PUBLISH_TOKEN")
  }
}

