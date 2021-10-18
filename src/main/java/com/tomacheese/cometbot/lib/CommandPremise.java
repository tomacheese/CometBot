package com.tomacheese.cometbot.lib;

import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.jda.JDACommandSender;
import com.tomacheese.cometbot.Main;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.Arrays;
import java.util.List;

public interface CommandPremise {
    String description();

    Cmd register(Command.Builder<JDACommandSender> builder);

    default void execute(CommandContext<JDACommandSender> context, CmdFunction handler) {
        MessageChannel channel = context.getSender().getChannel();
        if (context.getSender().getEvent().isEmpty()) {
            channel.sendMessage("メッセージデータを取得できなかったため、処理に失敗しました。").queue();
            return;
        }
        if (!context.getSender().getEvent().get().isFromGuild()) {
            Main.getLogger().warn("execute: Guildからのメッセージではない");
            return;
        }
        Guild guild = context.getSender().getEvent().get().getGuild();
        Member member = guild.getMember(context.getSender().getUser());
        if (member == null) {
            member = guild.retrieveMember(context.getSender().getUser()).complete();
            if (member == null) {
                Main.getLogger().warn("execute: member == null");
                return;
            }
        }
        Message message = context.getSender().getEvent().get().getMessage();
        handler.execute(guild, channel, member, message, context);
    }

    class Cmd {
        private final Command<JDACommandSender>[] commands;

        @SafeVarargs
        public Cmd(Command<JDACommandSender>... commands) {
            this.commands = commands;
        }

        /**
         * Commandリストを返します
         *
         * @return Commandリスト
         */
        public List<Command<JDACommandSender>> getCommands() {
            return Arrays.asList(this.commands);
        }
    }
}
