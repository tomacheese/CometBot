package com.tomacheese.cometbot;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.exceptions.*;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.jda.JDA4CommandManager;
import cloud.commandframework.jda.JDACommandSender;
import cloud.commandframework.jda.JDAGuildSender;
import cloud.commandframework.meta.CommandMeta;
import com.tomacheese.cometbot.lib.ClassFinder;
import com.tomacheese.cometbot.lib.CometConfig;
import com.tomacheese.cometbot.lib.CommandPremise;
import com.tomacheese.cometbot.lib.FeedManager;
import com.tomacheese.cometbot.tasks.Task_CrawlRSS;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.List;

public class Main {
    static Logger logger;
    static CometConfig config;
    static GitHub github;
    static JDA jda;
    static FeedManager feedManager;

    public static void main(String[] args) {
        logger = LoggerFactory.getLogger("CometBot");

        logger.info("Load config");
        config = new CometConfig();
        if(config.isInitFailed()) {
            logger.error("Failed load config, exit.");
            System.exit(1);
            return;
        }

        logger.info("Login github");
        try {
            github = new GitHubBuilder()
                .withOAuthToken(config.getGitHubToken())
                .build();
        } catch (IOException e) {
            logger.error("Failed login to GitHub, exit");
            e.printStackTrace();
            System.exit(1);
            return;
        }

        logger.info("Login discord");
        try {
            jda = JDABuilder.createDefault(config.getDiscordToken())
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES,
                    GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGE_TYPING)
                .setAutoReconnect(true)
                .setBulkDeleteSplittingEnabled(false)
                .setContextEnabled(false)
                .build()
                .awaitReady();
        } catch (InterruptedException | LoginException e) {
            logger.error("Failed login to discord, exit.");
            e.printStackTrace();
            System.exit(1);
            return;
        }
        registerCommand(jda);

