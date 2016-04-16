package com.netflix.archaius.scanner.parameterized;

import com.netflix.archaius.api.annotations.Configuration;

@Configuration(prefix = "app.${env}", params = {"env"}, allowFields = true)
public class ParameterizedAppConfig {

    private final String env;
    public String name;
    public Boolean flag;
    public Integer number;

    public ParameterizedAppConfig(String env) {
        this.env = env;
    }
}
