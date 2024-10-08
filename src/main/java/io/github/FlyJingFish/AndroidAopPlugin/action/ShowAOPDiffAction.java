package io.github.FlyJingFish.AndroidAopPlugin.action;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import io.github.FlyJingFish.AndroidAopPlugin.common.Constants;
import org.jetbrains.annotations.NotNull;

public class ShowAOPDiffAction extends AnAction {
    private static final String DIFF_WINDOW_TITLE = "Show differences from previous class contents";
    private static final String[] DIFF_TITLES = {"Previous version", "Current version"};
    private String previousCode;
    private VirtualFile previousFile;
    private Document document;
    private String extension;


    public ShowAOPDiffAction(String previousCode, VirtualFile previousFile, Document document, String extension) {
        super("Show differences",
                "Shows differences from the previous version of bytecode for this file",
                null);
        this.previousCode = previousCode;
        this.previousFile = previousFile;
        this.document = document;
        this.extension = extension;
    }

    @Override
    public void update(final AnActionEvent e) {
        e.getPresentation().setEnabled(!"".equals(previousCode) && (previousFile != null));
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        PsiFile psiFile = PsiFileFactory.getInstance(e.getProject()).createFileFromText(Constants.FILE_NAME, FileTypeManager.getInstance().getFileTypeByExtension(extension), "");
        DocumentContent currentContent = (previousFile == null) ? DiffContentFactory.getInstance().create("") : DiffContentFactory.getInstance().create(document.getText(), psiFile.getFileType());
        DocumentContent oldContent = (previousCode == null) ? DiffContentFactory.getInstance().create("") : DiffContentFactory.getInstance().create(previousCode, psiFile.getFileType());
        SimpleDiffRequest request = new SimpleDiffRequest(DIFF_WINDOW_TITLE, oldContent, currentContent, DIFF_TITLES[0], DIFF_TITLES[1]);
        DiffManager.getInstance().showDiff(e.getProject(), request);
    }


    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    // Property files
    public String getPreviousCode() {
        return previousCode;
    }

    public void setPreviousCode(String previousCode) {
        this.previousCode = previousCode;
    }

    public VirtualFile getPreviousFile() {
        return previousFile;
    }

    public void setPreviousFile(VirtualFile previousFile) {
        this.previousFile = previousFile;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}