import com.moowork.gradle.node.yarn.YarnTask

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.2.30"
  id("org.jetbrains.intellij") version "0.2.19"
  id("com.moowork.node") version "1.2.0"
}

group = "io.github.snrostov"
version = "0.1-SNAPSHOT"

intellij {
  version = "182-SNAPSHOT"
  setPlugins("org.jetbrains.kotlin:1.2.40-dev-1021-IJ2018.2-1@ideadev")
}

task("testsYarnInstall", type = YarnTask::class) {
  setWorkingDir(file("${project.projectDir}/src/test/testEnv"))
}

tasks {
  "test" {
    dependsOn("testsYarnInstall")
  }
}