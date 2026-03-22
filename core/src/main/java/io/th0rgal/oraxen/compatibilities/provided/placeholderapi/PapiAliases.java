package io.th0rgal.oraxen.compatibilities.provided.placeholderapi;

import org.bukkit.entity.Player;

/**
 * Minimal fallback used when PlaceholderAPI compatibility is removed.
 */
public final class PapiAliases {

    private PapiAliases() {}

    public static String setPlaceholders(Player player, String text) {
        return text;
    }
}

