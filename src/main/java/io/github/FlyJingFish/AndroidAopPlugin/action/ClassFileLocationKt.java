package io.github.FlyJingFish.AndroidAopPlugin.action;


import com.intellij.byteCodeViewer.ByteCodeViewerManager;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.util.JavaAnonymousClassesHelper;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import io.github.FlyJingFish.AndroidAopPlugin.common.Constants;
import io.github.FlyJingFish.AndroidAopPlugin.common.FileTypeExtension;
import io.github.FlyJingFish.AndroidAopPlugin.openclassfiles._internal.AnalyzeByteCodeAction;
import io.github.FlyJingFish.AndroidAopPlugin.util.AndroidAOPCode;
import io.github.FlyJingFish.AndroidAopPlugin.util.CurrentFileUtils;
import io.github.FlyJingFish.AndroidAopPlugin.view.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;

public class ClassFileLocationKt {
    private static class LocatedClassFile {
        String jvmClassName;
        VirtualFile virtualFile;
        String writableUrl;

        public LocatedClassFile(String jvmClassName, VirtualFile virtualFile) {
            this.jvmClassName = jvmClassName;
            this.virtualFile = virtualFile;
        }

        public LocatedClassFile(String jvmClassName, VirtualFile virtualFile, String writableUrl) {
            this.jvmClassName = jvmClassName;
            this.virtualFile = virtualFile;
            this.writableUrl = writableUrl;
        }
    }

    private static class LocationResult {
        LocatedClassFile locatedClassFile;
        String errorMessage;

        public LocationResult(LocatedClassFile locatedClassFile, String errorMessage) {
            this.locatedClassFile = locatedClassFile;
            this.errorMessage = errorMessage;
        }

        static LocationResult of(LocatedClassFile locatedClassFile) {
            return new LocationResult(locatedClassFile, null);
        }

        static LocationResult of(String errorMessage) {
            return new LocationResult(null, errorMessage);
        }
    }


    public static boolean isContainedInClass(PsiElement psiElement) {
        PsiClass containingClass = getContainingClass(psiElement);
        if (containingClass != null) {
            return getJVMClassName(containingClass) != null;
        } else {
            return false;
        }
    }

