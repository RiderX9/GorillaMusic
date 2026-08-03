# Third-Party Notices

## Echo Music Liquid Glass

The Liquid Glass effect in `ui/theme/LiquidGlassModifier.kt` is adapted from
Echo Music:

- Project: `EchoMusicApp/Echo-Music`
- Source revision: `4c6618d9873bef1ae2775fd99967f58483a02a9a`
- License: GNU General Public License v3.0

## Kyant0 Backdrop

The source under `ui/liquidglass/backdrop/` is adapted from the Backdrop 2.0.0
code vendored by Echo Music:

- Project: `Kyant0/backdrop`
- Copyright: 2025 Kyant0
- License: Apache License 2.0

## Helixform fluidrecyclerview

The scroll physics in `ui/components/FluidScroll.kt` — the exponential fling
decay, the critically damped spring-back, and the rubber-band function — are
ported from the `fluidrecyclerview` sources that Accord vendors under
`recyclerview/src/main/java/org/helixform/fluidrecyclerview/` (`FluidScroller.kt`,
`FluidSpringBack.kt`, `FluidRubberBand.kt`). Only the framework-agnostic maths
carries over; the RecyclerView plumbing is replaced by a Compose
`FlingBehavior` and a nested-scroll overscroll modifier.

- Project: `Helixform/fluidrecyclerview`
- Copyright: 2023 Helixform
- License: Apache License 2.0

## Accord

The player's Lossless quality pill and the up-next queue row spec are modelled
on Accord. Ported assets and specs:

- `res/drawable/apple_lossless_seeklogo.xml` — from Accord's drawable of the
  same name
- `res/drawable/ic_queue_remove.xml` — from Accord's `res/drawable/ic_remove.xml`
- The pill geometry and colours follow `quality_card` in `res/layout/full_player.xml`
- The queue row metrics follow `res/layout/adapter_list_card_playlist.xml`

Details:

- Project: `emylfy/Accord`
- Source revision: `653cb861e2b1e73ae122423de6f1ceb87b81bfcd`
- License: GNU General Public License v3.0

The individual source files retain their original attribution headers.
