package io.github.FlyJingFish.AndroidAopPlugin.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import io.github.FlyJingFish.AndroidAopPlugin.util.CurrentFileUtils;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

public class PluginConfig implements Configurable {

    private ApplicationConfig applicationConfig;
    private AOPPluginConfiguration configDialog;

    public PluginConfig() {
        this.applicationConfig = AOPPluginComponent.getApplicationConfig();

    }

    @Nls
    @Override
    public String getDisplayName() {
        return ApplicationConfig.APPLICATION_NAME;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        if (configDialog == null) configDialog = new AOPPluginConfiguration();
        return configDialog.getRootPane();
    }

    @Override
    public boolean isModified() {
        return configDialog != null && configDialog.isModified(applicationConfig);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (configDialog != null) {
            configDialog.getData(applicationConfig);
            CurrentFileUtils.INSTANCE.showCode();
        }
    }

    @Override
    public void reset() {
        if (configDialog != null) {
            configDialog.setData(applicationConfig);
        }
    }

    @Override
    public void disposeUIResources() {
        configDialog = null;
    }

}
