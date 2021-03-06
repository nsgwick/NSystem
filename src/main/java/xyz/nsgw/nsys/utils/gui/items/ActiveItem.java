/*
 * Copyright (c) Nicholas Williams 2021.
 */

package xyz.nsgw.nsys.utils.gui.items;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class ActiveItem extends MyItem {

    private final GuiAction<InventoryClickEvent> ACTION;

    public ActiveItem(@NotNull String name, @NotNull Material material, @NotNull GuiAction<InventoryClickEvent> action, @NotNull Component... lore) {
        super(name, material, lore);
        this.ACTION = action;
    }

    public GuiAction<InventoryClickEvent> getACTION() {
        return this.ACTION;
    }

    @Override
    public GuiItem build() {
        return new GuiItem(ItemBuilder.from(getMaterial())
                .name(getName())
                .amount(getQuantity())
                .lore(getLore())
                .build(), ACTION);
    }
}
