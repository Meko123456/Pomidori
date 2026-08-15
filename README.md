# Pomidori 🍅

**პომიდორი** (*pomidori* — Georgian for "tomato") — a focused, no-nonsense
**Pomodoro timer** for Android. *Pomodoro* is Italian for "tomato", after the
tomato-shaped kitchen timer the technique is named for.

Work in focused sessions, take short breaks, and let a longer break land after a
few rounds — with a reliable timer that keeps running when the screen is off.

## Planned features

- ⏱️ **Focus / break cycles** — configurable work, short-break and long-break
  lengths, and how many focus sessions before a long break.
- 🔔 **Runs in the background** — a foreground service drives the countdown so it
  survives the screen turning off, with **notification actions** (pause / resume
  / skip) and a chime when a session ends.
- 💾 **Remembered settings** — durations and preferences persisted with
  **DataStore**.
- 📊 **Session count** — a simple tally of focus sessions completed today.
- ⌚ **Wear OS tile** *(stretch)* — start a session from the wrist.

## Tech

Single-module Android app: **Jetpack Compose** + Material 3, a foreground
**Service** for the timer, **DataStore** for settings, and a pure Kotlin timer
engine that's unit-tested independent of Android.

## Status

🚧 Day 1 — README-first. See [issues](../../issues) for the roadmap.

## License

[MIT](LICENSE)
