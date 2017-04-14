package info.modoff.votingserver;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.core.helpers.Strings;

import java.net.MalformedURLException;
import java.net.URL;

@Mod(modid = VotingServer.MODID, version = VotingServer.VERSION, acceptableRemoteVersions = "*")
public class VotingServer {
    public static final String MODID = "votingserver";
    public static final String VERSION = "1.0";

    private String motd;
    private VoteCodeClient voteCodeClient;
    private URL votingUrl;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) throws Exception {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        this.motd = config.getString("MOTD", "server", "Welcome to the server! When you are ready to vote, use the command /vote", "Sent to players on login");
        String requestUrlString = config.getString("URL", "request", "", "The URL to the API");
        String requestSecret = config.getString("secret", "request", "", "The secret to send with the request");
        int requestTimeout = config.getInt("timeout", "request", 2000, 0, Integer.MAX_VALUE, "The timeout for the request in milliseconds");
        String requestUserAgent = config.getString("userAgent", "request", "MC", "The User-Agent to be used in the request");
        String votingUrlString = config.getString("URL", "voting", "", "The URL to the voting page");
        config.save();

        if (Strings.isEmpty(requestSecret)) {
            throw new Exception("Request secret is not defined. See the configuration file.");
        }

        URL requestUrl;
        try {
            requestUrl = new URL(requestUrlString);
        } catch (MalformedURLException e) {
            throw new Exception("Request URL \"" + requestUrlString + "\" is malformed", e);
        }
        try {
            this.votingUrl = new URL(votingUrlString);
        } catch (MalformedURLException e) {
            throw new Exception("Voting URL \"" + votingUrlString + "\" is malformed", e);
        }

        this.voteCodeClient = new VoteCodeClient(requestUrl, requestSecret, requestTimeout, requestUserAgent);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        if (Strings.isNotEmpty(motd)) {
            MinecraftForge.EVENT_BUS.register(new MOTDEventHandler(motd));
        }
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandVote(voteCodeClient, votingUrl));
    }
}
