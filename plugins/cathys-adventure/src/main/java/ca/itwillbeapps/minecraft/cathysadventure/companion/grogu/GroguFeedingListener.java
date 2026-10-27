package ca.itwillbeapps.minecraft.cathysadventure.companion.grogu;

import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import ca.itwillbeapps.minecraft.cathysadventure.items.metealbead.MetalBeadItem;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;

public class GroguFeedingListener implements Listener {
    private final CathysAdventure plugin;
    private final PlayerManager playerManager;
    private final MetalBeadItem metalBeadItem;
    private final GroguEntity groguEntity;

    public GroguFeedingListener(CathysAdventure plugin, PlayerManager playerManager, MetalBeadItem metalBeadItem, GroguEntity groguEntity) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.metalBeadItem = metalBeadItem;
        this.groguEntity = groguEntity;
    }

    /**
     * Listens to interactions with Grogu to handle taming and feeding.
     *
     * @param event The interaction event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof Allay allay)) {
            return;
        }

        if (!groguEntity.isGrogu(allay)) {
            return;
        }

        Player player = event.getPlayer();
        Player cathy = playerManager.getCathy();

        if (cathy == null || !cathy.getUniqueId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        boolean isMetalBead = metalBeadItem.isMetalBead(item);

        if (!allay.getPersistentDataContainer().getOrDefault(plugin.getKeys().tamedKey, PersistentDataType.BOOLEAN, false)) {
            if (!isMetalBead) {
                event.setCancelled(true);
                return;
            }

            allay.getPersistentDataContainer().set(plugin.getKeys().tamedKey, PersistentDataType.BOOLEAN, true);
            allay.playEffect(EntityEffect.TAMING_SUCCEEDED);
            allay.playEffect(EntityEffect.LOVE_HEARTS);
            cathy.sendMessage("Grogu trusts you now!");
            return;
        }

        boolean isAir = item.getType() == Material.AIR;
        boolean isFish = Tag.ITEMS_FISHES.isTagged(item.getType());

        if (!isMetalBead && !isFish && !isAir) {
            event.setCancelled(true);
            return;
        }

        if (isMetalBead) {
            allay.playEffect(EntityEffect.LOVE_HEARTS);
        } else if (isFish) {
            event.setCancelled(true);
            AttributeInstance maxHealthAttribute = allay.getAttribute(Attribute.MAX_HEALTH);
            
            if (maxHealthAttribute != null && allay.getHealth() < maxHealthAttribute.getValue()) {
                allay.setHealth(Math.min(maxHealthAttribute.getValue(), allay.getHealth() + Settings.GROGU_FISH_HEAL_AMOUT));
                allay.playEffect(EntityEffect.LOVE_HEARTS);
                consumeItem(player, item);
            }
        }
    }

    /**
     * Consumes one of the given item.
     *
     * @param player The player who used the item
     * @param item The item that was used
     */
    private void consumeItem(Player player, ItemStack item) {
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(ItemStack.empty());
        } else {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
        }
    }
}
