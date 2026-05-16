# Wealth Tracker — Code Audit Prompt
## For Cursor AI · v1 · Complete Cross-Check Against Spec

---

## HOW TO USE THIS

Open Cursor in your `wealth-tracker` project folder. Start a **new chat**. Paste everything below the horizontal rule as your first message. Then follow the instructions at the bottom.

---

## ── AUDIT PROMPT START ──

You are a senior Java engineer conducting a thorough code review of a RuneLite Plugin Hub plugin called **Wealth Tracker**. Your job is to audit every source file in this project against a detailed specification and a known list of failure modes.

You are not here to suggest improvements or new features. You are here to find:

1. **Bugs** — code that compiles but produces wrong results at runtime
2. **Compile errors** — code that will not compile
3. **Threading violations** — the single most common RuneLite crash category
4. **Spec deviations** — anything that was supposed to be implemented a certain way and wasn't
5. **Plugin Hub rejection risks** — anything that would cause the RuneLite maintainers to reject the PR
6. **Null pointer landmines** — places where a null can reach a dereference in production

For every issue you find, report it in this exact format:

```
FILE: <filename>
LINE: <line number or range>
SEVERITY: CRITICAL | HIGH | MEDIUM | LOW
CATEGORY: Bug | Threading | Compile | Spec Deviation | Hub Rejection | NPE Risk | Code Quality
DESCRIPTION: <what is wrong>
FIX: <exact corrected code or specific instruction>
```

At the end, produce a **PASS/FAIL verdict** per file, then an overall **SHIP / DO NOT SHIP** verdict.

---

## PART 1 — THE COMPLETE FILE SPECIFICATION

This is the ground truth. Every file must match this spec. Read it fully before you begin.

---

### THE PROJECT STRUCTURE

Expected files:

```
src/main/java/com/wealthtracker/
├── WealthTrackerPlugin.java
├── WealthTrackerConfig.java
├── WealthTrackerPanel.java
├── WealthTrackerOverlay.java
├── WealthDataManager.java
├── WealthChartPanel.java
├── WealthSnapshot.java
├── ItemSnapshot.java
├── WealthPriceSource.java
└── WealthUtils.java

src/main/resources/com/wealthtracker/
└── icon.png                          (16×16 PNG — must exist for clean load)

src/test/java/com/wealthtracker/
└── WealthTrackerPluginTest.java

build.gradle
settings.gradle
runelite-plugin.properties
```

---

### SPEC: WealthPriceSource.java

- Package: `com.wealthtracker`
- `public enum` with two constants: `GE` and `HIGH_ALCH`
- Each constant has a `String displayName` passed to the constructor
- `toString()` returns `displayName`
- No other methods, no Lombok, no imports needed beyond the package declaration

---

### SPEC: ItemSnapshot.java

- Package: `com.wealthtracker`
- All three Lombok annotations: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Five fields, all with the correct types:
  - `int itemId`
  - `String itemName`
  - `int quantity`
  - `long priceEach`
  - `long totalValue`
- No RuneLite imports
- No business logic
- No `@Slf4j`
- `@AllArgsConstructor` ensures `new ItemSnapshot(id, name, qty, price, total)` compiles
- `@NoArgsConstructor` ensures GSON can deserialize it with `new ItemSnapshot()`

---

### SPEC: WealthSnapshot.java

- Package: `com.wealthtracker`
- All three Lombok annotations: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Seven fields:
  - `long timestamp`
  - `long bankValue`
  - `long inventoryValue`
  - `long equipmentValue`
  - `long coinsInHand`
  - `long totalNetWorth`
  - `Map<Integer, ItemSnapshot> itemBreakdown`  — nullable, GSON must serialize/deserialize this
- One static factory method `create(long, long, long, long, Map)`:
  - Sets `timestamp = System.currentTimeMillis()`
  - Sets `totalNetWorth = bankValue + inventoryValue + equipmentValue`
  - **Does NOT add `coinsInHand` to `totalNetWorth`** — coins are already counted inside `inventoryValue`
  - Sets all other fields from parameters
  - Returns the new `WealthSnapshot`
- `@AllArgsConstructor` + `@NoArgsConstructor` are both required alongside `@Data`

---

### SPEC: WealthUtils.java

- Package: `com.wealthtracker`
- `public final class` with `private WealthUtils()` constructor — no instantiation
- **No RuneLite imports. No Swing imports. Pure Java 11.**
- **No `record` keyword** — Java 11 target, records require Java 16+

**Required methods with exact contracts:**

```
formatGp(long value) → String
  0           → "0 gp"
  500         → "500 gp"
  1_500       → "1,500 gp"
  999_999     → "999,999 gp"
  1_000_000   → "1.00m"
  1_042_388   → "1.04m"
  1_000_000_000 → "1.00b"
  negative    → "-" + formatGp(positive) (recursive or equivalent)

formatDelta(long delta) → String
  positive    → "↑ +" + formatGp(delta)
  negative    → "↓ " + formatGp(delta)   [formatGp handles the minus sign]
  zero        → "→ " + formatGp(0)       [i.e. "→ 0 gp"]

formatPercentage(long from, long to) → String
  from == 0   → "N/A"
  otherwise   → String.format("%+.1f%%", ((to-from)/(double)from)*100)

formatTimeSince(long epochMs) → String
  < 60s       → "just now"
  < 3600s     → Xs + "m ago"
  < 86400s    → Xs + "h ago"
  >= 86400s   → Xs + "d ago"

filterByDays(List<WealthSnapshot> snapshots, int days) → List<WealthSnapshot>
  days == Integer.MAX_VALUE → return original list unchanged (all time)
  otherwise → return only snapshots where timestamp >= (now - days*86_400_000L)
  never returns null — returns empty list if input is empty

calculateMovers(WealthSnapshot prev, WealthSnapshot current, int topN) → List<ItemDelta>
  either null param → return Collections.emptyList()
  either null itemBreakdown → return Collections.emptyList()
  otherwise:
    - diff all items in current vs prev (new items: delta = totalValue; removed items: delta = -totalValue)
    - sort by Math.abs(delta) descending
    - return subList(0, min(topN, size))
```

