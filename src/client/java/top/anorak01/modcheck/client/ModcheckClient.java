package top.anorak01.modcheck.client;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import top.anorak01.modcheck.Modcheck;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import top.anorak01.modcheck.NetworkHandler;

public class ModcheckClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerModcheckResponder();
    }

    private static void registerModcheckResponder() {
        ClientLoginNetworking.registerGlobalReceiver(NetworkHandler.MOD_CHECK_MODLIST_CHANNEL, (((client, handler, buf, listenerAdder) -> {
            Modcheck.LOGGER.info("Sending modlist");

            PacketByteBuf responseBuf = PacketByteBufs.create();

            Map<String, String> checksums = Modcheck.modlist_w_checksums;
            responseBuf.writeInt(checksums.size());
            checksums.forEach((fileName, checksum) -> {
                responseBuf.writeString(fileName);
                responseBuf.writeString(checksum);
            });

            return CompletableFuture.completedFuture(responseBuf);
        })));
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.MOD_CHECK_MODLIST_UPLOAD_CHANNEL, ((client, handler, buf, responseSender) -> {
            PacketByteBuf responseBuf = PacketByteBufs.create();

            Map<String, String> checksums = Modcheck.modlist_w_checksums;
            responseBuf.writeInt(checksums.size());
            checksums.forEach((fileName, checksum) -> {
                responseBuf.writeString(fileName);
                responseBuf.writeString(checksum);
            });

            responseSender.sendPacket(NetworkHandler.MOD_CHECK_MODLIST_UPLOAD_CHANNEL, responseBuf);
        }));
    }
}
