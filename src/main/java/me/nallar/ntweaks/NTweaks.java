package me.nallar.ntweaks;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

@Mod(modid = "nTweaks", name = "nTweaks", version = "1.7.10", acceptableRemoteVersions = "*")
public class NTweaks {
	private final MemoryLeakDetector memoryLeakDetector = new MemoryLeakDetector(300);

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent (priority = EventPriority.LOWEST)
	public void worldUnload(WorldEvent.Unload event) {
		memoryLeakDetector.scheduleLeakCheck(event.world, "World " + event.world.provider.getDimensionName(), true);
	}
}
