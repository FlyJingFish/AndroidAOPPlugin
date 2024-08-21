/*
 *
 *  Copyright 2011 Cédric Champeau
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
 *  Updated by: Kamiel
 */

package io.github.FlyJingFish.AndroidAopPlugin.config;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.EnumComboBoxModel;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Objects;

public class ASMPluginConfiguration {
    private JPanel contentPane;
    private JCheckBox skipDebugCheckBox;
    private JCheckBox skipFramesCheckBox;
    private JCheckBox skipCodeCheckBox;
    private JCheckBox expandFramesCheckBox;
    private JComboBox<CodeStyle> groovyCodeStyleComboBox;

    public ASMPluginConfiguration() {
    }

    public JComponent getRootPane() {
        return contentPane;
    }

    public void setData(ApplicationConfig applicationConfig) {
        skipDebugCheckBox.setSelected(applicationConfig.isPublic());
        skipFramesCheckBox.setSelected(applicationConfig.isProtected());
        skipCodeCheckBox.setSelected(applicationConfig.isPackage());
        expandFramesCheckBox.setSelected(applicationConfig.isPrivate());
        groovyCodeStyleComboBox.setSelectedItem(applicationConfig.getCodeStyle());
    }

    public void getData(ApplicationConfig applicationConfig) {
        applicationConfig.setPublic(skipDebugCheckBox.isSelected());
        applicationConfig.setProtected(skipFramesCheckBox.isSelected());
        applicationConfig.setPackage(skipCodeCheckBox.isSelected());
        applicationConfig.setPrivate(expandFramesCheckBox.isSelected());
        applicationConfig.setCodeStyle((CodeStyle) groovyCodeStyleComboBox.getSelectedItem());
    }

    public boolean isModified(ApplicationConfig applicationConfig) {
        if (skipDebugCheckBox.isSelected() != applicationConfig.isPublic()) return true;
        if (skipFramesCheckBox.isSelected() != applicationConfig.isProtected()) return true;
        if (skipCodeCheckBox.isSelected() != applicationConfig.isPackage()) return true;
        if (expandFramesCheckBox.isSelected() != applicationConfig.isPrivate()) return true;
        return !Objects.equals(groovyCodeStyleComboBox.getSelectedItem(), applicationConfig.getCodeStyle());
    }

    private void createUIComponents() {
        ComboBoxModel<CodeStyle> model = new EnumComboBoxModel<>(CodeStyle.class);
        groovyCodeStyleComboBox = new ComboBox<>(model);
        groovyCodeStyleComboBox.setRenderer(new GroovyCodeStyleCellRenderer<>());
    }

    private static class GroovyCodeStyleCellRenderer<T> implements ListCellRenderer<T> {
        private EnumMap<CodeStyle, JLabel> labels;

        private GroovyCodeStyleCellRenderer() {
            labels = new EnumMap<>(CodeStyle.class);
            for (CodeStyle codeStyle : CodeStyle.values()) {
                labels.put(codeStyle, new JLabel(codeStyle.label));
            }
        }

        public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            return labels.get(value);
        }
    }
}