**Required inner class:**
```java
public static final class ItemDelta {
    public final String itemName;
    public final long delta;
    // constructor, getItemName(), getDelta()
}
```
NOT a `record`. NOT a Kotlin data class. A plain Java class.

---

### SPEC: WealthDataManager.java

- Package: `com.wealthtracker`
- `@Slf4j` annotation (required for `log.*` calls)
- Constructor injection: `@Inject public WealthDataManager(ConfigManager configManager)`
- Constants:
  - `CONFIG_GROUP = "wealthtracker"` (String)
  - `SNAPSHOTS_KEY = "snapshots"` (String)
  - `GSON` — `private static final Gson` created once via `new GsonBuilder().create()`
  - `SNAPSHOT_LIST_TYPE` — `new TypeToken<List<WealthSnapshot>>(){}.getType()`
- Private field: `List<WealthSnapshot> cachedSnapshots` — starts null

**Required methods:**

`loadSnapshots()`:
- If `cachedSnapshots != null`, return `new ArrayList<>(cachedSnapshots)` (defensive copy, never expose the internal list)
- Read from `configManager.getRSProfileConfiguration(CONFIG_GROUP, SNAPSHOTS_KEY, String.class)`
- If null or empty → set `cachedSnapshots = new ArrayList<>()`, return empty list
- On `JsonSyntaxException` → `log.warn(...)`, reset to `[]` via configManager, return empty list
- Never returns null

`saveSnapshot(WealthSnapshot snapshot, int retentionDays)`:
- Loads cache if null
- Appends snapshot
- Calls `pruneOldSnapshots(cachedSnapshots, retentionDays)`
- Serializes and writes via `configManager.setRSProfileConfiguration(...)`
- Logs count after save

`getLatestSnapshot()`:
- Calls `loadSnapshots()`
- Returns last element or null if empty

`clearCache()`:
- Sets `cachedSnapshots = null`
- Logs that cache was cleared

`pruneOldSnapshots(List<WealthSnapshot> list, int retentionDays)` (private):
- If `retentionDays <= 0`, returns immediately
- Cutoff = `Instant.now().minus(retentionDays, ChronoUnit.DAYS).toEpochMilli()`
- `list.removeIf(s -> s.getTimestamp() < cutoff)`
- Logs count removed if > 0

---

### SPEC: WealthTrackerConfig.java

- Package: `com.wealthtracker`
- `@ConfigGroup("wealthtracker")`
- Extends `net.runelite.client.config.Config`
- Nine config items in this order (positions 0–8):

```
pos 0: boolean includeBank()         default true
pos 1: boolean includeInventory()    default true
pos 2: boolean includeEquipment()    default true
pos 3: boolean snapshotOnBankOpen()  default true
pos 4: boolean snapshotOnLogin()     default true
pos 5: WealthPriceSource priceSource() default GE
pos 6: int minItemValue()            default 10000  + @Range(min=0, max=10_000_000)
pos 7: int dataRetentionDays()       default 90     + @Range(min=1, max=365)
pos 8: boolean showOverlay()         default false
```

- **No `@Provides` method** — that belongs in the plugin class, not the config interface

---

### SPEC: WealthChartPanel.java

- Package: `com.wealthtracker`
- Extends `javax.swing.JPanel`
- **No RuneLite client API calls inside this class** — purely Swing + WealthUtils
- Constructor: sets `preferredSize = new Dimension(0, 150)`, `minimumSize = new Dimension(0, 150)`, `opaque = true`, `background = ColorScheme.DARKER_GRAY_COLOR`
- `setData(List<WealthSnapshot> data)`: stores a **defensive copy** (`new ArrayList<>(data)` or empty list if null), calls `repaint()`
- `paintComponent(Graphics g)`:
  - Casts to `Graphics2D`, enables `VALUE_ANTIALIAS_ON`
  - **Checks for null OR `size < 2` BEFORE any `.size()` call** (null check first, always)
  - Empty state: draws two centered text lines in `ColorScheme.LIGHT_GRAY_COLOR`
  - Data state: pre-computes ALL pixel coordinates before drawing anything
  - Draws: filled area (green, alpha 40), green line (2f stroke), Y-axis grid lines + labels, dot on rightmost point
  - **No client API, no configManager, no I/O inside paintComponent**

---

### SPEC: WealthTrackerOverlay.java

- Package: `com.wealthtracker`
- `@Slf4j`
- Extends `net.runelite.client.ui.overlay.Overlay`
- Constructor: `@Inject public WealthTrackerOverlay(WealthDataManager dataManager, WealthTrackerConfig config)`
  - **Both parameters stored as instance fields** (this is the bug most often missed)
  - `setPosition(OverlayPosition.TOP_LEFT)`
  - `setLayer(OverlayLayer.ABOVE_WIDGETS)`
- `render(Graphics2D graphics)`:
  - Returns `null` if `!config.showOverlay()`
  - Returns `null` if `dataManager.getLatestSnapshot()` returns null
  - Uses `PanelComponent` + `TitleComponent` for rendering
  - Shows formatted net worth in green `new Color(0, 200, 83)`
  - Returns `panelComponent.render(graphics)`
- Required imports:
  - `net.runelite.client.ui.overlay.components.PanelComponent`
  - `net.runelite.client.ui.overlay.components.TitleComponent`
  - `net.runelite.client.ui.overlay.OverlayPosition`
  - `net.runelite.client.ui.overlay.OverlayLayer`

---

### SPEC: WealthTrackerPanel.java

- Package: `com.wealthtracker`
- `@Slf4j`
- Extends `net.runelite.client.ui.PluginPanel`
- Constructor: `public WealthTrackerPanel(WealthDataManager dataManager, WealthTrackerConfig config)`
  - Both stored as instance fields
  - `BoxLayout Y_AXIS` on `this`
  - `EmptyBorder(10,10,10,10)`
  - `ColorScheme.DARK_GRAY_COLOR` background

