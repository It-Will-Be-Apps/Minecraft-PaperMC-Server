package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CheeseItem {
    private static final String ITEM_ID = "CHEESE";

    private final CathysAdventure plugin;

    public CheeseItem(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a cheese item.
     *
     * @return The created cheese item
     */
    public ItemStack create() {
        ItemStack cheese = ItemStack.of(Settings.CHEESE_BASE_MATERIAL);

        cheese.setData(DataComponentTypes.FOOD, FoodProperties.food()
            .nutrition(Settings.CHEESE_NUTRITION)
            .saturation(Settings.CHEESE_SATURATION)
            .canAlwaysEat(true)
            .build()
        );

        cheese.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
            .consumeSeconds(Settings.CHEESE_CONSUMPTION_SECONDS)
            .build()
        );

        cheese.editMeta(meta -> {
            meta.displayName(Component.text("Cheese", NamedTextColor.YELLOW));
            meta.lore(List.of(Component.text("Rumored to be very popular with some dogs...", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(plugin.getKeys().itemIdKey, PersistentDataType.STRING, ITEM_ID);
            meta.setMaxStackSize(1);
        });

        return cheese;
    }

    /**
     * Verifies if the given item is cheese.
     *
     * @param item The item to verify
     * @return Whether the item is cheese
     */
    public boolean isCheese(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        if (item.getType() != Settings.CHEESE_BASE_MATERIAL) {
            return false;
        }

        return ITEM_ID.equals(item.getPersistentDataContainer().get(plugin.getKeys().itemIdKey, PersistentDataType.STRING));
    }
}
