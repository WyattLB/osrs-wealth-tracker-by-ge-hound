# Wealth Tracker

A RuneLite plugin that records your OSRS net worth over time and shows it in the sidebar. It reads your bank, inventory, and worn gear by default. You can optionally include your looting bag, seed vault, or Group Ironman shared storage. Nothing is sent over the network; snapshots stay on your machine, per RuneLite profile.

Built for use with [GE Hound](https://gehound.com).

## What it does

- Takes a snapshot when you open your bank or log in (each can be turned off in settings)
- Shows total net worth, change since the last snapshot, and a history chart (1D, 7D, 30D, or all time)
- Lists bank, inventory, equipment, and optional containers in a breakdown
- Shows the top three items that moved the most GP since the previous snapshot
- Optional small overlay with your current total
- Export history to CSV
- Price items by GE or high alch; skip stacks below a minimum value you set

## Optional containers (off by default)

These only count toward net worth when enabled in the plugin config **and** RuneLite can see that container this session (you usually need to open it at least once):

| Setting | What it tracks |
|---------|----------------|
| Include Looting Bag | Looting bag contents |
| Include Seed Vault | Seed vault at the Farming Guild |
| Include GIM Storage | Group Ironman shared storage |

If a container is not open yet, that part of your wealth is treated as zero for that snapshot. That is normal, not a bug.

## Requirements

- Java 11 (Eclipse Temurin 11 works well)
- Git
- IntelliJ IDEA if you want to run from the IDE

## Build and run (macOS)

```bash
git clone https://github.com/WyattLB/osrs-wealth-tracker-by-ge-hound.git
cd osrs-wealth-tracker-by-ge-hound
chmod +x gradlew
./gradlew test
./gradlew run
```

`./gradlew run` starts a dev RuneLite client with the plugin loaded. Use VM option `-ea` and program args `--developer-mode --debug` if you run from IntelliJ instead (`WealthTrackerPluginTest` main class).

Shadow JAR (optional):

```bash
./gradlew shadowJar
java -ea -jar build/libs/osrs-wealth-tracker-by-ge-hound-1.0-SNAPSHOT-all.jar --developer-mode --debug
```

On macOS, point IntelliJ at Temurin 11, for example:

`/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home`

## Build and run (Windows)

```bat
git clone https://github.com/WyattLB/osrs-wealth-tracker-by-ge-hound.git
cd osrs-wealth-tracker-by-ge-hound
gradlew.bat test
gradlew.bat run
```

Or after `gradlew.bat shadowJar`:

```bat
java -ea -jar build\libs\osrs-wealth-tracker-by-ge-hound-1.0-SNAPSHOT-all.jar --developer-mode --debug
```

## Where data is stored

Snapshots are saved in RuneLite profile config (JSON in your profiles folder):

- macOS: `~/.runelite/profiles/`
- Windows: `%USERPROFILE%\.runelite\profiles\`

Look for the `wealthtracker.snapshots` key if you need to inspect or reset history manually. Old snapshots from before optional containers were added will show `0` for looting bag, seed vault, and GIM storage.

## Install for normal play

Plugin Hub listing is not live yet. Until then, build from source with the steps above or install the built plugin through RuneLite's developer mode.

## License

BSD 2-Clause. See [LICENSE](LICENSE).

## Issues

https://github.com/WyattLB/osrs-wealth-tracker-by-ge-hound/issues
