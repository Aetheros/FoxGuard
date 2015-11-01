package tk.elektrofuchse.fox.foxguard;

import com.flowpowered.math.vector.Vector2i;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.living.player.TargetPlayerEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.config.ConfigDir;
import org.spongepowered.api.service.event.EventManager;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.service.user.UserStorage;
import tk.elektrofuchse.fox.foxguard.commands.*;
import tk.elektrofuchse.fox.foxguard.flagsets.SimpleFlagSet;
import tk.elektrofuchse.fox.foxguard.listener.BlockEventListener;
import tk.elektrofuchse.fox.foxguard.listener.InteractListener;
import tk.elektrofuchse.fox.foxguard.listener.PlayerEventListener;
import tk.elektrofuchse.fox.foxguard.regions.RectRegion;
import tk.elektrofuchse.fox.foxguard.regions.util.BoundingBox2;

import javax.sql.DataSource;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by Fox on 8/16/2015.
 * Project: foxguard
 */
@Plugin(id = "foxguard", name = "FoxGuard", version = FoxGuardMain.PLUGIN_VERSION)
public class FoxGuardMain {

    public static final String PLUGIN_VERSION = "0.7";

    private static FoxGuardMain instance;


    @Inject
    private Logger logger;
    @Inject
    private Game game;
    @Inject
    private EventManager eventManager;
    @Inject
    @ConfigDir(sharedRoot = true)
    private File configDirectory;

    private SqlService sql;
    private UserStorage userStorage;
    private FGCommandMainDispatcher fgDispatcher;

    private boolean flagSetsLoaded = false;

    @Listener
    public void gameInit(GameInitializationEvent event) {
        instance = this;
        userStorage = game.getServiceManager().provide(UserStorage.class).get();
        new FoxGuardManager(this, game.getServer());
        new FGConfigManager();
        FGConfigManager.getInstance().save();

        registerCommands();
        registerListeners();
    }

    @Listener
    public void serverStarted(GameStartedServerEvent event) {
        FoxGuardManager fgm = FoxGuardManager.getInstance();
        fgm.setup(game.getServer());
        try {
            FoxGuardStorageManager.getInstance().initFlagSets();
            FoxGuardStorageManager.getInstance().loadFlagSets();
            flagSetsLoaded = true;
            FoxGuardStorageManager.getInstance().loadLinks();
        } catch (SQLException e) {
            e.printStackTrace();
        }

       /* fgm.addFlagSet(new SimpleFlagSet("test", 1));
        fgm.addRegion(game.getServer().getWorld("world").get(),
                new RectRegion("test", new BoundingBox2(new Vector2i(-100, -100), new Vector2i(100, 100))));
        fgm.link(game.getServer(), "world", "test", "test");*/
    }

    @Listener
    public void serverStopping(GameStoppingServerEvent event) {
        try {
            FoxGuardStorageManager.getInstance().writeFlagSets();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        FGConfigManager.getInstance().save();
    }

    @Listener
    public void worldUnload(UnloadWorldEvent event) {
        try {
            FoxGuardStorageManager.getInstance().writeWorld(event.getTargetWorld());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Listener
    public void worldLoad(LoadWorldEvent event) {
        FoxGuardManager.getInstance().populateWorld(event.getTargetWorld());
        try {
            FoxGuardStorageManager.getInstance().initWorld(event.getTargetWorld());
            FoxGuardStorageManager.getInstance().loadWorldRegions(event.getTargetWorld());
            if (flagSetsLoaded) FoxGuardStorageManager.getInstance().loadWorldLinks(event.getTargetWorld());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public DataSource getDataSource(String jdbcUrl) throws SQLException {
        if (sql == null) {
            sql = game.getServiceManager().provide(SqlService.class).get();
        }
        return sql.getDataSource(jdbcUrl);
    }

    private void registerCommands() {
        fgDispatcher = new FGCommandMainDispatcher();
        FGCommandDispatcher fgRegionDispatcher = new FGCommandDispatcher();
        FGCommandDispatcher fgFlagSetDispatcher = new FGCommandDispatcher();
        fgDispatcher.register(new CommandCreate(), "create", "construct", "new", "make", "define", "mk");
        fgDispatcher.register(new CommandDelete(), "delete", "del", "remove", "rem", "rm", "destroy");
        fgDispatcher.register(new CommandModify(), "modify", "mod", "change", "edit", "update");
        fgDispatcher.register(new CommandLink(), "link", "connect", "attach");
        fgDispatcher.register(new CommandUnlink(), "unlink", "disconnect", "detach");
        fgDispatcher.register(new CommandList(), "list", "ls");
        fgDispatcher.register(new CommandDetail(), "detail", "det", "show");
        fgDispatcher.register(new CommandState(), "state", "current", "cur");
        fgDispatcher.register(new CommandPosition(), "position", "pos", "p");
        fgDispatcher.register(new CommandAdd(), "add", "push");
        fgDispatcher.register(new CommandSubtract(), "subtract", "sub", "pop");
        fgDispatcher.register(new CommandFlush(), "flush", "clear");
        fgDispatcher.register(new CommandAbout(), "about", "info");
        fgDispatcher.register(new CommandTest(), "test");
        game.getCommandDispatcher().register(this, fgDispatcher, "foxguard", "foxg", "fguard", "fg");
    }

    private void registerListeners() {
        eventManager.registerListener(this, TargetPlayerEvent.class, new PlayerEventListener());
        eventManager.registerListener(this, ChangeBlockEvent.class, new BlockEventListener());
        eventManager.registerListener(this, InteractBlockEvent.class, new InteractListener());
    }

    public Logger getLogger() {
        return logger;
    }

    public Game getGame() {
        return game;
    }

    public static FoxGuardMain getInstance() {
        return instance;
    }

    public UserStorage getUserStorage() {
        return userStorage;
    }

    public File getConfigDirectory() {
        return configDirectory;
    }
}
