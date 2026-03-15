package de.weinschenk.starlink.dimension;

import de.weinschenk.starlink.Starlink;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

public class ModDimensions {

    public static final ResourceKey<Level> ORBIT_LEVEL_KEY = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(Starlink.MODID, "orbit")
    );

    public static final ResourceKey<LevelStem> ORBIT_LEVEL_STEM = ResourceKey.create(
            Registries.LEVEL_STEM,
            new ResourceLocation(Starlink.MODID, "orbit")
    );

    public static final ResourceKey<DimensionType> ORBIT_DIMENSION_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            new ResourceLocation(Starlink.MODID, "orbit_type")
    );
}
