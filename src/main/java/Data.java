import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;

public class Data {

    public MessageEmbed setupEmbed(Guild guild){
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Setup Required");
        eb.setAuthor("Splash",null,"https://cdn.discordapp.com/avatars/809476618749476904/43dd37debb3b8a0a9305d782b3d44645.webp?size=1024");
        eb.setColor(15495075);
        eb.setDescription("Please use /setup to set up the server. You will need to define a channel for confessions, a channel for logs, and a role for Confessions\n" +
                "If no role is defined, a role will be created called Confessions");
        eb.addField("Guild Name:", guild.getName(), false);

        return eb.build();
    }

}
