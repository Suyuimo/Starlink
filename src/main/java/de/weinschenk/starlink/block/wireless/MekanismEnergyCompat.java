package de.weinschenk.starlink.block.wireless;

import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.math.FloatingLong;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Isolates all Mekanism API references so that the mod loads cleanly
 * when Mekanism is not installed.
 *
 * Never reference this class directly — always go through the guard:
 *   if (MekanismEnergyCompat.isLoaded()) { ... }
 */
public final class MekanismEnergyCompat {

    public static final Capability<IStrictEnergyHandler> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private MekanismEnergyCompat() {}

    // -------------------------------------------------------------------------
    // Handler for the Transmitter (insert-only from outside)
    // -------------------------------------------------------------------------

    public static IStrictEnergyHandler transmitterHandler(EnergyTransmitterBlockEntity tx) {
        return new IStrictEnergyHandler() {
            @Override public int getEnergyContainerCount() { return 1; }

            @Override public FloatingLong getEnergy(int c)    { return FloatingLong.create(tx.getMekanismBuffer()); }
            @Override public void setEnergy(int c, FloatingLong e) { tx.setMekanismBuffer(e.doubleValue()); }
            @Override public FloatingLong getMaxEnergy(int c)    { return FloatingLong.MAX_VALUE; }
            @Override public FloatingLong getNeededEnergy(int c) { return FloatingLong.MAX_VALUE; }

            @Override
            public FloatingLong insertEnergy(int c, FloatingLong amount, @Nullable Action action) {
                if (action == null || !action.simulate())
                    tx.addMekanismBuffer(amount.doubleValue());
                return FloatingLong.ZERO; // infinite buffer — no remainder
            }

            @Override
            public FloatingLong extractEnergy(int c, FloatingLong amount, @Nullable Action action) {
                double avail = Math.min(amount.doubleValue(), tx.getMekanismBuffer());
                if (action == null || !action.simulate())
                    tx.addMekanismBuffer(-avail);
                return FloatingLong.create(avail);
            }
        };
    }

    // -------------------------------------------------------------------------
    // Handler for the Receiver (extract-only from outside)
    // -------------------------------------------------------------------------

    public static IStrictEnergyHandler receiverHandler(EnergyReceiverBlockEntity rx) {
        return new IStrictEnergyHandler() {
            @Override public int getEnergyContainerCount() { return 1; }

            @Override public FloatingLong getEnergy(int c)    { return FloatingLong.create(rx.getMekanismBuffer()); }
            @Override public void setEnergy(int c, FloatingLong e) { rx.setMekanismBuffer(e.doubleValue()); }
            @Override public FloatingLong getMaxEnergy(int c)    { return FloatingLong.MAX_VALUE; }
            @Override public FloatingLong getNeededEnergy(int c) { return FloatingLong.ZERO; }

            @Override
            public FloatingLong insertEnergy(int c, FloatingLong amount, @Nullable Action action) {
                return amount; // receiver does not accept energy from outside
            }

            @Override
            public FloatingLong extractEnergy(int c, FloatingLong amount, @Nullable Action action) {
                double avail = Math.min(amount.doubleValue(), rx.getMekanismBuffer());
                if (action == null || !action.simulate())
                    rx.addMekanismBuffer(-avail);
                return FloatingLong.create(avail);
            }
        };
    }

    // -------------------------------------------------------------------------
    // Wireless transfer: pull from tx buffer, push into rx buffer
    // -------------------------------------------------------------------------

    /**
     * Transfers Mekanism energy from transmitter to receiver.
     * Uses the full buffer as bandwidth — no artificial cap.
     * For Tier 8 + 20 physical sats, effectiveSats > 0 is guaranteed by the caller.
     */
    public static void transferEnergy(EnergyTransmitterBlockEntity tx,
                                      EnergyReceiverBlockEntity rx) {
        double available = tx.getMekanismBuffer();
        if (available <= 0) return;
        tx.setMekanismBuffer(0);
        rx.addMekanismBuffer(available);
    }

    // -------------------------------------------------------------------------
    // LazyOptional wrappers
    // -------------------------------------------------------------------------

    public static LazyOptional<IStrictEnergyHandler> lazyTransmitter(EnergyTransmitterBlockEntity tx) {
        return LazyOptional.of(() -> transmitterHandler(tx));
    }

    public static LazyOptional<IStrictEnergyHandler> lazyReceiver(EnergyReceiverBlockEntity rx) {
        return LazyOptional.of(() -> receiverHandler(rx));
    }

    @SuppressWarnings("unchecked")
    public static <T> LazyOptional<T> castLazy(LazyOptional<?> lazy, Capability<T> cap) {
        return (LazyOptional<T>) lazy;
    }
}
