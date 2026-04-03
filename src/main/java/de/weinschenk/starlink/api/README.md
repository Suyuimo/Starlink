# Starlink API

This package exposes a stable public API for other mod developers who want to
integrate with Starlink's satellite network.

---

## Overview

The API has two independent parts:

| Class | Purpose |
|---|---|
| `StarlinkAPI` | Query satellite coverage at any world position |
| `IStarlinkEndpoint` | Forge capability — mark your block as a Starlink wireless node |

You can use either part on its own, or both together.

---

## Part 1 — Satellite Queries (`StarlinkAPI`)

Use this when you just want to know how many satellites are overhead.
No registration, no capability — just call the static methods from your own
server-side tick.

```java
import de.weinschenk.starlink.api.StarlinkAPI;

// In your BlockEntity tick (server side only):
int total  = StarlinkAPI.getSatelliteCount(level, getBlockPos());
int energy = StarlinkAPI.getEnergySatelliteCount(level, getBlockPos());
int fluid  = StarlinkAPI.getFluidSatelliteCount(level, getBlockPos());
int items  = StarlinkAPI.getItemSatelliteCount(level, getBlockPos());
boolean ok = StarlinkAPI.hasCoverage(level, getBlockPos());
```

### Satellite type multipliers

Satellites are specialised — a single ENERGY satellite counts as **4.0** toward
energy bandwidth but only **0.5** toward fluid/item. The weighted counts reflect
this automatically:

| Type | energyCount | fluidCount | itemCount |
|---|---|---|---|
| BASIC | 1.0 | 1.0 | 1.0 |
| ENERGY | 4.0 | 0.5 | 0.5 |
| FLUID | 0.5 | 4.0 | 0.5 |
| ITEM | 0.5 | 0.5 | 4.0 |
| ADVANCED | 3.0 | 3.0 | 3.0 |

---

## Part 2 — Wireless Endpoint Capability (`IStarlinkEndpoint`)

Use this when your block should act as a transmitter, receiver, or bidirectional
node that players can link with Starlink's **Link Tool** and configure with
channels via the wireless GUI.

### What Starlink does for you automatically

Once your block entity exposes this capability, Starlink handles:

- **Link Tool** (`LinkToolItem`) — players can right-click your TX block to mark
  it, then right-click your RX block to link them. Channel mismatches are caught
  and reported.
- **Channel packet** (`SetWirelessChannelPacket`) — if a player changes the
  channel via the Starlink wireless screen, `setChannel()` is called on your
  endpoint.

You are responsible for:
- Persisting `channel` and `linkedTransmitterPos` in your block entity's NBT.
- The actual data transfer (energy, fluid, items) using Forge capabilities.
- Calling `StarlinkAPI.getEnergySatelliteCount()` (or the appropriate variant)
  to determine your transfer bandwidth.

### Minimal implementation — receiver only

```java
import de.weinschenk.starlink.api.IStarlinkEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class MyReceiverBlockEntity extends BlockEntity {

    private String channel = "";
    private BlockPos linkedTx = null;

    private final LazyOptional<IStarlinkEndpoint> endpoint = LazyOptional.of(() ->
        new IStarlinkEndpoint() {
            public String  getChannel()                    { return channel; }
            public void    setChannel(String ch)           { channel = ch; setChanged(); }
            public boolean canTransmit()                   { return false; }
            public boolean canReceive()                    { return true; }
            public BlockPos getLinkedTransmitter()         { return linkedTx; }
            public void setLinkedTransmitter(BlockPos pos) { linkedTx = pos; setChanged(); }
        }
    );

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == IStarlinkEndpoint.CAPABILITY) return endpoint.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        endpoint.invalidate();
    }

    // Don't forget to save/load channel and linkedTx in saveAdditional/load!
}
```

### Bidirectional node (like CC:Tweaked)

Return `true` for both `canTransmit()` and `canReceive()`. The Link Tool will
treat the block as a transmitter when right-clicked first, and as a receiver
when right-clicked second.

```java
public boolean canTransmit() { return true; }
public boolean canReceive()  { return true; }
```

### Using satellite data for bandwidth

After your RX block is linked to a TX block, use `StarlinkAPI` in your own tick
to cap the transfer rate:

```java
// Example: energy transfer
int satCount = StarlinkAPI.getEnergySatelliteCount(level, getBlockPos());
long maxRfPerTick = (long) satCount * RF_PER_SATELLITE;

// Pull from TX, push to local storage, capped at maxRfPerTick
```

### Persisting state (NBT example)

```java
@Override
protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    tag.putString("Channel", channel);
    if (linkedTx != null) {
        tag.putInt("TxX", linkedTx.getX());
        tag.putInt("TxY", linkedTx.getY());
        tag.putInt("TxZ", linkedTx.getZ());
    }
}

@Override
public void load(CompoundTag tag) {
    super.load(tag);
    channel = tag.getString("Channel");
    if (tag.contains("TxX")) {
        linkedTx = new BlockPos(tag.getInt("TxX"), tag.getInt("TxY"), tag.getInt("TxZ"));
    }
}
```

---

## Channels

Channels work the same as in Starlink's own wireless blocks:

| Channel value | Behaviour |
|---|---|
| `""` (empty) | Public — any TX/RX with an empty channel can be linked |
| `"myChannel"` | Private — only TX/RX with the same non-empty channel can be linked |

When the Link Tool detects a channel mismatch, it cancels the link and displays
an error message to the player.

---

## Dependency setup (build.gradle)

Add Starlink as a `compileOnly` dependency so your mod compiles against the API
but does not require Starlink at runtime (soft dependency):

```groovy
repositories {
    // Add wherever Starlink is published
}

dependencies {
    compileOnly fg.deobf("de.weinschenk:starlink:1.20.1-1.0-SNAPSHOT:api")
}
```

Declare the optional dependency in `mods.toml`:

```toml
[[dependencies."your_mod_id"]]
modId    = "starlink"
mandatory = false
versionRange = "[1.0,)"
ordering = "AFTER"
side     = "BOTH"
```

Guard all API calls at runtime:

```java
if (ModList.get().isLoaded("starlink")) {
    int sats = StarlinkAPI.getSatelliteCount(level, pos);
    // ...
}
```
