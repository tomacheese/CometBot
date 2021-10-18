package com.tomacheese.cometbot.tasks;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.tomacheese.cometbot.Main;
import com.tomacheese.cometbot.lib.FeedManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Task_CrawlRSS implements Job {
    @Override
    public void execute(JobExecutionContext context) {
        ExecutorService service = Executors.newFixedThreadPool(10,
            new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-%d").build());
        Set<FeedManager.FeedItem> feeds = Main.getFeedManager().getFeeds();
        feeds.forEach(
            f -> service.execute(new RunCrawlRSS(f))
        );
    }

    record RunCrawlRSS(FeedManager.FeedItem item) implements Runnable {
        @Override
        public void run() {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(item.feedUrl());
                SyndFeed feed = new SyndFeedInput().build(document);
                String feedTitle = feed.getTitle();
                List<SyndEntry> entries = feed.getEntries();

                TextChannel channel = Main.getJDA().getTextChannelById(item.sendToChannelId());
                if(channel == null){
                    Main.getLogger().error("Channel#%s is not found".formatted(item.sendToChannelId()));
                    return;
                }

                for(SyndEntry entry : entries) {
                    String desc = feed.getDescription();
                    channel.sendMessageEmbeds(new EmbedBuilder()
                        .setTitle("NEW ITEM: %s".formatted(feedTitle), item.feedUrl())
                        .addField("Title", entry.getTitle(), false)
                        .addField("Description", desc.length() >= MessageEmbed.VALUE_MAX_LENGTH ?
                            desc.substring(0, 1000) + "..." : desc, false)
                        .addField("Author", feed.getAuthor(), false)
                        .addField("URL", feed.getUri(), false)
                        .setColor(Color.YELLOW)
                        .build()
                    ).queue();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
