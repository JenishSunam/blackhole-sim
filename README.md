# Black Hole Simulator

A physics-accurate black hole simulation built on a Redmi 9A (2GB RAM, arm32) with Sketchware Pro.

**No PC. No Android Studio. Just a phone.**

## Features

- 🕳️ **5 preset scenarios** — Slingshot, Solar System, Chaos, Feast, Binary
- 📊 **Stats for nerds** — live physics readouts (consumed stars, FPS, uptime)
- 👁️ **Follow camera** — track any star as it orbits
- ⏱️ **Time scaling** — 0.1x to 10x simulation speed
- 🎯 **Trajectory prediction** — drag to see where your star will go (green = escape, red = capture)
- 🌊 **Relativistic effects** — Doppler shift, gravitational lensing, photon sphere, tidal stretching
- 🎨 **Pure Canvas rendering** — no external engines, no dependencies
- 🔒 **100% offline** — no ads, no tracking, no accounts

## Physics

- **Newtonian gravity** with softening (prevents infinite force near hole)
- **Escape velocity** calculation for trajectory color
- **Photon sphere** rendered at 1.5× Schwarzschild radius
- **Doppler shift** based on radial velocity (blue = approaching, red = receding)
- **Tidal stretching** — stars deform radially near event horizon
- **Relativistic jets** — particle emission on star consumption

## How to use

1. **SLINGSHOT** — drag from any point and release to launch a star
2. **SOLAR** — 6 stars in stable circular orbits
3. **CHAOS** — 15 random high-velocity stars
4. **FEAST** — stars spawn continuously, feed the black hole
5. **BINARY** — 12 stars in tight double-spiral orbits

- Tap **📊 STATS** for live readouts
- Tap **👁 FOLLOW** to track a star
- Tap **🔄 RESET** to reseed the current preset
- Drag the **TIME** slider to slow down or speed up

## Install

Download APK from [Releases](../../releases).

## Built on

- Redmi 9A (2GB RAM, arm32, Android 10)
- Sketchware Pro
- No PC, no IDE

## License

MIT
