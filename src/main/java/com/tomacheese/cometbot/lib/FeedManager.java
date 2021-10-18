package com.tomacheese.cometbot.lib;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FeedManager {
    Path path = Path.of("feeds.json");
    Set<FeedItem> feeds = new HashSet<>();

    public FeedManager() {
        load();
    }

    public void add(FeedItem item) {
        feeds.add(item);
        save();
    }

    public void remove(FeedItem item) {
        feeds.remove(item);
        save();
    }

    public Set<FeedItem> getFeeds() {
        return feeds;
    }

    @Nullable
    public FeedItem getItem(long channelId, String url) {
        return feeds.stream()
            .filter(o -> channelId == o.sendToChannelId() && o.feedUrl().equals(url))
            .findFirst()
            .orElse(null);
    }

    void save() {
        JSONArray array = new JSONArray();
        feeds.forEach(f -> {
            JSONArray itemIds = new JSONArray();
            for (String itemId : f.itemIds()){
                itemIds.put(itemId);
            }

            JSONObject object = new JSONObject();
            object.put("sendToChannelId", f.sendToChannelId());
            object.put("title", f.title());
            object.put("feedUrl", f.feedUrl());
            object.put("itemIds", itemIds);
            array.put(object);
        });
        try {
            Files.writeString(path, array.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void load() {
        if(!Files.exists(path)){
            return;
        }
        feeds.clear();
        try {
            JSONArray array = new JSONArray(Files.readString(path));
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                feeds.add(new FeedItem(
                    object.getLong("sendToChannelId"),
                    object.getString("title"),
                    object.getString("feedUrl"),
                    object.getJSONArray("itemIds")
                        .toList()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.toList())
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static record FeedItem(long sendToChannelId, String title, String feedUrl, List<String> itemIds) {
    }
}
