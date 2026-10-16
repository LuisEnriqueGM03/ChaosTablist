package com.chaos.tablist.platform;

import java.util.ServiceLoader;

public final class Services {
    public static final Platform PLATFORM = ServiceLoader.load(Platform.class, Services.class.getClassLoader())
        .findFirst().orElseThrow(() -> new IllegalStateException("Missing ChaosTablist loader integration"));
    private Services() {}
}
