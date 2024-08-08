package top.anorak01.modcheck;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

// Stuff for commands
import static net.minecraft.server.command.CommandManager.*;

public class Modcheck implements ModInitializer {
    public static final String MOD_ID = "ModCheck";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Map<String, String> modlist_w_checksums = new HashMap<>();

    public static final Map<UUID, Boolean> modCheckResponses = new HashMap<>();
    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static MinecraftServer server;

    public static boolean isSingleplayer;

    public static boolean isModCheckEnabled = true;
    public static UUID uploading_player;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        LOGGER.info("ModCheck starting");

        registerCommands();
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
            readModlist();
            LOGGER.info(modlist_w_checksums.toString());

        }
    }

    public static void regenerateModlist() {
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

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Command to turn mod checking off
            dispatcher.register(literal("modcheck_off")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> {
                        if (isModCheckEnabled) {
                            isModCheckEnabled = false;
                          context.getSource().sendFeedback(()->
                              Text.literal("ModCheck temporarily disabled!"), false);
                        } else {
                            context.getSource().sendFeedback(()->
                            Text.literal("ModCheck is already disabled!"), false);
                        }
                      return 1;
                    })
            );

            // Command to upload the mod and update it on the server
            dispatcher.register(literal("modcheck_upload")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> {
                        if (isModCheckEnabled) {
                            context.getSource().sendFeedback(()->
                                    Text.literal("Mod checking has to be disabled first!"), false);
                        } else if (context.getSource().isExecutedByPlayer()) {
                            uploading_player = context.getSource().getPlayer().getUuid();

                            //context.getSource().getPlayer().send();
                                    //ServerPlayNetworking.send()
                            NetworkHandler.sendModlistRequest(context.getSource().getPlayer());
                            context.getSource().sendFeedback(()->
                                    Text.literal("Uploading new modlist"), false);
                        } else {
                            context.getSource().sendFeedback(()->
                                    Text.literal("This command can only be executed by a player"), false);
                        }
                        return 1;
                    })


            );

        });
    }
}
