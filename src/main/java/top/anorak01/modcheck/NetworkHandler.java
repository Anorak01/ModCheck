package top.anorak01.modcheck;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static top.anorak01.modcheck.Modcheck.modCheckResponses;
import static top.anorak01.modcheck.Modcheck.scheduler;

public class NetworkHandler {
    public static final Identifier CHECKSUMS_PACKET_ID = new Identifier("modcheck", "checksums");
    public static final Identifier MOD_CHECK_REQUEST = new Identifier("modcheck", "mod_check_request");
    public static final Identifier MOD_CHECK_RESPONSE = new Identifier("modcheck", "mod_check_response");


    public static void register() {
        Modcheck.LOGGER.info("Registering network handlers");
        registerChecksumPacket();
        registerHandshakeWorkflow();
    }

    private static void registerHandshakeWorkflow() {
        ServerPlayNetworking.registerGlobalReceiver(MOD_CHECK_RESPONSE, (server, player, handler, buf, responseSender) -> {
            boolean hasMod = buf.readBoolean();

            if (!hasMod) {
                player.networkHandler.disconnect(Text.of("You don't have ModCheck mod installed!"));
            }
            Modcheck.LOGGER.info("Received ModCheck handshake from {}", player.getName().getString());
            modCheckResponses.put(player.getUuid(), true);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            modCheckResponses.put(player.getUuid(), false);
            sendModCheckRequest(player);
            Modcheck.LOGGER.info("Sending handshake request to {}", player.getName().getString());

            // Schedule a task to check if the client responded after 5 seconds
            scheduler.schedule(() -> {
                if (!modCheckResponses.get(player.getUuid())) {
                    player.networkHandler.disconnect(Text.of("You don't have ModCheck mod installed!"));
                }
            }, 5, TimeUnit.SECONDS);
        });
    }

    private static void registerChecksumPacket() {
        ServerPlayNetworking.registerGlobalReceiver(CHECKSUMS_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            int count = buf.readInt();
            Map<String, String> checksums = new HashMap<>();
            for (int i = 0; i < count; i++) {
                String fileName = buf.readString(32767);
                String checksum = buf.readString(32767);
                checksums.put(fileName, checksum);
            }
            validateChecksums(player, checksums);
        });
    }

    private static void validateChecksums(ServerPlayerEntity player, Map<String, String> checksums) {
        // Your validation logic here
        // Disconnect player if validation fails
        Modcheck.LOGGER.info("Received mods from: {} valid: {}", player.getName().getString(), checksums.equals(Modcheck.modlist_w_checksums));
        boolean valid = checksums.equals(Modcheck.modlist_w_checksums); // Replace with actual validation logic
        if (!valid) {
            Text reason = Text.of("Your mods don't match with what's expected.");
            player.networkHandler.disconnect(reason);
        }
    }

    private static void sendModCheckRequest(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, MOD_CHECK_REQUEST, buf);
    }


}
