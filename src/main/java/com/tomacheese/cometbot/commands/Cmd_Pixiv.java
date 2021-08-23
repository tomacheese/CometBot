package com.tomacheese.cometbot.commands;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.jda.JDACommandSender;
import cloud.commandframework.meta.CommandMeta;
import com.tomacheese.cometbot.lib.CommandPremise;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public class Cmd_Pixiv implements CommandPremise {
    @Override
    public String description() {
        return "";
    }

    @Override
    public CommandPremise.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new CommandPremise.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "ワードを追加・削除・リスト表示する。")
                .literal("words")
                .argument(EnumArgument.of(PixivType.class, "pixivType"))
                .argument(StringArgument.optional("string", StringArgument.StringMode.GREEDY))
                .handler(context -> execute(context, this::executeWords))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "ミュートタグを追加・削除・リスト表示する。")
                .literal("mutetags")
                .argument(EnumArgument.of(PixivType.class, "pixivType"))
                .argument(StringArgument.optional("string", StringArgument.StringMode.GREEDY))
                .handler(context -> execute(context, this::executeMuteTags))
                .build()
        );
    }

    private void executeWords(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        PixivType pixivType = context.get("pixivType");
        Optional<Object> string = context.getOptional("string");

        Path path = Paths.get("/var/CometProjects/pixiv-novel-checker/master", "searchwords.json");
        if (!path.toFile().exists()) {
            message.reply("`searchwords.json` ファイルが見つかりませんでした。").queue();
            return;
        }

        JSONObject object;
        try {
            object = new JSONObject(Files.readString(path));
        } catch (IOException e) {
            message.reply(String.format("`searchwords.json` の取得に失敗しました: `%s`", e.getMessage())).queue();
            return;
        }
        JSONArray words = object.getJSONArray("words");

        if (pixivType == PixivType.ADD && string.isPresent()) {
            words.put(string.get());
            message.reply("words に `" + string.get() + "` を追加しました。").queue();
            try {
                object.put("words", words);
                Files.write(path, Collections.singleton(object.toString()));
            } catch (IOException e) {
                message.reply(String.format("`searchwords.json` の書き込みに失敗しました: `%s`", e.getMessage())).queue();
            }
            return;
        } else if (pixivType == PixivType.REMOVE && string.isPresent()) {
            if (!words.toList().contains(string.get())) {
                message.reply("words に `" + string.get() + "` は見つかりません。").queue();
                return;
            }
            words.remove(words.toList().indexOf(string.get()));
            message.reply("words から `" + string.get() + "` を削除しました。").queue();
            try {
                object.put("words", words);
                Files.write(path, Collections.singleton(object.toString()));
            } catch (IOException e) {
                message.reply(String.format("`searchwords.json` の書き込みに失敗しました: `%s`", e.getMessage())).queue();
            }
            return;
        } else if (pixivType == PixivType.LIST) {
            message.reply("```\n" + words
                .toList()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")) + "\n```").queue();
            return;
        }
        message.reply("処理に失敗しました。必要な引数が足りないかもしれません。").queue();
    }

    private void executeMuteTags(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        PixivType pixivType = context.get("pixivType");
        Optional<Object> string = context.getOptional("string");

        Path path = Paths.get("/var/CometProjects/pixiv-novel-checker/master", "searchwords.json");
        if (!path.toFile().exists()) {
            message.reply("`searchwords.json` ファイルが見つかりませんでした。").queue();
            return;
        }

        JSONObject object;
        try {
            object = new JSONObject(Files.readString(path));
        } catch (IOException e) {
            message.reply(String.format("`searchwords.json` の取得に失敗しました: `%s`", e.getMessage())).queue();
            return;
        }
        JSONArray mutetags = object.getJSONArray("mutetags");

        if (pixivType == PixivType.ADD && string.isPresent()) {
            mutetags.put(string.get());
            message.reply("mutetags に `" + string.get() + "` を追加しました。").queue();
            try {
                object.put("mutetags", mutetags);
                Files.write(path, Collections.singleton(object.toString()));
            } catch (IOException e) {
                message.reply(String.format("`searchwords.json` の書き込みに失敗しました: `%s`", e.getMessage())).queue();
            }
            return;
        } else if (pixivType == PixivType.REMOVE && string.isPresent()) {
            if (!mutetags.toList().contains(string.get())) {
                message.reply("mutetags に `" + string.get() + "` は見つかりません。").queue();
                return;
            }
            mutetags.remove(mutetags.toList().indexOf(string.get()));
            message.reply("mutetags から `" + string.get() + "` を削除しました。").queue();
            try {
                object.put("mutetags", mutetags);
                Files.write(path, Collections.singleton(object.toString()));
            } catch (IOException e) {
                message.reply(String.format("`searchwords.json` の書き込みに失敗しました: `%s`", e.getMessage())).queue();
            }
            return;
        } else if (pixivType == PixivType.LIST) {
            message.reply("```\n" + mutetags
                .toList()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")) + "\n```").queue();
            return;
        }
        message.reply("処理に失敗しました。必要な引数が足りないかもしれません。").queue();
    }

    enum PixivType {
        ADD,
        REMOVE,
        LIST
    }
}
