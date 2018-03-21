package io.github.snrostov.kotlin.react.ide

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import org.jetbrains.kotlin.idea.framework.JSLibraryKind

class KotlinReactIdeTestCaseProjectDescriptor : DefaultLightProjectDescriptor() {
  override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
    addNodeJsKotlinJsLibrary(model, "@jetbrains/kotlin-extensions")
    addNodeJsKotlinJsLibrary(model, "@jetbrains/kotlin-react")
    addNodeJsKotlinJsLibrary(model, "@jetbrains/kotlin-react-dom")
  }

  private fun addNodeJsKotlinJsLibrary(model: ModifiableRootModel, packageId: String) {
    addLibrary(model, "src/test/testEnv/node_modules/$packageId", JSLibraryKind)
  }

  private fun addLibrary(model: ModifiableRootModel, libPath: String, kind: PersistentLibraryKind<*>): Library {
    val root =
      LocalFileSystem.getInstance().refreshAndFindFileByPath(libPath) ?: error("Cannot find `$libPath`")
    val libName = root.name
    val libraryTable = ProjectLibraryTable.getInstance(model.project)

    return WriteAction.computeAndWait<Library, RuntimeException> {
      libraryTable.modifiableModel.createLibrary(libName, kind).also { library ->
        library.modifiableModel.write {
          addRoot(root, OrderRootType.CLASSES)
        }

        model.addLibraryEntry(library)
      }
    }
  }
}

fun Library.ModifiableModel.write(actions: Library.ModifiableModel.() -> Unit) {
  try {
    actions()
    commit()
  } catch (t: Throwable) {
    dispose()
    throw t
  }
}