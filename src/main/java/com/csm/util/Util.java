package com.csm.util;

import com.csm.enums.Infra;

import java.io.IOException;
import java.util.Properties;

public class Util {

    public static String getValueProper(Infra infra, String value){
        Properties properties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            StringBuilder buil = new StringBuilder();
            buil.append("external-");
            buil.append(infra.name());
            buil.append(".properties");
            String external = buil.toString();
            properties.load(classLoader.getResourceAsStream(external));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties.getProperty(value);
    }
}
