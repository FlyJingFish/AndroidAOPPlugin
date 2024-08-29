package io.github.FlyJingFish.AndroidAopPlugin.action;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;

public class ShowBytecodeAction {
    private static class Inner{
        private static final ShowBytecodeAction instance = new ShowBytecodeAction();
    }
    public static ShowBytecodeAction getInstance(){
        return Inner.instance;
    }

    public void actionPerformed(AnActionEvent e) {
        PsiElement psiElement = getPsiElement(e);
        if (psiElement == null) {
            return;
        }
        Project project = e.getProject();
        if (project == null){
            return;
        }

        ClassFileLocationKt.openClassFile(psiElement,  project);
    }

    private PsiElement getPsiElement(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return null;
        }
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return e.getDataContext().getData(CommonDataKeys.PSI_ELEMENT);
        }
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (psiFile != null){
            int offset = editor.getCaretModel().getOffset();
            PsiElement psiElement = InjectedLanguageManager.getInstance(project).findInjectedElementAt(psiFile, offset);
            if (psiElement == null){
                psiElement = psiFile.findElementAt(offset);
            }
            return psiElement;
        }
        return null;
    }

}
