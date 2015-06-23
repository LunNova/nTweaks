package me.nallar.ntweaks;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import me.nallar.ntweaks.coremod.CoreMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

@Mod(modid = "nTweaks", name = "nTweaks", version = "1.7.10", acceptableRemoteVersions = "*")
public class NTweaks {
	private final MemoryLeakDetector memoryLeakDetector = new MemoryLeakDetector(300);

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
		if (CoreMod.config.getBool("unloadAllWorlds")) {
			FMLCommonHandler.instance().bus().register(new UnloadTickHandler());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void worldUnload(WorldEvent.Unload event) {
		memoryLeakDetector.scheduleLeakCheck(event.world, "World " + event.world.provider.getDimensionName(), CoreMod.config.getBool("cleanUnloadedWorlds"));
	}

	public static class UnloadTickHandler {
		private int counter = 0;

		@SubscribeEvent
		public void tick(TickEvent.ServerTickEvent tick) {
			if (tick.phase == TickEvent.Phase.END && (++counter % 2000 == 100)) {
				for (WorldServer worldServer : MinecraftServer.getServer().worldServers) {
					int id = worldServer.provider.dimensionId;
					if (id != 0
						&& worldServer.getChunkProvider().getLoadedChunkCount() == 0
						&& worldServer.playerEntities.isEmpty()) {
						CoreMod.log.info("Unloading world " + id);
						DimensionManager.unloadWorld(id);
					}
				}
			}
		}
	}
}