        feedManager = new FeedManager();
        registerTask();
    }

    static void registerCommand(JDA jda){
        logger.info("register Command");
        try {
            final JDA4CommandManager<JDACommandSender> manager = new JDA4CommandManager<>(
                jda,
                message -> "*",
                (sender, perm) -> {
                    logger.info("Check permission: " + perm);
                    MessageReceivedEvent event = sender.getEvent().orElse(null);
                    if (event == null || !event.isFromGuild() || event.getMember() == null) {
                        return false; // ???????????????NULL?????????????????????????????????????????????????????????????????????????????????
                    }
                    if(event.getGuild().getIdLong() != 375427073269039105L){
                        return false;
                    }
                    Member member = event.getMember();
                    return member.getIdLong() == 221991565567066112L;
                },
                AsynchronousCommandExecutionCoordinator.simpleCoordinator(),
                sender -> {
                    MessageReceivedEvent event = sender.getEvent().orElse(null);

                    if (sender instanceof JDAGuildSender jdaGuildSender) {
                        return new JDAGuildSender(event, jdaGuildSender.getMember(), jdaGuildSender.getTextChannel());
                    }

                    return null;
                },
                user -> {
                    MessageReceivedEvent event = user.getEvent().orElse(null);

                    if (user instanceof JDAGuildSender guildUser) {
                        return new JDAGuildSender(event, guildUser.getMember(), guildUser.getTextChannel());
                    }

                    return null;
                }
            );

            manager.registerExceptionHandler(NoSuchCommandException.class, (c, e) ->
                logger.info("NoSuchCommandException: " + e.getSuppliedCommand() + " (From " + c.getUser().getAsTag() + " in " + c.getChannel().getName() + ")")
            );

            manager.registerExceptionHandler(InvalidSyntaxException.class,
                (c, e) -> {
                    logger.info("InvalidSyntaxException: " + e.getMessage() + " (From " + c.getUser().getAsTag() + " in " + c.getChannel().getName() + ")");
                    if (c.getEvent().isPresent()) {
                        c.getEvent().get().getMessage().reply(String.format("??????????????????????????????????????????????????????: `%s`", e.getCorrectSyntax())).queue();
                    }
                });

            manager.registerExceptionHandler(NoPermissionException.class, (c, e) -> {
                logger.info("NoPermissionException: " + e.getMessage() + " (From " + c.getUser().getAsTag() + " in " + c.getChannel().getName() + ")");
                if (c.getEvent().isPresent()) {
                    c.getEvent().get().getMessage().reply("??????????????????????????????????????????????????????").queue();
                }
            });

            manager.registerExceptionHandler(CommandExecutionException.class, (c, e) -> {
                logger.info("CommandExecutionException: " + e.getMessage() + " (From " + c.getUser().getAsTag() + " in " + c.getChannel().getName() + ")");
                e.printStackTrace();
                if (c.getEvent().isPresent()) {
                    c.getEvent().get().getMessage().reply(MessageFormat.format("??????????????????????????????????????????: {0} ({1})",
                        e.getMessage(),
                        e.getClass().getName())).queue();
                }
            });

            ClassFinder classFinder = new ClassFinder();
            for (Class<?> clazz : classFinder.findClasses("com.tomacheese.cometbot.commands")) {
                if (!clazz.getName().startsWith("com.tomacheese.cometbot.commands.Cmd_")) {
                    continue;
                }
                if (clazz.getEnclosingClass() != null) {
                    continue;
                }
                if (clazz.getName().contains("$")) {
                    continue;
                }
                String commandName = clazz.getName().substring("com.tomacheese.cometbot.commands.Cmd_".length())
                    .toLowerCase();

                try {
                    Constructor<?> construct = clazz.getConstructor();
                    Object instance = construct.newInstance();
                    CommandPremise cmdPremise = (CommandPremise) instance;

                    Command.Builder<JDACommandSender> builder = manager.commandBuilder(
                        commandName,
                        ArgumentDescription.of(cmdPremise.description())
                    )
                        .meta(CommandMeta.DESCRIPTION, cmdPremise.description());
                    cmdPremise.register(builder).getCommands().forEach(cmd -> {
                        try {
                            manager.command(cmd);
                        } catch (AmbiguousNodeException e) {
                            getLogger().warn(String.format("%s: ???????????????????????????????????????????????????????????????????????????????????????: AmbiguousNodeException", cmd.toString()));
                            getLogger().warn("???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????literal???????????????????????????????????????????????????????????????????????????????????????");
                        }
                    });

                    getLogger().info(String.format("%s: ?????????????????????????????????????????????", commandName));
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    getLogger().warn(String.format("%s: ?????????????????????????????????????????????", commandName));
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    static void registerTask() {
        SchedulerFactory factory = new StdSchedulerFactory();
        List<TaskConfig> tasks = List.of(
            new TaskConfig(
                Task_CrawlRSS.class,
                "crawlrss",
                "cometbot",
                DailyTimeIntervalScheduleBuilder
                    .dailyTimeIntervalSchedule()
                    .startingDailyAt(TimeOfDay.hourMinuteAndSecondOfDay(0, 0, 0))
                    .withInterval(30, DateBuilder.IntervalUnit.MINUTE))
        );

        try {
            Scheduler scheduler = factory.getScheduler();
            scheduler.start();

            for (TaskConfig task : tasks) {
                logger.info("registerTask: " + task.name());
                scheduler.scheduleJob(
                    JobBuilder.newJob(task.clazz())
                        .withIdentity(task.name(), task.group())
                        .build(),
                    TriggerBuilder.newTrigger()
                        .withIdentity(task.name(), task.group())
                        .withSchedule(task.scheduleBuilder())
                        .build()
                );
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    record TaskConfig(Class<? extends Job> clazz, String name, String group,
                      ScheduleBuilder<?> scheduleBuilder) {
    }

    public static Logger getLogger() {
        return logger;
    }

    public static FeedManager getFeedManager() {
        return feedManager;
    }

    public static JDA getJDA() {
        return jda;
    }
}