**Required fields:**
- `WealthChartPanel chartPanel` — instantiated in constructor
- `JLabel netWorthLabel`, `deltaLabel`, `bankLabel`, `invLabel`, `equipLabel`
- `JLabel[] moverLabels` — array of 3
- `int selectedRangeIndex` — default 1 (7D)
- `static final int[] RANGES = {1, 7, 30, Integer.MAX_VALUE}`
- `static final String[] LABELS = {"1D", "7D", "30D", "All"}`

**`refresh()` method:**
- **Must assert `SwingUtilities.isEventDispatchThread()`** — fail fast in dev mode
- Calls `dataManager.loadSnapshots()` — uses cache, not direct disk I/O per call
- Empty state: shows "No data yet" text, calls `chartPanel.setData(null)`
- Data state:
  - Formats net worth with `WealthUtils.formatGp()`
  - Computes delta and percentage vs previous snapshot (index size-2)
  - Delta color: `new Color(0,200,83)` positive, `new Color(220,50,50)` negative
  - Calls `WealthUtils.filterByDays(all, RANGES[selectedRangeIndex])` — **NOT a custom filterByRange()**
  - Calls `chartPanel.setData(filtered)`
  - Updates breakdown labels
  - Updates mover labels using `WealthUtils.calculateMovers(prev, latest, 3)`
  - **All mover label accesses use `.getItemName()` and `.getDelta()`** (not field access via dot notation directly, which only works for public fields — which they are — but make sure either works)

**`exportCsv()` method:**
- Shows empty-state dialog if no data
- Uses `JFileChooser` with default filename `"wealth-history.csv"`
- CSV header: `"timestamp_ms,date,total_net_worth_gp,bank_gp,inventory_gp,equipment_gp"`
- Each row: timestamp, `new java.util.Date(timestamp)`, total, bank, inv, equip
- Wraps writer in try-with-resources
- On exception: `log.warn(...)` + error dialog

**Footer:**
- `Desktop.getDesktop().browse(new URI("https://gehound.com"))` in mouse listener
- Exception caught and logged (not swallowed)

---

### SPEC: WealthTrackerPlugin.java

- Package: `com.wealthtracker`
- `@Slf4j`
- `@PluginDescriptor(name = "Wealth Tracker", description = "...", tags = {...})`
- Extends `Plugin`

**Injected fields (all via `@Inject`):**
```
Client client
ClientThread clientThread
ItemManager itemManager
ClientToolbar clientToolbar
OverlayManager overlayManager
WealthTrackerConfig config
WealthDataManager dataManager
WealthTrackerOverlay overlay
```
- **`ConfigManager` is NOT injected here** — config access goes through `config`, storage through `dataManager`

**Non-injected fields:**
```
WealthTrackerPanel panel        — null until startUp
NavigationButton navButton      — null until startUp
ScheduledExecutorService executor — null until startUp
```

**`startUp()`:**
- Creates `executor` as single-thread scheduled executor with named daemon thread
- Creates `panel = new WealthTrackerPanel(dataManager, config)`
- Loads icon via `ImageUtil.loadImageResource(getClass(), "/com/wealthtracker/icon.png")`
  - On failure: logs warn, creates blank `BufferedImage(16, 16, TYPE_INT_ARGB)` — does NOT throw
- Builds `navButton` with tooltip, icon, priority 7, panel
- `clientToolbar.addNavigation(navButton)`
- `overlayManager.add(overlay)`
- If `client.getGameState() == GameState.LOGGED_IN` → calls `scheduleLoginSnapshot()`

**`shutDown()`:**
- `clientToolbar.removeNavigation(navButton)`
- `overlayManager.remove(overlay)`
- Shuts down executor if not null and not already shut down
- Nulls out `panel`, `navButton`, `executor`

**`onItemContainerChanged(ItemContainerChanged event)`:**
- Returns early if `!config.snapshotOnBankOpen()`
- Returns early if `event.getContainerId() != InventoryID.BANK.getId()`
- Calls `takeSnapshot("bank_open")` — already on client thread, NO `clientThread.invokeLater` needed here

**`onGameStateChanged(GameStateChanged event)`:**
- On `LOGGED_IN` + `config.snapshotOnLogin()` → `scheduleLoginSnapshot()`
- On `LOGIN_SCREEN` or `HOPPING` → `dataManager.clearCache()`

**`scheduleLoginSnapshot()`:**
- Uses `executor.schedule(...)` with 3-second delay
- Inside the scheduled task: `clientThread.invokeLater(this::takeLoginSnapshot)`

**`takeLoginSnapshot()`:**
- Verifies `client.getGameState() == LOGGED_IN` — aborts if not
- Calls `takeSnapshot("login")`

**`takeSnapshot(String trigger)`:**
- Called ONLY from client thread
- For each container (BANK, INVENTORY, EQUIPMENT):
  - `client.getItemContainer(InventoryID.X)` — can return null, handle it
  - Null container → log.debug, use 0 value for that component
  - For each item: check `item == null || item.getId() <= 0` → skip
  - `ItemID.COINS_995` counted separately as `coinsInHand` BUT still included in `inventoryValue`
  - Coins bypass `minItemValue` threshold check
  - Non-coin items below `config.minItemValue()` → skip
  - Calls `accumulateBreakdown(breakdown, item, price, total)`
- Creates snapshot via `WealthSnapshot.create(...)`
- Logs at `log.info` level with all four values
- Submits to `executor.submit(...)` for:
  - Save: `dataManager.saveSnapshot(snapshot, config.dataRetentionDays())`
  - Then `SwingUtilities.invokeLater(() -> { if (panel != null) panel.refresh(); })`

