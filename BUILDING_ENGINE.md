# Building the exact Menu Addon engine

The optional Menu Addon for Learned Skills v1.1.0 requires the patched engine included in the release ZIP.

- Supported game: Severed Chains `3.0.0-1780-devbuild`
- Upstream base: `d3d4c02cbf1c74a4db39cfc10520431a6b8cc150`
- Modified source commit: `9ad856f75b9ea0aa0675c4360b991d3dd0de06ef`
- Source branch: <https://github.com/IntiArtHub/Severed-Chains/tree/learned-skills-menu-v1.1.0>

To reproduce the engine JAR:

1. Check out the upstream base commit in a clean worktree.
2. Apply `ENGINE_MENU_EXTENSION.patch` with `git apply --index ENGINE_MENU_EXTENSION.patch`.
3. Build with JDK 25 using `gradlew.bat clean menuExtensionTest jar --no-daemon`.
4. Inspect `legend.core.Version` with `javap -classpath <built-jar> -constants legend.core.Version`. It must report `3.0.0-1780-devbuild` and commit `d3d4c02cbf1c74a4db39cfc10520431a6b8cc150`.
5. Confirm the patched JAR SHA-256 is `EAB4E369C4044EA2D8A930EC520476A93C0A269947F15B694A8A4A4E78BE2FD8`.

The patch contains exact release metadata because the upstream source tree uses packaging placeholders. `ENGINE_BASE.txt` records the pristine and patched binary hashes.