    public static void openClassFile(AnActionEvent e, PsiElement psiElement, Project project) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Locating class file ...") {
            LocationResult locationResult = null;

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                locationResult = ApplicationManager.getApplication().runReadAction((Computable<LocationResult>) () -> {
                    try {
                        return locateClassFile(psiElement);
                    } catch (Exception e) {
                        String errorMessage = "Class file could not be found";
                        if (e.getMessage() != null && !e.getMessage().isBlank()) {
                            errorMessage += ": " + e.getMessage();
                        }
                        return LocationResult.of(errorMessage);
                    }
                });
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                if (locationResult == null) {
                    return;
                }
                LocatedClassFile locatedClassFile = locationResult.locatedClassFile;
                if (locatedClassFile != null) {
                    updateToolWindowContents(project, locatedClassFile);
                } else {
                    if (!project.isDisposed()) {
                        try {
                            AnalyzeByteCodeAction.INSTANCE.actionPerformed(e);
                        } catch (Throwable ex) {
                            Messages.showWarningDialog(
                                    project,
                                    locationResult.errorMessage != null ? locationResult.errorMessage : "internal error",
                                    "AndroidAOP Code Viewer"
                            );
                        }

                    }
                }
            }


        });
    }

    private static byte[] readClassFile(LocatedClassFile locatedClassFile, Project project) {
        locatedClassFile.virtualFile.refresh(false, false);
        try {
            return loadClassFileBytes(locatedClassFile, project);
        } catch (Exception e) {
            return null;
        }
    }

    static byte[] loadClassFileBytes(LocatedClassFile locatedClassFile, Project project) throws IOException {
        if (FileTypeRegistry.getInstance().isFileOfType(locatedClassFile.virtualFile, JavaClassFileType.INSTANCE)) {
            return loadCompiledClassFileBytes(locatedClassFile, project);
        } else {
            return loadSourceClassFileBytes(locatedClassFile, project);
        }
    }


    private static byte[] loadCompiledClassFileBytes(LocatedClassFile locatedClassFile, Project project) throws IOException {
        ProjectFileIndex index = ProjectFileIndex.getInstance(project);
        VirtualFile file = locatedClassFile.virtualFile;
        String classFileName = StringUtil.getShortName(locatedClassFile.jvmClassName) + ".class";
        if (index.isInLibraryClasses(file)) {
            VirtualFile classFile = file.getParent().findChild(classFileName);
            byte[] byteArray;
            if (classFile != null) {
                byteArray = classFile.contentsToByteArray(false);
                locatedClassFile.writableUrl = classFile.getUrl();
            } else {
                throw new IOException("Class file not found");
            }
            return byteArray;
        } else {
            File classFile = new File(file.getParent().getPath(), classFileName);
            if (classFile.isFile()) {
                locatedClassFile.writableUrl = classFile.toURI().getPath();
                return FileUtil.loadFileBytes(classFile);
            } else {
                throw new IOException("Class file not found");
            }
        }
    }

    private static byte[] loadSourceClassFileBytes(LocatedClassFile locatedClassFile, Project project) throws IOException {
        ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
        @Nullable Module module = index.getModuleForFile(locatedClassFile.virtualFile);
        if (module == null) {
            throw new IOException("Module not found");
        }

        String relativePath = locatedClassFile.jvmClassName.replace('.', '/') + ".class";
        String[] paths = CompilerPaths.getOutputPaths(new Module[]{module});
        for (String path : paths) {
            File classFile = new File(path, relativePath);
            if (classFile.exists()) {
                locatedClassFile.writableUrl = classFile.toURI().getPath();
                return FileUtil.loadFileBytes(classFile);
            }
        }
        throw new IOException("Class file not found");
    }

    private static void updateToolWindowContents(Project project, LocatedClassFile locatedClassFile) {
        if (locatedClassFile != null) {
            try {
                ClassReader reader = new ClassReader(readClassFile(locatedClassFile, project));
                showCode(project, locatedClassFile.virtualFile, reader);
            } catch (Exception e) {
            }
        } else {
            showCode(project, null, null);
        }
    }

    public static void showCode(Project project, VirtualFile file, ClassReader reader) {

        ApplicationManager.getApplication().runWriteAction(() -> {
            ReplaceView replaceView = ReplaceView.getInstance(project);
            ReplaceViewKt replaceViewKt = ReplaceViewKt.getInstance(project);
            MatchView matchView = MatchView.getInstance(project);
            MatchViewKt matchViewKt = MatchViewKt.getInstance(project);
            ExtendsView extendsView = ExtendsView.getInstance(project);
            CollectView collectView = CollectView.getInstance(project);
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);


            if (file == null) {
                replaceView.setCode(null, Constants.NO_CLASS_FOUND);
                replaceViewKt.setCode(null, Constants.NO_CLASS_FOUND);
                matchView.setCode(null, Constants.NO_CLASS_FOUND);
                matchViewKt.setCode(null, Constants.NO_CLASS_FOUND);
                extendsView.setCode(null, Constants.NO_CLASS_FOUND);
                collectView.setCode(null, Constants.NO_CLASS_FOUND);
                toolWindowManager.getToolWindow(Constants.PLUGIN_WINDOW_NAME).activate(null);
                return;
            } else {
                Logger.getInstance(ShowAopCodeViewerAction.class).warn("file " + file);
            }

            if (reader == null) {
                return;
            }
            CurrentFileUtils.INSTANCE.setProject(project);
            CurrentFileUtils.INSTANCE.setFile(file);
            CurrentFileUtils.INSTANCE.setReader(reader);
            showCode(project, replaceView, replaceViewKt, matchView, matchViewKt, extendsView, collectView, toolWindowManager, file, reader);
        });
    }

    private static void showCode(
            Project project,
            ReplaceView replaceView,
            ReplaceViewKt replaceViewKt,
            MatchView matchView,
            MatchViewKt matchViewKt,
            ExtendsView extendsView,
            CollectView collectView,
            ToolWindowManager toolWindowManager,
            VirtualFile file,
            ClassReader reader
    ) {


        //            reader.accept(new TraceClassVisitor(new PrintWriter(stringWriter)), flags);
        AndroidAOPCode androidAOPCode = new AndroidAOPCode(reader);


        StringWriter replaceJavaCode =
                androidAOPCode.getReplaceContent(FileTypeExtension.JAVA);
        setCode(replaceJavaCode.toString(), replaceView, project, file, FileTypeExtension.JAVA);


        StringWriter replaceKotlinCode =
                androidAOPCode.getReplaceContent(FileTypeExtension.KOTLIN);
        setCode(replaceKotlinCode.toString(), replaceViewKt, project, file, FileTypeExtension.KOTLIN);

        StringWriter matchJavaCode = androidAOPCode.getMatchContent(
                FileTypeExtension.JAVA,
                false,
                false,
                false
        );
        setCode(matchJavaCode.toString(), matchView, project, file, FileTypeExtension.JAVA);

        StringWriter matchKotlinCode = androidAOPCode.getMatchContent(
                FileTypeExtension.KOTLIN,
                false,
                false,
                false
        );
        setCode(matchKotlinCode.toString(), matchViewKt, project, file, FileTypeExtension.KOTLIN);

        StringWriter extendsJavaCode = androidAOPCode.getModifyExtendsContent();
        setCode(extendsJavaCode.toString(), extendsView, project, file, FileTypeExtension.JAVA);

        StringWriter collectJavaCode = androidAOPCode.getCollectContent();
        setCode(collectJavaCode.toString(), collectView, project, file, FileTypeExtension.JAVA);

        ToolWindow toolWindow = toolWindowManager.getToolWindow(Constants.PLUGIN_WINDOW_NAME);
        if (toolWindow != null) {
            toolWindow.activate(null);
        }
    }

    private static void setCode(String code, ACodeView matchViewKt, Project project, VirtualFile file, FileTypeExtension fileTypeExtension) {
        PsiFile matchPsiFileKt = PsiFileFactory.getInstance(project)
                .createFileFromText(
                        Constants.FILE_NAME,
                        FileTypeManager.getInstance()
                                .getFileTypeByExtension(fileTypeExtension.getValue()),
                        code
                );
        CodeStyleManager.getInstance(project).reformat(matchPsiFileKt);
        matchViewKt.setCode(file, matchPsiFileKt.getText());
    }

    private static LocationResult locateClassFile(PsiElement psiElement) throws FileNotFoundException {
        PsiClass containingClass = getContainingClass(psiElement);
        if (containingClass == null) {
            throw new FileNotFoundException("<containing class>");
        }
        String jvmClassName = getJVMClassName(containingClass);
        if (jvmClassName == null) {
            throw new FileNotFoundException("<class name>");
        }
        VirtualFile virtualFile = getFileClass(containingClass).getOriginalElement().getContainingFile().getVirtualFile();
        return LocationResult.of(new LocatedClassFile(jvmClassName, virtualFile));
    }

    public static void show(String jvmClassName,VirtualFile virtualFile,Project project){
        LocationResult locationResult = LocationResult.of(new LocatedClassFile(jvmClassName, virtualFile));
        LocatedClassFile locatedClassFile = locationResult.locatedClassFile;
        if (locatedClassFile != null) {
            updateToolWindowContents(project, locatedClassFile);
        } else {
            if (!project.isDisposed()) {
                Messages.showWarningDialog(
                        project,
                        locationResult.errorMessage != null ? locationResult.errorMessage : "internal error",
                        "AndroidAOP Code Viewer"
                );
            }
        }
    }

    private static PsiClass getFileClass(PsiClass c) {
        if (!PsiUtil.isLocalOrAnonymousClass(c)) {
            return c;
        } else {
            PsiClass containingClass = PsiTreeUtil.getParentOfType(c, PsiClass.class);
            if (containingClass == null) {
                return c;
            } else {
                return getFileClass(containingClass);
            }
        }
    }


    @Nullable
    private static PsiClass getContainingClass(PsiElement psiElement) {
        IdeaPluginDescriptor byteCodeViewerPlugin = PluginManagerCore.getPlugin(PluginId.getId("ByteCodeViewer"));
        if (byteCodeViewerPlugin != null && byteCodeViewerPlugin.isEnabled()) {
            return ByteCodeViewerManager.getContainingClass(psiElement);
        } else {
            PsiClass containingClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class, false);
            if (containingClass instanceof PsiTypeParameter) {
                return getContainingClass(containingClass);
            } else {
                return containingClass;
            }
        }
    }

    @Nullable
    private static String getJVMClassName(PsiClass containingClass) {
        if (containingClass instanceof PsiAnonymousClass) {
            PsiClass containingClassOfAnonymous = PsiTreeUtil.getParentOfType(containingClass, PsiClass.class);
            if (containingClassOfAnonymous == null) {
                return null;
            }
            return getJVMClassName(containingClassOfAnonymous) + JavaAnonymousClassesHelper.getName(((PsiAnonymousClass) containingClass));
        } else {
            return ClassUtil.getJVMClassName(containingClass);
        }
    }
}
