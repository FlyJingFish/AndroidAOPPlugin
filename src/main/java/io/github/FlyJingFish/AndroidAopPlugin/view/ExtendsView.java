package io.github.FlyJingFish.AndroidAopPlugin.view;

import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import io.github.FlyJingFish.AndroidAopPlugin.common.FileTypeExtension;


public class ExtendsView extends ACodeView {

	public ExtendsView(final ToolWindowManager toolWindowManager, KeymapManager keymapManager, final Project project) {
		super(toolWindowManager, keymapManager, project, FileTypeExtension.JAVA.getValue());
	}

	public ExtendsView(Project project) {
		super(project,  FileTypeExtension.JAVA.getValue());
	}

	public static ExtendsView getInstance(Project project) {
		return project.getService(ExtendsView.class);
	}
}
