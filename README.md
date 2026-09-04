# Project AirSystem

<img src="docs/logo.png" alt="Project AirSystem" width="220">

A **NeoForge 1.21.1** mod about the air picture: unmanned aircraft, an air raid
alarm system and ground-based air defence. Works with **TACZ** as a soft
dependency — the mod runs with or without it.

NeoForge is the only dependency. The models use the vanilla `ModelPart` system
and are animated in the renderer.

**Drones are not entities.** In the air a drone is an object inside
`DroneFlightManager`; on the ground it is a block with a block entity. The
reasoning is under "How it works".

---

## What is in it

### Drones (20 aircraft)

| Class | Aircraft |
|---|---|
| Shahed loitering munitions | Shahed-131, Shahed-136, Shahed-238 (jet) |
| Reconnaissance | Orlan-10, Orlan-30, Eleron-3, ZALA 421-16E, ZALA 421-08, Granat-4 |
| Strike and loitering | Lancet-1, Lancet-3, KUB-BLA, Inokhodets (Orion), S-70 Okhotnik |
| Ukrainian systems | Leleka-100, SHARK, PD-2, Liutyi (An-196), UJ-22 Airborne, RAM II UAV |

Every aircraft has its own model built to the proportions of the real thing: a
Shahed is a delta with fins on the tips and a pusher propeller, a Lancet has two
X wings, an Orlan is a conventional layout with the tail on a boom, the PD-2 is
an aeroplane with four lift rotors, the S-70 a flying wing with a turbojet.
Performance — speed, ceiling, toughness, warhead — lives in `DroneKind` and
differs from type to type.

The models carry the detail that is actually visible in flight and on the pad: a
gimballed sensor with a lens window and a thermal channel, a pitot boom ahead of
the nose, ailerons and elevons as separate strips along the trailing edge, a
dorsal spine and access panels, navigation lights on the tips (red to port,
green to starboard), intakes and exhaust stubs on piston types, warning stripes
by the warhead. Types that land have real gear: tricycle legs and wheels on the
aeroplane layouts, landing feet under the booms of the PD-2, a skid on the
twin-boom aircraft and flying wings.

### Anti-aircraft guns

* **Gepard** — tracked SPAAG, twin 35 mm guns, two radars.
* **Slinger** — compact remote weapon station, 30 mm.
* **Terrahawk Paladin** — container mount with a 360-degree radar, 30 mm.
* **MANTIS** — unmanned auto air defence: a 35 mm revolver cannon on a pedestal
  with its own masted radar and no crew at all.

The first three work two ways: a gunner sits at the sight (left click fires), or
`Shift + right click` switches to automatic. MANTIS always runs itself.

**IFF.** In automatic mode a mount knows where every drone took off from.
Anything launched within **70 blocks** of the mount counts as friendly and is
never engaged, deliberately or by a stray burst. Everything launched further out
is tracked. The radius is configurable (`turret.friendlyRadius`).

**Automatic fire misses.** Every burst rolls a hit chance that falls off with
range and with target speed. On a failed roll the burst is deliberately walked
wide, and you can watch the tracers go past. The base chance is
`turret.autoHitChance` (0.55 by default), multiplied by the accuracy of the
individual mount.

### Air raid alarm

* **Alarm loudspeaker** — wails, audible over 160 blocks. The sound is
  synthesised as a real electromechanical siren: the rotor chops the air (two
  ports at a ratio of 1.2 give the characteristic chord), the signal passes
  through a horn resonant at 1150 and 2300 Hz, and spin-up, coast-down,
  rotational amplitude modulation and urban reverberation are added.
* **Air raid button** — raises the alert and sounds the all-clear.
* **Linking cable** — binds loudspeakers to a button: click the loudspeaker,
  then the button. With nothing bound, a button drives every siren within 64
  blocks.

Players within earshot get an "AIR RAID ALERT" banner on screen.

### Radar

* **Radar station** — a masted dish that sweeps for airborne drones out to 320
  blocks. Terrain masks the beam, so a contact behind a ridge is not on the
  scope until it clears the skyline.
* **Radar scope** — a console showing a plan position indicator: range rings,
  contacts with their course, and a track list with bearing, range, altitude
  and speed.

Bind a station to a scope with the linking cable — click the station, then the
scope. One scope takes several stations and merges their coverage.

The scope names a contact only when it is friendly by the same launch-point rule
the guns use. Anything launched further out shows as a size class alone, which
is what a radar return actually tells you.

---

## How it works

A Minecraft entity is convenient while it stays near a player. A drone flies
kilometres away while the operator stays put, and at that point the entity gets
in the way: it stops being tracked beyond the tracking radius, it ticks with the
rest of the world, and it needs the client to "see" it. So the aircraft are
built differently:

| State | What it is | What draws it |
|---|---|---|
| On the ground | `airsystem:drone` block with a block entity | `DroneBlockRenderer` |
| Airborne | `DroneFlight` object in `DroneFlightManager` | `DroneFlightRenderer`, in the world pass |
| Released bomb | `Munition` object in the same place | the same renderer |

