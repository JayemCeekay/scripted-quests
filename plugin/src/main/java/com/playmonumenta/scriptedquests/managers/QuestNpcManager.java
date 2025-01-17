package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public class QuestNpcManager {
	private final Plugin mPlugin;
	private final Map<EntityType, Map<String, QuestNpc>> mNpcs = new HashMap<>();
	private final EnumSet<EntityType> mEntityTypes = EnumSet.noneOf(EntityType.class);

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		mNpcs.clear();
		mEntityTypes.clear();

		QuestUtils.loadScriptedQuests(plugin, "npcs", sender, (object) -> {
			// Load this file into a QuestNpc object
			QuestNpc npc = new QuestNpc(object);

			// Track this type of entity from now on when entities are interacted with
			EntityType type = npc.getEntityType();
			mEntityTypes.add(type);

			// Check if an existing NPC already exists with quest components
			Map<String, QuestNpc> entityNpcMap = mNpcs.get(type);
			if (entityNpcMap == null) {
				// This is the first NPC of this type - create the map for it
				entityNpcMap = new HashMap<String, QuestNpc>();
				mNpcs.put(type, entityNpcMap);
			}

			QuestNpc existingNpc = entityNpcMap.get(npc.getNpcName());
			if (existingNpc != null) {
				// Existing NPC - add the new quest components to it
				existingNpc.addFromQuest(plugin, npc);
			} else {
				entityNpcMap.put(npc.getNpcName(), npc);
			}

			return npc.getNpcName() + ":" + Integer.toString(npc.getComponents().size());
		});
	}

	public QuestNpcManager(Plugin plugin) {
		mPlugin = plugin;
	}

	public @Nullable QuestNpc getInteractNPC(Entity entity) {
		return getInteractNPC(entity.getCustomName(), entity.getType());
	}

	public @Nullable QuestNpc getInteractNPC(String npcName, EntityType entityType) {
		// Only search for the entity's name if we have a quest with that entity type
		if (!mEntityTypes.contains(entityType)) {
			return null;
		}

		// Only entities with custom names
		if (npcName == null || npcName.isEmpty()) {
			return null;
		}

		// Return the NPC if we have an NPC with that name
		Map<String, QuestNpc> entityNpcMap = mNpcs.get(entityType);
		if (entityNpcMap == null) {
			mPlugin.getLogger().severe("BUG! EntityTypes contains type '" +
			                          entityType.toString() + "' but there is no map for it!");
			return null;
		} else {
			QuestNpc npc = entityNpcMap.get(QuestNpc.squashNpcName(npcName));
			if (npc != null) {
				return npc;
			}
		}

		return null;
	}

	public boolean interactEvent(QuestContext context, String npcName, EntityType entityType, boolean force) {
		QuestNpc npc = getInteractNPC(npcName, entityType);
		if (npc != null) {
			return interactEvent(context, npcName, entityType, npc, force);
		}
		return false;
	}

	public boolean interactEvent(QuestContext context, String npcName, EntityType entityType, QuestNpc npc, boolean force) {
		// Only one interaction per player per tick
		if (!force && !MetadataUtils.checkOnceThisTick(context.getPlugin(), context.getPlayer(), "ScriptedQuestsNPCInteractNonce")) {
			return false;
		}

		// Check if race allows this
		if (!context.getPlugin().mRaceManager.isNotRacingOrAllowsNpcInteraction(context.getPlayer())) {
			return false;
		}

		if (npc != null) {
			return npc.interactEvent(context, QuestNpc.squashNpcName(npcName), entityType);
		}
		return false;
	}

	public Stream<QuestNpc> getNpcsStream() {
		return mNpcs.values().stream().flatMap(e -> e.values().stream());
	}
}
