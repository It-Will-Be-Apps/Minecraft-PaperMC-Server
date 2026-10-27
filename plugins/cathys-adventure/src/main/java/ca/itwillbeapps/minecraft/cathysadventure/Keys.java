package ca.itwillbeapps.minecraft.cathysadventure;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class Keys {
    private static final String ITEM_ID_KEY = "ITEM_ID";
    private static final String SOULBOUND_ITEM_KEY = "SOULBOUND_ITEM";
    private static final String CHEESE_FURNACE_RECIPE_ID_KEY = "CHEESE_FURNACE_RECIPE";
    private static final String CHEESE_SMOKER_RECIPE_ID_KEY = "CHEESE_SMOKER_RECIPE";
    private static final String CHEESE_CAMPFIRE_RECIPE_ID_KEY = "CHEESE_CAMPFIRE_RECIPE";
    private static final String ENTITY_ID_KEY = "ENTITY_ID";
    private static final String DAMAGE_BONUS_ACTIVE_KEY = "DAMAGE_BONUS_ACTIVE";
    private static final String DAMAGE_MODIFIER_KEY = "DAMAGE_MODIFIER_KEY";
    private static final String OBELIX_HOME_LOCATION_KEY = "OBELIX_HOME_LOCATION_KEY";
    private static final String GROGU_JAIL_LOCATION_KEY = "GROGU_JAIL_LOCATION_KEY";
    private static final String TAMED_KEY = "TAMED_KEY";

    public final NamespacedKey itemIdKey;
    public final NamespacedKey soulboundItemKey;
    public final NamespacedKey cheeseFurnaceRecipeIdKey;
    public final NamespacedKey cheeseSmokerRecipeIdKey;
    public final NamespacedKey cheeseCampfireRecipeIdKey;
    public final NamespacedKey entityIdKey;
    public final NamespacedKey damageBonusActiveKey;
    public final NamespacedKey damageModifierKey;
    public final NamespacedKey obelixHomeLocationKey;
    public final NamespacedKey groguJailLocationKey;
    public final NamespacedKey tamedKey;

    public Keys(Plugin plugin) {
        itemIdKey = new NamespacedKey(plugin, ITEM_ID_KEY);
        soulboundItemKey = new NamespacedKey(plugin, SOULBOUND_ITEM_KEY);
        cheeseFurnaceRecipeIdKey = new NamespacedKey(plugin, CHEESE_FURNACE_RECIPE_ID_KEY);
        cheeseSmokerRecipeIdKey = new NamespacedKey(plugin, CHEESE_SMOKER_RECIPE_ID_KEY);
        cheeseCampfireRecipeIdKey = new NamespacedKey(plugin, CHEESE_CAMPFIRE_RECIPE_ID_KEY);
        entityIdKey = new NamespacedKey(plugin, ENTITY_ID_KEY);
        damageBonusActiveKey = new NamespacedKey(plugin, DAMAGE_BONUS_ACTIVE_KEY);
        damageModifierKey = new NamespacedKey(plugin, DAMAGE_MODIFIER_KEY);
        obelixHomeLocationKey = new NamespacedKey(plugin, OBELIX_HOME_LOCATION_KEY);
        groguJailLocationKey = new NamespacedKey(plugin, GROGU_JAIL_LOCATION_KEY);
        tamedKey = new NamespacedKey(plugin, TAMED_KEY);
    }
}
