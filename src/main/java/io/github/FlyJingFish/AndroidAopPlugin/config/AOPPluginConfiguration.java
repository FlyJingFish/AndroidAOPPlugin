package io.github.FlyJingFish.AndroidAopPlugin.config;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.EnumComboBoxModel;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Objects;

public class AOPPluginConfiguration {
    private JPanel contentPane;
    private JCheckBox publicCheckBox;
    private JCheckBox protectedCheckBox;
    private JCheckBox packageCheckBox;
    private JCheckBox privateCheckBox;
    private JComboBox<ReplaceProxy> replaceProxyComboBox;
    private JComboBox<CopyAnnotation> copyAnnotationJComboBox;
    private JComboBox<ImportPackage> importPackageJComboBox;

    public AOPPluginConfiguration() {
    }

    public JComponent getRootPane() {
        return contentPane;
    }

    public void setData(ApplicationConfig applicationConfig) {
        publicCheckBox.setSelected(applicationConfig.isPublic());
        protectedCheckBox.setSelected(applicationConfig.isProtected());
        packageCheckBox.setSelected(applicationConfig.isPackage());
        privateCheckBox.setSelected(applicationConfig.isPrivate());
        replaceProxyComboBox.setSelectedItem(applicationConfig.getReplaceProxy());
        copyAnnotationJComboBox.setSelectedItem(applicationConfig.getCopyAnnotation());
        importPackageJComboBox.setSelectedItem(applicationConfig.getImportPackage());
    }

    public void getData(ApplicationConfig applicationConfig) {
        applicationConfig.setPublic(publicCheckBox.isSelected());
        applicationConfig.setProtected(protectedCheckBox.isSelected());
        applicationConfig.setPackage(packageCheckBox.isSelected());
        applicationConfig.setPrivate(privateCheckBox.isSelected());
        applicationConfig.setReplaceProxy((ReplaceProxy) replaceProxyComboBox.getSelectedItem());
        applicationConfig.setCopyAnnotation((CopyAnnotation) copyAnnotationJComboBox.getSelectedItem());
        applicationConfig.setImportPackage((ImportPackage) importPackageJComboBox.getSelectedItem());
    }
    public boolean isModified(ApplicationConfig applicationConfig) {
        if (publicCheckBox == null || protectedCheckBox == null || packageCheckBox == null
                || privateCheckBox == null || replaceProxyComboBox == null || copyAnnotationJComboBox == null
                || importPackageJComboBox == null){
            return false;
        }
        if (publicCheckBox.isSelected() != applicationConfig.isPublic()) return true;
        if (protectedCheckBox.isSelected() != applicationConfig.isProtected()) return true;
        if (packageCheckBox.isSelected() != applicationConfig.isPackage()) return true;
        if (privateCheckBox.isSelected() != applicationConfig.isPrivate()) return true;
        return !Objects.equals(replaceProxyComboBox.getSelectedItem(), applicationConfig.getReplaceProxy())
                || !Objects.equals(copyAnnotationJComboBox.getSelectedItem(), applicationConfig.getCopyAnnotation())
                || !Objects.equals(importPackageJComboBox.getSelectedItem(), applicationConfig.getImportPackage());

    }

    private void createUIComponents() {
        ComboBoxModel<ReplaceProxy> model = new EnumComboBoxModel<>(ReplaceProxy.class);
        replaceProxyComboBox = new ComboBox<>(model);
        replaceProxyComboBox.setRenderer(new GroovyCodeStyleCellRenderer<>());

        ComboBoxModel<CopyAnnotation> model2 = new EnumComboBoxModel<>(CopyAnnotation.class);
        copyAnnotationJComboBox = new ComboBox<>(model2);
        copyAnnotationJComboBox.setRenderer(new CopyAnnotationCellRenderer<>());

        ComboBoxModel<ImportPackage> model3 = new EnumComboBoxModel<>(ImportPackage.class);
        importPackageJComboBox = new ComboBox<>(model3);
        importPackageJComboBox.setRenderer(new ImportPackageCellRenderer<>());
    }

    private static class GroovyCodeStyleCellRenderer<T> implements ListCellRenderer<T> {
        private EnumMap<ReplaceProxy, JLabel> labels;

        private GroovyCodeStyleCellRenderer() {
            labels = new EnumMap<>(ReplaceProxy.class);
            for (ReplaceProxy replaceProxy : ReplaceProxy.values()) {
                labels.put(replaceProxy, new JLabel(replaceProxy.label));
            }
        }

        public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            return labels.get(value);
        }
    }

    private static class CopyAnnotationCellRenderer<T> implements ListCellRenderer<T> {
        private EnumMap<CopyAnnotation, JLabel> labels;

        private CopyAnnotationCellRenderer() {
            labels = new EnumMap<>(CopyAnnotation.class);
            for (CopyAnnotation replaceProxy : CopyAnnotation.values()) {
                labels.put(replaceProxy, new JLabel(replaceProxy.label));
            }
        }

        public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            return labels.get(value);
        }
    }

    private static class ImportPackageCellRenderer<T> implements ListCellRenderer<T> {
        private EnumMap<ImportPackage, JLabel> labels;

        private ImportPackageCellRenderer() {
            labels = new EnumMap<>(ImportPackage.class);
            for (ImportPackage replaceProxy : ImportPackage.values()) {
                labels.put(replaceProxy, new JLabel(replaceProxy.label));
            }
        }

        public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            return labels.get(value);
        }
    }
}
