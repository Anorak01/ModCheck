package top.anorak01.modcheck;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Modcheck implements ModInitializer {
    public static final String MOD_ID = "ModCheck";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Map<String, String> modlist_w_checksums = new HashMap<>();

    public static final Map<UUID, Boolean> modCheckResponses = new HashMap<>();
    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static MinecraftServer server;

    public static boolean isSingleplayer;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        LOGGER.info("ModCheck starting");

        readModlist();
        LOGGER.info(modlist_w_checksums.toString());
        LOGGER.info("ModCheck started");
    }

    private void onServerStarting(MinecraftServer mcserver) {
        server = mcserver;
        isSingleplayer = server.isSingleplayer();
        if (isSingleplayer) {
            // don't register events
            LOGGER.info("Detected SinglePlayer environment, ModCheck disabled");
        } else {
            NetworkHandler.register();
        }
    }

    public static void regenerateModlist() { // obsoleted by external modlist
        File modsfolder = new File(FabricLoader.getInstance().getGameDir().toFile(), "mods");
        for (File mod : Objects.requireNonNull(modsfolder.listFiles())) {
            if (mod.isFile() && mod.canRead()) {
                String checksum = MakeChecksum.makeChecksum(mod);
                modlist_w_checksums.put(mod.getName(), checksum);
            }
        }
    }

    private static void readModlist() {
        File modfile = new File(FabricLoader.getInstance().getGameDir().toFile(), "modlist.txt");
        if (modfile.isFile()){
            LOGGER.info("Modlist file found, loading");
        } else {
            LOGGER.error("Modlist file not found, using server mods as modlist");
            regenerateModlist();
            return;
        }
        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(modfile.toPath())) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        modlist_w_checksums = props.stringPropertyNames()
                .stream()
                .collect(Collectors.toMap(key -> key, props::getProperty));
    }
}
