package de.weinschenk.starlink.item;

import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.entity.SatelliteType;
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

    // Satellit-Varianten: jede Version hat andere Bandbreiten-Multiplikatoren
    public static final RegistryObject<Item> SATELLITE_BASIC =
            ITEMS.register("satellite_basic",    () -> new SatelliteItem(SatelliteType.BASIC));
    public static final RegistryObject<Item> SATELLITE_ENERGY =
            ITEMS.register("satellite_energy",   () -> new SatelliteItem(SatelliteType.ENERGY));
    public static final RegistryObject<Item> SATELLITE_FLUID =
            ITEMS.register("satellite_fluid",    () -> new SatelliteItem(SatelliteType.FLUID));
    public static final RegistryObject<Item> SATELLITE_ITEM =
            ITEMS.register("satellite_item",     () -> new SatelliteItem(SatelliteType.ITEM));
    public static final RegistryObject<Item> SATELLITE_ADVANCED =
            ITEMS.register("satellite_advanced", () -> new SatelliteItem(SatelliteType.ADVANCED));

    // ── Neue Materialien ─────────────────────────────────────────────────────

    // Titanium-Erz-Drops: Rohtitanium → geschmolzen → Titanium-Barren
    public static final RegistryObject<Item> RAW_TITANIUM =
            ITEMS.register("raw_titanium", () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> TITANIUM_INGOT =
            ITEMS.register("titanium_ingot", () -> new Item(new Item.Properties().stacksTo(64)));

    // Stahl-Barren: Eisen + Kohle → Mittelstufe für Strukturblöcke
    public static final RegistryObject<Item> STEEL_INGOT =
            ITEMS.register("steel_ingot", () -> new Item(new Item.Properties().stacksTo(64)));

    // Platine: Gold + Redstone + Kupfer → Elektronik-Komponente
    public static final RegistryObject<Item> CIRCUIT_BOARD =
            ITEMS.register("circuit_board", () -> new Item(new Item.Properties().stacksTo(64)));

    // Solarpanel: Glas + Gold + Platine + Stahl → Satelliten-Energieversorgung
    public static final RegistryObject<Item> SOLAR_PANEL =
            ITEMS.register("solar_panel", () -> new Item(new Item.Properties().stacksTo(16)));
}
