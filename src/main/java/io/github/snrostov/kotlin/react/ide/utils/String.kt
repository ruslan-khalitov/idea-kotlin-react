package io.github.snrostov.kotlin.react.ide.utils

fun String.toLowerCaseFirst() =
  if (isEmpty()) "" else this[0].toLowerCase() + substring(1)