What that buys:

* **Range.** A flight is not tied to the entity tracking radius. The server
  sends compact snapshots (`DroneSyncPayload`) to everyone in view range and
  instrument data (`DroneTelemetryPayload`) only to the operator.
* **Cost.** None of the entity overhead: no tracker, no attribute sync, no
  collision search every tick. Motion is one ray from the last point to the
  next, and that same ray finds the collision.
* **Persistence.** Flights live in the dimension's `SavedData` and survive a
  server restart.
* **Hits.** Since bullets will not hit the drone by themselves, the server looks
  for projectiles near it each tick and checks whether their movement segment
  passed through the airframe. Arrows and TACZ bullets both work this way; the
  guns trace their own line, and a nearby explosion reaches the aircraft through
  `ExplosionEvent`.
* **Feed.** There is nothing for a spectator camera to attach to, so the
  operator is put into spectator mode, the server quietly carries their position
  along with the drone so chunks keep loading, and the client moves the camera
  itself from the same snapshots. That costs no packets, and the view stays with
  the player — the camera behaves like a gimbal. Rebuilding the tracked-entity
  list, the expensive part of moving a player, happens on a chunk change rather
  than every tick.
* **Smoothness.** Snapshots arrive unevenly, so the client does not take the
  position directly but eases the drone towards it over a few ticks, otherwise
  the model stutters. A far jump — a drone entering view range — is applied at
  once instead of gliding in from a distance.

## Flying a drone

1. **World map** (right click) opens a view of the terrain from above. Left
   click marks a point and copies its coordinates to the clipboard; right click
   drags the map, the wheel zooms.
2. Place the aircraft on the ground (right click the item onto a solid surface).
   It becomes a pad block.
3. **Remote control** — right click the parked aircraft to link it.
4. Right click the remote in the air to open the launch screen. "Paste" pulls
   the coordinates out of the clipboard, "LAUNCH" sends the aircraft to the
   target.
5. The **feed from the aircraft** opens. The buttons along the top are "Manual
   control" (after which the aircraft follows the keys — `W`/`S` pitch, `A`/`D`
   heading, `Space` throttle, `Shift` throttle off), "Strike" (an immediate dive
   for a loitering munition, bomb release for a strike carrier) and "View"
   (switching between heading-locked and free gimbal).
6. Reconnaissance aircraft and strike carriers also get a **"Land"** button: the
   aircraft turns for its own pad, descends to circuit height, comes down the
   glide path and lands — the pad block reappears and the remote re-links to it
   by itself. Loitering munitions do not land.

Recovery also starts on its own when there is exactly enough fuel to get home:
the aircraft heads for the pad instead of falling. The phase is visible in the
telemetry ("Returning", "On approach").

An aircraft on a pad can be picked back up: `Shift` + right click, or simply
break the block. When a flight ends, the operator returns to their own position
in their previous game mode.

### Commands

```
/airsystem flights                # what is airborne right now
/airsystem abort                  # recall everything
/airsystem launch <type> <x y z>  # launch at a target, for testing
```

---

## Impact

* **The explosion** is staged. The vanilla picture is disabled entirely
  (`spawnParticles = false`, a silent `airsystem:silence`) and the client draws
  all of it:
  * **flash** — a full-screen wash from white to orange, dimmed by distance and
    killed by a wall between the player and the epicentre, together with a short
    punch to the field of view;
  * **core** — a fireball of `flash`/`explosion_emitter`, flame, lava drops,
    firework sparks and thrown-up soil;
  * **blast wave** — a ring of dust and soot spreading at 2.4 blocks per tick
    (about 18 ticks), plus a condensation ring in the air;
  * **smoke column** — rises and spreads for about seven seconds, with ash.

  Detail falls off with distance so that far-away impacts do not load the
  client.
* **Screen shake** — stronger the closer the player is to the epicentre (up to
  90 blocks), decaying over time.
* **Shockwave** — spreads from the epicentre over about 16 ticks and breaks
  glazing; the Shahed-136 radius is 35 blocks. The further the glass, the lower
  the chance it goes. The block list is the `airsystem:shockwave_fragile` tag.
* **Fires** — from types with an incendiary warhead (Shahed-136, Shahed-238,
  Lancet-3, Liutyi, RAM II).
* **Sound** — a sharp report up close, a dull boom from far away, arriving late
  by the speed of sound (about 17 blocks per tick).

## Shooting a drone down

* The hit point is converted into airframe coordinates, so where you hit
  matters: the **centre**, the engine bay or the flying surfaces.
* **Three centre hits** detonate it in the air.
* Any hit has a **20 %** chance of taking out the engine: the aircraft stalls
  into a smoking spiral and explodes on impact. The chance is twice as high
  through the engine bay and lower through a wing.
* TACZ bullets are recognised automatically; ordinary projectiles work too.

---

## TACZ compatibility

The mod is **not compiled** against TACZ and does not require it:

