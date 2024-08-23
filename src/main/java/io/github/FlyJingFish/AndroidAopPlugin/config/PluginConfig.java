package io.github.FlyJingFish.AndroidAopPlugin.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

public class PluginConfig implements Configurable {

    private ApplicationConfig applicationConfig;
    private ASMPluginConfiguration configDialog;

    public PluginConfig() {
        this.applicationConfig = ASMPluginComponent.getApplicationConfig();

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
        if (configDialog == null) configDialog = new ASMPluginConfiguration();
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
