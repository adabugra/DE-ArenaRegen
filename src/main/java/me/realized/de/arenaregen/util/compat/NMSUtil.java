package me.realized.de.arenaregen.util.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import me.realized.de.arenaregen.util.ReflectionUtil;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public final class NMSUtil {

    private static Constructor<?> BLOCK_POS_CONSTRUCTOR;
    private static Object BLOCK_ENUM;
    private static Method WORLD_GET_HANDLE;
    private static Method WORLD_C_17;
    private static Method WORLD_C;
    private static Method WORLD_UPDATE_LIGHTING_17;
    private static Method WORLD_UPDATE_LIGHTING;
    private static Method CHUNK_GET_HANDLE;
    private static Method BLOCK_FROM_LEGACY_DATA;
    private static Method BLOCK_GET_DATA;
    private static Method MAGIC_GET_BLOCK;
    private static Method CHUNK_SET_BLOCK_7;
    private static Method CHUNK_SET_BLOCK_12;
    private static Method CHUNK_SET_BLOCK;

    private static Method GET_CHUNK_PROVIDER;
    private static Method GET_LIGHT_ENGINE;
    private static Method LIGHT_ENGINE_A;

    static {
        try {
            final Class<?> BLOCK_POS = ReflectionUtil.getNMSClass("BlockPosition");
            BLOCK_POS_CONSTRUCTOR = ReflectionUtil.getConstructor(BLOCK_POS, Double.TYPE, Double.TYPE, Double.TYPE);
            WORLD_GET_HANDLE = ReflectionUtil.getMethod(ReflectionUtil.getCBClass("CraftWorld"), "getHandle");
            final Class<?> WORLD = ReflectionUtil.getNMSClass("World");
            final Class<?> ENUM_SKY_BLOCK = ReflectionUtil.getNMSClass("EnumSkyBlock");
            WORLD_C_17 = ReflectionUtil.getMethod(WORLD, "c", ENUM_SKY_BLOCK, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            WORLD_C = ReflectionUtil.getMethod(WORLD, "c", ENUM_SKY_BLOCK, BLOCK_POS);
            GET_CHUNK_PROVIDER = ReflectionUtil.getMethod(WORLD, "getChunkProvider");
            final Class<?> CHUNK_PROVIDER = ReflectionUtil.getNMSClass("IChunkProvider");
            GET_LIGHT_ENGINE = ReflectionUtil.getMethod(CHUNK_PROVIDER, "getLightEngine");
            final Class<?> LIGHT_ENGINE = ReflectionUtil.getNMSClass("LightEngine");
            LIGHT_ENGINE_A = ReflectionUtil.getMethod(LIGHT_ENGINE, "a", BLOCK_POS);

            if (CompatUtil.isPaper()) {
                WORLD_UPDATE_LIGHTING_17 = ReflectionUtil.getMethod(WORLD, "updateLighting", ENUM_SKY_BLOCK, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                WORLD_UPDATE_LIGHTING = ReflectionUtil.getMethod(WORLD, "updateLighting", ENUM_SKY_BLOCK, BLOCK_POS);
            }

            BLOCK_ENUM = ReflectionUtil.getEnumConstant(ENUM_SKY_BLOCK, 1);
            CHUNK_GET_HANDLE = ReflectionUtil.getMethod(ReflectionUtil.getCBClass("CraftChunk"), "getHandle");
            final Class<?> BLOCK = ReflectionUtil.getNMSClass("Block");
            BLOCK_FROM_LEGACY_DATA = ReflectionUtil.getMethod(BLOCK, "fromLegacyData", Integer.TYPE);
            BLOCK_GET_DATA = ReflectionUtil.getMethod(BLOCK, "getBlockData");
            MAGIC_GET_BLOCK = ReflectionUtil.getMethod(ReflectionUtil.getCBClass("util.CraftMagicNumbers"), "getBlock", Material.class);
            final Class<?> CHUNK = ReflectionUtil.getNMSClass("Chunk");
            CHUNK_SET_BLOCK_7 = ReflectionUtil.getMethod(CHUNK, "a", Integer.TYPE, Integer.TYPE, Integer.TYPE, BLOCK, Integer.TYPE);
            final Class<?> BLOCK_DATA = ReflectionUtil.getNMSClass("IBlockData");
            CHUNK_SET_BLOCK_12 = ReflectionUtil.getMethod(CHUNK, "a", BLOCK_POS, BLOCK_DATA);
            CHUNK_SET_BLOCK = ReflectionUtil.getMethod(CHUNK, "a", BLOCK_POS, BLOCK_DATA, Boolean.TYPE);

            if (CHUNK_SET_BLOCK == null) {
                CHUNK_SET_BLOCK = ReflectionUtil.getMethod(CHUNK, "setType", BLOCK_POS, BLOCK_DATA, Boolean.TYPE);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public static void setBlockFast(final Block block, final Material material, final int data) {
        final Chunk chunk = block.getChunk();
        final int x = block.getX();
        final int y = block.getY();
        final int z = block.getZ();

        try {
            final Object chunkHandle = CHUNK_GET_HANDLE.invoke(chunk);
            Object nmsBlock = MAGIC_GET_BLOCK.invoke(null, material);

            if (CompatUtil.isPre1_8()) {
                CHUNK_SET_BLOCK_7.invoke(chunkHandle, x & 0x0F, y, z & 0x0F, nmsBlock, data);

                if (block.getType() == Material.AIR || isSurrounded(block.getWorld(), x, y, z)) {
                    return;
                }

                final Object worldHandle = WORLD_GET_HANDLE.invoke(block.getWorld());

                if (CompatUtil.isPaper()) {
                    WORLD_UPDATE_LIGHTING_17.invoke(worldHandle, BLOCK_ENUM, x, y, z);
                } else {
                    WORLD_C_17.invoke(worldHandle, BLOCK_ENUM, x, y, z);
                }
            } else {
                final Object blockPos = BLOCK_POS_CONSTRUCTOR.newInstance(x, y, z);

                if (CompatUtil.isPre1_13()) {
                    nmsBlock = BLOCK_FROM_LEGACY_DATA.invoke(nmsBlock, data);
                    CHUNK_SET_BLOCK_12.invoke(chunkHandle, blockPos, nmsBlock);
                } else {
                    CHUNK_SET_BLOCK.invoke(chunkHandle, blockPos, BLOCK_GET_DATA.invoke(nmsBlock), true);
                }

                if (block.getType() == Material.AIR || isSurrounded(block.getWorld(), x, y, z)) {
                    return;
                }

                final Object worldHandle = WORLD_GET_HANDLE.invoke(block.getWorld());

                if (CompatUtil.isPre1_14()) {
                    if (CompatUtil.isPaper() && WORLD_UPDATE_LIGHTING != null) {
                        WORLD_UPDATE_LIGHTING.invoke(worldHandle, BLOCK_ENUM, blockPos);
                    } else {
                        WORLD_C.invoke(worldHandle, BLOCK_ENUM, blockPos);
                    }
                } else {
                    LIGHT_ENGINE_A.invoke(GET_LIGHT_ENGINE.invoke(GET_CHUNK_PROVIDER.invoke(worldHandle)), blockPos);
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private static boolean isSurrounded(final World world, final int x, final int y, final int z) {
        final Block base = world.getBlockAt(x, y, z);
        final Block east = base.getRelative(BlockFace.EAST);
        final Block west = base.getRelative(BlockFace.WEST);
        final Block south = base.getRelative(BlockFace.SOUTH);
        final Block north = base.getRelative(BlockFace.NORTH);
        final Block up = base.getRelative(BlockFace.UP);
        final Block down = base.getRelative(BlockFace.DOWN);
        return !east.getType().isTransparent()
            && !west.getType().isTransparent()
            && !up.getType().isTransparent()
            && !down.getType().isTransparent()
            && !south.getType().isTransparent()
            && !north.getType().isTransparent();
    }

    private NMSUtil() {}
}
