package ca.itwillbeapps.minecraft.cathysadventure;

import org.bukkit.Material;
import org.bukkit.block.Biome;

public class Settings {

    // Location settings
    public static final int LOCATION_SEARCH_RADIUS = 8;
    public static final int BIOME_SEARCH_RADIUS = 2000;
    public static final int LOCATION_HEIGHT_RANGE = 10;
    public static final int MAX_LOCATION_HEIGHT_RANGE = 200;
    public static final int MAX_LOCATION_SEARCH_ATTEMPTS = 10;

    // Cheese settings
    public static final Material CHEESE_BASE_MATERIAL = Material.HONEYCOMB;
    public static final int CHEESE_NUTRITION = 4;
    public static final float CHEESE_SATURATION = 0.0f;
    public static final float CHEESE_CONSUMPTION_SECONDS = 1.6f;
    public static final float CHEESE_COOKING_EXPERIENCE = 0.0f;
    public static final int CHEESE_FURNACE_COOKING_TIME_TICKS = 200;
    public static final int CHEESE_SMOKER_COOKING_TIME_TICKS = 100;
    public static final int CHEESE_CAMPFIRE_COOKING_TIME_TICKS = 600;

    // Summoning horn settings
    public static final Material SUMMONING_HORN_BASE_MATERIAL = Material.GOAT_HORN;

    // Fetch ball settings
    public static final Material FETCH_BALL_BASE_MATERIAL = Material.SNOWBALL;

    // Metal bead settings
    public static final Material METAL_BEAD_BASE_MATERIAL = Material.SNOWBALL;

    // Companions settings
    public static final double COMPANIONS_MINIMUM_HEALTH = 1.0;
    
    // Everest settings
    public static final int EVEREST_THREAT_RADIUS = 16;
    public static final int EVEREST_SAFE_DISTANCE = 24;
    public static final int EVEREST_APPROACH_THRESHOLD = 32;
    public static final double EVEREST_STALKING_SPEED = 1.0;
    public static final double EVEREST_FLEEING_SPEED = 1.2;
    public static final double EVEREST_CHEESE_SPEED = 1.5;

    // Obélix settings
    public static final Biome OBELIX_HOME_BIOME = Biome.SNOWY_PLAINS;

    // Great Pyrenees settings
    public static final int GP_TELEPORT_THRESHOLD = 64;
    public static final int GP_SUMMON_DISTANCE = 1;
    public static final long GP_MAX_ENRAGE_DURATION = 30l * 20l;
    public static final double GP_CHEESE_HEAL_AMOUNT = 4.0;

    // Grogu settings
    public static final int GROGU_MIN_ROAMING_DELAY = 2;
    public static final int GROGU_MAX_ROAMING_DELAY = 5;
    public static final int GROGU_ROAMING_RADIUS = 5;
    public static final int GROGU_TELEPORT_THRESHOLD = 16;
    public static final int GROGU_SUMMON_DISTANCE = 3;
    public static final double GROGU_FISH_HEAL_AMOUT = 6.0;
}
