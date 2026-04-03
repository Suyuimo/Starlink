# Starlink — Satellite Wireless Transfer Mod

A Minecraft Forge 1.20.1 mod that adds a fully functional satellite network to your world.
Launch rockets, deploy satellites into orbit, and use their bandwidth for long-range wireless
transfer of energy, fluids, and items — all without cables.

---

## Features

### Satellites & Orbit

- **5 satellite types**, each specialised for a different purpose:

  | Type | Energy | Fluid | Item | Lifetime |
  |---|---|---|---|---|
  | Basic | ×1.0 | ×1.0 | ×1.0 | ~5 MC days |
  | Energy | ×4.0 | ×0.5 | ×0.5 | ~3 MC days |
  | Fluid | ×0.5 | ×4.0 | ×0.5 | ~4 MC days |
  | Item | ×0.5 | ×0.5 | ×4.0 | ~4 MC days |
  | Advanced | ×3.0 | ×3.0 | ×3.0 | ~7 MC days |

- Satellites have a **limited lifetime** — after it expires the satellite crashes and is removed,
  with a server-wide notification.
- A custom **Orbit dimension** lets you fly up and see your satellites as physical entities in space.
- **Orbit Glasses** and **Receiver Glasses** give you a HUD overlay showing all active satellites.

### Wireless Transfer Blocks

Eight tiers of transmitter/receiver pairs for three transfer types:

| Type | T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8 |
|---|---|---|---|---|---|---|---|---|
| Energy (RF/t) | 64/sat | 256/sat | 1 024/sat | 4 096/sat | 16 384/sat | 65 536/sat | 262 144/sat | ∞* |
| Fluid (mB/t) | 100/sat | 400/sat | 1 600/sat | 6 400/sat | 25 600/sat | 102 400/sat | 409 600/sat | ∞* |
| Items (items/s) | 1/sat | 4/sat | 16/sat | 64/sat | 256/sat | 1 024/sat | 4 096/sat | ∞* |

*T8 achieves unlimited bandwidth when 20 or more satellites are overhead.*

- **Channel system** — set a channel name on TX and RX to keep transfers private.
  Empty channel = public.
- **Link Tool** — right-click a transmitter, then right-click a receiver to link them.

### Resources & Machines

- **Petroleum Shale Ore** — mine for Raw Rocket Fuel (Fortune supported).
- **Distillation Chamber** — refines Raw Rocket Fuel into usable Rocket Fuel.
- **Satellite Workbench** — crafts all 5 satellite types.
- **Rocket Workbench** — assembles the rocket and manages the launch sequence.
- **Launch Pad + Launch Controller V2** — positions and fires the rocket.

### Integrations

- **CC:Tweaked** (optional) — place a **Satellite Interface** block next to a computer cable
  to expose satellite data via peripheral API:
  ```lua
  local iface = peripheral.find("starlink_satellite_interface")
  print(iface.getSatelliteCount())       -- total satellites overhead
  print(iface.getEnergySatelliteCount()) -- weighted count for RF
  print(iface.hasCoverage())             -- true/false
  ```
- **Mekanism** (optional) — energy transmitters and receivers also accept and output
  Mekanism Joules in addition to standard Forge RF.

---

## Getting Started

1. **Find or smelt resources** — mine Petroleum Shale for rocket fuel, Titanium Ore for
   titanium ingots, and craft steel ingots from iron.
2. **Build the rocket** — use the Rocket Workbench to assemble a rocket, then fill it with
   Rocket Fuel via the Distillation Chamber.
3. **Craft satellites** — use the Satellite Workbench. Choose the type based on what you
   want to transfer wirelessly.
4. **Launch** — place the Launch Pad and Launch Controller, load the rocket with satellites,
   and fire.
5. **Place wireless blocks** — craft a matching tier of Transmitter and Receiver, link them
   with the Link Tool, and connect them to your storage/machines.
6. **Watch the sky** — put on the Orbit Glasses to see your satellites passing overhead in
   real time.

---

## Satellite Recipes (Satellite Workbench)

| Satellite | Pattern | Key materials |
|---|---|---|
| Basic | `SPS / TCT / SPS` | Steel, Solar Panel, Titanium, Circuit Board |
| Energy | `GRG / RCR / GRG` | Gold, Redstone, Circuit Board |
| Fluid | `SCS / CTC / SCS` | Steel, Copper, Titanium |
| Item | `SHS / HCH / SHS` | Steel, Hopper, Circuit Board |
| Advanced | `DPD / TCT / DPD` | Diamond, Solar Panel, Titanium, Circuit Board |

---

## Wireless Block Recipes (Crafting Table)

All 48 wireless blocks follow the same pattern in a standard crafting table.

**Transmitter:** ` A ` / `MCM` / `MMM`
**Receiver:** `MMM` / `MCM` / ` A `

Where `M` = tier material, `C` = Circuit Board, `A` = type material:

| Tier | M | Energy A | Fluid A | Item A |
|---|---|---|---|---|
| T1 | Iron Ingot | Redstone | Lapis Lazuli | Hopper |
| T2 | Copper Ingot | Redstone | Lapis Lazuli | Hopper |
| T3 | Gold Ingot | Redstone | Lapis Lazuli | Hopper |
| T4 | Steel Ingot | Glowstone Dust | Prismarine Shard | Ender Pearl |
| T5 | Titanium Ingot | Glowstone Dust | Prismarine Shard | Ender Pearl |
| T6 | Diamond | Glowstone Dust | Prismarine Shard | Ender Pearl |
| T7 | Emerald | Blaze Rod | Nautilus Shell | Eye of Ender |
| T8 | Netherite Ingot | Blaze Rod | Nautilus Shell | Eye of Ender |

---

## Requirements

| Dependency | Version | Required |
|---|---|---|
| Minecraft | 1.20.1 | ✅ |
| Minecraft Forge | 47.2.32+ | ✅ |
| Mekanism | 10.4.6+ | ❌ Optional |
| CC:Tweaked | 1.108.4+ | ❌ Optional |

---

## For Mod Developers — API

Starlink exposes a public API so other mods can read satellite data or register their
own wireless transmitter/receiver blocks.

See [`src/main/java/de/weinschenk/starlink/api/README.md`](src/main/java/de/weinschenk/starlink/api/README.md)
for the full API documentation.

**Quick example:**
```java
// Query satellite coverage at any server-side position
int sats = StarlinkAPI.getSatelliteCount(level, pos);

// Expose your block as a Starlink wireless endpoint
if (cap == IStarlinkEndpoint.CAPABILITY) return myEndpoint.cast();
```

---

## Building from Source

```bash
git clone https://github.com/Suyuimo/Starlink.git
cd starlink
./gradlew build
```

The compiled jar is placed in `build/libs/`.

---

## License

MIT — see [LICENSE](LICENSE) for details.

---

*Made by Sven Weinschenk*
