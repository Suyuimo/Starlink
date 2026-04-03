package de.weinschenk.starlink.item;

import de.weinschenk.starlink.entity.SatelliteType;
import net.minecraft.world.item.Item;

public class SatelliteItem extends Item {

    private final SatelliteType satType;

    public SatelliteItem(SatelliteType type) {
        super(new Item.Properties().stacksTo(16));
        this.satType = type;
    }

    public SatelliteType getSatelliteType() {
        return satType;
    }
}
