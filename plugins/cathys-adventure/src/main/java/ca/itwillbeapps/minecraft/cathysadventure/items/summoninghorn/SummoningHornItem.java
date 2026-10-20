package ca.itwillbeapps.minecraft.cathysadventure.items.summoninghorn;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class SummoningHornItem {
    private static final String ITEM_ID = "SUMMONING_HORN";

    private final CathysAdventure plugin;

    public SummoningHornItem(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a summoning horn.
     *
     * @return The created summoning horn item
     */
    public ItemStack create() {
        ItemStack summoningHorn = ItemStack.of(Settings.SUMMONING_HORN_BASE_MATERIAL);

        summoningHorn.editMeta(meta -> {
            meta.displayName(Component.text("Summoning Horn", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(Component.text("Summons the most loyals of companions to your side.", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(plugin.getKeys().itemIdKey, PersistentDataType.STRING, ITEM_ID);
            meta.getPersistentDataContainer().set(plugin.getKeys().soulboundItemKey, PersistentDataType.BOOLEAN, true);
        });

        return summoningHorn;
    }

    /**
     * Verifies if the given item is a summoning horn.
     *
     * @param item The item to verify
     * @return Whether the item is a summoning horn
     */
    public boolean isSummoningHorn(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        if (item.getType() != Settings.SUMMONING_HORN_BASE_MATERIAL) {
            return false;
        }

        return ITEM_ID.equals(item.getPersistentDataContainer().get(plugin.getKeys().itemIdKey, PersistentDataType.STRING));
    }
}
