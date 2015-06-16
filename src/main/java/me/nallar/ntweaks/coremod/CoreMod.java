package me.nallar.ntweaks.coremod;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.Side;
import me.nallar.modpatcher.ModPatcher;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;

import java.io.*;
import java.util.*;

@IFMLLoadingPlugin.SortingIndex(1001) // Magic value, after deobf transformer.
public class CoreMod implements IFMLLoadingPlugin {
	public static final Logger log = LogManager.getLogger("NTweaks");
	public static Config config = new Config(new File((File) cpw.mods.fml.relauncher.FMLInjectionData.data()[6], "config/NTweaks.cfg"));
	private static boolean isClient = FMLLaunchHandler.side() == Side.CLIENT;

	private static void logToFile() {
		FileAppender fa = FileAppender.createAppender("./logs/NTweaks.log", "false", "false", "PatcherAppender", "true", "true", "true", null, null, "false", null, null);
		fa.start();
		((org.apache.logging.log4j.core.Logger) LogManager.getLogger("ModPatcher")).addAppender(fa);
		((org.apache.logging.log4j.core.Logger) LogManager.getLogger("JavaPatcher")).addAppender(fa);
		((org.apache.logging.log4j.core.Logger) log).addAppender(fa);
	}

	public static float getMobSpawningMultiplier() {
		return 1f;
	}

	public static boolean shouldHandleSpawning(WorldServer worldServer) {
		return true;
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[0];
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return ModPatcher.getSetupClass();
	}

	private void addPatch(String name, String description, boolean enabledByDefault) {
		config.add(name, description, String.valueOf(enabledByDefault));
		if (config.getBool(name)) {
			InputStream is = CoreMod.class.getResourceAsStream("/" + name + ".json");
			if (is == null) {
				ModPatcher.getPatcher().readPatchesFromXmlInputStream(CoreMod.class.getResourceAsStream("/" + name + ".xml"));
			} else {
				ModPatcher.getPatcher().readPatchesFromJsonInputStream(is);
			}
		}
	}

	private void addClientPatch(String name, String description, boolean enabledByDefault) {
		if (isClient) {
			addPatch(name, description, enabledByDefault);
		}
	}

	private void addServerPatch(String name, String description, boolean enabledByDefault) {
		if (!isClient) {
			addPatch(name, description, enabledByDefault);
		}
	}

	@Override
	public void injectData(Map<String, Object> stringObjectMap) {
		logToFile();

		addPatch("mobSpawning", "Improved mob spawning algorithm which scales mob caps at night and has better performance.", true);

		addPatch("dontLoadSpawnChunks", "Don't load spawn chunks, allow worlds to unload.", false);

		addClientPatch("tileEntityRenderRange", "Reduces the default tileEntity render range", true);

		addClientPatch("tileEntityCullingCheckOrder", "Check tile entity range culling before frustrum culling", false); // TODO determine if this actually helps

		config.save();
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
