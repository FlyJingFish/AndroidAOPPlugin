package io.github.FlyJingFish.AndroidAopPlugin.view;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;

public class AOPCodeOutlineToolWindowFactory implements ToolWindowFactory {

    public void createToolWindowContent(final Project project, final ToolWindow toolWindow) {
        toolWindow.getContentManager().addContent(ApplicationManager.getApplication().getService(ContentFactory.class).createContent(ReplaceView.getInstance(project), "Replace", false));
        toolWindow.getContentManager().addContent(ApplicationManager.getApplication().getService(ContentFactory.class).createContent(ReplaceViewKt.getInstance(project), "Replace.Kt", false));
        toolWindow.getContentManager().addContent(ApplicationManager.getApplication().getService(ContentFactory.class).createContent(MatchView.getInstance(project), "Match", false));
        toolWindow.getContentManager().addContent(ApplicationManager.getApplication().getService(ContentFactory.class).createContent(MatchViewKt.getInstance(project), "Match.Kt", false));
        toolWindow.getContentManager().addContent(ApplicationManager.getApplication().getService(ContentFactory.class).createContent(ExtendsView.getInstance(project), "Extends", false));
        toolWindow.getContentManager().addContent(ApplicationManager.getApplication().getService(ContentFactory.class).createContent(CollectView.getInstance(project), "Collect", false));
    }

}