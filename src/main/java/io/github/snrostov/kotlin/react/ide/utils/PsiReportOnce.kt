package io.github.snrostov.kotlin.react.ide.utils

import com.intellij.psi.PsiElement

inline fun PsiElement?.reportOnce(block: (PsiElement) -> Unit) {
  if (this != null) {
//    if (getUserData(ALREADY_REPORTED) == null) {
//      putUserData(ALREADY_REPORTED, true)
    // todo(how?) reportOnce
    block(this)
//    }
  }
}

//val ALREADY_REPORTED = Key<Boolean>("ALREADY_REPORTED")
