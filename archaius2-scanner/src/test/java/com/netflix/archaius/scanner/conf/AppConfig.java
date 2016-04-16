package com.netflix.archaius.scanner.conf;

import com.netflix.archaius.api.annotations.Configuration;

@Configuration(prefix = "app", allowFields = true)
public class AppConfig {

    public String name;
    public Boolean flag;
    public Integer number;
}
