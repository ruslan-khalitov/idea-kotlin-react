plugins {
  id("org.jetbrains.kotlin.jvm") version "1.2.30"
  id("org.jetbrains.intellij") version "0.2.19"
}

group = "io.github.snrostov"
version = "0.1-SNAPSHOT"

intellij {
//  version = "181.4203.6"
}

dependencies {
  compileOnly(files("lib/kotlin-idea-plugin-internals/formatter-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/ide-common-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/idea-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/idea-android-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/idea-core-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/idea-gradle-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/idea-jps-common-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/idea-jvm-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/idea-maven-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/idea-test-framework-1.2-SNAPSHOT-tests.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/kotlin-gradle-tooling-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/psi-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/descriptors-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/frontend-1.2-SNAPSHOT.jar"))
  compileOnly(files("lib/kotlin-idea-plugin-internals/resolution-1.2-SNAPSHOT.jar"))
}

task("configureIde", type = Copy::class) {
  from("/Users/sergey/p/idea-kotlin-react/lib/action.script")
  into("/Users/sergey/p/idea-kotlin-react/build/idea-sandbox/system/plugins")
}

tasks {
  "runIde" {
    dependsOn("configureIde")
  }
}

