/*
 * Copyright 2022 KCodeYT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ms.kevi.plotplugin.util.async;

import cn.nukkit.Player;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockState;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.IChunk;
import cn.nukkit.math.BlockVector3;
import ms.kevi.plotplugin.util.WhenDone;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class AsyncLevelWorker {

    private final Level level;
    private final Set<IChunk> usedChunks;
    private final Queue<Runnable> queue;

    public AsyncLevelWorker(Level level) {
        this.level = level;
        this.usedChunks = new HashSet<>();
        this.queue = new ArrayDeque<>();
    }

    public void queueFill(BlockVector3 startPos, BlockVector3 endPos, BlockState blockState) {
        this.queue.add(() -> {
            for (int x = startPos.getX(); x <= endPos.getX(); x++) {
                for (int z = startPos.getZ(); z <= endPos.getZ(); z++) {
                    final IChunk IChunk = this.level.getChunk(x >> 4, z >> 4);
                    if (IChunk == null) continue;
                    this.addChunk(IChunk);

                    for (int y = startPos.getY(); y <= endPos.getY(); y++) {
                        IChunk.setBlockState(x & 15, y, z & 15, blockState, 0);
                        IChunk.setBlockState(x & 15, y, z & 15, BlockAir.STATE, 1);
                    }
                }
            }
        });
    }

    public void addTask(Supplier<Set<IChunk>> chunkTask) {
        this.queue.add(() -> {
            for (IChunk usedChunk : chunkTask.get())
                this.addChunk(usedChunk);
        });
    }

    private void addChunk(IChunk IChunk) {
        for (IChunk fullChunkB : this.usedChunks)
            if (fullChunkB != null && IChunk.getX() == fullChunkB.getX() && IChunk.getZ() == fullChunkB.getZ())
                return;
        this.usedChunks.add(IChunk);
    }

    public void runQueue() {
        this.runQueue(null);
    }

    public void runQueue(WhenDone whenDone) {
        if (whenDone != null) whenDone.addTask();
        TaskExecutor.executeAsync(() -> {
            this.queue.forEach(Runnable::run);

            TaskExecutor.execute(() -> {
                for (IChunk IChunk : this.usedChunks) {
                    if (IChunk.getProvider() == null || IChunk.getProvider().getLevel() == null) continue;
                    final Level level = IChunk.getProvider().getLevel();
                    for (Player player : level.getChunkPlayers(IChunk.getX(), IChunk.getZ()).values())
                        level.requestChunk(IChunk.getX(), IChunk.getZ(), player);
                }

                this.queue.clear();
                this.usedChunks.clear();
                if (whenDone != null) whenDone.done();
            });
        });
    }

}
