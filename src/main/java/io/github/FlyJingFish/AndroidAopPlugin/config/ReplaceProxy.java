package io.github.FlyJingFish.AndroidAopPlugin.config;

public enum ReplaceProxy {
    NoneProxy("Not Use @ProxyMethod"),
    Proxy("Use @ProxyMethod");

    String label;

    ReplaceProxy(final String label) {
        this.label = label;
    }
}
