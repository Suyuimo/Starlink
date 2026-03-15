package de.weinschenk.starlink.block;

import de.weinschenk.starlink.entity.ModEntities;
import de.weinschenk.starlink.entity.RocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class LaunchControllerBlockEntity extends BlockEntity {

    /** Alle aktuell geladenen Controller — für den Reset-Command. WeakRef damit GC nicht blockiert wird. */
    public static final Set<LaunchControllerBlockEntity> ALL_LOADED =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public static final int FUEL_REQUIRED = 4;
    private static final int VALIDATE_INTERVAL_TICKS = 40; // alle 2 Sekunden prüfen

    // Offset-Muster der 8 LaunchPad-Blöcke rund um den Controller (gleiche Y-Ebene)
    private static final BlockPos[] PAD_OFFSETS = {
            new BlockPos(-1, 0, -1), new BlockPos(0, 0, -1), new BlockPos(1, 0, -1),
            new BlockPos(-1, 0,  0),                          new BlockPos(1, 0,  0),
            new BlockPos(-1, 0,  1), new BlockPos(0, 0,  1), new BlockPos(1, 0,  1)
    };

    private int fuel = 0;
    private boolean launching = false;
    private boolean orbitAxisX = true;   // true = X-Achse, false = Z-Achse

    public LaunchControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_CONTROLLER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        ALL_LOADED.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ALL_LOADED.remove(this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaunchControllerBlockEntity be) {
        if (level.getGameTime() % VALIDATE_INTERVAL_TICKS != 0) return;

        // Multiblock-Status aktuell halten (sichtbares Feedback für den Spieler)
        boolean valid = be.isMultiblockValid(level, pos);
        if (valid != state.getValue(LaunchControllerBlock.READY)) {
            level.setBlock(pos, state.setValue(LaunchControllerBlock.READY, valid), 3);
        }
    }

    /**
     * Versucht einen Raketenstart durchzuführen.
     * Gibt ein LaunchResult-Enum mit einer Nachricht zurück.
     */
    public LaunchResult tryLaunch(Level level, BlockPos pos, Player player) {
        if (launching) return LaunchResult.ALREADY_LAUNCHING;
        if (!isMultiblockValid(level, pos)) return LaunchResult.INVALID_MULTIBLOCK;
        if (fuel < FUEL_REQUIRED) return LaunchResult.NOT_ENOUGH_FUEL;

        // Treibstoff verbrauchen und Rakete spawnen
        fuel -= FUEL_REQUIRED;
        launching = true;
        setChanged();

        RocketEntity rocket = new RocketEntity(ModEntities.ROCKET.get(), level);
        rocket.setControllerPos(pos);
        if (player != null) rocket.setLaunchingPlayer(player.getUUID());
        // Rakete startet 1 Block über dem Controller-Block
        rocket.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        level.addFreshEntity(rocket);

        return LaunchResult.SUCCESS;
    }

    /**
     * Wird von der RocketEntity aufgerufen wenn sie die Dimension verlässt.
     */
    public void onRocketDeparted() {
        launching = false;
        setChanged();
    }

    /**
     * Prüft ob alle 8 Nachbarblöcke LaunchPadBlöcke sind.
     */
    private boolean isMultiblockValid(Level level, BlockPos pos) {
        for (BlockPos offset : PAD_OFFSETS) {
            if (!level.getBlockState(pos.offset(offset)).is(ModBlocks.LAUNCH_PAD.get())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fügt Fuel hinzu, gibt zurück wie viel tatsächlich aufgenommen wurde.
     */
    public int addFuel(int amount) {
        int space = FUEL_REQUIRED - fuel;
        int added = Math.min(space, amount);
        fuel += added;
        setChanged();
        return added;
    }

    public int getFuel() { return fuel; }
    /** Nur für ContainerData-Sync auf dem Client — kein setChanged(). */
    public void setFuelSync(int value) { fuel = value; }
    public boolean isLaunching() { return launching; }
    public boolean isOrbitAxisX() { return orbitAxisX; }
    public void setOrbitAxisX(boolean axisX) { orbitAxisX = axisX; setChanged(); }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Fuel", fuel);
        tag.putBoolean("Launching", launching);
        tag.putBoolean("OrbitAxisX", orbitAxisX);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fuel = tag.getInt("Fuel");
        launching = tag.getBoolean("Launching");
        orbitAxisX = !tag.contains("OrbitAxisX") || tag.getBoolean("OrbitAxisX");
    }

    // -------------------------------------------------------------------------

    public enum LaunchResult {
        SUCCESS("Rakete gestartet! T-minus..."),
        ALREADY_LAUNCHING("Eine Rakete ist bereits unterwegs."),
        INVALID_MULTIBLOCK("Startbasis unvollständig! 3x3 LaunchPad-Blöcke benötigt."),
        NOT_ENOUGH_FUEL("Nicht genug Treibstoff! Benötigt: " + FUEL_REQUIRED + " Kanister.");

        private final String message;

        LaunchResult(String message) { this.message = message; }
        public String message() { return message; }
    }
}
