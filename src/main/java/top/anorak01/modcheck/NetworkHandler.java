package top.anorak01.modcheck;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

import static top.anorak01.modcheck.Modcheck.*;

public class NetworkHandler {
    public static final Identifier MOD_CHECK_MODLIST_CHANNEL = new Identifier("modcheck", "mod_check_modlist_channel");

    public static void register() {
        Modcheck.LOGGER.info("Registering network handlers");
        registerModlistHandler();
    }


    private static void registerModlistHandler(){
        ServerLoginNetworking.registerGlobalReceiver(MOD_CHECK_MODLIST_CHANNEL, ((server, handler, understood, buf, synchronizer, responseSender) -> {
            if (!understood) {
                handler.disconnect(Text.of("You don't seem to have the ModCheck mod installed"));
            } else {
                int bufLen = buf.readInt();
                Map<String, String> modlist = new HashMap<>();
                for (int i = 0; i < bufLen; i++) {
                    String modname = buf.readString();
                    String checksum = buf.readString();
                    modlist.put(modname, checksum);
                }

                if (modlist.equals(Modcheck.modlist_w_checksums)) {
                    String conInfo = handler.getConnectionInfo();
                    String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
                    String uuid = conInfo.substring(conInfo.indexOf("id=")+3, conInfo.indexOf(","));

                    LOGGER.info("Modlist verified for {}: {}", name, uuid);
                } else {
                    handler.disconnect(Text.of("Your mods don't match with the expected modlist"));
                }
            }
        }
        ));
        ServerLoginConnectionEvents.QUERY_START.register(((handler, server1, sender, synchronizer) -> {
            String conInfo = handler.getConnectionInfo();
            String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
            String uuid = conInfo.substring(conInfo.indexOf("id=")+3, conInfo.indexOf(","));

            PacketByteBuf buf = PacketByteBufs.create();
            sender.sendPacket(MOD_CHECK_MODLIST_CHANNEL, buf);

            Modcheck.LOGGER.info("Sending modlist request to {}: {}", name, uuid);
        }
        ));
    }
}
