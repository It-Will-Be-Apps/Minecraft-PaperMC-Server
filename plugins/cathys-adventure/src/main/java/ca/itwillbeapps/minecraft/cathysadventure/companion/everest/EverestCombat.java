package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

import java.util.UUID;

import org.bukkit.Bukkit;
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

public class EverestCombat {
    private final CathysAdventure plugin;
    private final EverestManager everestManager;

    private BukkitTask everestCombatTask;
    private boolean everestIsEnraged; // TODO Change this to a UUID, boolean map later to support multiple wolves, initialize the map to empty on start()
    private UUID enrageTargetUuid;
    private BukkitTask everestEnrageTimeoutTask;

    public EverestCombat(CathysAdventure plugin, EverestManager everestManager) {
        this.plugin = plugin;
        this.everestManager = everestManager;
    }

    /**
     * Starts Everest's combat loop.
     */
    public void start() {
        if (everestCombatTask != null && !everestCombatTask.isCancelled()) {
            return;
        }

        everestIsEnraged = false; // TODO later, initialize empty map of enraged dogs

        everestCombatTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1l, 5l);

        plugin.getLogger().info("Everest combat loop started");
    }

    /**
     * Processes one combat tick for Everest.
     */
    private void tick() {
        Wolf everest = everestManager.getEverest();

        if (everest == null || everest.isDead() || !everest.isValid() || !everest.isTamed()) {
            return;
        }

        AttributeInstance attackDamageAttribute = everest.getAttribute(Attribute.ATTACK_DAMAGE);
        AttributeInstance maxHealthAttribute = everest.getAttribute(Attribute.MAX_HEALTH);

        if (attackDamageAttribute == null || maxHealthAttribute == null) {
            return;
        }

        PersistentDataContainer data = everest.getPersistentDataContainer();
        boolean damageBonusActive = data.getOrDefault(plugin.getKeys().damageBonusActiveKey, PersistentDataType.BOOLEAN, false);

        if (damageBonusActive && everest.getHealth() <= 1.0) {
            damageBonusActive = false;
            data.set(plugin.getKeys().damageBonusActiveKey, PersistentDataType.BOOLEAN, false);
        } else if (!damageBonusActive && everest.getHealth() >= maxHealthAttribute.getValue()) {
            damageBonusActive = true;
            data.set(plugin.getKeys().damageBonusActiveKey, PersistentDataType.BOOLEAN, true);
        }

        double damageModifier = 0;

        if (everestIsEnraged) {
            damageModifier = 1.0;
        } else if (damageBonusActive) {
            damageModifier = 0.5;
        }

        updateDamageModifier(attackDamageAttribute, damageModifier);

        if (everestIsEnraged) {
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
     * Has Everest attack the enrage target.
     */
    private void attackEnrageTarget() {
        Wolf everest = everestManager.getEverest();
        Entity entity = Bukkit.getEntity(enrageTargetUuid);

        if (entity == null || entity.isDead() || !entity.isValid() || !(entity instanceof LivingEntity target)) {
            stopEnrage();
            return;
        }

        everest.setAngry(true);
        everest.setTarget(target);
    }

    /**
     * Starts the enrage effect for the all active wolves, on the given target.
     *
     * @param target The target to attack
     */
    public void startEnrage(Mob target) {
        Wolf everest = everestManager.getEverest();
        
        if (everest == null || !target.isValid() || target.isDead()) {
            return;
        }
        
        everestIsEnraged = true;
        enrageTargetUuid = target.getUniqueId();
        everest.setTamed(false);
        everest.setAngry(true);
        everest.setTarget(target);

        if (everestEnrageTimeoutTask != null) {
            everestEnrageTimeoutTask.cancel();
        }

        everestEnrageTimeoutTask = Bukkit.getScheduler().runTaskLater(plugin, this::stopEnrage, Settings.GP_MAX_ENRAGE_DURATION);
    }

    /**
     * Stops the enrage effect for all wolves.
     */
    private void stopEnrage() {
        Wolf everest = everestManager.getEverest();

        if (everest == null) {
            return;
        }

        everestIsEnraged = false;
        everest.setAngry(false);
        everest.setTamed(true);
        everest.setSitting(false);

        if (everestEnrageTimeoutTask != null) {
            everestEnrageTimeoutTask.cancel();
            everestEnrageTimeoutTask = null;
        }
    }

    /**
     * Returns whether Everest is currently enraged.
     *
     * @return Whether Everest is currently enraged
     */
    public boolean getEverestIsEnraged() {
        return everestIsEnraged;
    }

    /**
     * Stops Everest's combat loop and resets her rage state and target.
     */
    public void stop() {
        if (everestCombatTask != null) {
            everestCombatTask.cancel();
            everestCombatTask = null;
        }

        if (everestEnrageTimeoutTask != null) {
            everestEnrageTimeoutTask.cancel();
            everestEnrageTimeoutTask = null;
        }

        stopEnrage();

        plugin.getLogger().info("Everest combat loop stopped");
    }
}
