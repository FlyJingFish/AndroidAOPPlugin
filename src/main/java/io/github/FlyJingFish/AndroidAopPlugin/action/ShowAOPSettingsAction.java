package io.github.FlyJingFish.AndroidAopPlugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.util.IconLoader;
import io.github.FlyJingFish.AndroidAopPlugin.MyPluginClass;
import io.github.FlyJingFish.AndroidAopPlugin.config.PluginConfig;


public class ShowAOPSettingsAction extends AnAction {

    public ShowAOPSettingsAction() {
        super("Settings", "Show settings for AndroidAOP plugin", IconLoader.getIcon("/icons/setting.svg", MyPluginClass.class));
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), PluginConfig.class);
    }
}