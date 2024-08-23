package io.github.FlyJingFish.AndroidAopPlugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.task.ProjectTask;
import com.intellij.task.ProjectTaskManager;
import com.intellij.util.containers.OrderedSet;
import com.intellij.util.io.URLUtil;

import io.github.FlyJingFish.AndroidAopPlugin.common.Constants;
import io.github.FlyJingFish.AndroidAopPlugin.common.FileTypeExtension;
import io.github.FlyJingFish.AndroidAopPlugin.config.AOPPluginComponent;
import io.github.FlyJingFish.AndroidAopPlugin.config.ApplicationConfig;
import io.github.FlyJingFish.AndroidAopPlugin.util.AndroidAOPCode;
import io.github.FlyJingFish.AndroidAopPlugin.view.MatchViewKt;
import io.github.FlyJingFish.AndroidAopPlugin.view.ReplaceView;
import io.github.FlyJingFish.AndroidAopPlugin.view.MatchView;
import io.github.FlyJingFish.AndroidAopPlugin.view.ReplaceViewKt;
import org.objectweb.asm.ClassReader;


import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class ShowAopCodeViewerAction extends AnAction {

    Module module;

    @Override
    public void update(final AnActionEvent e) {
        final VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        final Project project = e.getData(PlatformDataKeys.PROJECT);
        final Presentation presentation = e.getPresentation();
        if (project == null || virtualFile == null) {
            presentation.setEnabled(false);
            Logger.getInstance(ShowAopCodeViewerAction.class).error("project == null || virtualFile == null");
            return;
        }
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        presentation.setEnabled(psiFile instanceof PsiClassOwner);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        if (psiFile == null) {
            Logger.getInstance(ShowAopCodeViewerAction.class).error("psiFile == null");
            return;
        }
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null) {
            Logger.getInstance(ShowAopCodeViewerAction.class).error("virtualFile == null");
            return;
        }
        module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(virtualFile);

        if (module == null){
            ShowBytecodeAction.INSTANCE.actionPerformed(e);
            return;
        }

        ProjectTaskManager projectTaskManager = ProjectTaskManager.getInstance(project);
        ProjectTask buildTask = projectTaskManager.createModulesBuildTask(module, true, true, true);

        Logger.getInstance(ShowAopCodeViewerAction.class).info("run buildTask");
        projectTaskManager.run(buildTask).onSuccess(result -> {

            if (!result.hasErrors()) {
                PsiClassOwner file = (PsiClassOwner) PsiManager.getInstance(project).findFile(virtualFile);

                if (file == null) {
                    Logger.getInstance(ShowAopCodeViewerAction.class).error("file == null");
                    return;
                }
                VirtualFile fileOutputDirectory = getOutputFile(file, virtualFile);
                fileOutputDirectory.refresh(false, false);
                updateToolWindowContents(e.getProject(), fileOutputDirectory);
            }
        });
    }

    public void actionPerformedCode(AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        if (psiFile == null) {
            Logger.getInstance(ShowAopCodeViewerAction.class).error("psiFile == null");
            return;
        }
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null) {
            Logger.getInstance(ShowAopCodeViewerAction.class).error("virtualFile == null");
            return;
        }
        module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(virtualFile);

        ProjectTaskManager projectTaskManager = ProjectTaskManager.getInstance(project);
        ProjectTask buildTask = projectTaskManager.createModulesBuildTask(module, true, true, true);

        Logger.getInstance(ShowAopCodeViewerAction.class).info("run buildTask");
        projectTaskManager.run(buildTask).onSuccess(result -> {

            if (!result.hasErrors()) {
                PsiClassOwner file = (PsiClassOwner) PsiManager.getInstance(project).findFile(virtualFile);

                if (file == null) {
                    Logger.getInstance(ShowAopCodeViewerAction.class).error("file == null");
                    return;
                }
                VirtualFile fileOutputDirectory = getOutputFile(file, virtualFile);
                fileOutputDirectory.refresh(false, false);
                updateToolWindowContents(e.getProject(), fileOutputDirectory);
            }
        });
    }

    private VirtualFile getOutputFile(PsiClassOwner file, VirtualFile vFile) {
        // determine whether this is a production or test file
        Boolean isProduction = module.getModuleScope(false).contains(vFile);

        String pkg = file.getPackageName().replace('.', File.separatorChar);

        OrderedSet<String> possibleOutputDirectories = findModuleOutputDirectories(isProduction);

        VirtualFileSystem virtualFileManager = VirtualFileManager.getInstance().getFileSystem(URLUtil.FILE_PROTOCOL);

        Logger.getInstance(ShowAopCodeViewerAction.class).warn("pkg " + pkg);
        for (String possibleOutputDirectory : possibleOutputDirectories) {
            Logger.getInstance(ShowAopCodeViewerAction.class).warn("possibleOutputDirectory " + possibleOutputDirectory);
            String classFile = vFile.getNameWithoutExtension() + ".class";
            Logger.getInstance(ShowAopCodeViewerAction.class).warn("classFile " + classFile);
            String path = Paths.get(possibleOutputDirectory, pkg, classFile).toString();
            Logger.getInstance(ShowAopCodeViewerAction.class).warn("path " + path);
            VirtualFile file1 = virtualFileManager.refreshAndFindFileByPath(path);
            if (file1 != null) {
                return file1;
            }
        }

        return null;
    }

    private OrderedSet<String> findModuleOutputDirectories(Boolean production) {
        ArrayList<String> outputPaths = new ArrayList<String>();

        CompilerModuleExtension compilerExtension = CompilerModuleExtension.getInstance(module);
        CompilerProjectExtension compilerProjectExtension = CompilerProjectExtension.getInstance(module.getProject());
        if (production) {
            VirtualFile moduleFile = compilerExtension.getCompilerOutputPath();
            if (moduleFile != null) {
                outputPaths.add(moduleFile.getPath());
            } else {
                Logger.getInstance(ShowAopCodeViewerAction.class).warn("moduleFile == null ");
                VirtualFile projectFile = compilerProjectExtension.getCompilerOutput();
                if (projectFile != null) {
                    outputPaths.add(projectFile.getPath());
                }
            }
        } else {
            VirtualFile moduleFile = compilerExtension.getCompilerOutputPathForTests();
            if (moduleFile != null) {
                outputPaths.add(moduleFile.getPath());
            } else {
                VirtualFile projectFile = compilerProjectExtension.getCompilerOutput();
                if (projectFile != null) {
                    outputPaths.add(projectFile.getPath());
                }
            }
        }

        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        for (OrderEnumerationHandler.Factory handlerFactory : OrderEnumerationHandler.EP_NAME.getExtensions()) {
            if (handlerFactory.isApplicable(module)) {
                OrderEnumerationHandler handler = handlerFactory.createHandler(module);
                List<String> outputUrls = new ArrayList<>();
                handler.addCustomModuleRoots(OrderRootType.CLASSES, moduleRootManager, outputUrls, production, !production);

                for (String outputUrl : outputUrls) {
                    outputPaths.add(VirtualFileManager.extractPath(outputUrl).replace('/', File.separatorChar));
                }
            }

        }
        return new OrderedSet(outputPaths);
    }

    /**
     * Reads the .class file, processes it through the ASM TraceVisitor and ASMifier to update the contents of the two
     * tabs of the tool window.
     *
     * @param project the project instance
     * @param file    the class file
     */
    private void updateToolWindowContents(final Project project, final VirtualFile file) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            ReplaceView replaceView = ReplaceView.getInstance(project);
            ReplaceViewKt replaceViewKt = ReplaceViewKt.getInstance(project);
            MatchView matchView = MatchView.getInstance(project);
            MatchViewKt matchViewKt = MatchViewKt.getInstance(project);
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);


            if (file == null) {
                replaceView.setCode(file, Constants.NO_CLASS_FOUND);
                replaceViewKt.setCode(file, Constants.NO_CLASS_FOUND);
                matchView.setCode(file, Constants.NO_CLASS_FOUND);
                matchViewKt.setCode(file, Constants.NO_CLASS_FOUND);
                toolWindowManager.getToolWindow(Constants.PLUGIN_WINDOW_NAME).activate(null);
                return;
            } else {
                Logger.getInstance(ShowAopCodeViewerAction.class).warn("file " + file.toString());
            }


            ClassReader reader = null;
            try {
                file.refresh(false, false);
                reader = new ClassReader(file.contentsToByteArray());
            } catch (IOException e) {
                return;
            }
            ApplicationConfig applicationConfig = AOPPluginComponent.getApplicationConfig();


