package io.github.snrostov.kotlin.react.ide.insepctions

import io.github.snrostov.kotlin.react.ide.KotlinReactIdeTestCase
import org.junit.Test


class RComponentInspectionTest : KotlinReactIdeTestCase() {
  override fun getTestDataPath() =
    "src/test/testData/insepctions"

  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(RComponentInspection::class.java)
  }

  @Test
  fun test000() {
    myFixture.testHighlighting("test000.kt")
  }
}