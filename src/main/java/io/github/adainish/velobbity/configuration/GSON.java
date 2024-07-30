package io.github.adainish.velobbity.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;

public class GSON
{
    public static Gson PRETTY_MAIN_GSON()
    {
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC, Modifier.FINAL)
                .create();
    }
}
