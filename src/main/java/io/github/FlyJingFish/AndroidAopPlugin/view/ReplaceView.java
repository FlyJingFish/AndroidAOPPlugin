package io.github.FlyJingFish.AndroidAopPlugin.view;

import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import io.github.FlyJingFish.AndroidAopPlugin.common.FileTypeExtension;


public class ReplaceView extends ACodeView {

	public ReplaceView(final ToolWindowManager toolWindowManager, KeymapManager keymapManager, final Project project) {
//		super(toolWindowManager, keymapManager, project, ASMPluginComponent.getApplicationConfig().getFileType());
		super(toolWindowManager, keymapManager, project, FileTypeExtension.JAVA.getValue());
	}

	public ReplaceView(Project project) {
		super(project,  FileTypeExtension.JAVA.getValue());
	}

	public static ReplaceView getInstance(Project project) {
		return project.getService(ReplaceView.class);
	}
}
