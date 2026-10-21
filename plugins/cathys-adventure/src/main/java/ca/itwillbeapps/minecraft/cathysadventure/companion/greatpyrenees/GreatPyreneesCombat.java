package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Wolf;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;

public class GreatPyreneesCombat {
    private final CathysAdventure plugin;
    private final GreatPyreneesManager greatPyreneesManager;

    private BukkitTask greatPyreneesCombatTask;
    private boolean greatPyreneesAreEnraged;
    private UUID greatPyreneesEnrageTargetUuid;
    private BukkitTask greatPyreneesEnrageTimeoutTask;

    public GreatPyreneesCombat(CathysAdventure plugin, GreatPyreneesManager greatPyreneesManager) {
        this.plugin = plugin;
        this.greatPyreneesManager = greatPyreneesManager;
    }

    /**
     * Starts the great pyrenees' combat loop.
     */
    public void start() {
        if (greatPyreneesCombatTask != null && !greatPyreneesCombatTask.isCancelled()) {
            return;
        }

        greatPyreneesAreEnraged = false;

        greatPyreneesCombatTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1l, 5l);

        plugin.getLogger().info("Great Pyrenees combat loop started");
    }

    /**
     * Processes one combat tick for the great pyrenees.
     */
    private void tick() {
        for (Wolf wolf : greatPyreneesManager.getGreatPyrenees()) {
            if (!wolf.isDead() && wolf.isValid() && wolf.isTamed()) {
                AttributeInstance attackDamageAttribute = wolf.getAttribute(Attribute.ATTACK_DAMAGE);
                AttributeInstance maxHealthAttribute = wolf.getAttribute(Attribute.MAX_HEALTH);

                if (attackDamageAttribute == null || maxHealthAttribute == null) {
                    continue;
                }

                PersistentDataContainer data = wolf.getPersistentDataContainer();
                boolean damageBonusActive = data.getOrDefault(plugin.getKeys().damageBonusActiveKey, PersistentDataType.BOOLEAN, false);

                if (damageBonusActive && wolf.getHealth() <= 1.0) {
                    damageBonusActive = false;
                    data.set(plugin.getKeys().damageBonusActiveKey, PersistentDataType.BOOLEAN, false);
                } else if (!damageBonusActive && wolf.getHealth() >= maxHealthAttribute.getValue()) {
                    damageBonusActive = true;
                    data.set(plugin.getKeys().damageBonusActiveKey, PersistentDataType.BOOLEAN, true);
                }

                double damageModifier = 0;

                if (greatPyreneesAreEnraged) {
                    damageModifier = 1.0;
                } else if (damageBonusActive) {
                    damageModifier = 0.5;
                }

                updateDamageModifier(attackDamageAttribute, damageModifier);
            }
        }

        if (greatPyreneesAreEnraged) {
            attackEnrageTarget();
        }
    }

    /**
     * Updates the damage modifier for the given attack damage attribute. If
     * the specified value is zero, the damage modifier will be removed.
     *
     * @param attackDamageAttribute The attack damage attribute to update
     * @param newDamageModifierValue The multiplicative damage modifier value
     *        to apply
     */
    private void updateDamageModifier(AttributeInstance attackDamageAttribute, double newDamageModifierValue) {
        if (newDamageModifierValue == 0) {
            attackDamageAttribute.removeModifier(plugin.getKeys().damageModifierKey);
            return;
        }

        AttributeModifier currentDamageModifier = attackDamageAttribute.getModifier(plugin.getKeys().damageModifierKey);

        if (currentDamageModifier != null) {
            double currentDamageModifierValue = currentDamageModifier.getAmount();

            if (currentDamageModifierValue == newDamageModifierValue && currentDamageModifier.getOperation() == AttributeModifier.Operation.MULTIPLY_SCALAR_1) {
                return;
            }

            attackDamageAttribute.removeModifier(currentDamageModifier);
        }

        AttributeModifier newDamageModifier = new AttributeModifier(plugin.getKeys().damageModifierKey, newDamageModifierValue, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        attackDamageAttribute.addModifier(newDamageModifier);
    }

    /**
     * Has the given great pyrenees attack the enrage target.
     */
    private void attackEnrageTarget() {
        Entity entity = plugin.getServer().getEntity(greatPyreneesEnrageTargetUuid);

        if (entity == null || entity.isDead() || !entity.isValid() || !(entity instanceof LivingEntity target)) {
            stopEnrage();
            return;
        }

        for (Wolf wolf : greatPyreneesManager.getGreatPyrenees()) {
            if (wolf.isTamed()) {
                wolf.setAngry(true);
                wolf.setTarget(target);
            }
        }
    }

    /**
     * Starts the enrage effect for the all active great pyrenees, on the given
     * target.
     *
     * @param target The target to attack
     */
    public void startEnrage(Mob target) {
        greatPyreneesAreEnraged = true;
        greatPyreneesEnrageTargetUuid = target.getUniqueId();

        for (Wolf wolf : greatPyreneesManager.getGreatPyrenees()) {
            if (!wolf.isDead() && wolf.isValid() && wolf.isTamed()) {
                wolf.setTamed(false);
                wolf.setAngry(true);
                wolf.setTarget(target);
            }
        }

        if (greatPyreneesEnrageTimeoutTask != null) {
            greatPyreneesEnrageTimeoutTask.cancel();
        }

        greatPyreneesEnrageTimeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::stopEnrage, Settings.GP_MAX_ENRAGE_DURATION);
    }

    /**
     * Stops the enrage effect for all great pyrenees.
     */
    private void stopEnrage() {
        greatPyreneesAreEnraged = false;

        for (Wolf wolf : greatPyreneesManager.getGreatPyrenees()) {
            if (!wolf.isDead() && wolf.isValid()) {
                wolf.setAngry(false);
                wolf.setTamed(true);
                wolf.setSitting(false);
            }
        }

        if (greatPyreneesEnrageTimeoutTask != null) {
            greatPyreneesEnrageTimeoutTask.cancel();
            greatPyreneesEnrageTimeoutTask = null;
        }
    }

    /**
     * Returns whether the great pyrenees are currently enraged.
     *
     * @return Whether the great pyrenees are currently enraged
     */
    public boolean getGreatPyreneesAreEnraged() {
        return greatPyreneesAreEnraged;
    }

    /**
     * Stops the great pyrenees' combat loop and resets their rage state and
     * target.
     */
    public void stop() {
        if (greatPyreneesCombatTask != null) {
            greatPyreneesCombatTask.cancel();
            greatPyreneesCombatTask = null;
        }

        if (greatPyreneesEnrageTimeoutTask != null) {
            greatPyreneesEnrageTimeoutTask.cancel();
            greatPyreneesEnrageTimeoutTask = null;
        }

        stopEnrage();

        plugin.getLogger().info("Great Pyrenees combat loop stopped");
    }
}
