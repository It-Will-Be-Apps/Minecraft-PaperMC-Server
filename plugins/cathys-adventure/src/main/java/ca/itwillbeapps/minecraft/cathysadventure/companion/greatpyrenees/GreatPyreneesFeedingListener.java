package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

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
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.everest.EverestEntity;
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.obelix.ObelixEntity;
import ca.itwillbeapps.minecraft.cathysadventure.items.cheese.CheeseItem;
import ca.itwillbeapps.minecraft.cathysadventure.items.summoninghorn.SummoningHornItem;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;
import net.kyori.adventure.text.Component;

public class GreatPyreneesFeedingListener implements Listener {
    private final PlayerManager playerManager;
    private final CheeseItem cheeseItem;
    private final SummoningHornItem summoningHornItem;
    private final EverestEntity everestEntity;
    private final ObelixEntity obelixEntity;

    public GreatPyreneesFeedingListener(PlayerManager playerManager, CheeseItem cheeseItem, SummoningHornItem summoningHornItem,
            EverestEntity everestEntity, ObelixEntity obelixEntity) {

        this.playerManager = playerManager;
        this.cheeseItem = cheeseItem;
        this.summoningHornItem = summoningHornItem;
        this.everestEntity = everestEntity;
        this.obelixEntity = obelixEntity;
    }

    /**
     * Listens to interactions with great pyrenees to handle taming and
     * feeding.
     *
     * @param event The interaction event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof Wolf wolf)) {
            return;
        }

        if (!everestEntity.isEverest(wolf) && !obelixEntity.isObelix(wolf)) { // TODO Also check for puppies
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        boolean isCheese = cheeseItem.isCheese(item);

        if (!wolf.isTamed()) {
            event.setCancelled(true);

            Player cathy = playerManager.getCathy();

            if (cathy == null || !cathy.getUniqueId().equals(player.getUniqueId()) || !isCheese) {
                return;
            }

            if (everestEntity.isEverest(wolf)) {
                tameEverest(wolf, cathy, item);
            } else if (obelixEntity.isObelix(wolf)) {
                tameObelix(wolf, cathy, item);
            }

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
     * Tames Everest.
     *
     * @param everest The Everest entity 
     * @param cathy The Cathy player
     * @param cheese The cheese item
     */
    private void tameEverest(Wolf everest, Player cathy, ItemStack cheese) {
        consumeItem(cathy, cheese);

        everest.customName(Component.text("Everest"));
        everest.setTamed(true);
        everest.setOwner(cathy);
        everest.setSitting(false);
        everest.playEffect(EntityEffect.TAMING_SUCCEEDED);
        everest.getWorld().dropItemNaturally(everest.getLocation(), summoningHornItem.create());
        everest.getWorld().dropItemNaturally(everest.getLocation(), new ItemStack(Material.MAP));

        cathy.sendMessage("Everest trusts you now!");
    }

    /**
     * Tames Obelix.
     *
     * @param obelix The Obelix entity 
     * @param cathy The Cathy player
     * @param cheese The cheese item
     */
    private void tameObelix(Wolf obelix, Player cathy, ItemStack cheese) {
        consumeItem(cathy, cheese);

        obelix.customName(Component.text("Obélix"));
        obelix.setTamed(true);
        obelix.setOwner(cathy);
        obelix.setSitting(false);
        obelix.playEffect(EntityEffect.TAMING_SUCCEEDED);
        // obelix.getWorld().dropItemNaturally(obelix.getLocation(), summoningHornItem.create()); // TODO drop the metal bead
        // obelix.getWorld().dropItemNaturally(obelix.getLocation(), new ItemStack(Material.MAP)); // TODO drop the map to Grogu

        cathy.sendMessage("Obélix trusts you now!");
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
