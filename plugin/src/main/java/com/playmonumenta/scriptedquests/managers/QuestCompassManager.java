package com.playmonumenta.scriptedquests.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestCompass;
import com.playmonumenta.scriptedquests.quests.components.CompassLocation;
import com.playmonumenta.scriptedquests.quests.components.DeathLocation;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

public class QuestCompassManager {
	private static class ValidCompassEntry {
		private final CompassLocation mLocation;
		private final String mTitle;

		private ValidCompassEntry(CompassLocation loc, String title) {
			mLocation = loc;
			mTitle = title;
		}

		private void directPlayer(Player player) {
			MessagingUtils.sendRawMessage(player, mTitle + ": " + mLocation.getMessage());
			player.setCompassTarget(mLocation.getLocation());
		}
	}

	private static class CompassCacheEntry {
		private final int mLastRefresh;
		private final List<ValidCompassEntry> mEntries;

		private CompassCacheEntry(Player player, List<ValidCompassEntry> entries) {
			mLastRefresh = player.getTicksLived();
			mEntries = entries;
		}

		private boolean isStillValid(Player player) {
			return Math.abs(player.getTicksLived() - mLastRefresh) < 200;
		}
	}

	private final List<QuestCompass> mQuests = new ArrayList<QuestCompass>();
	private final Map<UUID, CompassCacheEntry> mCompassCache = new HashMap<UUID, CompassCacheEntry>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		mQuests.clear();
		QuestUtils.loadScriptedQuests(plugin, "compass", sender, (object) -> {
			QuestCompass quest = new QuestCompass(plugin.mWorld, object);
			mQuests.add(quest);
			return quest.getQuestName() + ":" + Integer.toString(quest.getMarkers().size());
		});
	}

	@SuppressWarnings("unchecked")
	private List<ValidCompassEntry> getCurrentMarkerTitles(Player player) {
		/*
		 * First check the cache - if it is still valid, returned the cached data
		 * This dramatically improves performance when there are many compass entries
		 */
		CompassCacheEntry cachedEntry = mCompassCache.get(player.getUniqueId());
		if (cachedEntry != null && cachedEntry.isStillValid(player)) {
			return cachedEntry.mEntries;
		}


		/*
		 * No cached entry - need to recompute everything
		 */
		List<ValidCompassEntry> entries = new ArrayList<ValidCompassEntry>();
		for (QuestCompass quest : mQuests) {
			List<CompassLocation> questMarkers = quest.getMarkers(player);

			// Add all the valid markers/titles to the list
			for (int i = 0; i < questMarkers.size(); i++) {
				String title = ChatColor.AQUA + "" + ChatColor.BOLD + quest.getQuestName()
				               + ChatColor.RESET + "" + ChatColor.AQUA;

				if (questMarkers.size() > 1) {
					title += " [" + (i + 1) + "/" + questMarkers.size() + "]";
				}

				entries.add(new ValidCompassEntry(questMarkers.get(i), title));
			}
		}

		// Add player death locations
		if (player.hasMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY)) {
			List<DeathLocation> deathEntries =
			    (List<DeathLocation>)player.getMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY).get(0).value();
			for (int i = 0; i < deathEntries.size(); i++) {
				String title = ChatColor.RED + "" + ChatColor.BOLD + "Death"
				               + ChatColor.RESET + "" + ChatColor.AQUA;

				if (deathEntries.size() > 1) {
					title += " [" + (i + 1) + "/" + deathEntries.size() + "]";
				}

				entries.add(new ValidCompassEntry(deathEntries.get(i), title));
			}
		}

		// Cache this result for later
		mCompassCache.put(player.getUniqueId(), new CompassCacheEntry(player, entries));

		return entries;
	}

	private int showCurrentQuest(Player player, int index) {
		List<ValidCompassEntry> entries = getCurrentMarkerTitles(player);

		if (index >= entries.size()) {
			index = 0;
		}

		if (entries.size() == 0) {
			MessagingUtils.sendActionBarMessage(player, "You have no active quest.");
		} else {
			entries.get(index).directPlayer(player);
		}

		return index;
	}

	public void showCurrentQuest(Player player) {
		int index = ScoreboardUtils.getScoreboardValue(player, "locationIndex");

		showCurrentQuest(player, index);
	}

	public void cycleQuestTracker(Player player) {
		int index = ScoreboardUtils.getScoreboardValue(player, "locationIndex") + 1;

		index = showCurrentQuest(player, index);

		ScoreboardUtils.setScoreboardValue(player, "locationIndex", index);
	}
}
