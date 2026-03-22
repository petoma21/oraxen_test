package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.OraxenPack;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class ReloadCommand {

    public static void reloadItems(@Nullable CommandSender sender) {
        Message.RELOAD.send(sender, AdventureUtils.tagResolver("reloaded", "items"));
        OraxenItems.loadItems();
        OraxenPlugin.get().getInvManager().regen();
        Bukkit.getPluginManager().callEvent(new OraxenItemsLoadedEvent());

        if (Settings.UPDATE_ITEMS.toBool() && Settings.UPDATE_ITEMS_ON_RELOAD.toBool()) {
            Message.UPDATING_USER_ITEMS.log();
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                // Use runForEntity for Folia compatibility - inventory must be accessed on player's region thread
                SchedulerUtil.runForEntity(player, () -> {
                    PlayerInventory inventory = player.getInventory();
                    for (int i = 0; i < inventory.getSize(); i++) {
                        ItemStack oldItem = inventory.getItem(i);
                        ItemStack newItem = ItemUpdater.updateItem(oldItem);
                        if (oldItem == null || oldItem.equals(newItem))
                            continue;
                        inventory.setItem(i, newItem);
                    }
                });
            }
        }
    }

    public static void reloadPack(@Nullable CommandSender sender) {
        Message.PACK_REGENERATED.send(sender);
        OraxenPack.reloadPack();
    }

    CommandAPICommand getReloadCommand() {
        return new CommandAPICommand("reload")
                .withAliases("rl")
                .withPermission("oraxen.command.reload")
                .withArguments(new TextArgument("type").replaceSuggestions(
                        ArgumentSuggestions.strings("items", "pack", "configs", "all")))
                .executes((sender, args) -> {
                    switch (((String) args.get("type")).toUpperCase()) {
                        case "ITEMS" -> reloadItems(sender);
                        case "PACK" -> reloadPack(sender);
                        case "CONFIGS" -> OraxenPlugin.get().reloadConfigs();
                        default -> {
                            MechanicsManager.unloadListeners();
                            MechanicsManager.unregisterTasks();
                            OraxenPlugin.get().reloadConfigs();
                            MechanicsManager.registerNativeMechanics();
                            reloadItems(sender);
                            reloadPack(sender);
                        }
                    }
                });
    }
}
