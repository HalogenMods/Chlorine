package dev.hanetzer.chlorine.plugin.ftb;

import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.util.math.ChunkPos;

public class FTBChunksChecker {
    public static void queueChunk(int x, int z) {
        FTBChunksClient.rerenderCache.add(new ChunkPos(x, z));
    }
}
