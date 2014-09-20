package me.nallar.bettermobspawning;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import me.nallar.javapatcher.mappings.MethodDescription;
import me.nallar.modpatcher.ModPatcher;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;

import java.util.*;

@IFMLLoadingPlugin.SortingIndex(1001) // Magic value, after deobf transformer.
public class CoreMod implements IFMLLoadingPlugin {
	public static final Logger log = LogManager.getLogger("nTweaks");

	private static void logToFile() {
		FileAppender fa = FileAppender.createAppender("./logs/Patcher.log", "false", "false", "PatcherAppender", "true", "true", "true", null, null, "false", null, null);
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
		ModPatcher.getPatcher().readPatchesFromJsonInputStream(CoreMod.class.getResourceAsStream("/" + name + ".json"));
	}

	@Override
	public void injectData(Map<String, Object> stringObjectMap) {
		logToFile();
		boolean isClient = true; // TODO Don't just invent a value
		addPatch("mobSpawning", "Improved mob spawning algorithm which scales mob caps at night and has better performance.", true);
		addPatch("tileEntityRenderRange", "Reduces the default tileEntity render range. Client only.", isClient);

		log.info(ModPatcher.getPatcher().getMappings().map(MethodDescription.fromString("net.minecraft.tileentity.TileEntity", "getMaxRenderDistanceSquared")).toString());
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
