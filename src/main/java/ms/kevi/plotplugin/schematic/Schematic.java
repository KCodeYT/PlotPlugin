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

package ms.kevi.plotplugin.schematic;

import cn.nukkit.Server;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.BinaryStream;
import cn.nukkit.utils.Zlib;
import com.github.luben.zstd.Zstd;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.schematic.format.SchematicSerializer;
import ms.kevi.plotplugin.schematic.format.SchematicSerializers;
import ms.kevi.plotplugin.util.Allowed;
import ms.kevi.plotplugin.util.ShapeType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
@Getter
@ToString
@EqualsAndHashCode
public class Schematic {

    private static final byte[] MAGIC = {0x53, 0x43, 0x48, 0x45, 0x4D};

    private final List<SchematicBlock> blockPalette;
    private final Object2IntMap<Vector3> blocks;
    private final Map<BlockVector3, SchematicBlockEntity> blockEntities;

    public Schematic() {
        this.blockPalette = new ArrayList<>();
        this.blocks = new Object2IntArrayMap<>();
        this.blockEntities = new HashMap<>();
    }

    public boolean isEmpty() {
        return this.blockPalette.isEmpty() && this.blocks.isEmpty() && this.blockEntities.isEmpty();
    }

    public void addBlock(Vector3 vector3, SchematicBlock block) {
        int index = this.blockPalette.indexOf(block);
        if(index == -1) {
            this.blockPalette.add(block);
            index = this.blockPalette.size() - 1;
        }

        this.blocks.put(vector3, index);
    }

    public void addBlockEntity(BlockVector3 blockVector3, String type, CompoundTag compoundTag) {
        this.blockEntities.put(blockVector3, new SchematicBlockEntity(type, compoundTag));
    }

    public void buildInChunk(Vector3 start, FullChunk fullChunk, ShapeType[] shapes, Allowed<ShapeType> allowedShapes, Integer minX, Integer minZ, Integer maxX, Integer maxZ) {
        final int startX = start.getFloorX();
        final int startY = start.getFloorY();
        final int startZ = start.getFloorZ();

        for(Object2IntMap.Entry<Vector3> entry : this.blocks.object2IntEntrySet()) {
            final Vector3 blockVector = entry.getKey();
            final SchematicBlock schematicBlock = this.blockPalette.get(entry.getIntValue());

            final int x = startX + blockVector.getFloorX();
            final int y = startY + blockVector.getFloorY();
            final int z = startZ + blockVector.getFloorZ();

            if(minX != null && (x < minX || x > maxX)) continue;
            if(minZ != null && (z < minZ || z > maxZ)) continue;

            if(fullChunk.getX() == x >> 4 && fullChunk.getZ() == z >> 4) {
                final int bX = x & 15;
                final int bZ = z & 15;
                final ShapeType shapeType = shapes[(bZ << 4) | bX];
                if(allowedShapes.isDisallowed(shapeType)) continue;

                fullChunk.setBlockStateAtLayer(bX, y, bZ, 0, schematicBlock.getLayer0());
                fullChunk.setBlockStateAtLayer(bX, y, bZ, 1, schematicBlock.getLayer1());
            }
        }

        for(Map.Entry<BlockVector3, SchematicBlockEntity> entry : this.blockEntities.entrySet()) {
            final SchematicBlockEntity blockEntity = entry.getValue();
            final int x = startX + entry.getKey().getX();
            final int y = startY + entry.getKey().getY();
            final int z = startZ + entry.getKey().getZ();

            if(minX != null && (x < minX || x > maxX)) continue;
            if(minZ != null && (z < minZ || z > maxZ)) continue;

            if(fullChunk.getX() == x >> 4 && fullChunk.getZ() == z >> 4) {
                final int bX = x & 15;
                final int bZ = z & 15;
                final ShapeType shapeType = shapes[(bZ << 4) | bX];
                if(allowedShapes.isDisallowed(shapeType)) continue;

                try {
                    BlockEntity.createBlockEntity(blockEntity.getType(), fullChunk, blockEntity.getCompoundTag().
                            putString("id", blockEntity.getType()).
                            putInt("x", x).
                            putInt("y", y).
                            putInt("z", z)
                    );
                } catch(Exception e) {
                    PlotPlugin.INSTANCE.getLogger().error("Could not create block entity " + blockEntity.getType() + " in Chunk[" + fullChunk.getX() + ", " + fullChunk.getZ() + "] at " + x + ":" + y + ":" + z + "!", e);
                }
            }
        }
    }

    public synchronized void init(File file) {
        try(final FileInputStream fileInputStream = new FileInputStream(file)) {
            final byte[] bytes = new byte[fileInputStream.available()];
            final BinaryStream binaryStream = new BinaryStream(Arrays.copyOf(bytes, fileInputStream.read(bytes)));

            if(!Arrays.equals(binaryStream.get(MAGIC.length), MAGIC)) {
                binaryStream.setBuffer(Zlib.inflate(binaryStream.getBuffer()));
                binaryStream.setOffset(0);

                SchematicSerializers.get(1).deserialize(this, binaryStream);
                Server.getInstance().getScheduler().scheduleDelayedTask(null, () -> this.save(file), 1);
                return;
            }

            final int version = binaryStream.getByte();

            final int decompressedSize = binaryStream.getLInt();
            binaryStream.setBuffer(Zstd.decompress(binaryStream.get(), decompressedSize));
            binaryStream.setOffset(0);

            SchematicSerializers.get(version).deserialize(this, binaryStream);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void save(File file) {
        try(final FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            final SchematicSerializer schematicSerializer = SchematicSerializers.getLatest();
            final BinaryStream contentBinaryStream = new BinaryStream();
            final BinaryStream headerBinaryStream = new BinaryStream();

            schematicSerializer.serialize(this, contentBinaryStream);

            headerBinaryStream.put(MAGIC);
            headerBinaryStream.putByte((byte) schematicSerializer.version());

            headerBinaryStream.putLInt(contentBinaryStream.getCount());
            headerBinaryStream.put(Zstd.compress(contentBinaryStream.get()));

            fileOutputStream.write(headerBinaryStream.getBuffer());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

}
