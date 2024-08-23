/*
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public
 License as published by the Free Software Foundation; either
 version 2 of the license, or (at your option) any later version.
 */

package io.github.FlyJingFish.AndroidAopPlugin.action

import com.intellij.byteCodeViewer.ByteCodeViewerManager
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.util.JavaAnonymousClassesHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import io.github.FlyJingFish.AndroidAopPlugin.common.Constants
import io.github.FlyJingFish.AndroidAopPlugin.common.FileTypeExtension
import io.github.FlyJingFish.AndroidAopPlugin.config.AOPPluginComponent
import io.github.FlyJingFish.AndroidAopPlugin.util.AndroidAOPCode
import io.github.FlyJingFish.AndroidAopPlugin.view.MatchView
import io.github.FlyJingFish.AndroidAopPlugin.view.MatchViewKt
import io.github.FlyJingFish.AndroidAopPlugin.view.ReplaceView
import io.github.FlyJingFish.AndroidAopPlugin.view.ReplaceViewKt
import org.objectweb.asm.ClassReader
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private class LocationResult private constructor(val locatedClassFile: LocatedClassFile?, val errorMessage: String?) {
    companion object {
        fun of(locatedClassFile: LocatedClassFile) = LocationResult(locatedClassFile, null)
        fun of(errorMessage: String) = LocationResult(null, errorMessage)
    }
}

data class LocatedClassFile(val jvmClassName: String, val virtualFile: VirtualFile, var writableUrl: String? = null)

fun isContainedInClass(psiElement: PsiElement) : Boolean {
    val containingClass = getContainingClass(psiElement)
    return if (containingClass != null) {
        getJVMClassName(containingClass) != null
    } else {
        false
    }
}

fun openClassFile(psiElement: PsiElement, project: Project) {
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Locating class file ...") {
        var locationResult: LocationResult? = null

        override fun run(indicator: ProgressIndicator) {
            locationResult = ApplicationManager.getApplication().runReadAction(Computable {
                try {
                    val result = locateClassFile(psiElement)
                    result
                } catch (e: Exception) {
                    LocationResult.of("Class file could not be found" + (if (e.message.isNullOrBlank()) "" else ": " + e.message))
                }
            })
        }

        override fun onSuccess() {
            val locatedClassFile = locationResult?.locatedClassFile
            if (locatedClassFile != null) {
                updateToolWindowContents(project,locatedClassFile)
            } else {
                if (!project.isDisposed) {
                    Messages.showWarningDialog(
                        project,
                        locationResult?.errorMessage ?: "internal error",
                        "Jclasslib Bytecode Viewer"
                    )
                }
            }
        }
    })
}

private fun readClassFile(locatedClassFile: LocatedClassFile, project: Project): ByteArray? {
    locatedClassFile.virtualFile.refresh(false, false)
    return try {
        loadClassFileBytes(locatedClassFile, project)
    } catch (e: Exception) {
        null
    }
}

fun loadClassFileBytes(locatedClassFile: LocatedClassFile, project: Project): ByteArray =
    if (FileTypeRegistry.getInstance().isFileOfType(locatedClassFile.virtualFile, JavaClassFileType.INSTANCE)) {
        loadCompiledClassFileBytes(locatedClassFile, project)
    } else {
        loadSourceClassFileBytes(locatedClassFile, project)
    }

private fun loadCompiledClassFileBytes(locatedClassFile: LocatedClassFile, project: Project): ByteArray {
    val index = ProjectFileIndex.getInstance(project)
    val file = locatedClassFile.virtualFile
    val classFileName = StringUtil.getShortName(locatedClassFile.jvmClassName) + ".class"
    return if (index.isInLibraryClasses(file)) {
        val classFile = file.parent.findChild(classFileName)
        classFile?.contentsToByteArray(false).also {
            locatedClassFile.writableUrl = classFile?.url
        } ?: throw IOException("Class file not found")
    } else {
        val classFile = File(file.parent.path, classFileName)
        if (classFile.isFile) {
            locatedClassFile.writableUrl = classFile.toURI().path
            FileUtil.loadFileBytes(classFile)
        } else {
            throw IOException("Class file not found")
        }
    }
}

private fun loadSourceClassFileBytes(locatedClassFile: LocatedClassFile, project: Project): ByteArray {
    val index = ProjectRootManager.getInstance(project).fileIndex
    val module = index.getModuleForFile(locatedClassFile.virtualFile) ?: throw IOException("Module not found")

    val relativePath = locatedClassFile.jvmClassName.replace('.', '/') + ".class"
    for (path in CompilerPaths.getOutputPaths(arrayOf(module)).toList()) {
        val classFile = File(path, relativePath)
        if (classFile.exists()) {
            locatedClassFile.writableUrl = classFile.toURI().path
            return FileUtil.loadFileBytes(classFile)
        }
    }
    throw IOException("Class file not found")
}

