package io.github.FlyJingFish.AndroidAopPlugin.common;

public enum FileTypeExtension {
    JAVA("java"),
    KOTLIN("kt");

    FileTypeExtension(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }
}
