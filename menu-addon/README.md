# Learned Skills Menu Addon v1.0.0

This addon is optional. Learned Skills gameplay does not require it. It adds **Main Menu â†’ More â†’ Skills**, showing exact mastery progress, rank, **Learning now**, **Not learning**, **MASTERED**, and Skill Effect information.

The addon appears as a separate optional mod in the campaign Mods screen. Enable both **Learned Skills** and **Learned Skills Menu Addon**.

> [!WARNING]
> This Menu Addon supports only Severed Chains `3.0.0-1739-devbuild`, commit `b69c397a2ccb6165e12480d13523840073f08a80`. Do **not** update Severed Chains while it is installed. Uninstall the Menu Addon **before** updating Severed Chains.

It replaces one core engine JAR; standard Learned Skills does not. QoL+, forks, and other modified engine distributions may conflict and are rejected by the installer.

Install the standard `learned-skills-1.0.0.jar` first. In the player package, run `Verify Menu Addon.bat`, then `Install Menu Addon.bat`. The installer requests the game directory, verifies all relevant hashes, shows the mandatory warning, and requires typing `INSTALL`.

The pristine backup is stored as a `.bak` outside launcher-scanned JAR locations. The uninstaller restores it only if both the active patched engine and backup still match the recorded hashes. If an external update changed the engine, it removes only its verified addon JAR and will not restore or downgrade the engine blindly.

Corresponding modified engine source:

- Branch: https://github.com/IntiArtHub/Severed-Chains/tree/learned-skills-menu-v1.0.0
- Exact source commit: https://github.com/IntiArtHub/Severed-Chains/commit/3fd2f3879da16f687f0d6bfaca46095f85c9eecd
- Base: https://github.com/Legend-of-Dragoon-Modding/Severed-Chains/commit/b69c397a2ccb6165e12480d13523840073f08a80