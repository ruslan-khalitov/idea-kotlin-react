package io.github.snrostov.kotlin.react.ide.insepctions

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.junit.Test


class RComponentBuilderExpressionsInspectionTest : LightCodeInsightFixtureTestCase() {
  override fun getTestDataPath() =
    "src/test/resources/io/github/snrostov/kotlin/react/ide/insepctions/builder"

  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(RComponentInspection::class.java)
  }

  @Test
  fun test() {
    myFixture.testHighlighting("test.kt")
  }
}