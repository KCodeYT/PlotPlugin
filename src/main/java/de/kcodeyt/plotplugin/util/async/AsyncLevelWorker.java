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

package de.kcodeyt.plotplugin.util.async;

import cn.nukkit.Player;
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.math.BlockVector3;
import de.kcodeyt.plotplugin.util.WhenDone;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

public class AsyncLevelWorker {

    private final Level level;
    private final Set<FullChunk> usedChunks;
    private final Queue<Runnable> queue;

    public AsyncLevelWorker(Level level) {
        this.level = level;
        this.usedChunks = new HashSet<>();
        this.queue = new ArrayDeque<>();
    }

    public void queueFill(BlockVector3 startPos, BlockVector3 endPos, BlockState blockState) {
        this.queue.add(() -> {
            final int xMin = Math.min(startPos.getX(), endPos.getX());
            final int xMax = Math.max(startPos.getX(), endPos.getX());
            final int yMin = Math.min(startPos.getY(), endPos.getY());
            final int yMax = Math.min(255, Math.max(startPos.getY(), endPos.getY()));
            final int zMin = Math.min(startPos.getZ(), endPos.getZ());
            final int zMax = Math.max(startPos.getZ(), endPos.getZ());

            for(int x = xMin; x <= xMax; x++) {
                for(int z = zMin; z <= zMax; z++) {
                    final BaseFullChunk fullChunk = this.level.getChunk(x >> 4, z >> 4);
                    if(fullChunk == null) continue;
                    this.addChunk(fullChunk);

                    for(int y = yMin; y <= yMax; y++)
                        fullChunk.setBlockState(x & 15, y, z & 15, blockState);
                }
            }
        });
    }

    public void queueSetBlock(BlockVector3 blockVector3, BlockState blockState) {
        this.queue.add(() -> {
            final BaseFullChunk fullChunk = this.level.getChunk(blockVector3.getX() >> 4, blockVector3.getZ() >> 4);
            if(fullChunk == null) return;
            this.addChunk(fullChunk);

            fullChunk.setBlockState(blockVector3.getX() & 15, blockVector3.getY(), blockVector3.getZ() & 15, blockState);
        });
    }

    public void addTask(Supplier<Set<FullChunk>> chunkTask) {
        this.queue.add(() -> {
            for(FullChunk usedChunk : chunkTask.get())
                this.addChunk(usedChunk);
        });
    }

    private void addChunk(FullChunk fullChunk) {
        for(FullChunk fullChunkB : this.usedChunks)
            if(fullChunkB != null && fullChunk.getX() == fullChunkB.getX() && fullChunk.getZ() == fullChunkB.getZ())
                return;
        this.usedChunks.add(fullChunk);
    }

    public void runQueue() {
        this.runQueue(null);
    }

    public void runQueue(WhenDone whenDone) {
        if(whenDone != null) whenDone.addTask();
        TaskExecutor.executeAsync(() -> {
            this.queue.forEach(Runnable::run);

            TaskExecutor.execute(() -> {
                for(FullChunk fullChunk : this.usedChunks) {
                    if(fullChunk.getProvider() == null || fullChunk.getProvider().getLevel() == null) continue;
                    final Level level = fullChunk.getProvider().getLevel();
                    for(Player player : level.getChunkPlayers(fullChunk.getX(), fullChunk.getZ()).values())
                        level.requestChunk(fullChunk.getX(), fullChunk.getZ(), player);
                }

                this.queue.clear();
                this.usedChunks.clear();
                if(whenDone != null) whenDone.done();
            });
        });
    }

}
