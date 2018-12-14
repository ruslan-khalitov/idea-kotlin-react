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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.20-eap-25"
    id("org.jetbrains.intellij") version "0.3.12"
    id("com.moowork.node") version "1.2.0"
}

group = "io.github.snrostov"
version = "0.1-SNAPSHOT"

intellij {
    version = "183-SNAPSHOT"
    setPlugins("org.jetbrains.kotlin:1.3.20-eap-25-IJ2018.3-1@eap")
}

task("testsYarnInstall", type = YarnTask::class) {
    setWorkingDir(file("${project.projectDir}/src/test/testEnv"))
}

repositories {
    jcenter()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://oss.sonatype.org/content/repositories/releases/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

tasks {
    "test" {
        dependsOn("testsYarnInstall")
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}