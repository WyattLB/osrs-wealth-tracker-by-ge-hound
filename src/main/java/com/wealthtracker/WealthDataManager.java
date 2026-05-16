package com.wealthtracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
public class WealthDataManager
{
	// Snapshot data is stored by RuneLite's ConfigManager in the active RSProfile.
	// To find it manually for testing/debugging:
	//   macOS:   ~/.runelite/profiles/<profile-name>/<profile-name>.properties
	//   Windows: %USERPROFILE%\.runelite\profiles\<profile-name>\<profile-name>.properties
	// To test corrupt-data handling: open the file, find the "wealthtracker.snapshots" key,
	// replace its value with "INVALID_JSON", save, then reload the plugin.
	private static final String CONFIG_GROUP = "wealthtracker";
	private static final String SNAPSHOTS_KEY = "snapshots";
	private static final Gson GSON = new GsonBuilder().create();
	private static final Type SNAPSHOT_LIST_TYPE =
		new TypeToken<List<WealthSnapshot>>(){}.getType();

	private final ConfigManager configManager;
	private List<WealthSnapshot> cachedSnapshots = null;

	@Inject
	public WealthDataManager(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	public List<WealthSnapshot> loadSnapshots()
	{
		if (cachedSnapshots != null)
		{
			return new ArrayList<>(cachedSnapshots);
		}

		String json = configManager.getRSProfileConfiguration(
			CONFIG_GROUP, SNAPSHOTS_KEY, String.class);

		if (json == null || json.trim().isEmpty())
		{
			log.debug("WealthTracker: no saved snapshots found for this profile");
			cachedSnapshots = new ArrayList<>();
			return new ArrayList<>();
		}

		try
		{
			List<WealthSnapshot> loaded = GSON.fromJson(json, SNAPSHOT_LIST_TYPE);
			cachedSnapshots = loaded != null ? loaded : new ArrayList<>();
			log.debug("WealthTracker: loaded {} snapshots from profile", cachedSnapshots.size());
			return new ArrayList<>(cachedSnapshots);
		}
		catch (JsonSyntaxException e)
		{
			log.warn("WealthTracker: corrupt snapshot data detected, resetting history. Error: {}",
				e.getMessage());
			cachedSnapshots = new ArrayList<>();
			configManager.setRSProfileConfiguration(CONFIG_GROUP, SNAPSHOTS_KEY, "[]");
			return new ArrayList<>();
		}
	}

	public void saveSnapshot(WealthSnapshot snapshot, int retentionDays)
	{
		if (cachedSnapshots == null)
		{
			loadSnapshots();
		}

		cachedSnapshots.add(snapshot);
		pruneOldSnapshots(cachedSnapshots, retentionDays);

		String json = GSON.toJson(cachedSnapshots);
		configManager.setRSProfileConfiguration(CONFIG_GROUP, SNAPSHOTS_KEY, json);
		log.debug("WealthTracker: saved {} snapshots to profile", cachedSnapshots.size());
	}

	public WealthSnapshot getLatestSnapshot()
	{
		List<WealthSnapshot> snapshots = loadSnapshots();
		if (snapshots.isEmpty()) return null;
		return snapshots.get(snapshots.size() - 1);
	}

	public void clearCache()
	{
		log.debug("WealthTracker: clearing snapshot cache");
		cachedSnapshots = null;
	}

	private void pruneOldSnapshots(List<WealthSnapshot> list, int retentionDays)
	{
		if (retentionDays <= 0) return;
		long cutoff = Instant.now()
			.minus(retentionDays, ChronoUnit.DAYS)
			.toEpochMilli();
		int before = list.size();
		list.removeIf(s -> s.getTimestamp() < cutoff);
		int removed = before - list.size();
		if (removed > 0)
		{
			log.debug("WealthTracker: pruned {} old snapshots (retention: {} days)",
				removed, retentionDays);
		}
	}
}