* TACZ bullets are recognised by projectile type and damage drones;
* the guns accept TACZ ammunition (any item in the `tacz` namespace with "ammo"
  or a matching calibre in its id) and fall back to the mod's own
  `airsystem:ammo_35mm` and `airsystem:ammo_30mm` rounds without it.

---

## Building

```bash
./gradlew build              # jar in build/libs/
./gradlew runClient          # run the client
./gradlew runServer          # run a dedicated server
./gradlew runGameTestServer  # flight model tests
```

### Tests

`src/main/java/.../test/DroneFlightTests.java` holds the tests for the flight
engine. Because a drone is not an entity, its logic can be driven directly: the
tests build a flight, tick it and check that the aircraft closes on the target,
rolls into a dive over it, stalls on engine failure, breaks up after three
centre hits, and that the hit zones — centre, engine, wing, miss — are told
apart correctly. The arena is an empty structure generated by
`tools/build_gametest_structure.py`.

JDK 21 is required. The only dependency is NeoForge 21.1.209.

### Generating the assets

Models, textures, sounds and language files are produced by scripts, which can
be edited and re-run:

```bash
python3 tools/build_drones.py     # drone models and textures -> DroneLayers.java
python3 tools/build_turrets.py    # gun models -> TurretLayers.java
python3 tools/build_assets.py     # icons, blocks, tags, recipes, loot
python3 tools/build_sounds.py     # sound synthesis (siren, engines, blasts)
python3 tools/build_lang.py       # ru_ru and en_us
python3 tools/validate_assets.py  # references and translations
python3 tools/audio_check.py DIR  # sound metrics and spectrograms
python3 tools/check_models.py     # find detached parts in the models
python3 tools/render3d.py         # render models to PNG (all, or by name)
python3 tools/build_logo.py       # mod logo
```

How the tools are laid out:

| File | Purpose |
|---|---|
| `geolib.py` | cubes, box-UV packing, texture painting, Java emission |
| `airframes.py` | airframe kit: tapered fuselage, swept wing, tails, skids, barrels, tracks |
| `render3d.py` | offline model renderer (numpy/scipy): Z buffer, antialiasing, four views and silhouette metrics |
| `dsp.py` | filters, envelopes, reverb, harmonic synthesis |
| `audio_check.py` | peak, RMS, crest factor, spectral centroid, loop seam and spectrograms |
| `check_models.py` | connectivity groups: catches parts floating free of the airframe |
| `build_logo.py` | mod logo |

The detached-part check is built into `validate_assets.py`: if a geometry edit
tears a fin or the sensor off the body, the asset build says so.

Models are eyeballed through `render3d.py` and sounds through `audio_check.py`:
the siren should show a harmonic comb with a sweep on the spectrogram, a
detonation an attack of a few milliseconds and a heavy low tail.

`pillow`, `numpy`, `scipy` and `soundfile` are needed. Every sound is
synthesised procedurally; the mod contains no third-party audio.

`render3d.py` rasterises a model with a Z buffer (rotations from
`scipy.spatial.transform.Rotation`, antialiasing and edge emphasis from
`scipy.ndimage`) and prints projection metrics: `scipy.ndimage.label` catches a
silhouette that has fallen into separate blobs, and the contour moments give
span over length and the elongation of the profile. With `--textured` the faces
are painted with the mean colour from the finished atlas, which shows exactly
what the game will show.

Models are described as cubes through `tools/airframes.py` and emitted straight
into Java: `client/model/DroneLayers.java` and `TurretLayers.java` are generated
files, so edit the scripts instead. Box UV is laid out automatically, so the
textures follow the same geometry. The geometry rules that decide whether an
aircraft is recognisable:

* the planform is defined by the leading and trailing edges separately — a
  Shahed's trailing edge is nearly straight, and only that makes the silhouette
  a triangle rather than a diamond;
* wings thicken at the root and thin at the tip, and control surfaces are their
  own cubes along the trailing edge, not decals on top of the skin;
* detail is sparse but large: a picket fence of one-unit steps reads as a
  defect, not as a contour;
* wing segments and body sections are laid out on the whole grid, or slits open
  up between the cubes and the model looks frayed.

---

## Configuration

Every numeric parameter lives in the common config
(`config/airsystem-common.toml`): flight time and cruise altitude, engine
failure chance, remote link range, chunk loading and view radii, the cap on
simultaneous flights, shake and audibility radii, shockwave travel time, the IFF
radius and the base automatic hit chance, radar range and sweep period, plus
switches for glass breaking and fires.

---

## License

The mod is distributed under the **GNU General Public License v3.0 or later** —
the full text is in [LICENSE](LICENSE).

In short: the code and assets may be taken, changed and redistributed, but a
derivative work has to stay under the GPL with its sources open. A mod that
depends on AirSystem falls under the same condition.

The code, models, textures and sounds are written from scratch: geometry, UV
layouts, PNGs and OGGs are all generated by the scripts in `tools/`, and no
third-party assets ship in the build. NeoForge is a compile-time dependency only
and does not end up in the jar.

The Mojang EULA applies on top of the license: the mod may not be sold or put
behind a paywall.

© 2026 RBXLU
