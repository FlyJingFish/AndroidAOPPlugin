package io.github.FlyJingFish.AndroidAopPlugin.view;

import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import io.github.FlyJingFish.AndroidAopPlugin.common.FileTypeExtension;

public class ReplaceViewKt extends ACodeView {

	public ReplaceViewKt(final Project project, KeymapManager keymapManager, final ToolWindowManager toolWindowManager) {
		super(toolWindowManager, keymapManager, project, FileTypeExtension.KOTLIN.getValue());
	}

	public ReplaceViewKt(Project project) {
		super(project,  FileTypeExtension.KOTLIN.getValue());
	}


	public static ReplaceViewKt getInstance(Project project) {
		return project.getService(ReplaceViewKt.class);
	}
}
