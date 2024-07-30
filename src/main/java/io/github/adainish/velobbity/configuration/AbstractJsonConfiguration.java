package io.github.adainish.velobbity.configuration;

import com.google.gson.*;
import io.github.adainish.velobbity.Velobbbity;
import org.slf4j.event.Level;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a configuration file that is stored in JSON format.
 * While being easy to read and write, JSON is also human-readable.
 * Developers can write comments for details, store data, retrieve data, and store data in a nested format.
 * This class is used to read and write to the configuration file.
 * This class is used to store the configuration file path.
 * This class is used to store the configuration file content.
 * see {@link AbstractJsonConfiguration#loadConfig()}
 */
public abstract class AbstractJsonConfiguration {
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final String pathString;
    private JsonObject config;
    private Path filePath;

    public AbstractJsonConfiguration(String pathString) {
        this.pathString = pathString;
        loadConfig();
    }

    public AbstractJsonConfiguration(String directoryPath, String fileName, Gson gson) {
        this.pathString = directoryPath + "/" + fileName + ".json";
        this.gson = gson;
        loadConfig();
    }

    public AbstractJsonConfiguration(String directoryPath, String fileName) {
        this.pathString = directoryPath + "/" + fileName + ".json";
        loadConfig();
    }

    public void loadConfig() {
        File file = new File(this.pathString);
        if (!file.exists()) {
            try {
                Files.createDirectories(Paths.get(file.getParent()));
                if(file.createNewFile()) {
                    Velobbbity.instance.getLogger().atLevel(Level.INFO).log("Configuration file created: " + file.getName());
                } else {
                    throw new RuntimeException("Could not create configuration file: " + file.getName());
                }
            } catch (IOException | SecurityException | UnsupportedOperationException e) {
                Velobbbity.instance.getLogger().atLevel(Level.TRACE).log(e.getMessage());
                throw new RuntimeException("Could not create configuration directory", e);
            }
        } else {
            Velobbbity.instance.getLogger().atLevel(Level.INFO).log("Configuration file found: " + file.getName());
        }
        this.filePath = Paths.get(this.pathString);
        try (FileReader reader = new FileReader(filePath.toFile())) {
            config = gson.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            config = new JsonObject();
        }
    }

    public boolean hasKey(String key) {
        if (config == null)
            return false;
        return config.has(key);
    }

    public void remove(String key) {
        config.remove(key);
    }

    public Boolean getBoolean(String key) {
        JsonElement element = get(key);
        return element != null ? element.getAsBoolean() : null;
    }

    public Integer getInt(String key) {
        JsonElement element = get(key);
        return element != null ? element.getAsInt() : null;
    }

    public Double getDouble(String key) {
        JsonElement element = get(key);
        return element != null ? element.getAsDouble() : null;
    }

    public String getString(String key) {
        JsonElement element = get(key);
        return element != null ? element.getAsString() : null;
    }

    public void setBoolean(String key, boolean value) {
        set(key, new JsonPrimitive(value));
    }

    public void setInt(String key, int value) {
        set(key, new JsonPrimitive(value));
    }

    public void setDouble(String key, double value) {
        set(key, new JsonPrimitive(value));
    }

    public void setString(String key, String value) {
        set(key, new JsonPrimitive(value));
    }

    public JsonElement get(String key) {
        return config.get(key);
    }

    public <T> T getObject(String key, Class<T> classOfT) {
        return gson.fromJson(config.get(key), classOfT);
    }

    public <T> List<T> getList(String key, Class<T> classOfT) {
        List<T> list = new ArrayList<>();
        JsonArray jsonArray = config.getAsJsonArray(key);
        jsonArray.forEach(jsonElement -> list.add(gson.fromJson(jsonElement, classOfT)));
        return list;
    }

    public AbstractJsonConfiguration getSubConfig(String key) {
        JsonObject subConfigJson = config.getAsJsonObject(key);
        if (subConfigJson == null) {
            subConfigJson = new JsonObject();
            config.add(key, subConfigJson);
        }

        JsonObject finalSubConfigJson = subConfigJson;
        return new AbstractJsonConfiguration(pathString.toString(), key) {
            @Override
            public void loadConfig() {
                config = finalSubConfigJson;
            }
            @Override
            public void save() {
                set(key, config);
                AbstractJsonConfiguration.this.save();
            }
        };
    }

    public void set(String key, JsonElement value) {
        config.add(key, value);
        save();
    }

    public void setSubConfigElement(String key, String subKey, JsonElement value) {
        if (config == null) {
            config = new JsonObject();
        }
        JsonObject subConfig = config.getAsJsonObject(key);
        if (subConfig == null) {
            subConfig = new JsonObject();
            config.add(key, subConfig);
        }
        subConfig.add(subKey, value);
        save();
    }

    public void setSubConfigElement(String key, String subKey, String value) {
        setSubConfigElement(key, subKey, new JsonPrimitive(value));
    }

    public void setSubConfigElement(String key, String subKey, Integer value) {
        setSubConfigElement(key, subKey, new JsonPrimitive(value));
    }

    public void setSubConfigElement(String key, String subKey, Double value) {
        setSubConfigElement(key, subKey, new JsonPrimitive(value));
    }

    public void setSubConfigElement(String key, String subKey, Boolean value) {
        setSubConfigElement(key, subKey, new JsonPrimitive(value));
    }

    public JsonElement getSubConfigElement(String key, String subKey) {
        if (this.config == null) {
            this.config = new JsonObject();
        }
        JsonObject subConfig = config.getAsJsonObject(key);
        return subConfig != null ? subConfig.get(subKey) : null;
    }

    public String getSubConfigString(String key, String subKey) {
        JsonElement element = getSubConfigElement(key, subKey);
        return element != null ? element.getAsString() : null;
    }

    public Integer getSubConfigInt(String key, String subKey) {
        JsonElement element = getSubConfigElement(key, subKey);
        return element != null ? element.getAsInt() : null;
    }

    public Double getSubConfigDouble(String key, String subKey) {
        JsonElement element = getSubConfigElement(key, subKey);
        return element != null ? element.getAsDouble() : null;
    }

    public Boolean getSubConfigBoolean(String key, String subKey) {
        JsonElement element = getSubConfigElement(key, subKey);
        return element != null ? element.getAsBoolean() : null;
    }

    public void setSubConfig(String key, AbstractJsonConfiguration subConfig) {
        config.add(key, subConfig.config);
    }

    public <T> void setList(String key, List<T> list) {
        JsonArray jsonArray = new JsonArray();
        list.forEach(item -> jsonArray.add(gson.toJsonTree(item)));
        config.add(key, jsonArray);
    }

    public void addSubComment(String key, String subKey, String comment) {
        if (config == null) {
            config = new JsonObject();
        }
        JsonObject subConfig = config.getAsJsonObject(key);
        if (subConfig == null) {
            subConfig = new JsonObject();
            config.add(key, subConfig);
        }
        subConfig.addProperty(subKey + "_comment", comment);
        save();
    }

    public void addComment(String key, String comment) {
        if (config == null) {
            config = new JsonObject();
        }
        config.addProperty(key + "_comment", comment);
        save();
    }


    public void save() {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            gson.toJson(config, writer);
        } catch (IOException | NullPointerException e) {
            Velobbbity.instance.getLogger().atLevel(Level.TRACE).log(e.getMessage());
        }
    }

    public void reload() {
        Velobbbity.instance.getLogger().atLevel(Level.INFO).log("A request to reload configuration: " + filePath.toString() + " was made");
        // Reload the configuration
        loadConfig();
    }
}
