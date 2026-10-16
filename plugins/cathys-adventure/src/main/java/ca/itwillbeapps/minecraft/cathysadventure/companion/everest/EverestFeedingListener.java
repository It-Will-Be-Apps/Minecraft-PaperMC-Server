package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import ca.itwillbeapps.minecraft.cathysadventure.items.cheese.CheeseItem;
import ca.itwillbeapps.minecraft.cathysadventure.items.summoninghorn.SummoningHornItem;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;
import net.kyori.adventure.text.Component;

public class EverestFeedingListener implements Listener {
    private final PlayerManager playerManager;
    private final CheeseItem cheeseItem;
    private final SummoningHornItem summoningHornItem;
    private final EverestEntity everestEntity;

    public EverestFeedingListener(PlayerManager playerManager, CheeseItem cheeseItem, SummoningHornItem summoningHornItem, EverestEntity everestEntity) {
        this.playerManager = playerManager;
        this.cheeseItem = cheeseItem;
        this.summoningHornItem = summoningHornItem;
        this.everestEntity = everestEntity;
    }

    /**
     * Listens to interactions with Everest to handle taming and feeding.
     *
     * @param event The interaction event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEverestInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof Wolf wolf)) {
            return;
        }

        if (!everestEntity.isEverest(wolf)) {
            return;
        }

        Player player = event.getPlayer();
        Player cathy = playerManager.getCathy();

        ItemStack item = player.getInventory().getItemInMainHand();
        boolean isCheese = cheeseItem.isCheese(item);

        if (!wolf.isTamed()) {
            event.setCancelled(true);

            if (cathy == null || !cathy.getUniqueId().equals(player.getUniqueId()) || !isCheese) {
                return;
            }

            consumeItem(player, item);

            wolf.customName(Component.text("Everest"));
            wolf.setTamed(true);
            wolf.setOwner(cathy);
            wolf.setSitting(false);
            wolf.playEffect(EntityEffect.TAMING_SUCCEEDED);
            wolf.getWorld().dropItemNaturally(wolf.getLocation(), summoningHornItem.create());
            wolf.getWorld().dropItemNaturally(wolf.getLocation(), new ItemStack(Material.MAP));

            player.sendMessage("Everest trusts you now!");
            return;
        }

        if (isCheese) {
            AttributeInstance maxHealthAttribute = wolf.getAttribute(Attribute.MAX_HEALTH);
            
            if (maxHealthAttribute != null) {
                if (wolf.getHealth() < maxHealthAttribute.getValue()) {
                    event.setCancelled(true);
                    wolf.setHealth(Math.min(maxHealthAttribute.getValue(), wolf.getHealth() + Settings.GP_CHEESE_HEAL_AMOUNT));
                    consumeItem(player, item);
                } else if (wolf.getLoveModeTicks() == 0) {
                    event.setCancelled(true);
                    wolf.setLoveModeTicks(600);
                    wolf.playEffect(EntityEffect.LOVE_HEARTS);
                    consumeItem(player, item);
                }
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
