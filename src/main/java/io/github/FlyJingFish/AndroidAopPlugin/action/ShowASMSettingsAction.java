/*
 *
 *  Copyright 2017 Kamiel Ahmadpour
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package io.github.FlyJingFish.AndroidAopPlugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.util.IconLoader;
import io.github.FlyJingFish.AndroidAopPlugin.MyPluginClass;
import io.github.FlyJingFish.AndroidAopPlugin.config.PluginConfig;

import javax.swing.*;

public class ShowASMSettingsAction extends AnAction {

    public ShowASMSettingsAction() {
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