//            reader.accept(new TraceClassVisitor(new PrintWriter(stringWriter)), flags);

            AndroidAOPCode androidAOPCode = new AndroidAOPCode(reader);


            StringWriter replaceJavaCode = androidAOPCode.getReplaceContent(FileTypeExtension.JAVA);
            PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(Constants.FILE_NAME, FileTypeManager.getInstance().getFileTypeByExtension(FileTypeExtension.JAVA.getValue()), replaceJavaCode.toString());
            CodeStyleManager.getInstance(project).reformat(psiFile);
            replaceView.setCode(file, psiFile.getText());


            StringWriter replaceKotlinCode = androidAOPCode.getReplaceContent(FileTypeExtension.KOTLIN);
            PsiFile psiFileKt = PsiFileFactory.getInstance(project).createFileFromText(Constants.FILE_NAME, FileTypeManager.getInstance().getFileTypeByExtension(FileTypeExtension.KOTLIN.getValue()), replaceKotlinCode.toString());
            CodeStyleManager.getInstance(project).reformat(psiFileKt);
            replaceViewKt.setCode(file, psiFileKt.getText());

            StringWriter matchJavaCode = androidAOPCode.getMatchContent(FileTypeExtension.JAVA,false,false,false);
            PsiFile matchPsiFile = PsiFileFactory.getInstance(project).createFileFromText(Constants.FILE_NAME, FileTypeManager.getInstance().getFileTypeByExtension(FileTypeExtension.JAVA.getValue()), matchJavaCode.toString());
            CodeStyleManager.getInstance(project).reformat(matchPsiFile);
            matchView.setCode(file, matchPsiFile.getText());

            StringWriter matchKotlinCode = androidAOPCode.getMatchContent(FileTypeExtension.KOTLIN,false,false,false);
            PsiFile matchPsiFileKt = PsiFileFactory.getInstance(project).createFileFromText(Constants.FILE_NAME, FileTypeManager.getInstance().getFileTypeByExtension(FileTypeExtension.KOTLIN.getValue()), matchKotlinCode.toString());
            CodeStyleManager.getInstance(project).reformat(matchPsiFileKt);
            matchViewKt.setCode(file, matchPsiFileKt.getText());


            toolWindowManager.getToolWindow(Constants.PLUGIN_WINDOW_NAME).activate(null);
        });
    }
}
