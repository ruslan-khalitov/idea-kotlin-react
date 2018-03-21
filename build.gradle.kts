/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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