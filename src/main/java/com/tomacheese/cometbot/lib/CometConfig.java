package com.tomacheese.cometbot.lib;

import com.tomacheese.cometbot.Main;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CometConfig {
    boolean initFailed = true;
    String discordToken;
    String githubToken;

    public CometConfig() {
        Logger logger = Main.getLogger();

        Path path = Path.of("config.json");
        JSONObject config;
        try {
            config = new JSONObject(Files.readString(path));
        } catch (IOException e) {
            logger.error("config.json is not found.");
            e.printStackTrace();
            return;
        } catch (JSONException e){
            logger.error("config.json is not json parsable.");
            e.printStackTrace();
            return;
        }
        discordToken = getString(logger, config, "discordToken");
        if(discordToken == null){
            return;
        }

        githubToken = getString(logger, config, "githubToken");
        if(githubToken == null){
            return;
        }

        initFailed = false;
    }

    private String getString(Logger logger, JSONObject config, String key){
        if(!config.has(key)){
            logger.error("config[" + key + "] is not found");
            return null;
        }
        return config.getString(key);
    }

    public boolean isInitFailed() {
        return initFailed;
    }

    public String getDiscordToken() {
        return discordToken;
    }

    public String getGitHubToken() {
        return githubToken;
    }
}
