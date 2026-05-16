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

## Development

Requires Java 11+.

```bash
./gradlew test
./gradlew run          # dev client with plugin loaded
./gradlew shadowJar    # fat JAR for manual launch
```

Dev client VM args: `-ea`  
Program args: `--developer-mode --debug`

## Install

Build from source or install via RuneLite Plugin Hub once published.

Support: https://github.com/WyattLB/osrs-wealth-tracker-by-ge-hound
