package ca.itwillbeapps.minecraft.cathysadventure.bootstrap;

import org.bukkit.entity.Wolf.Variant;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.data.WolfVariantRegistryEntry.Builder;
import io.papermc.paper.registry.data.client.ClientTextureAsset;
import io.papermc.paper.registry.event.RegistryComposeEvent;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.WolfVariantKeys;
import net.kyori.adventure.key.Key;

public class CathysAdventureBootstrap implements PluginBootstrap {
    public static final String FEMALE_GP_VARIANT = "cathys-adventure:femalegp";
    public static final String FEMALE_GP_WILD_TEXTURE = "cathys-adventure:entity/wolf/femalegp";
    public static final String FEMALE_GP_ANGRY_TEXTURE = "cathys-adventure:entity/wolf/femalegp_angry";
    public static final String FEMALE_GP_TAMED_TEXTURE = "cathys-adventure:entity/wolf/femalegp_tame";
    public static final String FEMALE_GP_BABY_TEXTURE = "cathys-adventure:entity/wolf/femalegp_baby";
    public static final String FEMALE_GP_BABY_ANGRY_TEXTURE = "cathys-adventure:entity/wolf/femalegp_baby_angry";
    public static final String FEMALE_GP_BABY_TAMED_TEXTURE = "cathys-adventure:entity/wolf/femalegp_baby_tame";

    public static final String MALE_GP_VARIANT = "cathys-adventure:malegp";
    public static final String MALE_GP_WILD_TEXTURE = "cathys-adventure:entity/wolf/malegp";
    public static final String MALE_GP_ANGRY_TEXTURE = "cathys-adventure:entity/wolf/malegp_angry";
    public static final String MALE_GP_TAMED_TEXTURE = "cathys-adventure:entity/wolf/malegp_tame";
    public static final String MALE_GP_BABY_TEXTURE = "cathys-adventure:entity/wolf/malegp_baby";
    public static final String MALE_GP_BABY_ANGRY_TEXTURE = "cathys-adventure:entity/wolf/malegp_baby_angry";
    public static final String MALE_GP_BABY_TAMED_TEXTURE = "cathys-adventure:entity/wolf/malegp_baby_tame";

    /**
     * Bootstraps the plugin and registers the new mob variants.
     */
    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.WOLF_VARIANT.compose(), this::registerMobVariants);
    }

    /**
     * Registers all mob variants and textures for the plugin.
     *
     * @param event The registry compose event
     */
    private void registerMobVariants(RegistryComposeEvent<Variant, Builder> event) {
        registerFemaleGreatPyreneesVariant(event);
        registerMaleGreatPyreneesVariant(event);
    }

    /**
     * Registers a wolf variant for female great pyrenees dogs.
     *
     * @param event The registry compose event
     */
    private void registerFemaleGreatPyreneesVariant(RegistryComposeEvent<Variant, Builder> event) {
        event.registry().register(WolfVariantKeys.create(Key.key(FEMALE_GP_VARIANT)), builder -> builder
            .wildClientTextureAsset(ClientTextureAsset.clientTextureAsset(FEMALE_GP_WILD_TEXTURE))
            .angryClientTextureAsset(ClientTextureAsset.clientTextureAsset(FEMALE_GP_ANGRY_TEXTURE))
            .tameClientTextureAsset(ClientTextureAsset.clientTextureAsset(FEMALE_GP_TAMED_TEXTURE))
            .babyWildClientTextureAsset(ClientTextureAsset.clientTextureAsset(FEMALE_GP_BABY_TEXTURE))
            .babyAngryClientTextureAsset(ClientTextureAsset.clientTextureAsset(FEMALE_GP_BABY_ANGRY_TEXTURE))
            .babyTameClientTextureAsset(ClientTextureAsset.clientTextureAsset(FEMALE_GP_BABY_TAMED_TEXTURE))
        );
    }

    /**
     * Registers a wolf variant for male great pyrenees dogs.
     *
     * @param event The registry compose event
     */
    private void registerMaleGreatPyreneesVariant(RegistryComposeEvent<Variant, Builder> event) {
        event.registry().register(WolfVariantKeys.create(Key.key(MALE_GP_VARIANT)), builder -> builder
            .wildClientTextureAsset(ClientTextureAsset.clientTextureAsset(MALE_GP_WILD_TEXTURE))
            .angryClientTextureAsset(ClientTextureAsset.clientTextureAsset(MALE_GP_ANGRY_TEXTURE))
            .tameClientTextureAsset(ClientTextureAsset.clientTextureAsset(MALE_GP_TAMED_TEXTURE))
            .babyWildClientTextureAsset(ClientTextureAsset.clientTextureAsset(MALE_GP_BABY_TEXTURE))
            .babyAngryClientTextureAsset(ClientTextureAsset.clientTextureAsset(MALE_GP_BABY_ANGRY_TEXTURE))
            .babyTameClientTextureAsset(ClientTextureAsset.clientTextureAsset(MALE_GP_BABY_TAMED_TEXTURE))
        );
    }
}
