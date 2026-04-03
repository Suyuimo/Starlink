package de.weinschenk.starlink.api;

import net.minecraft.core.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import javax.annotation.Nullable;

/**
 * Forge capability that any block entity can expose to participate in
 * Starlink's satellite wireless network.
 *
 * <h3>Quick start</h3>
 * <pre>{@code
 * // In your block entity:
 * private final LazyOptional<IStarlinkEndpoint> endpoint =
 *         LazyOptional.of(() -> new IStarlinkEndpoint() {
 *             private String channel = "";
 *             private BlockPos linkedTx = null;
 *
 *             public String  getChannel()                 { return channel; }
 *             public void    setChannel(String ch)        { channel = ch; }
 *             public boolean canTransmit()                { return false; }
 *             public boolean canReceive()                 { return true; }
 *             public BlockPos getLinkedTransmitter()      { return linkedTx; }
 *             public void setLinkedTransmitter(BlockPos p){ linkedTx = p; }
 *         });
 *
 * @Override
 * public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
 *     if (cap == IStarlinkEndpoint.CAPABILITY) return endpoint.cast();
 *     return super.getCapability(cap, side);
 * }
 * }</pre>
 *
 * Once exposed, Starlink's {@code LinkToolItem} can link this block to a
 * transmitter, and {@code SetWirelessChannelPacket} can update its channel.
 * Use {@link StarlinkAPI} to query satellite counts from your own tick logic.
 */
public interface IStarlinkEndpoint {

    /** The Forge capability token — expose this in {@code getCapability()}. */
    Capability<IStarlinkEndpoint> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    // ── Channel ───────────────────────────────────────────────────────────────

    /** Returns the channel name. Empty string means public (no filtering). */
    String getChannel();

    /** Sets the channel name. Called by Starlink when the player uses the GUI or LinkTool. */
    void setChannel(String channel);

    // ── Direction ─────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if this endpoint can act as a transmitter.
     * A bidirectional node should return {@code true} for both canTransmit and canReceive.
     */
    boolean canTransmit();

    /**
     * Returns {@code true} if this endpoint can act as a receiver.
     * A bidirectional node should return {@code true} for both canTransmit and canReceive.
     */
    boolean canReceive();

    // ── Linking (only relevant when canReceive() == true) ─────────────────────

    /**
     * Returns the position of the linked transmitter, or {@code null} if not yet linked.
     * Starlink's LinkToolItem stores the transmitter position here when the player links blocks.
     */
    @Nullable
    BlockPos getLinkedTransmitter();

    /**
     * Called by Starlink's LinkToolItem to store the linked transmitter position.
     * Persist this value in your block entity's NBT.
     */
    void setLinkedTransmitter(@Nullable BlockPos pos);
}
