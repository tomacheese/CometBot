package com.tomacheese.cometbot.commands;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.jda.JDACommandSender;
import cloud.commandframework.meta.CommandMeta;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.tomacheese.cometbot.Main;
import com.tomacheese.cometbot.lib.CommandPremise;
import com.tomacheese.cometbot.lib.FeedManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class Cmd_Rss implements CommandPremise {
    @Override
    public String description() {
        return "";
    }

    @Override
    public CommandPremise.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new CommandPremise.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "RSSを登録する")
                .literal("add")
                .argument(StringArgument.optional("url"))
                .handler(context -> execute(context, this::addRSS))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "フィード登録を解除します")
                .literal("remove")
                .argument(StringArgument.optional("url"))
                .handler(context -> execute(context, this::removeRSS))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "登録されているフィード一覧を表示します。")
                .literal("list")
                .argument(IntegerArgument.<JDACommandSender>newBuilder("page").withMin(1).asOptionalWithDefault(1).build())
                .handler(context -> execute(context, this::listRSS))
                .build()
        );
    }

    private void addRSS(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        String url = context.get("url");
        FeedManager feedManager = Main.getFeedManager();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(url);
            SyndFeed feed = new SyndFeedInput().build(document);
            String feedTitle = feed.getTitle();
            List<SyndEntry> entries = feed.getEntries();
            List<String> urls = entries.stream().map(SyndEntry::getUri).collect(Collectors.toList());

            FeedManager.FeedItem item = new FeedManager.FeedItem(channel.getIdLong(), feedTitle, url, urls);
            if (feedManager.getItem(channel.getIdLong(), url) != null) {
                message.reply(":x: 該当するフィードはすでに登録済みです").queue();
                return;
            }
            feedManager.add(item);
            message.replyEmbeds(new EmbedBuilder()
                .setTitle(":o: フィードを登録しました。")
                .addField("Channel Id", channel.getId(), false)
                .addField("Feed Title", feedTitle, false)
                .addField("Feed Url", url, false)
                .addField("Entries Count", String.valueOf(entries.size()), false)
                .setTimestamp(Instant.now())
                .setColor(Color.GREEN)
                .build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
            message.reply(":x: %s: %s".formatted(e.getClass().getSimpleName(), e.getMessage())).queue();
        }
    }

    private void removeRSS(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        String url = context.get("url");
        FeedManager feedManager = Main.getFeedManager();

        FeedManager.FeedItem item = feedManager.getItem(channel.getIdLong(), url);
        if(item == null){
            message.reply(":x: 該当するフィードが見つかりませんでした。").queue();
            return;
        }
        feedManager.remove(item);
        message.replyEmbeds(new EmbedBuilder()
            .setTitle(":o: フィード登録を解除しました。")
            .addField("Channel Id", String.valueOf(item.sendToChannelId()), false)
            .addField("Feed Title", item.title(), false)
            .addField("Feed Url", url, false)
            .setTimestamp(Instant.now())
            .setColor(Color.GREEN)
            .build()).queue();
    }


    private void listRSS(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        int page = context.get("page");
        long skip = (page - 1) * 25L;
        FeedManager feedManager = Main.getFeedManager();

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("フィード一覧")
            .setTimestamp(Instant.now())
            .setColor(Color.GREEN);

        for (FeedManager.FeedItem item : feedManager.getFeeds().stream().skip(skip).limit(25).collect(Collectors.toList())){
            embed.addField("%s -> <#%s>".formatted(item.title(), item.sendToChannelId()), item.feedUrl(), false);
        }

        message.replyEmbeds(embed.build()).queue();
    }
}
