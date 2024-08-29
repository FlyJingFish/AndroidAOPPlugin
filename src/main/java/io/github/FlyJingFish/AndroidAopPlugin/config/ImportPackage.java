package io.github.FlyJingFish.AndroidAopPlugin.config;

public enum ImportPackage {
    Import("Import Package"),
    NoneImport("Not Import Package");

    String label;

    ImportPackage(final String label) {
        this.label = label;
    }
}
