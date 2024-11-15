package io.github.FlyJingFish.AndroidAopPlugin.common

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import io.github.FlyJingFish.AndroidAopPlugin.openclassfiles._internal.ClassFilesFinderService
import io.github.FlyJingFish.AndroidAopPlugin.openclassfiles._internal.ClassFilesFinderService.Result
import org.objectweb.asm.ClassReader

@Service(Service.Level.PROJECT)
class ByteCodeAnalyserOpenClassFileService(val project: Project) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun openPsiFiles(psiFiles: List<PsiFile>) {
    run { service -> service.findByPsiFiles(psiFiles) }
  }

  fun openPsiElements(psiElementToPsiElementOriginPsiFile: Map<PsiElement, PsiFile?>) {
    run { service -> service.findByPsiElements(psiElementToPsiElementOriginPsiFile) }
  }

  fun openVirtualFiles(files: List<VirtualFile>) {
    run { service -> service.findByVirtualFiles(files) }
  }

  internal fun openClassFiles(classFiles: List<ClassFile>) {
    run { service -> service.findByClassFiles(classFiles) }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun run(findBy: (ClassFilesFinderService) -> Result) {
    val result = findBy(project.getService(ClassFilesFinderService::class.java))
    handleResult(result)
  }

  private fun handleResult(result: Result) {
    if (result.classFilesToOpen.isEmpty()){
      Messages.showWarningDialog(
        project,
        "实在是无法解析该class喽～",
        "AndroidAOP Code Viewer"
      )
    }else{
      result.classFilesToOpen.forEach {
        openClassFile(it, project)
      }
    }
  }

  fun getClassNameFromClassFile(file: VirtualFile): String {
    val inputStream = file.inputStream
    val classReader = ClassReader(inputStream)
    return classReader.className
  }
  fun openClassFile(classFile: ClassFile, project: Project) {
    val clazz = Class.forName("io.github.FlyJingFish.AndroidAopPlugin.action.ClassFileLocationKt")
    val method = clazz.getDeclaredMethod("show",String::class.java, VirtualFile::class.java,Project::class.java)
    method.isAccessible = true
    method.invoke(null,getClassNameFromClassFile(classFile.file).replace("/","."),classFile.file,project)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

}