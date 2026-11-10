# Shield Bot (Fabric 1.21.11)

Carpet-style `/player` bots for practising **shield stunning** and **stun-webbing**.
The difference from Carpet: when you hit the bot (including the hit right after you
break its shield) it **does not hop up**, so the cobweb lands right under its feet.

Install it in your own client's `mods` folder. It works in **singleplayer** (and on a
LAN world you host). It does not work on other people's servers, because bots have to
exist on the server. No Fabric API needed. Don't install it together with Carpet: both add `/player`.

## Build the jar

You need Java 21 (the JDK, not just the runtime).

- Windows: open the folder, click its address bar, type `cmd`, press Enter, then run `gradlew build`
- Mac/Linux: `./gradlew build`

The first build downloads Minecraft and takes a few minutes. The mod ends up in
`build/libs/shieldbot-1.21.11-1.0.0.jar`. Put that file in `.minecraft/mods` with
Fabric Loader 0.18.2 or newer for 1.21.11.

**Without installing anything:** make a GitHub repo, upload this whole folder, open the
**Actions** tab, and when the `build` run is green, download the `shieldbot-jar` artifact.

## Practice setup (stun-webbing)

```
/player spawn Bot
```
Drop the bot a shield (press Q while looking at it). It picks it up into its hand.
```
/player Bot use continuous     <- holds the shield up, raises it again after it's disabled
/player Bot track              <- keeps facing you, so the shield is always toward you
```
Then: axe to disable the shield, then hit, then place a web under the bot.

## All commands

| Command | What it does |
|---|---|
| `/player spawn <name>` | Spawn a bot where you stand (survival) |
| `/player spawn <name> in creative` | Spawn in another game mode |
| `/player <name> spawn at <x y z> facing <yaw pitch>` | Carpet-style spawn |
| `/player <name> kill` | Remove the bot |
| `/player <name> use [once \| continuous \| interval <ticks>]` | Right-click (shield, eating, placing...) |
| `/player <name> attack [once \| continuous \| interval <ticks>]` | Left-click |
| `/player <name> jump [once \| continuous \| interval <ticks>]` | Jump |
| `/player <name> stop` | Stop all actions |
| `/player <name> look north\|south\|east\|west\|up\|down` | Turn to a direction |
| `/player <name> look me` | Look at you once |
| `/player <name> look at <x y z>` / `look <yaw pitch>` | Look at a spot or angle |
| `/player <name> turn left\|right\|back` | Turn relative |
| `/player <name> track` / `untrack` | Keep facing the nearest real player (off when you use `look`) |
| `/player <name> knockback nojump` | **Default.** Pushed back, never lifted off the ground |
| `/player <name> knockback none` | Doesn't move at all when hit |
| `/player <name> knockback normal` | Vanilla (hops when hit) |
| `/player <name> heal` | Full health and food |
| `/player <name> hotbar <1-9>` | Pick a hotbar slot |
| `/player <name> swapHands` | Swap main hand and offhand (shield to offhand) |
| `/player <name> drop` / `dropStack` | Drop held item |
| `/player <name> sneak` / `unsneak` / `sprint` / `unsprint` | |
| `/player <name> move forward\|backward\|left\|right` / `move` | Walk / stop walking |

Bots spawn fresh every time (empty inventory). When a bot dies it disappears; spawn it again.

## Credits
The fake-player code is adapted from [Carpet Mod](https://github.com/gnembon/fabric-carpet)
by gnembon (MIT license, see `LICENSE-carpet`).
