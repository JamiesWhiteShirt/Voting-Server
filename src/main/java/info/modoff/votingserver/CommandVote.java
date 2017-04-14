package info.modoff.votingserver;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CommandVote extends CommandBase {
    private final Executor executor;
    private final VoteCodeClient voteCodeClient;
    private final URL votingUrl;

    public CommandVote(VoteCodeClient voteCodeClient, URL votingUrl) {
        // Using a single threaded executor. It has the following properties:
        // - Vote code requests will execute sequentially
        // - No more than one vote code request process can be active at any given time
        // - If the thread exits, a new one will take its place
        executor = Executors.newSingleThreadExecutor();
        this.voteCodeClient = voteCodeClient;
        this.votingUrl = votingUrl;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public String getName() {
        return "vote";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/vote";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

        Entity senderEntity = sender.getCommandSenderEntity();
        if (senderEntity instanceof EntityPlayer) {
            UUID uuid = senderEntity.getUniqueID();

            voteCodeClient.getAsync(uuid, executor).handle((code, exception) -> server.addScheduledTask(() -> {
                // This has to be done as a scheduled task to prevent concurrent modification
                // The ICommandSender may have been invalidated, therefore the player must be fetched
                // The player may have disconnected in the time it took to fetch, resulting in the player being null
                EntityPlayer player = server.getPlayerList().getPlayerByUUID(uuid);
                if (player != null) {
                    if (code != null) {
                        try {
                            String decoratedUrl = votingUrl + "?code=" + URLEncoder.encode(code, "UTF-8");

                            ITextComponent codeMessage = new TextComponentString("Your secret voting code: ");
                            ITextComponent codeComponent = new TextComponentString(code);
                            codeComponent.getStyle().setBold(true);
                            codeMessage.appendSibling(codeComponent);
                            player.sendMessage(codeMessage);

                            ITextComponent urlMessage = new TextComponentString("Vote here: ");
                            ITextComponent urlComponent = new TextComponentString(decoratedUrl);
                            urlComponent.getStyle()
                                    .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, decoratedUrl))
                                    .setColor(TextFormatting.BLUE)
                                    .setUnderlined(true);
                            urlMessage.appendSibling(urlComponent);
                            player.sendMessage(urlMessage);
                        } catch (UnsupportedEncodingException e) {
                            // This should never happen
                            e.printStackTrace();
                        }
                    }
                    if (exception != null) {
                        System.err.println("Error getting vote code for player " + player.getName() + " " + uuid);
                        System.err.println(exception.getMessage());
                        ITextComponent message = new TextComponentString("Something went wrong processing your vote. Try again later or poke a team member. Error: " + exception.getMessage());
                        message.getStyle().setColor(TextFormatting.RED);
                        player.sendMessage(message);
                    }
                }
            }));
        } else {
            throw new CommandException("Must be player to vote!");
        }
    }
}
