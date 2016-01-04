package me.nallar.ntweaks;

import com.google.common.base.Splitter;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import me.nallar.ntweaks.coremod.CoreMod;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import java.lang.ref.*;
import java.util.*;

@Mod(modid = "nTweaks", name = "nTweaks", version = "1.7.10", acceptableRemoteVersions = "*")
public class NTweaks {
	private static final boolean cleanWorlds = CoreMod.cleanUnloadedWorlds;
	private static final Set<Integer> unloadWorldBlacklist = new HashSet<Integer>();
	private final MemoryLeakDetector memoryLeakDetector = new MemoryLeakDetector(300);
	public static final ArrayList<WeakReference<Entity>> morphEntities = new ArrayList<WeakReference<Entity>>();

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
		if (CoreMod.config.getBool("unloadAllWorlds")) {
			FMLCommonHandler.instance().bus().register(new UnloadTickHandler());
		}
		String unloadWorldBlacklist = CoreMod.config.get("unloadWorldBlacklist");
		for (String part : Splitter.on(',').trimResults().omitEmptyStrings().split(unloadWorldBlacklist)) {
			NTweaks.unloadWorldBlacklist.add(Integer.valueOf(part));
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void worldUnload(WorldEvent.Unload event) {
		memoryLeakDetector.scheduleLeakCheck(event.world, "World " + event.world.provider.getDimensionName(), cleanWorlds);
		if (!cleanWorlds) {
			return;
		}
		if (event.world.isRemote || event.world.provider.dimensionId != 0) {
			removeOldMorphWorlds(event.world);
		}
		if (event.world.isRemote) {
			Minecraft.getMinecraft().renderGlobal.setWorldAndLoadRenderers(null);
		}
	}

	private void removeOldMorphWorlds(World remove) {
		List<WeakReference<Entity>> toRemove = new ArrayList<WeakReference<Entity>>();
		for (WeakReference<Entity> w : morphEntities) {
			Entity e = w.get();
			if (e == null) {
				toRemove.add(w);
				continue;
			}
			if (e.worldObj == remove) {
				if (remove.isRemote) {
					e.worldObj = null;
				} else {
					e.worldObj = DimensionManager.getWorld(0);
				}
			}
		}
		morphEntities.removeAll(toRemove);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void worldLoad(WorldEvent.Load event) {
		if (!cleanWorlds) {
			return;
		}
		List<WeakReference<Entity>> toRemove = new ArrayList<WeakReference<Entity>>();
		for (WeakReference<Entity> w : morphEntities) {
			Entity e = w.get();
			if (e == null) {
				toRemove.add(w);
				continue;
			}
			if (e.worldObj == null || e.worldObj.isRemote || e.worldObj.provider == null) {
				e.worldObj = event.world;
			}
		}
		morphEntities.removeAll(toRemove);
	}

	public static boolean canUnload(WorldServer worldServer) {
		int id = worldServer.provider.dimensionId;
		return id != 0
			&& !unloadWorldBlacklist.contains(id)
			&& worldServer.getChunkProvider().getLoadedChunkCount() == 0
			&& worldServer.playerEntities.isEmpty();
	}

	public static class UnloadTickHandler {
		private int counter = 0;

		@SubscribeEvent
		public void tick(TickEvent.ServerTickEvent tick) {
			if (tick.phase == TickEvent.Phase.END && (++counter % 2400 == 100)) {
				for (WorldServer worldServer : MinecraftServer.getServer().worldServers) {
					if (canUnload(worldServer))
						DimensionManager.unloadWorld(worldServer.provider.dimensionId);
				}
			}
		}
	}
}
