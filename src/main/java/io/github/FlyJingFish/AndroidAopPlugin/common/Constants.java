package io.github.FlyJingFish.AndroidAopPlugin.common;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public abstract class Constants {
    public static final String PLUGIN_WINDOW_NAME = "AOPCode";
    public static final String FILE_NAME = "androidAop-plugin";
    public static final String NO_CLASS_FOUND = "// couldn't generate bytecode view, no .class file found";
    public static final String COMPONENT_NAME = "AndroidAOPPluginConfiguration";
    public static final Icon ACTION_ICON = IconLoader.getIcon("/images/pluginIcon.svg", Constants.class);
    private Constants() {

    }
}