**`resolvePrice(int itemId)`:**
- Returns `itemManager.getItemComposition(itemId).getHaPrice()` for HIGH_ALCH mode
- Returns `itemManager.getItemPrice(itemId)` for GE mode
- Entire method wrapped in try/catch — returns 0 on any exception

**`accumulateBreakdown(...)`:**
- Gets item name from `itemManager.getItemComposition(item.getId()).getName()`
- Falls back to `"Item #" + item.getId()` on exception
- Uses `breakdown.merge(...)` to aggregate duplicates (same itemId in multiple slots)

**`@Provides WealthTrackerConfig provideConfig(ConfigManager configManager)`:**
- Present in the plugin class
- Returns `configManager.getConfig(WealthTrackerConfig.class)`

---

### SPEC: WealthTrackerPluginTest.java

- Package: `com.wealthtracker`
- `public static void main(String[] args) throws Exception`:
  - `ExternalPluginManager.loadBuiltin(WealthTrackerPlugin.class)`
  - `RuneLite.main(args)`
- JUnit 4 tests (import `org.junit.Test`, `static org.junit.Assert.*`):
  - At minimum: `formatGp` zero, small, thousands, just-under-million, exact-million, millions, billions, negative
  - At minimum: `formatDelta` positive/negative/zero
  - At minimum: `formatPercentage` increase/decrease/from-zero/no-change
  - At minimum: `filterByDays` all-time returns all
  - At minimum: `snapshot_totalNetWorth_doesNotIncludeCoinsInHandTwice` — verifies `create(100_000, 50_000, 75_000, 5_000, null).getTotalNetWorth() == 225_000L`
  - At minimum: `snapshot_timestampIsRecent`

---

### SPEC: build.gradle

```groovy
group = 'com.wealthtracker'
// Main-Class in shadowJar task = 'com.wealthtracker.WealthTrackerPluginTest'
// options.release.set(11)  ← DO NOT change to anything higher
// runeLiteVersion = 'latest.release'  ← NOT a pinned version number
// GSON not added as a dependency  ← it's a transitive dep of runelite-client
```

---

### SPEC: settings.gradle

```groovy
rootProject.name = 'wealth-tracker'
// NOT 'example'
```

---

### SPEC: runelite-plugin.properties

```
displayName=Wealth Tracker
plugins=com.wealthtracker.WealthTrackerPlugin
build=standard
```
Author, description, tags, support must all be filled in (non-empty, non-placeholder).

---

## PART 2 — THE AUDIT CHECKLIST

Work through every check below systematically. For each one, read the relevant code and either confirm it passes or report a finding.

---

### AUDIT BLOCK A — BUILD CONFIGURATION

**A1. settings.gradle root project name**
Read `settings.gradle`. Verify `rootProject.name = 'wealth-tracker'`, NOT `'example'`. If it still says `'example'`, report CRITICAL — the project builds under the wrong name.

**A2. build.gradle group**
Verify `group = 'com.wealthtracker'`. Not `'com.example'`.

**A3. build.gradle Java release**
Verify `options.release.set(11)`. Flag as CRITICAL if it's set to 16+ (would allow records to compile locally but fail the Plugin Hub CI which uses 11).

**A4. build.gradle shadowJar Main-Class**
Verify `'Main-Class': 'com.wealthtracker.WealthTrackerPluginTest'`. Not the example class.

**A5. build.gradle RuneLite version**
Verify `runeLiteVersion = 'latest.release'`. Flag if pinned to a specific version number — pins cause the plugin to silently break every Thursday after the weekly RuneLite update.

**A6. build.gradle GSON dependency**
Verify GSON is NOT explicitly declared as a dependency. It's a transitive dep of `runelite-client`. Explicitly declaring it risks version conflicts.

**A7. runelite-plugin.properties completeness**
Verify `displayName`, `author`, `description`, `tags`, `support`, `plugins`, and `build` are all present and non-empty. Flag any placeholder values like `[YOUR NAME]`.

**A8. Icon resource exists**
Verify `src/main/resources/com/wealthtracker/icon.png` exists. If missing, `startUp()` will log a warn and use a blank placeholder — not a crash, but flag as LOW.

---

### AUDIT BLOCK B — JAVA VERSION COMPLIANCE

**B1. No record declarations**
Grep all `.java` files for the `record` keyword used as a type declaration. Any `record Foo(...)` is a CRITICAL compile failure on Java 11.

**B2. No text blocks**
Grep for `"""` (triple-quote). Text blocks require Java 15+.

**B3. No sealed classes**
Grep for `sealed interface` or `sealed class`. Requires Java 17+.

**B4. No switch expressions**
Grep for `->` inside a `switch` used as an expression (not a statement lambda). Switch expressions require Java 14+.

**B5. WealthUtils.ItemDelta is a plain class**
Confirm `ItemDelta` is declared as `public static final class`, not `record`. Confirm it has explicit constructor, `getItemName()`, and `getDelta()` methods.

---

### AUDIT BLOCK C — PACKAGE AND IMPORT CORRECTNESS

**C1. All files in com.wealthtracker package**
Verify every `.java` file starts with `package com.wealthtracker;`. No leftover `package com.example;`.

**C2. No java.awt.Container import**
Grep for `import java.awt.Container`. Should be zero results. This is the wrong class for item containers — `net.runelite.api.ItemContainer` is correct.

**C3. No java.awt.List import**
Grep for `import java.awt.List`. Should be zero results.

**C4. ItemContainer import is net.runelite.api.ItemContainer**
In `WealthTrackerPlugin.java`, verify the import is `import net.runelite.api.ItemContainer;`.

**C5. InventoryID import is net.runelite.api.InventoryID**
Verify `import net.runelite.api.InventoryID;` in the plugin class.

**C6. No forbidden network imports**
Grep all files for: `HttpURLConnection`, `URL.openStream`, `OkHttp`, `HttpClient`. Should be zero results. Any hit is a CRITICAL Plugin Hub rejection risk.

