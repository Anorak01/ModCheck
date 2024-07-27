package top.anorak01.modcheck.client;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import top.anorak01.modcheck.Modcheck;

import java.util.Map;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import top.anorak01.modcheck.NetworkHandler;

public class ModcheckClient implements ClientModInitializer {
    private boolean modCheckSent = false;


    @Override
    public void onInitializeClient() {
        registerModcheckClientJoin();
        registerModCheckHandshake();
    }

    private static void sendChecksumsToServer(Map<String, String> checksums) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(checksums.size());
        checksums.forEach((fileName, checksum) -> {
            buf.writeString(fileName);
            buf.writeString(checksum);
        });

        ClientPlayNetworking.send(NetworkHandler.CHECKSUMS_PACKET_ID, buf);
    }

    private static void registerModcheckClientJoin() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Modcheck.LOGGER.info("Sending modlist to server");
            Modcheck.regenerateModlist();
            sendChecksumsToServer(Modcheck.modlist_w_checksums);
        });
    }

    private void registerModCheckHandshake() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.MOD_CHECK_REQUEST, ((client, handler, buf, responseSender) -> {
            modCheckSent=false;
            if (client.player != null && client.world != null) {
                boolean hasMod = FabricLoader.getInstance().isModLoaded("modcheck");

                PacketByteBuf responseBuf = PacketByteBufs.create();
                responseBuf.writeBoolean(hasMod);

                ClientPlayNetworking.send(NetworkHandler.MOD_CHECK_RESPONSE, responseBuf);
            }
        }
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && !modCheckSent) {
                Modcheck.LOGGER.info("Sending handshake");
                boolean hasMod = FabricLoader.getInstance().isModLoaded("modcheck");

                PacketByteBuf responseBuf = PacketByteBufs.create();
                responseBuf.writeBoolean(hasMod);

                ClientPlayNetworking.send(NetworkHandler.MOD_CHECK_RESPONSE, responseBuf);

                modCheckSent = true;
            }
        });
    }
}
