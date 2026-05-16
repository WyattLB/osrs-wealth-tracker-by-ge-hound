# Wealth Tracker

A passive, read-only RuneLite plugin that tracks your total OSRS net worth (bank + inventory + equipment) over time. No network requests — all data stays local per account.

## Features

- Snapshots on bank open and login (configurable)
- Sidebar panel with net worth, delta vs last snapshot, history chart (1D / 7D / 30D / All)
- Breakdown by bank, inventory, and equipment
- Top 3 item movers between snapshots
- Optional in-game overlay
- CSV export
- GE or High Alch pricing

## Setup

### Prerequisites

- Java 11 (Eclipse Temurin 11 recommended)
- IntelliJ IDEA Community Edition
- Git

### macOS

```bash
# 1. Clone the repo
git clone https://github.com/WyattLB/osrs-wealth-tracker-by-ge-hound.git
cd osrs-wealth-tracker-by-ge-hound

# 2. Make Gradle executable (required on macOS)
chmod +x gradlew

# 3. Run tests
./gradlew test

# 4. Launch dev client
./gradlew shadowJar
java -ea -jar build/libs/osrs-wealth-tracker-by-ge-hound-1.0-SNAPSHOT-all.jar --developer-mode --debug
```

IntelliJ: Open the project, set SDK to Eclipse Temurin 11
(`/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home`),
then run `WealthTrackerPluginTest.main()` with VM arg `-ea` and
program args `--developer-mode --debug`.

### Windows

```bat
gradlew.bat test
gradlew.bat shadowJar
java -ea -jar build\libs\osrs-wealth-tracker-by-ge-hound-1.0-SNAPSHOT-all.jar --developer-mode --debug
```

## Data Storage

Snapshots are stored locally in your RuneLite profile config:

- **macOS:** `~/.runelite/profiles/`
- **Windows:** `%USERPROFILE%\.runelite\profiles\`

No data leaves your machine. This plugin makes zero network requests.

## Install

Build from source or install via RuneLite Plugin Hub once published.

Support: https://github.com/WyattLB/osrs-wealth-tracker-by-ge-hound

## License

BSD 2-Clause. See [LICENSE](LICENSE).
