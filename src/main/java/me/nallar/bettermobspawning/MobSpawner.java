package me.nallar.bettermobspawning;

import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.*;

public class MobSpawner {
	private static long hash(int x, int z) {
		return (((long) x) << 32) | (z & 0xffffffffL);
	}

	private static final int closeRange = 1;
	private static final int farRange = 5;
	private static final int spawnVariance = 6;
	private static final int clumping = 6;
	private static final int maxChunksPerPlayer = square(farRange * 2) - square(closeRange * 2);
	private static int surfaceChance;
	private static int gapChance;

	private static int square(int a) {
		return a * a;
	}

	private static Chunk getChunkFromBlockCoords(WorldServer w, int x, int z) {
		IChunkProvider p = w.getChunkProvider();
		if (p.chunkExists(x >> 4, z >> 4)) {
			return p.provideChunk(x >> 4, z >> 4);
		}
		return null;
	}

	private static int getPseudoRandomHeightValue(int wX, int wZ, WorldServer worldServer, boolean surface, int gapChance) {
		Chunk chunk = getChunkFromBlockCoords(worldServer, wX, wZ);
		if (chunk == null) {
			return -1;
		}
		int x = wX & 15;
		int z = wZ & 15;
		int height = chunk.getHeightValue(x, z);
		if (surface) {
			return height;
		}
		boolean inGap = false;
		int lastGap = 0;
		for (int y = 1; y < height; y++) {
			Block block = chunk.getBlock(x, y, z);
			if (block == null || block == Blocks.air) {
				if (!inGap) {
					inGap = true;
					if (gapChance++ % 3 == 0) {
						return y;
					}
					lastGap = y;
				}
			} else {
				inGap = false;
			}
		}
		return lastGap == 0 ? height : lastGap;
	}

