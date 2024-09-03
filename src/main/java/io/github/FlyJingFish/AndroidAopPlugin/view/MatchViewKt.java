package io.github.FlyJingFish.AndroidAopPlugin.view;

import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import io.github.FlyJingFish.AndroidAopPlugin.common.FileTypeExtension;

public class MatchViewKt extends ACodeView {

	public MatchViewKt(final Project project, KeymapManager keymapManager, final ToolWindowManager toolWindowManager) {
//		super(toolWindowManager, keymapManager, project, ASMPluginComponent.getApplicationConfig().getFileType());
		super(toolWindowManager, keymapManager, project, FileTypeExtension.KOTLIN.getValue());
	}

	public MatchViewKt(Project project) {
		super(project,  FileTypeExtension.KOTLIN.getValue());
	}


	public static MatchViewKt getInstance(Project project) {
		return project.getService(MatchViewKt.class);
	}
}
