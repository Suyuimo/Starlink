package de.weinschenk.starlink.block.wireless;

import de.weinschenk.starlink.Starlink;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModWirelessBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Starlink.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Starlink.MODID);

    @SuppressWarnings("unchecked")
    public static final RegistryObject<Block>[] ENERGY_TRANSMITTERS = new RegistryObject[8];
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Block>[] ENERGY_RECEIVERS    = new RegistryObject[8];
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Block>[] ITEM_TRANSMITTERS   = new RegistryObject[8];
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Block>[] ITEM_RECEIVERS      = new RegistryObject[8];
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Block>[] FLUID_TRANSMITTERS  = new RegistryObject[8];
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Block>[] FLUID_RECEIVERS     = new RegistryObject[8];

    static {
        for (int i = 0; i < 8; i++) {
            final int tier = i + 1;

            ENERGY_TRANSMITTERS[i] = BLOCKS.register("energy_transmitter_t" + tier,
                    () -> new EnergyTransmitterBlock(tier, baseProps(MapColor.COLOR_YELLOW)));
            ITEMS.register("energy_transmitter_t" + tier,
                    () -> new BlockItem(ENERGY_TRANSMITTERS[tier - 1].get(), new Item.Properties()));

            ENERGY_RECEIVERS[i] = BLOCKS.register("energy_receiver_t" + tier,
                    () -> new EnergyReceiverBlock(tier, baseProps(MapColor.COLOR_YELLOW)));
            ITEMS.register("energy_receiver_t" + tier,
                    () -> new BlockItem(ENERGY_RECEIVERS[tier - 1].get(), new Item.Properties()));

            ITEM_TRANSMITTERS[i] = BLOCKS.register("item_transmitter_t" + tier,
                    () -> new ItemTransmitterBlock(tier, baseProps(MapColor.COLOR_CYAN)));
            ITEMS.register("item_transmitter_t" + tier,
                    () -> new BlockItem(ITEM_TRANSMITTERS[tier - 1].get(), new Item.Properties()));

            ITEM_RECEIVERS[i] = BLOCKS.register("item_receiver_t" + tier,
                    () -> new ItemReceiverBlock(tier, baseProps(MapColor.COLOR_CYAN)));
            ITEMS.register("item_receiver_t" + tier,
                    () -> new BlockItem(ITEM_RECEIVERS[tier - 1].get(), new Item.Properties()));

            FLUID_TRANSMITTERS[i] = BLOCKS.register("fluid_transmitter_t" + tier,
                    () -> new FluidTransmitterBlock(tier, baseProps(MapColor.COLOR_BLUE)));
            ITEMS.register("fluid_transmitter_t" + tier,
                    () -> new BlockItem(FLUID_TRANSMITTERS[tier - 1].get(), new Item.Properties()));

            FLUID_RECEIVERS[i] = BLOCKS.register("fluid_receiver_t" + tier,
                    () -> new FluidReceiverBlock(tier, baseProps(MapColor.COLOR_BLUE)));
            ITEMS.register("fluid_receiver_t" + tier,
                    () -> new BlockItem(FLUID_RECEIVERS[tier - 1].get(), new Item.Properties()));
        }
    }

    private static BlockBehaviour.Properties baseProps(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(3.0f, 6.0f)
                .sound(SoundType.METAL)
                .noOcclusion();
    }
}
