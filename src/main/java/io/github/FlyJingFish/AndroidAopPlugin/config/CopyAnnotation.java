package io.github.FlyJingFish.AndroidAopPlugin.config;

public enum CopyAnnotation {
    Copy("Copy Annotation"),
    NoneCopy("Not Copy Annotation");

    String label;

    CopyAnnotation(final String label) {
        this.label = label;
    }
}
