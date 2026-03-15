package de.weinschenk.starlink.block.wireless;

import de.weinschenk.starlink.Starlink;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;

public class ModWirelessBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Starlink.MODID);

    public static final RegistryObject<BlockEntityType<EnergyTransmitterBlockEntity>> ENERGY_TRANSMITTER =
            BLOCK_ENTITIES.register("energy_transmitter", () -> BlockEntityType.Builder
                    .of(EnergyTransmitterBlockEntity::new, blocks(ModWirelessBlocks.ENERGY_TRANSMITTERS))
                    .build(null));

    public static final RegistryObject<BlockEntityType<EnergyReceiverBlockEntity>> ENERGY_RECEIVER =
            BLOCK_ENTITIES.register("energy_receiver", () -> BlockEntityType.Builder
                    .of(EnergyReceiverBlockEntity::new, blocks(ModWirelessBlocks.ENERGY_RECEIVERS))
                    .build(null));

    public static final RegistryObject<BlockEntityType<ItemTransmitterBlockEntity>> ITEM_TRANSMITTER =
            BLOCK_ENTITIES.register("item_transmitter", () -> BlockEntityType.Builder
                    .of(ItemTransmitterBlockEntity::new, blocks(ModWirelessBlocks.ITEM_TRANSMITTERS))
                    .build(null));

    public static final RegistryObject<BlockEntityType<ItemReceiverBlockEntity>> ITEM_RECEIVER =
            BLOCK_ENTITIES.register("item_receiver", () -> BlockEntityType.Builder
                    .of(ItemReceiverBlockEntity::new, blocks(ModWirelessBlocks.ITEM_RECEIVERS))
                    .build(null));

    public static final RegistryObject<BlockEntityType<FluidTransmitterBlockEntity>> FLUID_TRANSMITTER =
            BLOCK_ENTITIES.register("fluid_transmitter", () -> BlockEntityType.Builder
                    .of(FluidTransmitterBlockEntity::new, blocks(ModWirelessBlocks.FLUID_TRANSMITTERS))
                    .build(null));

    public static final RegistryObject<BlockEntityType<FluidReceiverBlockEntity>> FLUID_RECEIVER =
            BLOCK_ENTITIES.register("fluid_receiver", () -> BlockEntityType.Builder
                    .of(FluidReceiverBlockEntity::new, blocks(ModWirelessBlocks.FLUID_RECEIVERS))
                    .build(null));

    @SuppressWarnings("unchecked")
    private static Block[] blocks(RegistryObject<Block>[] arr) {
        return Arrays.stream(arr).map(RegistryObject::get).toArray(Block[]::new);
    }
}
