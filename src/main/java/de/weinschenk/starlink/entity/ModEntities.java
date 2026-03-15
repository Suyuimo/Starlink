package de.weinschenk.starlink.entity;

import de.weinschenk.starlink.Starlink;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Starlink.MODID);

    public static final RegistryObject<EntityType<SatelliteEntity>> SATELLITE =
            ENTITY_TYPES.register("satellite", () ->
                    EntityType.Builder.<SatelliteEntity>of(SatelliteEntity::new, MobCategory.MISC)
                            .sized(30.0f, 5.0f)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("satellite"));

    public static final RegistryObject<EntityType<RocketEntity>> ROCKET =
            ENTITY_TYPES.register("rocket", () ->
                    EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
                            .sized(1.0f, 4.0f)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("rocket"));

    public static final RegistryObject<EntityType<RocketV2Entity>> ROCKET_V2 =
            ENTITY_TYPES.register("rocket_v2", () ->
                    EntityType.Builder.<RocketV2Entity>of(RocketV2Entity::new, MobCategory.MISC)
                            .sized(1.0f, 4.0f)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("rocket_v2"));
}
