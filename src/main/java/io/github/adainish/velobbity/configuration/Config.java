package io.github.adainish.velobbity.configuration;

import com.google.gson.Gson;

public class Config extends AbstractJsonConfiguration {
    public Config(String pathString, String fileName, Gson gson) {
        super(pathString, fileName, gson);
    }
}
