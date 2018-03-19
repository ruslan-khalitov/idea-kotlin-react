package io.github.snrostov.kotlin.react.ide.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parents
import org.jetbrains.kotlin.psi.KtFile

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

fun firstCommonParent(a: PsiElement?, b: PsiElement?): PsiElement? {
  if (a == null || b == null) return null
  var prev: PsiElement? = null
  val ap = a.parents().takeWhile { it !is KtFile }.toList().reversed()
  val bp = b.parents().takeWhile { it !is KtFile }.toList().reversed()
  ap.forEachIndexed { index, psiElement ->
    if (bp[index] != psiElement) return prev
    prev = psiElement
  }
  return prev
}