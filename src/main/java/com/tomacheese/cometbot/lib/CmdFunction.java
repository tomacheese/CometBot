package com.tomacheese.cometbot.lib;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.jda.JDACommandSender;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface CmdFunction {
    void execute(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context);
}