	public static boolean spawningHook(WorldServer worldServer, boolean peaceful, boolean hostile, boolean animal) {
		if (!CoreMod.shouldHandleSpawning(worldServer)) {
			return false;
		}
		if (worldServer.getWorldTime() % clumping != 0) {
			return true;
		}
		float entityMultiplier = worldServer.playerEntities.size() * CoreMod.getMobSpawningMultiplier();
		if (entityMultiplier == 0) {
			return true;
		}

		final Profiler profiler = worldServer.theProfiler;
		profiler.startSection("creatureTypes");
		IChunkProvider p = worldServer.getChunkProvider();
		boolean dayTime = worldServer.isDaytime();
		float mobMultiplier = entityMultiplier * (dayTime ? 1 : 2);
		Map<EnumCreatureType, Integer> requiredSpawns = (Map<EnumCreatureType, Integer>) new EnumMap(EnumCreatureType.class);
		for (EnumCreatureType creatureType : EnumCreatureType.values()) {
			int count = (int) ((creatureType.getPeacefulCreature() ? entityMultiplier : mobMultiplier) * creatureType.getMaxNumberOfCreature());
			if (!(creatureType.getPeacefulCreature() && !peaceful || creatureType.getAnimal() && !animal || !creatureType.getPeacefulCreature() && !hostile) && count > worldServer.countEntities(creatureType.getCreatureClass())) {
				requiredSpawns.put(creatureType, count);
			}
		}
		profiler.endSection();

		if (requiredSpawns.isEmpty()) {
			return true;
		}

		profiler.startSection("spawnableChunks");
		int attemptedSpawnedMobs = 0;
		LongSet closeChunks = new LongSet();
		Collection<EntityPlayer> entityPlayers = worldServer.playerEntities;
		LongList spawnableChunks = new LongList(entityPlayers.size() * maxChunksPerPlayer);
		for (EntityPlayer entityPlayer : entityPlayers) {
			int pX = entityPlayer.chunkCoordX;
			int pZ = entityPlayer.chunkCoordZ;
			int x = pX - closeRange;
			int maxX = pX + closeRange;
			int startZ = pZ - closeRange;
			int maxZ = pZ + closeRange;
			for (; x <= maxX; x++) {
				for (int z = startZ; z <= maxZ; z++) {
					closeChunks.add(hash(x, z));
				}
			}
		}
		for (EntityPlayer entityPlayer : entityPlayers) {
			int pX = entityPlayer.chunkCoordX;
			int pZ = entityPlayer.chunkCoordZ;
			int x = pX - farRange;
			int maxX = pX + farRange;
			int startZ = pZ - farRange;
			int maxZ = pZ + farRange;
			for (; x <= maxX; x++) {
				for (int z = startZ; z <= maxZ; z++) {
					long hash = hash(x, z);
					if (!closeChunks.contains(hash) || !p.chunkExists(x, z)) {
						spawnableChunks.add(hash);
					}
				}
			}
		}
		profiler.endStartSection("spawnMobs");

		int size = spawnableChunks.size;
		if (size == 0) {
			return true;
		}

		SpawnLoop:
		for (Map.Entry<EnumCreatureType, Integer> entry : requiredSpawns.entrySet()) {
			EnumCreatureType creatureType = entry.getKey();
			long hash = spawnableChunks.get(worldServer.rand.nextInt(size));
			int x = (int) (hash >> 32);
			int z = (int) hash;
			int sX = x * 16 + worldServer.rand.nextInt(16);
			int sZ = z * 16 + worldServer.rand.nextInt(16);
			boolean surface = !(worldServer.provider instanceof WorldProviderHell) && creatureType.getPeacefulCreature() || (dayTime ? surfaceChance++ % 5 == 0 : surfaceChance++ % 5 != 0);
			int gap = gapChance++;
			int sY;
			if (creatureType == EnumCreatureType.waterCreature) {
				String biomeName = worldServer.getBiomeGenForCoords(sX, sZ).biomeName;
				if (!"Ocean".equals(biomeName) && !"River".equals(biomeName)) {
					continue;
				}
				sY = getPseudoRandomHeightValue(sX, sZ, worldServer, true, gap) - 2;
			} else {
				sY = getPseudoRandomHeightValue(sX, sZ, worldServer, surface, gap);
			}
			if (sY < 0) {
				continue;
			}
			if (worldServer.getBlock(sX, sY, sZ).getMaterial() == creatureType.getCreatureMaterial()) {
				IEntityLivingData unusedIEntityLivingData = null;
				for (int i = 0; i < ((clumping * 3) / 2); i++) {
					int ssX = sX + (worldServer.rand.nextInt(spawnVariance) - spawnVariance / 2);
					int ssZ = sZ + (worldServer.rand.nextInt(spawnVariance) - spawnVariance / 2);

					if (!p.chunkExists(ssX >> 4, ssZ >> 4)) {
						continue;
					}

					int ssY;

					if (creatureType == EnumCreatureType.waterCreature) {
						ssY = sY;
					} else if (creatureType == EnumCreatureType.ambient) {
						ssY = worldServer.rand.nextInt(63) + 1;
					} else {
						ssY = getPseudoRandomHeightValue(ssX, ssZ, worldServer, surface, gap);
						if (ssY == -1 ||
								!worldServer.getBlock(ssX, ssY - 1, ssZ).getMaterial().isOpaque() ||
								!worldServer.getBlock(ssX, ssY - 1, ssZ).canCreatureSpawn(creatureType, worldServer, ssX, ssY - 1, ssZ)) {
							continue;
						}
					}

					if (creatureType == EnumCreatureType.waterCreature || (!worldServer.getBlock(ssX, ssY - 1, ssZ).getMaterial().isLiquid())) {
						BiomeGenBase.SpawnListEntry creatureClass = worldServer.spawnRandomCreature(creatureType, ssX, ssY, ssZ);
						if (creatureClass == null) {
							break;
						}

						EntityLiving spawnedEntity;
						try {
							spawnedEntity = (EntityLiving) creatureClass.entityClass.getConstructor(World.class).newInstance(worldServer);
							spawnedEntity.setLocationAndAngles((double) ssX, (double) ssY, (double) ssZ, worldServer.rand.nextFloat() * 360.0F, 0.0F);

							Event.Result canSpawn = ForgeEventFactory.canEntitySpawn(spawnedEntity, worldServer, ssX, ssY, ssZ);
							if (canSpawn == Event.Result.ALLOW || (canSpawn == Event.Result.DEFAULT && spawnedEntity.getCanSpawnHere())) {
								worldServer.spawnEntityInWorld(spawnedEntity);
								if (!ForgeEventFactory.doSpecialSpawn(spawnedEntity, worldServer, ssX, ssY, ssZ)) {
									unusedIEntityLivingData = spawnedEntity.onSpawnWithEgg(unusedIEntityLivingData);
								}
							}
							attemptedSpawnedMobs++;
						} catch (Exception e) {
							System.err.println("Failed to spawn entity " + creatureClass);
							e.printStackTrace();
							break SpawnLoop;
						}
					}
				}
			}
			if (attemptedSpawnedMobs >= 24) {
				break;
			}
		}
		profiler.endSection();
		return true;
	}
}
