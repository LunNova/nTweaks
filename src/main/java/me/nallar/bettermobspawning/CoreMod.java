package me.nallar.bettermobspawning;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import me.nallar.modpatcher.ModPatcher;
import net.minecraft.world.WorldServer;

import java.util.*;

@IFMLLoadingPlugin.SortingIndex(1001) // Magic value, after deobf transformer.
public class CoreMod implements IFMLLoadingPlugin {
	public static float getMobSpawningMultiplier() {
		return 1.5f;
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

	@Override
	public void injectData(Map<String, Object> stringObjectMap) {
		ModPatcher.getPatcher().readPatchesFromJsonInputStream(CoreMod.class.getResourceAsStream("/modpatcher.json"));
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
