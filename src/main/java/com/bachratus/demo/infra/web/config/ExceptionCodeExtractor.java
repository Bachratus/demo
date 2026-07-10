package com.bachratus.demo.infra.web.config;

public final class ExceptionCodeExtractor {

    private ExceptionCodeExtractor() {
    }

    public static String extract(RuntimeException exception) {
        return exception.getClass().getSimpleName()
                .replace("Exception", "")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();
    }

}
