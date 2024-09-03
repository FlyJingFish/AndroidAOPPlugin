package io.github.FlyJingFish.AndroidAopPlugin.view;

import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import io.github.FlyJingFish.AndroidAopPlugin.common.FileTypeExtension;

/**
 * The groovified view displays @groovyx.ast.bytecode.Bytecode code for methods.
 */
public class MatchView extends ACodeView {

	public MatchView(final Project project, KeymapManager keymapManager, final ToolWindowManager toolWindowManager) {
//		super(toolWindowManager, keymapManager, project, ASMPluginComponent.getApplicationConfig().getFileType());
		super(toolWindowManager, keymapManager, project, FileTypeExtension.JAVA.getValue());
	}


	public MatchView(Project project) {
		super(project,  FileTypeExtension.JAVA.getValue());
	}

	public static MatchView getInstance(Project project) {
		return project.getService(MatchView.class);
	}
}