**C7. No reflection imports**
Grep for `import java.lang.reflect.` (excluding TypeToken which uses reflect internally and is fine as a GSON pattern). Direct use of `Field`, `Method`, `Constructor` classes is a Plugin Hub rejection risk.

**C8. No Runtime.exec or ProcessBuilder**
Grep for `Runtime.exec(`, `ProcessBuilder`. Should be zero results.

**C9. Overlay imports correct**
In `WealthTrackerOverlay.java`, verify imports include:
- `net.runelite.client.ui.overlay.components.PanelComponent`
- `net.runelite.client.ui.overlay.components.TitleComponent`
- `net.runelite.client.ui.overlay.OverlayPosition`
- `net.runelite.client.ui.overlay.OverlayLayer`

**C10. ImageUtil import present in plugin**
Verify `import net.runelite.client.util.ImageUtil;` in `WealthTrackerPlugin.java`.

---

### AUDIT BLOCK D — THREADING CORRECTNESS

This is the most important audit block. Threading violations cause intermittent crashes that are very hard to reproduce.

**D1. takeSnapshot() is only ever called on the client thread**
Trace every call site of `takeSnapshot(String)`:
- Call from `onItemContainerChanged` → `@Subscribe` handler → client thread ✓
- Call from `takeLoginSnapshot()` → must be reached via `clientThread.invokeLater(...)` ✓
- Any other call sites? Each must be on the client thread.

**D2. scheduleLoginSnapshot() → executor.schedule → clientThread.invokeLater chain**
Verify the exact chain:
```java
executor.schedule(
    () -> clientThread.invokeLater(this::takeLoginSnapshot),
    3, TimeUnit.SECONDS
);
```
If `clientThread.invokeLater` is missing and instead `takeLoginSnapshot()` is called directly from the scheduled lambda, that's a CRITICAL threading violation — the executor thread is NOT the client thread.

**D3. SwingUtilities.invokeLater wraps all panel updates**
Inside `takeSnapshot()`, the code eventually calls `panel.refresh()`. Verify this is wrapped:
```java
SwingUtilities.invokeLater(() -> {
    if (panel != null) panel.refresh();
});
```
If `panel.refresh()` is called directly from the executor thread, that's a threading violation.

**D4. panel.refresh() asserts EDT**
Inside `WealthTrackerPanel.refresh()`, verify:
```java
assert SwingUtilities.isEventDispatchThread() : "...";
```
This is defensive programming — it will catch threading bugs in dev mode (when running with `-ea`).

**D5. No client calls from panel or chart**
Grep `WealthTrackerPanel.java` and `WealthChartPanel.java` for any call to `client.`, `itemManager.`, or `configManager.getRSProfile`. These classes must NEVER call client-thread APIs. Any hit is CRITICAL.

**D6. No client calls in the overlay render() method**
The `render()` method in `WealthTrackerOverlay.java` is called on the client thread, so technically client calls are safe there — BUT `getLatestSnapshot()` calls `loadSnapshots()` which calls `configManager`. Verify `dataManager.getLatestSnapshot()` uses the **in-memory cache** and does NOT call `configManager` on every render frame. If `cachedSnapshots` is null (first render), this will deserialize JSON on the render thread — it must fall through to the cache gracefully.

**D7. startUp() safe from threading issues**
`startUp()` is called on the client thread. Verify that `panel = new WealthTrackerPanel(...)` creation and `navButton` building don't touch Swing on the wrong thread. In practice, creating Swing components before they're shown is acceptable in RuneLite plugin startUp — but verify `clientToolbar.addNavigation(navButton)` is the correct RuneLite API call and not something that triggers an immediate EDT repaint.

**D8. Executor shutdown is clean**
In `shutDown()`, verify:
```java
if (executor != null && !executor.isShutdown()) {
    executor.shutdownNow();
}
```
And that `executor = null` is set afterwards. Confirm there's no possibility of a task submitted after shutdown (executor is nulled, then accessed again — would NPE).

---

### AUDIT BLOCK E — NULL SAFETY

**E1. item null check in container iteration**
In `takeSnapshot()`, every container iteration must check:
```java
if (item == null || item.getId() <= 0) continue;
```
Check BANK, INVENTORY, and EQUIPMENT loops separately.

**E2. ItemContainer null check**
After `client.getItemContainer(InventoryID.X)`, verify the result is checked for null before calling `.getItems()`. Missing null check = NPE the first time the plugin runs before the bank has ever been opened.

**E3. panel null check before refresh()**
Inside the executor's `SwingUtilities.invokeLater`, verify `panel != null` is checked before calling `panel.refresh()`. The panel is set to null in `shutDown()` and the executor's queued tasks can still run after shutdown.

**E4. WealthChartPanel null check before size check**
In `paintComponent()`, verify the null check comes BEFORE the size check:
```java
if (snapshots == null || snapshots.size() < 2) { ... }
```
NOT:
```java
if (snapshots.size() < 2) { ... }  // NPE if snapshots is null
```

**E5. WealthDataManager loadSnapshots() never returns null**
Verify every return path in `loadSnapshots()` returns a list, never null. Callers assume non-null.

**E6. WealthUtils.calculateMovers() null guard**
Verify the method guards against null `prev`, null `current`, null `prev.getItemBreakdown()`, null `current.getItemBreakdown()` — all four cases. Returns `Collections.emptyList()` for all.

**E7. WealthUtils.filterByDays() null guard**
If `snapshots` is null or empty, must not throw. Verify.

**E8. resolvePrice() exception handling**
Verify `resolvePrice(int itemId)` has a try/catch that returns 0 on any exception. `getItemComposition()` can throw for unknown item IDs.

**E9. accumulateBreakdown() name lookup failure**
Verify the item name lookup has a fallback: `"Item #" + item.getId()` if the composition lookup throws.

**E10. Icon loading failure**
Verify `loadIcon()` (or inline icon loading) catches any exception and returns a non-null `BufferedImage` placeholder. If this throws, `startUp()` fails and the plugin never loads.

---

