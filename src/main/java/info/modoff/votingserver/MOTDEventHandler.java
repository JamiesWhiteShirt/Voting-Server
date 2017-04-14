package info.modoff.votingserver;

import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class MOTDEventHandler {
    private final String message;

    public MOTDEventHandler(String message) {
        this.message = message;
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        event.player.sendMessage(new TextComponentString(message));
    }
}
