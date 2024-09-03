package io.github.FlyJingFish.AndroidAopPlugin.view;


import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.PopupHandler;
import io.github.FlyJingFish.AndroidAopPlugin.action.ShowAOPDiffAction;
import io.github.FlyJingFish.AndroidAopPlugin.action.ShowAOPHintAction;
import io.github.FlyJingFish.AndroidAopPlugin.action.ShowAOPSettingsAction;
import io.github.FlyJingFish.AndroidAopPlugin.common.Constants;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;

public class ACodeView extends SimpleToolWindowPanel implements Disposable {
    protected final Project project;

    protected final ToolWindowManager toolWindowManager;
    protected final KeymapManager keymapManager;
    protected Editor editor;
    private ShowAOPDiffAction showAOPDiffAction;
    private String fileExtension;
    private DefaultActionGroup group;

    public ACodeView( final Project project, final String fileExtension) {
        super(true, true);
        this.toolWindowManager = null;
        this.keymapManager = null;
        this.project = project;
        this.fileExtension = fileExtension;
        setupUI(fileExtension);
    }

    public ACodeView(final ToolWindowManager toolWindowManager, KeymapManager keymapManager, final Project project, final String fileExtension) {
        super(true, true);
        this.toolWindowManager = toolWindowManager;
        this.keymapManager = keymapManager;
        this.project = project;
        this.fileExtension = fileExtension;
        setupUI(fileExtension);
    }

    private void setupUI(String extension) {
        final EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("");
        editor = editorFactory.createEditor(document, project, FileTypeManager.getInstance().getFileTypeByExtension(extension), true);
        showAOPDiffAction = new ShowAOPDiffAction(null, null, document, extension);

        final JComponent editorComponent = editor.getComponent();
        add(editorComponent);
        group = new DefaultActionGroup();
        group.add(showAOPDiffAction);
        group.add(new ShowAOPSettingsAction());
        group.add(new ShowAOPHintAction());

        final ActionManager actionManager = ActionManager.getInstance();
        final ActionToolbar actionToolBar = actionManager.createActionToolbar(Constants.PLUGIN_WINDOW_NAME, group, true);
        final JPanel buttonsPanel = new JPanel(new BorderLayout());
        buttonsPanel.add(actionToolBar.getComponent(), BorderLayout.CENTER);
//        PopupHandler.installPopupHandler(editor.getContentComponent(), group, Constants.PLUGIN_WINDOW_NAME, actionManager);
        PopupHandler.installPopupMenu(editor.getContentComponent(), group, Constants.PLUGIN_WINDOW_NAME, new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
        setToolbar(buttonsPanel);
    }

    public void setCode(final VirtualFile file, final String code) {
        setCode(file,code,fileExtension);
    }
    public void setCode(final VirtualFile file, final String code,final String extension) {
        if (!fileExtension.equals(extension)){
//            final EditorFactory editorFactory = EditorFactory.getInstance();
//            Document document = editorFactory.createDocument("");
//            editor = editorFactory.createEditor(document, project, FileTypeManager.getInstance().getFileTypeByExtension(extension), true);
//            showASMDiffAction.setExtension(extension);
//            final ActionManager actionManager = ActionManager.getInstance();
//            PopupHandler.installPopupHandler(editor.getContentComponent(), group, Constants.PLUGIN_WINDOW_NAME, actionManager);

            fileExtension = extension;
        }
        final String text = showAOPDiffAction.getDocument().getText();
        if (showAOPDiffAction.getPreviousFile() == null || file == null || showAOPDiffAction.getPreviousFile().getPath().equals(file.getPath()) && !Constants.NO_CLASS_FOUND.equals(text)) {
            if (file != null) showAOPDiffAction.setPreviousCode(text);
        } else if (!showAOPDiffAction.getPreviousFile().getPath().equals(file.getPath())) {
            showAOPDiffAction.setPreviousCode(""); // reset previous code
        }
        showAOPDiffAction.getDocument().setText(code);
        if (file != null) showAOPDiffAction.setPreviousFile(file);
        editor.getScrollingModel().scrollTo(editor.offsetToLogicalPosition(0), ScrollType.MAKE_VISIBLE);
    }


    @Override
    public void dispose() {
        if (editor != null) {
            final EditorFactory editorFactory = EditorFactory.getInstance();
            editorFactory.releaseEditor(editor);
            editor = null;
        }
    }
}
