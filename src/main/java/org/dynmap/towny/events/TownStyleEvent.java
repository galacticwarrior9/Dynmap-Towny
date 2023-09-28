package org.dynmap.towny.events;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.dynmap.towny.objects.TownStyle;
import org.jetbrains.annotations.NotNull;

public class TownStyleEvent extends Event {
    private static HandlerList handlers = new HandlerList();
    private final Town town;
    private final TownStyle style;

    public TownStyleEvent(@NotNull Town town, @NotNull TownStyle style) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.town = town;
        this.style = style;
    }

    /**
     *
     * @return the TownStyle of the town; this is mutable
     */
    @NotNull
    public TownStyle getStyle() {
        return style;
    }

    @NotNull
    public Town getTown() {
        return town;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
