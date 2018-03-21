package io.github.snrostov.kotlin.react.ide

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import io.github.snrostov.kotlin.react.ide.KotlinReactIdeTestCaseProjectDescriptor

open class KotlinReactIdeTestCase : LightCodeInsightFixtureTestCase() {
  override fun getTestDataPath() =
    "src/test/testData/insepctions"

  override fun getProjectDescriptor() = KotlinReactIdeTestCaseProjectDescriptor()
}