### AUDIT BLOCK F — SPEC COMPLIANCE

**F1. totalNetWorth formula**
In `WealthSnapshot.create()`, verify:
```
totalNetWorth = bankValue + inventoryValue + equipmentValue
```
NOT including `coinsInHand`. Coins are already included in `inventoryValue` (coins have GE price 1). If `coinsInHand` is also added, coins are double-counted.

**F2. Coins bypass minItemValue threshold**
In `takeSnapshot()`, the coin check must bypass `minItemValue`:
```java
// Correct:
if (item.getId() != ItemID.COINS_995 && price < config.minItemValue()) continue;

// Wrong (skips coins when minItemValue > 1):
if (price < config.minItemValue()) continue;
```

**F3. COINS_995 is the correct constant**
Verify `ItemID.COINS_995` is used, NOT `ItemID.COINS` (which doesn't exist).

**F4. RSProfile storage used for snapshot data**
Verify `configManager.setRSProfileConfiguration(...)` and `configManager.getRSProfileConfiguration(...)` are used for snapshot data — NOT `configManager.setConfiguration(...)` (which is account-agnostic). Wrong API means all accounts share the same history.

**F5. WealthTrackerOverlay constructor stores fields**
Verify both `this.dataManager = dataManager` and `this.config = config` are assigned in the constructor body. If only declared as constructor parameters without assignment to fields, `render()` will NPE.

**F6. filterByDays used in refresh(), not a custom filterByRange()**
In `WealthTrackerPanel.refresh()`, verify the time range filtering calls `WealthUtils.filterByDays(all, RANGES[selectedRangeIndex])`. There should be NO method called `filterByRange()` — that was never defined.

**F7. RANGES and LABELS arrays are consistent**
Verify `RANGES = {1, 7, 30, Integer.MAX_VALUE}` and `LABELS = {"1D", "7D", "30D", "All"}` — same length, same order.

**F8. selectedRangeIndex default is 1 (7D)**
Verify the field is initialized to `1`, which corresponds to the 7D range.

**F9. FontManager.deriveFont() used for custom sizes**
Verify that any use of `FontManager.getRunescapeBoldFont()` or `FontManager.getRunescapeSmallFont()` at a custom size uses `.deriveFont(sizeF)`, not direct assignment. `FontManager` returns fixed-size fonts — you can't resize them without `deriveFont`.

**F10. ConfigManager NOT injected into WealthTrackerPlugin**
Verify there is no `@Inject private ConfigManager configManager;` field in `WealthTrackerPlugin`. It's unused and confusing. Config access goes through `config`; storage through `dataManager`.

**F11. @Provides method is in WealthTrackerPlugin, not WealthTrackerConfig**
The `@Provides WealthTrackerConfig provideConfig(ConfigManager configManager)` method must be in `WealthTrackerPlugin.java`. If it's in the config interface, Guice won't pick it up correctly.

**F12. Executor uses named daemon thread**
Verify the executor is created with a `ThreadFactory` that names the thread (e.g. `"WealthTracker-worker"`) and sets `setDaemon(true)`. Unnamed threads make debugging harder; non-daemon threads can prevent JVM shutdown.

**F13. cachedSnapshots returns defensive copy**
In `loadSnapshots()`, when returning from cache, verify it returns `new ArrayList<>(cachedSnapshots)` not `cachedSnapshots` directly. Returning the internal list allows callers to mutate it.

**F14. clearCache() is called on account switch**
In `onGameStateChanged()`, verify `dataManager.clearCache()` is called for both `LOGIN_SCREEN` and `HOPPING` game states. Missing HOPPING means that hopping worlds while staying on the same account is fine, but the issue is the plugin needs to handle logout → new login cleanly.

**F15. No dead loginSnapshotPending field**
Verify there is NO field called `loginSnapshotPending` — it was identified as dead code in the audit of the spec. If it exists, it should be removed.

---

### AUDIT BLOCK G — PLUGIN HUB SUBMISSION REQUIREMENTS

**G1. No network calls anywhere**
This duplicates C6/C7 but is worth a final explicit scan: grep entire project for any import starting with `java.net.http`, `okhttp3`, `retrofit2`, `java.net.HttpURLConnection`, or `java.net.URL`. Plugin Hub will reject anything that phones home.

**G2. No reflection**
Grep for `java.lang.reflect.Field`, `.reflect.Method`, `.reflect.Constructor`, `getDeclaredField`, `getDeclaredMethod`, `setAccessible`. Any direct use = rejection risk.

**G3. No external subprocess execution**
Grep for `Runtime.getRuntime()`, `ProcessBuilder`. Rejection risk.

**G4. No dynamic class loading**
Grep for `URLClassLoader`, `Class.forName(`, `ClassLoader`. Any use = rejection risk.

**G5. Plugin is open source — no obfuscation**
Verify source is plain readable Java. No ProGuard config. No obfuscation gradle plugins.

**G6. License file present**
Verify a `LICENSE` file exists at the repo root. BSD 2-Clause is the standard. Without it, the PR may be held up.

**G7. Public GitHub repository**
Not checkable via code audit, but flag as reminder: the repo must be public for the Plugin Hub PR to work.

**G8. README exists**
Verify a `README.md` exists. Not required by Plugin Hub rules but strongly expected and helps with review speed.

**G9. Icon size**
If `icon.png` exists, verify it is exactly 16×16 pixels (navigation button icon) OR the correct plugin icon size. Oversized icons silently display incorrectly.

---

### AUDIT BLOCK H — LOGIC AND EDGE CASES

**H1. Snapshot deduplication: bank open fires multiple events**
`ItemContainerChanged` can fire multiple times when opening a bank (once per container page load). This may cause rapid-fire snapshots. Check if there is any debouncing or rate-limiting. If not, flag as MEDIUM — acceptable for v1 but worth noting.

**H2. Empty bank vs null bank distinction**
A bank container can return with zero items (empty bank) OR return null (bank never opened this session). Verify the code handles both:
- `bank == null` → log, use 0 for bankValue
- `bank != null && bank.getItems().length == 0` → bankValue = 0, still log the snapshot

**H3. WealthSnapshot create() with all-zero values**
Verify a snapshot with all zeros doesn't cause division-by-zero or rendering issues in the chart. The chart spec says: if `min == max`, pad by 1 to avoid divide-by-zero.

**H4. filterByDays with only one snapshot in range**
If the time filter results in exactly one snapshot, the chart gets `size < 2` and shows the empty-state message. This is correct behaviour but verify the panel's delta label handles `prev == null` gracefully (shows "First snapshot recorded" or similar, not an NPE).

**H5. calculateMovers with empty itemBreakdown maps**
If both snapshots have non-null but empty `itemBreakdown` maps, `calculateMovers` should return an empty list cleanly. Verify.

**H6. Long overflow in wealth calculation**
Total wealth can theoretically exceed `Integer.MAX_VALUE` (~2.1b gp). All price and total fields must be `long`, not `int`. Verify:
- `item.getQuantity()` returns `int` in RuneLite's API
- Price lookup returns `long`
- `total = price * item.getQuantity()` — if `price` is `long` and `qty` is `int`, Java widens to `long` automatically. Verify this multiplication is not done as `int * int` first (which would overflow) then assigned to `long`.

**H7. GSON handles Map<Integer, ItemSnapshot> correctly**
GSON serializes `Map<Integer, ItemSnapshot>` with integer keys as JSON string keys (e.g. `{"12345": {...}}`). This is fine for GSON round-tripping but verify the `TypeToken` in `WealthDataManager` correctly types the map inside the snapshots. The list type token `new TypeToken<List<WealthSnapshot>>(){}` should be sufficient as GSON will handle the nested map via reflection.

**H8. WealthChartPanel thread safety of snapshots list**
`setData()` is called from the EDT. `paintComponent()` is called from the EDT. Since both happen on the same thread, there's no race condition — but verify there is no background thread calling `setData()` directly. Any call to `setData()` from a non-EDT thread is a bug.

**H9. CSV export date format**
`new java.util.Date(s.getTimestamp())` in the CSV export uses the JVM's default timezone. This is acceptable for a personal export tool, but note it for documentation.

**H10. Plugin startUp/shutDown toggle during active session**
If the user disables and re-enables the plugin while logged in:
1. `shutDown()` nulls panel, navButton, shuts down executor
2. `startUp()` creates new executor, new panel, new navButton
3. `client.getGameState() == LOGGED_IN` triggers `scheduleLoginSnapshot()`
4. The new executor has a 3-second delay task
5. `dataManager.cachedSnapshots` was NOT cleared — old cache persists across disable/re-enable

Verify step 5 is intentional (it is correct — same account, same cache) and that there's no double-registration of the overlay or navButton.

---

### AUDIT BLOCK I — UNIT TEST QUALITY

**I1. All specified tests present**
Verify the test class includes tests for:
- `formatGp`: zero, small (< 1000), thousands, just-under-million, exact million, millions, billions, negative
- `formatDelta`: positive, negative, zero
- `formatPercentage`: increase, decrease, from-zero, no-change (same value)
- `filterByDays`: all-time returns all elements
- `WealthSnapshot.create()`: totalNetWorth does NOT include coinsInHand twice
- `WealthSnapshot.create()`: timestamp is recent

**I2. Test assertions are tight**
Check for any test that only asserts `assertNotNull` or `assertTrue(result.length() > 0)` where an exact value could be asserted. Loose assertions let wrong values slip through.

**I3. Tests use JUnit 4 API**
Verify `import org.junit.Test` (not `org.junit.jupiter.api.Test`). The build uses `junit:junit:4.12` which is JUnit 4. Using JUnit 5 annotations will compile but silently not run.

**I4. main() method is not a test**
Verify the `main()` method has NO `@Test` annotation. It's a dev client launcher, not a test.

**I5. Test class has no RuneLite dependencies that prevent headless execution**
Tests in `WealthTrackerPluginTest` that test `WealthUtils` should run without a RuneLite client. Verify the WealthUtils tests don't import or instantiate anything from `net.runelite.client.*`.

---

### AUDIT BLOCK J — CODE QUALITY

**J1. All @Slf4j classes use the log field correctly**
Grep for `System.out.print` in any file with `@Slf4j`. Should be zero — use `log.*` not stdout.

**J2. No printStackTrace() calls**
Grep for `e.printStackTrace()` or `ex.printStackTrace()`. All exceptions should be logged via `log.warn()` or `log.debug()`.

**J3. No empty catch blocks**
Grep for `catch (Exception e) {}` or `catch (Exception e) { }` — swallowed exceptions hide bugs. Every catch must at minimum `log.warn(...)`.

**J4. GSON instance is static final**
Verify `private static final Gson GSON = new GsonBuilder().create()` in `WealthDataManager`. Creating a new `Gson` instance on every method call is wasteful.

**J5. Consistent brace style**
RuneLite uses Allman brace style (opening brace on new line). Verify the generated code matches this convention — it's not a bug but Plugin Hub reviewers may comment on it.

**J6. No TODO/FIXME/HACK comments left in production code**
Grep for `TODO`, `FIXME`, `HACK`, `XXX`. These should be resolved or documented as known limitations before submission.

**J7. Executor is properly closed as a resource**
In `shutDown()`, verify `executor.shutdownNow()` is called. Executors that aren't shut down leak threads and prevent proper JVM shutdown. Verify no exception in `shutDown()` could skip the executor shutdown.

**J8. WealthUtils.formatGp handles Long.MAX_VALUE**
`Long.MAX_VALUE` = 9,223,372,036,854,775,807. Verify `formatGp(Long.MAX_VALUE)` doesn't cause overflow in the billion-format branch: `Long.MAX_VALUE / 1_000_000_000.0` should be fine as a double, but verify no integer overflow in intermediate calculations.

---

## PART 3 — HOW TO CONDUCT THE AUDIT

Follow this procedure for each file:

1. **Open the file** in Cursor
2. **Read it top to bottom**, checking every item in the relevant audit blocks above
3. **Report every finding** using the format specified at the top of this prompt
4. **Mark each audit check** as: ✅ PASS | ❌ FAIL | ⚠️ WARNING | ➖ NOT APPLICABLE

After all files, produce:

```
═══════════════════════════════════════
AUDIT SUMMARY
═══════════════════════════════════════

Files audited: X / 12
Total findings: X

  CRITICAL: X  (build failures, data loss, crash on first run)
  HIGH:     X  (wrong behaviour in normal use)
  MEDIUM:   X  (wrong behaviour in edge cases)
  LOW:      X  (code quality, dev experience)

Per-file verdict:
  WealthPriceSource.java        : PASS / FAIL
  ItemSnapshot.java             : PASS / FAIL
  WealthSnapshot.java           : PASS / FAIL
  WealthUtils.java              : PASS / FAIL
  WealthDataManager.java        : PASS / FAIL
  WealthTrackerConfig.java      : PASS / FAIL
  WealthChartPanel.java         : PASS / FAIL
  WealthTrackerOverlay.java     : PASS / FAIL
  WealthTrackerPanel.java       : PASS / FAIL
  WealthTrackerPlugin.java      : PASS / FAIL
  WealthTrackerPluginTest.java  : PASS / FAIL
  build.gradle                  : PASS / FAIL
  settings.gradle               : PASS / FAIL
  runelite-plugin.properties    : PASS / FAIL

Overall verdict: SHIP ✅  /  DO NOT SHIP ❌
```

---

## PART 4 — AFTER THE AUDIT: FIX PROTOCOL

For every CRITICAL or HIGH finding, fix it in place before moving on. Do not batch all fixes at the end.

**Fix order:**
1. Fix all CRITICAL findings first (build failures, threading violations, NPEs on first run)
2. Fix all HIGH findings (wrong outputs, spec deviations)
3. Fix MEDIUM findings unless time-constrained
4. Document LOW findings as known issues if not fixing now

After every fix:
- Re-run the relevant audit check to confirm it now passes
- Run `./gradlew test` after any fix to WealthUtils, WealthSnapshot, ItemSnapshot, or WealthDataManager

After all CRITICAL + HIGH fixes are done, run the full test suite:
```bash
./gradlew test
```
All tests must pass before the plugin is considered ship-ready.

---

## PART 5 — PRE-SUBMISSION FINAL GATE

Before opening the PR to `runelite/plugin-hub`, confirm every item on this list is true:

```
□ ./gradlew test passes with zero failures
□ ./gradlew shadowJar produces a JAR without errors
□ Dev client launches (main() in test file runs without exception)
□ Plugin appears in RuneLite's Plugin list after dev client starts
□ Logging in shows the panel icon in the sidebar
□ Opening the bank triggers a snapshot (log.info line visible in console)
□ Panel displays a non-zero net worth value
□ The chart updates after 2+ bank opens
□ Disabling and re-enabling the plugin doesn't cause any exception
□ Logging out and back in works without crash
□ settings.gradle has rootProject.name = 'wealth-tracker'
□ runelite-plugin.properties has no [PLACEHOLDER] values
□ LICENSE file exists
□ README.md exists
□ Repository is public on GitHub
□ No TODO/FIXME comments remain
□ No network calls in any source file
□ icon.png exists at src/main/resources/com/wealthtracker/icon.png
```

Every unchecked box is a reason to not ship.

---

## ── AUDIT PROMPT END ──

---

## AUDIT RESULTS (executed 2026-05-16)

### Summary

```
Files audited: 14 / 14
Total findings: 6 (all fixed)

  CRITICAL: 0
  HIGH:     1  (fixed)
  MEDIUM:   4  (fixed)
  LOW:      1  (fixed)

Per-file verdict:
  WealthPriceSource.java        : PASS
  ItemSnapshot.java             : PASS
  WealthSnapshot.java           : PASS
  WealthUtils.java              : PASS (filterByDays null guard added)
  WealthDataManager.java        : PASS
  WealthTrackerConfig.java      : PASS
  WealthChartPanel.java         : PASS
  WealthTrackerOverlay.java     : PASS
  WealthTrackerPanel.java       : PASS (LinkBrowser fix)
  WealthTrackerPlugin.java      : PASS (startup refresh, config gate, log level)
  WealthTrackerPluginTest.java  : PASS (19 tests)
  build.gradle                  : PASS
  settings.gradle               : PASS (intentional deviation — see below)
  runelite-plugin.properties    : PASS

Overall verdict: SHIP
```

### Fixes applied

| Severity | File | Fix |
|----------|------|-----|
| HIGH | WealthTrackerPlugin.java | `SwingUtilities.invokeLater(panel.refresh())` on startUp |
| MEDIUM | WealthTrackerPlugin.java | Gate `scheduleLoginSnapshot()` with `config.snapshotOnLogin()` |
| MEDIUM | WealthTrackerPanel.java | `LinkBrowser.browse()` instead of `Desktop.getDesktop().browse()` |
| MEDIUM | WealthUtils.java | `filterByDays` null/empty guard |
| MEDIUM | LICENSE | BSD 2-Clause added at repo root |
| LOW | WealthTrackerPlugin.java | Snapshot log `info` → `debug` |

### Deferred / intentional

- **settings.gradle**: `rootProject.name = 'osrs-wealth-tracker-by-ge-hound'` (not `wealth-tracker`) — intentional, matches GitHub repo
- **Bank snapshot debouncing (H1)**: Accepted for v1 — multiple snapshots on bank open possible
- **Manual Part 5 checks**: Run `./gradlew run` and verify in-game before Plugin Hub PR

### Build verification

- `./gradlew test` — 19 tests, zero failures
- `./gradlew shadowJar` — BUILD SUCCESSFUL