# Learned Skills Menu Addon v1.1.0 private test package

This addon is optional. Learned Skills gameplay does not require it. It adds **Main Menu â†’ More â†’ Skills**, showing exact mastery progress, rank, **Learning now**, **Not learning**, **MASTERED**, and Skill Effect information.

The addon appears as a separate optional mod in the campaign Mods screen. Enable both **Learned Skills** and **Learned Skills Menu Addon**.

> [!WARNING]
> This test package supports only Severed Chains `3.0.0-1780-devbuild`, commit `d3d4c02cbf1c74a4db39cfc10520431a6b8cc150`. Do **not** update Severed Chains while it is installed. Uninstall the Menu Addon **before** updating Severed Chains.

It replaces one core engine JAR; standard Learned Skills does not. QoL+, forks, and other modified engine distributions may conflict and are rejected by the installer.

Install the local v1.1.0 release candidate core JAR first. In the player package, run `Verify Menu Addon.bat`, then `Install Menu Addon.bat`. The installer requests the game directory, verifies all relevant hashes, shows the mandatory warning, and requires typing `INSTALL`. This remains a private 1780 test kit and is not a published v1.1 release artifact.

The pristine backup is stored as a `.bak` outside launcher-scanned JAR locations. The uninstaller restores it only if both the active patched engine and backup still match the recorded hashes. If an external update changed the engine, it removes only its verified addon JAR and will not restore or downgrade the engine blindly.

The exact 1780 engine source is the commit recorded in the installer package and must match its SHA-256 checks.
