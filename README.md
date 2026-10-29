# Lemuroid Plus

A personal fork of [Lemuroid](https://github.com/Swordfish90/Lemuroid), the open-source Android emulator built on Libretro, tuned for small handhelds like the Retroid Pocket Classic.

Lemuroid Plus installs **next to** the original Lemuroid (app id `com.swordfish.lemuroid.plus`), so you can keep both.

## What's different from Lemuroid

### Library
- **No box art.** Box art is never searched for or downloaded. Every game shows a clean tile with its initials.
- **Tiles match their system.** Each game tile is a solid block in the same color as its system on the Systems screen. Systems that share a card (Genesis/Sega CD, Neo Geo Pocket/Color, WonderSwan/Color, FBNeo/MAME) use a slightly darker shade for the second one.
- **Smaller tiles** (116dp instead of 144dp), so more games fit on small screens.
- **Just the game name** under each tile, on up to two lines. No system or publisher line.
- **Clean names.** Tags in brackets such as `(USA)`, `(World)`, `(Rev 1)` or `[!]` are hidden. Custom names you set are shown exactly as typed.
- No custom artwork option.

### In game
- **Fast-forward button.** A small round button in the top-right corner of the game screen toggles fast-forward (it turns yellow when on). It has been removed from the in-game menu. The fast-forward gamepad shortcut still works.
- **No tilt sensor** option in the in-game menu or settings.
- **bsnes for SNES.** Choose between Snes9x (default, fastest) and bsnes (more accurate, much heavier) in **Settings → Core selection**. bsnes is downloaded from the official libretro build server the first time it is selected.

### Saves and backups
- **Lemuroid folder.** Saves, save states and state previews are kept in `Lemuroid/` on the main storage (`Lemuroid/saves`, `Lemuroid/states`, `Lemuroid/state-previews`), where any file manager can reach them. The app asks for "All files access" on first launch for this. Existing saves are moved there automatically. If you decline, saves stay in the app's private folder as before.
- **BIOS folder.** Drop BIOS files into `Lemuroid/bios` (one folder for all systems, any file names) and tap **Rescan** in Settings. They are recognized by checksum, the same as BIOS files in the ROM folder, which still works too. **Settings → BIOS** shows what was detected.
- **Settings → Backup.**
  - **Export saves** writes all saves and states to one zip file, wherever you choose.
  - **Import saves** takes one or more files from anywhere on the device:
    - a backup zip made by Export saves restores saves and states (after a confirmation);
    - `.sav` / `.srm` files from other emulators or flash carts are matched to games by name (`Pokemon Emerald (USA).sav` for `Pokemon Emerald (USA).gba`), and for any other name you pick the game from a searchable list.
  - Any in-game save that gets replaced is kept as `.srm.bak` in `Lemuroid/saves`, so an import can be undone.
- **Drop-in .sav files.** A `.sav` named like the ROM and placed in `Lemuroid/saves` is also picked up the next time the game starts (if it is newer than Lemuroid's own save), then renamed to `.sav.imported` so it only imports once.

## Install

1. Open the **Actions** tab, pick the latest green **Build Lemuroid Plus APK** run and download **lemuroid-plus-apk** (a zip with the APK inside). If a release has been published, you can also download the APK from **Releases**.
2. Copy the APK to the device and open it with a file manager. Allow "install unknown apps" if Android asks.
3. On first launch, tap **Allow** and turn on "Allow access to manage all files" so saves go to the `Lemuroid` folder.
4. Pick your ROM folder in **Settings**. Emulator cores download the first time you play each system, so be online for that.

## Build

Every push to `main` builds the APK on GitHub Actions. To build by hand, open **Actions → Build Lemuroid Plus APK → Run workflow**.

To publish a release with the APK attached: open **Releases → Draft a new release**, create a tag such as `v1.0`, add notes and press **Publish release**. The workflow builds the APK and attaches it to the release a few minutes later.

To build locally (JDK 17 and the Android SDK required):

```bash
git clone --recurse-submodules https://github.com/contactvirgilio/LemuroidPlusFork
cd LemuroidPlusFork
./gradlew assembleFreeDynamicRelease
```

The APK ends up in `lemuroid-app/build/outputs/apk/freeDynamic/release/`. It is signed with `release.jks` if present, otherwise with the debug key included in this repo. Keep using the same key so updates install over the previous version.

## Supported systems

Atari 2600, Atari 7800, Atari Lynx, NES, SNES, Game Boy, Game Boy Color, Game Boy Advance, Sega Genesis / Mega Drive, Sega CD, Master System, Game Gear, Nintendo 64, PlayStation, PSP, Arcade (FinalBurn Neo, MAME 2003-Plus), Nintendo DS, PC Engine, Neo Geo Pocket / Color, WonderSwan / Color, DOS and Nintendo 3DS, using the same Libretro cores as Lemuroid.

## Credits and license

All the real work is [Lemuroid](https://github.com/Swordfish90/Lemuroid) by Filippo Scognamiglio and its contributors, built on [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) and the [Libretro](https://www.libretro.com/) cores. If you enjoy it, consider supporting the original project.

Lemuroid Plus is distributed under the same license, the GNU General Public License v3.0. See [COPYING](COPYING).
