package de.weinschenk.starlink.item;

import de.weinschenk.starlink.Starlink;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Starlink.MODID);

    // Treibstoff-Kanister — aus Erde gewonnen / gecraftet
    public static final RegistryObject<Item> ROCKET_FUEL =
            ITEMS.register("rocket_fuel", () -> new Item(new Item.Properties().stacksTo(16)));

    // Rohstoff: wird zu Fuel verarbeitet (z.B. aus einem neuen Erz)
    public static final RegistryObject<Item> ROCKET_FUEL_RAW =
            ITEMS.register("rocket_fuel_raw", () -> new Item(new Item.Properties().stacksTo(64)));

    // Orbit-Brille: Helm-Slot, zeigt alle Satelliten global
    public static final RegistryObject<Item> ORBIT_GLASSES =
            ITEMS.register("orbit_glasses", OrbitGlassesItem::new);

    // Empfänger-Brille: zeigt nur Satelliten in Empfänger-Reichweite (±100 Blöcke)
    public static final RegistryObject<Item> RECEIVER_GLASSES =
            ITEMS.register("receiver_glasses", ReceiverGlassesItem::new);

    // Satellitenverbinder: verknüpft Transmitter mit Receiver
    public static final RegistryObject<Item> LINK_TOOL =
            ITEMS.register("link_tool", LinkToolItem::new);

    // Satellit V2: wird in der Satelliten-Workbench gecraftet, in Rakete V2 geladen
    public static final RegistryObject<Item> SATELLITE =
            ITEMS.register("satellite", SatelliteItem::new);
}
