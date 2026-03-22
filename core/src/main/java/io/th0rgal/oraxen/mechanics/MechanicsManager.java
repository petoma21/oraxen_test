package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenNativeMechanicsRegisteredEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class MechanicsManager {

    private static final Map<String, MechanicFactory> FACTORIES_BY_MECHANIC_ID = new HashMap<>();
    private static final Map<String, List<SchedulerUtil.ScheduledTask>> MECHANIC_TASKS = new HashMap<>();
    private static final Map<String, List<Listener>> MECHANICS_LISTENERS = new HashMap<>();
    private static final Set<String> NATIVE_MECHANIC_IDS = Set.of(
            "block", "noteblock", "stringblock", "chorusblock"
    );

    public static void registerNativeMechanics() {
        // reset only native mechanics so external/custom factories work
        NATIVE_MECHANIC_IDS.forEach(MechanicsManager::unregisterMechanicFactory);

        registerFactory("block", BlockMechanicFactory::new);
        registerFactory("noteblock", NoteBlockMechanicFactory::new);
        registerFactory("stringblock", StringBlockMechanicFactory::new);
        registerFactory("chorusblock", ChorusBlockMechanicFactory::new);

        SchedulerUtil.runTask(() -> Bukkit.getPluginManager().callEvent(new OraxenNativeMechanicsRegisteredEvent()));
    }

    /**
     * Register a new MechanicFactory
     *
     * @param mechanicId the id of the mechanic
     * @param factory    the MechanicFactory of the mechanic
     * @param enabled    if the mechanic should be enabled by default or not
     */
    public static void registerMechanicFactory(String mechanicId, MechanicFactory factory, boolean enabled) {
        if (enabled) FACTORIES_BY_MECHANIC_ID.put(mechanicId, factory);
    }

    public static void unregisterMechanicFactory(String mechanicId) {
        FACTORIES_BY_MECHANIC_ID.remove(mechanicId);
        unloadListeners(mechanicId);
        unregisterTasks(mechanicId);
    }

    /**
     * This method is deprecated and will be removed in a future release.<br>
     * Use {@link #registerMechanicFactory(String, MechanicFactory, boolean)} instead.
     *
     * @param mechanicId  the id of the mechanic
     * @param constructor the constructor of the mechanic
     */
    @Deprecated(forRemoval = true, since = "1.158.0")
    public static void registerMechanicFactory(final String mechanicId, final FactoryConstructor constructor) {
        registerFactory(mechanicId, constructor);
    }

    private static void registerFactory(final String mechanicId, final FactoryConstructor constructor) {
        final Entry<File, YamlConfiguration> mechanicsEntry = OraxenPlugin.get().getResourceManager().getMechanicsEntry();
        final YamlConfiguration mechanicsConfig = mechanicsEntry.getValue();
        final boolean updated = false;
        ConfigurationSection factorySection = mechanicsConfig.getConfigurationSection(mechanicId);
        if (factorySection != null && factorySection.getBoolean("enabled"))
            FACTORIES_BY_MECHANIC_ID.put(mechanicId, constructor.create(factorySection));

        try {
            if (updated) mechanicsConfig.save(mechanicsEntry.getKey());
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers a scheduled task for a mechanic so it can be cancelled during reload/unload.
     */
    public static void registerTask(String mechanicId, SchedulerUtil.ScheduledTask task) {
        if (task == null) return;
        MECHANIC_TASKS.compute(mechanicId, (key, value) -> {
            if (value == null) value = new ArrayList<>();
            value.add(task);
            return value;
        });
    }

    public static void unregisterTasks() {
        MECHANIC_TASKS.values().forEach(tasks -> tasks.forEach(task -> {
            if (task != null) task.cancel();
        }));
        MECHANIC_TASKS.clear();

        StorageMechanic.clearRuntimeCaches();
    }

    public static void unregisterTasks(String mechanicId) {
        MECHANIC_TASKS.computeIfPresent(mechanicId, (key, value) -> {
            value.forEach(task -> {
                if (task != null) task.cancel();
            });
            return null;
        });
    }

    public static void registerListeners(final JavaPlugin plugin, String mechanicId, final Listener... listeners) {
        for (final Listener listener : listeners)
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        MECHANICS_LISTENERS.compute(mechanicId, (key, value) -> {
            if (value == null) value = new ArrayList<>();
            value.addAll(Arrays.asList(listeners));
            return value;
        });
    }

    public static void unloadListeners() {
        for (final Listener listener : MECHANICS_LISTENERS.values().stream().flatMap(Collection::stream).toList())
            HandlerList.unregisterAll(listener);
        MECHANICS_LISTENERS.clear();
    }

    public static void unloadListeners(String mechanicId) {
        List<Listener> listeners = MECHANICS_LISTENERS.remove(mechanicId);
        if (listeners == null) return;
        for (final Listener listener : listeners)
            HandlerList.unregisterAll(listener);

    }

    public static boolean isMechanicEnabled(String mechanicId) {
        return FACTORIES_BY_MECHANIC_ID.containsKey(mechanicId);
    }

    public static MechanicFactory getMechanicFactory(final String mechanicID) {
        return FACTORIES_BY_MECHANIC_ID.get(mechanicID);
    }

    /**
     * Returns all registered mechanic factories.
     * Used by SchemaGenerator for automatic schema extraction.
     *
     * @return Unmodifiable map of mechanic ID to factory
     */
    public static Map<String, MechanicFactory> getAllFactories() {
        return Collections.unmodifiableMap(FACTORIES_BY_MECHANIC_ID);
    }

    @FunctionalInterface
    public interface FactoryConstructor {
        MechanicFactory create(ConfigurationSection section);
    }

}
