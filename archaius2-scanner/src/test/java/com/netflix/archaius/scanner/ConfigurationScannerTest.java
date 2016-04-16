package com.netflix.archaius.scanner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.archaius.ConfigMapper;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.MappingException;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.scanner.conf.AppConfig;
import com.netflix.archaius.scanner.parameterized.ParameterizedAppConfig;
import com.netflix.archaius.visitor.PrintStreamVisitor;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class ConfigurationScannerTest {

    @Test
    public void testConfig() throws MappingException {
        final Properties props = new Properties();
        props.setProperty("app.name", "str_value");
        props.setProperty("app.number", "123");
        props.setProperty("app.flag", "true");

        Injector injector = getInjector(props);

        ConfigurationScanner scanner = injector.getInstance(ConfigurationScanner.class);
        scanner.validate("com.netflix.archaius.scanner.conf");

        AppConfig appConfig = injector.getInstance(AppConfig.class);

        Assert.assertTrue(appConfig.flag);
        Assert.assertEquals("str_value", appConfig.name);
        Assert.assertEquals(123, appConfig.number.intValue());
    }

    @Test(expected = RuntimeException.class)
    public void testWithMissingConfig() throws MappingException {
        final Properties props = new Properties();
        props.setProperty("app.name", "str_value");
//        props.setProperty("app.number", "123"); // Simulate config not found.
        props.setProperty("app.flag", "true");

        Injector injector = getInjector(props);

        ConfigurationScanner scanner = injector.getInstance(ConfigurationScanner.class);
        scanner.validate("com.netflix.archaius.scanner.conf");

        AppConfig appConfig = injector.getInstance(AppConfig.class);
        // Exception thrown.
    }

    @Test(expected = RuntimeException.class)
    public void testWithMismatchingConfig() throws MappingException {
        final Properties props = new Properties();
        props.setProperty("app.name", "str_value");
        props.setProperty("app.number", "123");
        props.setProperty("app.flag", "NOT_A_BOOLEAN"); // should trigger exception when instantiating AppConfig

        Injector injector = getInjector(props);

        ConfigurationScanner scanner = injector.getInstance(ConfigurationScanner.class);
        scanner.validate("com.netflix.archaius.scanner.conf");

        AppConfig appConfig = injector.getInstance(AppConfig.class);
        // Exception thrown.
    }

    @Test
    public void testParameterized() throws MappingException {
        final Properties props = new Properties();
        props.setProperty("app.prod.name", "str_value");
        props.setProperty("app.prod.number", "123");
        props.setProperty("app.prod.flag", "true");
        props.setProperty("env", "prod");

        Injector injector = getInjector(props);

        ConfigurationScanner scanner = injector.getInstance(ConfigurationScanner.class);
        scanner.validate("com.netflix.archaius.scanner.parameterized");

        Config config = injector.getInstance(Config.class);
        Assert.assertEquals("prod", config.getString("env"));

        config.accept(new PrintStreamVisitor(System.err));

        ConfigMapper binder = new ConfigMapper();

        ParameterizedAppConfig serviceConfig = new ParameterizedAppConfig("prod");
        binder.mapConfig(serviceConfig, config);

        Assert.assertTrue(serviceConfig.flag);
        Assert.assertEquals("str_value", serviceConfig.name);
        Assert.assertEquals(123, serviceConfig.number.intValue());
    }

    private Injector getInjector(final Properties props) {
        return Guice.createInjector(
                new ArchaiusModule() {
                    @Override
                    protected void configureArchaius() {
                        bindApplicationConfigurationOverride().toInstance(MapConfig.from(props));
                    }
                });
    }
}
