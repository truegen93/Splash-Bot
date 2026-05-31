import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class DiscordListener extends ListenerAdapter {

    Data data = new Data();
    DataManager dM = new DataManager("data.ser");
    Map<Long, String> dataMap = dM.getDataMap();

    public void setupGuild(Guild guild){
        if(!dataMap.containsKey(guild.getIdLong())){
            guild.getDefaultChannel().asTextChannel().sendMessageEmbeds(data.setupEmbed(guild)).queue();
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        event.getJDA().getPresence().setActivity(Activity.competing("Pantsu Sniffing Contest 2026"));

        event.getJDA().getGuilds().forEach(g -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Guild Name: ").append(g.getName()).append(" | ").append(g.getId()).append(" | ");
            g.retrieveInvites().queue(e -> {
                if(e.get(0) != null) sb.append(e.get(0).getCode()); sb.append(" | ");
            });
            sb.append("Owner ID: ").append(g.getOwnerId());
            System.out.println(sb);
        });
    }

    public boolean isBotUser(User user){
        return user.isBot();
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {

        if(event.getName().equalsIgnoreCase("> Quote This!")){
            event.deferReply(true).queue();

            Long guildId = event.getGuild().getIdLong();

            // Fetches the specific map (Integer, String) only for this guild.
            // If the guild's file has never been opened before during this runtime session,
            // it safely loads it from disk 'guild_ID.ser' right here, otherwise it uses the fast RAM cache.
            Map<Integer, String> guildQuotes = dM.getGuildQuotes(guildId);

            // TODO: Use your upcoming custom method to handle formatting your embeds here
            if (guildQuotes.isEmpty()) {
                event.getHook().sendMessage("This server doesn't have any saved quotes yet!").setEphemeral(true).queue();
            } else {

            }
        }
    }

    public static final Pattern MASKED_LINK_PATTERN = Pattern.compile(
            "\\[([^\\]]+)\\]\\((<?)(https?://|www\\.|discord\\.(?:gg|io|me|li))[^\\s\\)]+?(>?)\\)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern URL = Pattern.compile(
            "\\b(https?://|www\\.|discord\\.(?:gg|io|me|li))\\S+",
            Pattern.CASE_INSENSITIVE
    );

    public String isAllowed(String text){

        String code = "default";
        if(text.isBlank()){
            code = "noText";
        }else if(MASKED_LINK_PATTERN.matcher(text).find()){
            code = "maskedURL";
        }else if(URL.matcher(text).find()){
            code = "url";
        }

        return code;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(isBotUser(event.getUser())){
            event.reply("Fuck off, Clanker!").queue();
        }

        String command = event.getName();

        if(command.equalsIgnoreCase("void")){
            Guild g = event.getGuild();
            if(!dataMap.containsKey(event.getGuild().getIdLong())){
                setupGuild(event.getGuild());
            }else{
                if(isAllowed(event.getOption("confession").getAsString()).equalsIgnoreCase("default"))
                {
                    EmbedBuilder builder = new EmbedBuilder();
                    String title = event.getOption("title").getAsString();
                    String confession = event.getOption("confession").getAsString();
                    if (event.getOption("image") != null) {
                        builder.setThumbnail(event.getOption("image").getAsString());
                    }


                    builder.setTitle(title);
                    builder.setDescription(confession);
                    builder.addField("Want to submit your own confession?", " Run </void:1508465790591045763>", false);
                    String config = dataMap.get(event.getGuild().getIdLong());
                    String confIdChan = config.split(":")[0];
                    String adminChannel = config.split(":")[1];
                    if (g.getTextChannelById(confIdChan) != null) {
                        if (g.getTextChannelById(confIdChan).isNSFW()) {
                            g.getTextChannelById(confIdChan).sendMessageEmbeds(builder.build()).queue(m -> {
                                if (g.getTextChannelById(adminChannel) != null) {
                                    builder.addField("Sent by :", event.getUser().getAsMention(), true);
                                    builder.addField("Jump To:", m.getJumpUrl(), true);
                                    g.getTextChannelById(adminChannel).sendMessageEmbeds(builder.build()).queue();
                                }
                                event.reply("Your confession was sent. Click [here](<" + m.getJumpUrl() + ">) to jump straight to it!").setEphemeral(true).queue();
                            });
                        } else {
                            event.reply("Sorry, this command relies on the Confessions channel being marked NSFW to comply with Discord ToS").setEphemeral(true).queue();
                        }
                    }
                }else{
                    switch (isAllowed(event.getOption("confession").getAsString())){
                        case "url":
                            event.reply("# **UNAUTHORISED CONTENT FOUND:**\n\n> You tried to submit a URL in your confession.").setEphemeral(true).queue();
                            break;
                        case "maskedURL":
                            event.reply("# **UNAUTHORISE CONTENT FOUND:**\n\n> You tried to send a masked link/inline url in your confession.").setEphemeral(true).queue();
                            break;
                        case "noText":
                            event.reply("# **NO CONTENT FOUND**:\n\n> You tried to send a confession with no content").setEphemeral(true).queue();

                    }
                }




            }
        }
//        if(command.equalsIgnoreCase("setup")){
//            System.out.println(event.getOption("confession_channel").getAsLong());
//            System.out.println(event.getOption("log_channel").getAsString());
//            System.out.println(event.getOption("confessions_role").getAsString());
//        }
        if(command.equalsIgnoreCase("setup")){
            TextChannel confessionsChannel = event.getGuild().getTextChannelById(event.getOption("confession_channel").getAsLong());
            TextChannel logChannel = event.getGuild().getTextChannelById(event.getOption("log_channel").getAsLong());

            String storage = confessionsChannel.getIdLong()+":"+logChannel.getIdLong();
            dM.onDataReceived(event.getGuild().getIdLong(), storage);
            event.reply("Setup Complete\n **Log Channel: **"+logChannel.getAsMention()+"\n**Confessions Channel: **"+confessionsChannel.getAsMention()).setEphemeral(true).queue();
        }
        if(command.equals("x")){
            event.deferReply().queue();
            String url = event.getOption("url").getAsString();
            Scraper.TwitterProfile profile = Scraper.fetchFullProfile(url);

            if(profile == null){
                event.getHook().sendMessage("❌ Unable to fetch profile metadata. Please ensure the account exists and is public.").setEphemeral(true).queue();
                return;
            }

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(profile.displayName, url);
            eb.setDescription(profile.bio);
            eb.setColor(profile.themeColor);

            if(profile.pfpUrl != null && !profile.pfpUrl.isEmpty()){
                eb.setImage(profile.pfpUrl);
            }
            eb.setFooter("X Account Embed | Powered by Splash",event.getJDA().getSelfUser().getAvatarUrl());

            event.getHook().sendMessageEmbeds(eb.build()).queue();



        }
        if(command.equals("whoami")){
            Member member = event.getMember();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Who is "+member.getUser().getName() +"?", member.getUser().getAvatarUrl());
            eb.setImage(member.getUser().getAvatarUrl());
            eb.addField("Display Name", member.getEffectiveName(), true);
            eb.addField("Username (if different)", member.getUser().getName(), true);
            eb.addField("ID(Long)", member.getId().toString(), true);
            eb.addField("Date Created", member.getTimeCreated().toLocalDate().toString(), true);
            eb.addField("Date Joined "+event.getGuild().getName(), member.getTimeJoined().toLocalDate().toString(), true);
            eb.addField("Highest Role", member.getRoles().get(0).getName(), true);
            eb.addField("Online Status", member.getOnlineStatus().toString(), true);
            User user = member.getUser();
            user.retrieveProfile().queue(up ->{
                if(up.getBannerUrl()!= null) eb.setImage(up.getBannerUrl());
            });
            StringBuilder sb = new StringBuilder();
            sb.append("| ");
            member.getRoles().forEach(role -> {
                sb.append(" ").append(role.getAsMention()).append(" |");
            });
            eb.addField("All Roles", sb.toString(), true);

            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
        }
    }
}