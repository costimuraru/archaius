package com.netflix.archaius.scanner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.annotations.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ConfigurationScanner {
    private final Logger LOG = LoggerFactory.getLogger(ConfigurationScanner.class);
    private Config config;

    @Inject
    public ConfigurationScanner(Config config) {
        this.config = config;
    }

    public void validate(String packageName) {
        Reflections reflectionPaths = new Reflections(packageName, new TypeAnnotationsScanner());
        Set<Class<?>> configurationClasses = reflectionPaths.getTypesAnnotatedWith(Configuration.class, true);

        for (Class<?> clazz : configurationClasses) {
            Configuration annonation = clazz.getAnnotation(Configuration.class);
            for (Field f : clazz.getDeclaredFields()) {
                if (annonation.prefix().contains("${")) {
                    if (Arrays.asList(annonation.params()).contains(f.getName())) {
                        // This is the param field. Skip it.
                        continue;
                    }
                    validateParameterizedField(clazz, annonation, f);
                } else {
                    String configPath = annonation.prefix() + "." + f.getName();
                    validateField(clazz.getName(), configPath, f);
                }
            }
        }
    }

    private void validateParameterizedField(Class<?> clazz, Configuration annonation, Field f) {
        Collection<String> possibleConfigs = getKeys(config, annonation.prefix(), f.getName());

        boolean validField = false;
        for (String configPath : possibleConfigs) {
            try {
                validateField(clazz.getName(), configPath, f);
                validField = true;
            } catch (Exception e) {
            }
        }
        if (!validField) {
            throw new RuntimeException("Unable to bind config for " + clazz.getName() + "::" + f.getName()
                    + " (" + f.getType().getName() + "). No suitable config found.");
        }
    }

    private Collection<String> getKeys(Config config, String parameterizedPath, String fieldName) {
        int position = parameterizedPath.indexOf("${");
        Collection<String> possibleConfigs = Lists.newArrayList();
        if (position >= 0) {
            String prefix = parameterizedPath.substring(0, position);
            Iterator<String> keys = config.getKeys(prefix);
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.endsWith(fieldName)) {
                    possibleConfigs.add(key);
                }
            }
        }
        return possibleConfigs;
    }

    private void validateField(String parentClassName, String configPath, Field f) {
        String fieldName = f.getName();
        Class<?> fieldClass = f.getType();
        Type paramType = f.getAnnotatedType().getType();
        String fieldFullName = parentClassName + "::" + fieldName;

        Object property = null;
        try {
            property = config.get(fieldClass, configPath);
        } catch (Exception e) {
            try {
                property = config.getRawProperty(configPath);
            } catch (Exception e2) {
            }
        }

        if (property == null) {
            throw new RuntimeException("Can't bind " + fieldFullName + " of type " + fieldClass.getName() +
                    ": Config path '" + configPath + "' not found.");
        }

        if (!fieldClass.isAssignableFrom(property.getClass())) {
            throw new RuntimeException("Can't bind '" + fieldFullName + "' (" + fieldClass.getName() +
                    ") to config path '" + configPath + " (" + property.getClass().getName() + ")': Type mismatch.");
        }

        if (List.class.isAssignableFrom(fieldClass)) {

            List propertyList = (List) property;
            if (propertyList.size() > 0) {
                Class<?> listType = (Class) ((ParameterizedType) paramType).getActualTypeArguments()[0];
                Class<?> actualClass = propertyList.get(0).getClass();

                if (!listType.isAssignableFrom(actualClass)) {
                    throw new RuntimeException("Can't bind List '" + fieldFullName + "' (" + listType.getName() +
                            ") to config path '" + configPath + " (" + actualClass.getName() + ")': Type mismatch.");
                }
            }
        }
    }

}
