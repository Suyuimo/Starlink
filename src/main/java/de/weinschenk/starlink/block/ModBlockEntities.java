package de.weinschenk.starlink.block;

import de.weinschenk.starlink.Starlink;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Starlink.MODID);

    public static final RegistryObject<BlockEntityType<ReceiverBlockEntity>> RECEIVER =
            BLOCK_ENTITIES.register("receiver",
                    () -> BlockEntityType.Builder
                            .of(ReceiverBlockEntity::new, ModBlocks.RECEIVER.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<LaunchControllerBlockEntity>> LAUNCH_CONTROLLER =
            BLOCK_ENTITIES.register("launch_controller",
                    () -> BlockEntityType.Builder
                            .of(LaunchControllerBlockEntity::new, ModBlocks.LAUNCH_CONTROLLER.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<DistillationChamberBlockEntity>> DISTILLATION_CHAMBER =
            BLOCK_ENTITIES.register("distillation_chamber",
                    () -> BlockEntityType.Builder
                            .of(DistillationChamberBlockEntity::new, ModBlocks.DISTILLATION_CHAMBER.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<RocketV2BlockEntity>> ROCKET_V2 =
            BLOCK_ENTITIES.register("rocket_v2",
                    () -> BlockEntityType.Builder
                            .of(RocketV2BlockEntity::new, ModBlocks.ROCKET_V2.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<LaunchControllerV2BlockEntity>> LAUNCH_CONTROLLER_V2 =
            BLOCK_ENTITIES.register("launch_controller_v2",
                    () -> BlockEntityType.Builder
                            .of(LaunchControllerV2BlockEntity::new, ModBlocks.LAUNCH_CONTROLLER_V2.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<InfiniteEnergyCellBlockEntity>> INFINITE_ENERGY_CELL =
            BLOCK_ENTITIES.register("infinite_energy_cell",
                    () -> BlockEntityType.Builder
                            .of(InfiniteEnergyCellBlockEntity::new, ModBlocks.INFINITE_ENERGY_CELL.get())
                            .build(null));
}
