package io.github.FlyJingFish.AndroidAopPlugin.action;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import io.github.FlyJingFish.AndroidAopPlugin.MyPluginClass;
import io.github.FlyJingFish.AndroidAopPlugin.config.PluginConfig;


public class ShowAOPHintAction extends AnAction {

    public ShowAOPHintAction() {
        super("Help", "Show Help for AndroidAOP plugin", IconLoader.getIcon("/icons/question.svg", MyPluginClass.class));
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        int result = Messages.showOkCancelDialog(
                "不理解生成的代码？请前往GitHub阅读Wiki文档",
                "AndroidAOP Code Viewer",
                "前往Wiki",
                "取消",
                IconLoader.getIcon("/icons/question.svg", MyPluginClass.class)
        );

        if (result == Messages.OK) {
            String url = "https://flyjingfish.github.io/AndroidAOP/zh/AOP_Helper/";
            BrowserUtil.browse(url);
        }
    }
}