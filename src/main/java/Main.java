import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;

import java.util.EnumSet;

public class Main {

    static Data data = new Data();

    public static void main(String[] args) {

        String token = System.getenv("BOT_TOKEN");
        if(token == null || token.trim().isEmpty()) {
            System.err.println("Bot token is missing!");
            System.exit(1);
        }else {

            JDA jda = JDABuilder.createLight(token, EnumSet.of(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_INVITES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGES)).setEventPassthrough(true).addEventListeners(new DiscordListener()).build();

            CommandListUpdateAction cmds = jda.updateCommands();

            cmds.addCommands(Commands.slash("void", "Send a message into the void and watch it anonymously appear").addOption(OptionType.STRING, "title", "Confession Title", true)
                            .addOption(OptionType.STRING, "confession", "Your confession in full. All formatting is accepted", true)
                            .addOption(OptionType.STRING, "image", "URL of an Image to be the in the confession", false),
                    Commands.slash("whoami", "Get an ephemeral look at yourself!"),
                    /**Commands.message("> Quote This!"),**/
                    Commands.slash("setup", "Setup the server for Confessions").addOption(OptionType.CHANNEL, "confession_channel", "A channel where confessions will go. Must be marked NSFW", true)
                            .addOption(OptionType.CHANNEL, "log_channel", "A channel for the logs", true)
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER)),
                    Commands.slash("x", "Make a fancy Embed of someones X account. Much better than standard embeds").addOption(OptionType.STRING, "url", "Full X URL including https://"));

            cmds.queue(commands -> commands.forEach(System.out::println));
        }
    }
}