private fun updateToolWindowContents(project: Project, locatedClassFile: LocatedClassFile?) {
    ApplicationManager.getApplication().runWriteAction {
        val replaceView = ReplaceView.getInstance(project)
        val replaceViewKt = ReplaceViewKt.getInstance(project)
        val matchView = MatchView.getInstance(project)
        val matchViewKt = MatchViewKt.getInstance(project)
        val toolWindowManager = ToolWindowManager.getInstance(project)

        val file:VirtualFile? = locatedClassFile?.virtualFile
        if (file == null) {
            replaceView.setCode(file, Constants.NO_CLASS_FOUND)
            replaceViewKt.setCode(file, Constants.NO_CLASS_FOUND)
            matchView.setCode(file, Constants.NO_CLASS_FOUND)
            matchViewKt.setCode(file, Constants.NO_CLASS_FOUND)
            toolWindowManager.getToolWindow(Constants.PLUGIN_WINDOW_NAME)!!
                .activate(null)
            return@runWriteAction
        } else {
            Logger.getInstance(ShowAopCodeViewerAction::class.java)
                .warn("file $file")
        }




        var reader: ClassReader? = null
        try {
            reader = ClassReader(readClassFile(locatedClassFile,project))
        } catch (e: Exception) {
            return@runWriteAction
        }
        val applicationConfig = AOPPluginComponent.getApplicationConfig()


        //            reader.accept(new TraceClassVisitor(new PrintWriter(stringWriter)), flags);
        val androidAOPCode = AndroidAOPCode(reader)


        val replaceJavaCode =
            androidAOPCode.getReplaceContent(FileTypeExtension.JAVA)
        val psiFile = PsiFileFactory.getInstance(project).createFileFromText(
            Constants.FILE_NAME,
            FileTypeManager.getInstance()
                .getFileTypeByExtension(FileTypeExtension.JAVA.value),
            replaceJavaCode.toString()
        )
        CodeStyleManager.getInstance(project).reformat(psiFile)
        replaceView.setCode(file, psiFile.text)


        val replaceKotlinCode =
            androidAOPCode.getReplaceContent(FileTypeExtension.KOTLIN)
        val psiFileKt = PsiFileFactory.getInstance(project)
            .createFileFromText(
                Constants.FILE_NAME,
                FileTypeManager.getInstance()
                    .getFileTypeByExtension(FileTypeExtension.KOTLIN.value),
                replaceKotlinCode.toString()
            )
        CodeStyleManager.getInstance(project).reformat(psiFileKt)
        replaceViewKt.setCode(file, psiFileKt.text)

        val matchJavaCode = androidAOPCode.getMatchContent(
            FileTypeExtension.JAVA,
            false,
            false,
            false
        )
        val matchPsiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(
                Constants.FILE_NAME,
                FileTypeManager.getInstance()
                    .getFileTypeByExtension(FileTypeExtension.JAVA.value),
                matchJavaCode.toString()
            )
        CodeStyleManager.getInstance(project).reformat(matchPsiFile)
        matchView.setCode(file, matchPsiFile.text)

        val matchKotlinCode = androidAOPCode.getMatchContent(
            FileTypeExtension.KOTLIN,
            false,
            false,
            false
        )
        val matchPsiFileKt = PsiFileFactory.getInstance(project)
            .createFileFromText(
                Constants.FILE_NAME,
                FileTypeManager.getInstance()
                    .getFileTypeByExtension(FileTypeExtension.KOTLIN.value),
                matchKotlinCode.toString()
            )
        CodeStyleManager.getInstance(project).reformat(matchPsiFileKt)
        matchViewKt.setCode(file, matchPsiFileKt.text)
        toolWindowManager.getToolWindow(Constants.PLUGIN_WINDOW_NAME)!!
            .activate(null)
    }
}

private fun locateClassFile(psiElement: PsiElement): LocationResult {
    val containingClass = getContainingClass(psiElement) ?: throw FileNotFoundException("<containing class>")
    val jvmClassName = getJVMClassName(containingClass) ?: throw FileNotFoundException("<class name>")
    val virtualFile = getFileClass(containingClass).originalElement.containingFile.virtualFile
    return LocationResult.of(LocatedClassFile(jvmClassName, virtualFile))
}

private tailrec fun getFileClass(c: PsiClass): PsiClass =
    if (!PsiUtil.isLocalOrAnonymousClass(c)) {
        c
    } else {
        val containingClass = PsiTreeUtil.getParentOfType(c, PsiClass::class.java)
        if (containingClass == null) {
            c
        } else {
            getFileClass(containingClass)
        }
    }

private fun getContainingClass(psiElement: PsiElement): PsiClass? {
    val byteCodeViewerPlugin = PluginManagerCore.getPlugin(PluginId.getId("ByteCodeViewer"))
    return if (byteCodeViewerPlugin != null && byteCodeViewerPlugin.isEnabled) {
        ByteCodeViewerManager.getContainingClass(psiElement)
    } else {
        val containingClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass::class.java, false)
        if (containingClass is PsiTypeParameter) {
            getContainingClass(containingClass)
        } else {
            containingClass
        }
    }
}

private fun getJVMClassName(containingClass: PsiClass): String? {
    return if (containingClass is PsiAnonymousClass) {
        val containingClassOfAnonymous = PsiTreeUtil.getParentOfType(containingClass, PsiClass::class.java) ?: return null
        getJVMClassName(containingClassOfAnonymous) + JavaAnonymousClassesHelper.getName(containingClass)
    } else {
        ClassUtil.getJVMClassName(containingClass)
    }
}
