# Game Boy, Game Boy Color and GBA carts with a label (kept 2026-11-23)

The app now uses label-less versions of these three carts (`lemuroid-app/src/main/res/drawable-nodpi/media_gb.webp`,
`media_gbc.webp`, `media_gba.webp`). These are the previous files, kept in case they're wanted back.

To restore: copy `media_<system>.webp` and `media_<system>_label.webp` back into `drawable-nodpi`, and in
`GameMediaImage.kt` give those systems their `R.drawable.media_<system>_label` again.

This folder isn't part of the app build.
