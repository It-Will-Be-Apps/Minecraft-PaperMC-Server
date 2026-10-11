package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.persistence.PersistentDataType;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CheeseItem {
    private static final String ITEM_ID = "CHEESE";
    private static final Material BASE_MATERIAL = Material.HONEYCOMB;
    private static final int NUTRITION = 4;
    private static final float SATURATION = 0.0f;
    private static final float CONSUMPTION_SECONDS = 1.6f;

    private final NamespacedKey itemIdKey;

    public CheeseItem(CathysAdventure plugin) {
        this.itemIdKey = new NamespacedKey(plugin, ITEM_ID);
    }

    /**
     * Creates a cheese item.
     *
     * @return The created cheese item
     */
    public ItemStack create() {
        ItemStack cheese = ItemStack.of(BASE_MATERIAL);

        cheese.editMeta(meta -> {
            meta.displayName(Component.text("Cheese", NamedTextColor.YELLOW));
            meta.lore(List.of(Component.text("Rumored to be very popular with some dogs...", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, ITEM_ID);
        });

        cheese.setData(DataComponentTypes.FOOD, FoodProperties.food()
            .nutrition(NUTRITION)
            .saturation(SATURATION)
            .canAlwaysEat(true)
            .build()
        );

        cheese.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
            .consumeSeconds(CONSUMPTION_SECONDS)
            .build()
        );

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

        if (item.getType() != BASE_MATERIAL) {
            return false;
        }

        return ITEM_ID.equals(item.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING));
    }
}
