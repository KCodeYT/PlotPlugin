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

package de.kcodeyt.plotplugin.schematic;

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.BinaryStream;
import de.kcodeyt.plotplugin.PlotPlugin;
import de.kcodeyt.plotplugin.util.Allowed;
import de.kcodeyt.plotplugin.util.ShapeType;
import de.kcodeyt.plotplugin.util.async.TaskHelper;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@ToString
@EqualsAndHashCode
public class Schematic {

    private final Map<Vector3, SchematicBlock> blocks;
    private final Map<BlockVector3, SchematicBlockEntity> blockEntities;

    public Schematic() {
        this.blocks = new HashMap<>();
        this.blockEntities = new HashMap<>();
    }

    public boolean isEmpty() {
        return this.blocks.isEmpty() && this.blockEntities.isEmpty();
    }

    public void addBlock(Vector3 vector3, SchematicBlock block) {
        this.blocks.put(vector3, block);
    }

    public void addBlockEntity(BlockVector3 blockVector3, String type, CompoundTag compoundTag) {
        this.blockEntities.put(blockVector3, new SchematicBlockEntity(type, compoundTag));
    }

    public void buildInChunk(TaskHelper taskHelper, Vector3 vector3, FullChunk fullChunk, ShapeType[] shapes, Allowed<ShapeType> allowedShapes) {
        final int startX = vector3.getFloorX();
        final int startY = vector3.getFloorY();
        final int startZ = vector3.getFloorZ();

        taskHelper.addAsyncTask(() -> {
            for(Map.Entry<Vector3, SchematicBlock> entry : this.blocks.entrySet()) {
                final SchematicBlock schematicBlock = entry.getValue();

                final int x = startX + entry.getKey().getFloorX();
                final int y = startY + entry.getKey().getFloorY();
                final int z = startZ + entry.getKey().getFloorZ();

                if(fullChunk.getX() == x >> 4 && fullChunk.getZ() == z >> 4) {
                    final int bX = x & 15;
                    final int bZ = z & 15;
                    final ShapeType shapeType = shapes[(bZ << 4) | bX];
                    if(allowedShapes.isDisallowed(shapeType)) continue;

                    fullChunk.setBlockStateAtLayer(bX, y, bZ, 0, schematicBlock.getLayer0());
                    fullChunk.setBlockStateAtLayer(bX, y, bZ, 1, schematicBlock.getLayer1());
                }
            }
        });

        taskHelper.addAsyncTask(() -> {
            for(Map.Entry<BlockVector3, SchematicBlockEntity> entry : this.blockEntities.entrySet()) {
                final SchematicBlockEntity blockEntity = entry.getValue();
                final int x = startX + entry.getKey().getX();
                final int y = startY + entry.getKey().getY();
                final int z = startZ + entry.getKey().getZ();

                if(fullChunk.getX() == x >> 4 && fullChunk.getZ() == z >> 4) {
                    final int bX = x & 15;
                    final int bZ = z & 15;
                    final ShapeType shapeType = shapes[(bZ << 4) | bX];
                    if(allowedShapes.isDisallowed(shapeType)) continue;

                    taskHelper.addSyncTask(() -> {
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
                    });
                }
            }
        });
    }

    public synchronized void init(File file) {
        try {
            final FileInputStream fileInputStream = new FileInputStream(file);

            final byte[] bytes = new byte[fileInputStream.available()];
            final BinaryStream binaryStream = new BinaryStream(this.decompress(Arrays.copyOf(bytes, fileInputStream.read(bytes))));
            final int blocks = binaryStream.getVarInt();
            for(int i = 0; i < blocks; i++) {
                final int blockX = binaryStream.getVarInt();
                final int blockY = binaryStream.getVarInt();
                final int blockZ = binaryStream.getVarInt();

                final int layer0Id = binaryStream.getVarInt();
                final int layer0Meta = binaryStream.getVarInt();
                final int layer1Id = binaryStream.getVarInt();
                final int layer1Meta = binaryStream.getVarInt();

                this.addBlock(new Vector3(blockX, blockY, blockZ), new SchematicBlock(BlockState.of(layer0Id, layer0Meta), BlockState.of(layer1Id, layer1Meta)));
            }

            final int blockEntities = binaryStream.getVarInt();
            for(int i = 0; i < blockEntities; i++) {
                final int x = binaryStream.getVarInt();
                final int y = binaryStream.getVarInt();
                final int z = binaryStream.getVarInt();

                final String type = binaryStream.getString();

                final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(binaryStream.getByteArray());
                final NBTInputStream nbtStream = new NBTInputStream(byteArrayInputStream);
                final CompoundTag compoundTag = (CompoundTag) CompoundTag.readNamedTag(nbtStream);
                nbtStream.close();
                byteArrayInputStream.close();
                this.addBlockEntity(new BlockVector3(x, y, z), type, compoundTag);
            }

            fileInputStream.close();
        } catch(IOException | DataFormatException e) {
            e.printStackTrace();
        }
    }

    public synchronized void save(File file) {
        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(file);

            synchronized(this.blocks) {
                final BinaryStream binaryStream = new BinaryStream();
                binaryStream.putVarInt(this.blocks.size());
                for(Vector3 blockVector : this.blocks.keySet()) {
                    final SchematicBlock schematicBlock = this.blocks.get(blockVector);

                    binaryStream.putVarInt(blockVector.getFloorX());
                    binaryStream.putVarInt(blockVector.getFloorY());
                    binaryStream.putVarInt(blockVector.getFloorZ());

                    binaryStream.putVarInt(schematicBlock.getLayer0().getBlockId());
                    binaryStream.putVarInt(schematicBlock.getLayer0().getHugeDamage().intValue());
                    binaryStream.putVarInt(schematicBlock.getLayer1().getBlockId());
                    binaryStream.putVarInt(schematicBlock.getLayer1().getHugeDamage().intValue());
                }

                binaryStream.putVarInt(this.blockEntities.size());
                for(BlockVector3 blockVector : this.blockEntities.keySet()) {
                    final SchematicBlockEntity blockEntity = this.blockEntities.get(blockVector);
                    binaryStream.putVarInt(blockVector.getX());
                    binaryStream.putVarInt(blockVector.getY());
                    binaryStream.putVarInt(blockVector.getZ());

                    binaryStream.putString(blockEntity.getType());

                    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    final NBTOutputStream nbtStream = new NBTOutputStream(byteArrayOutputStream);
                    CompoundTag.writeNamedTag(blockEntity.getCompoundTag(), nbtStream);
                    byteArrayOutputStream.flush();
                    binaryStream.putByteArray(byteArrayOutputStream.toByteArray());
                    byteArrayOutputStream.close();
                }

                fileOutputStream.write(this.compress(binaryStream.getBuffer()));
            }

            fileOutputStream.flush();
            fileOutputStream.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] compress(byte[] data) throws IOException {
        final Deflater deflater = new Deflater();
        deflater.setInput(data);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        deflater.finish();
        final byte[] buffer = new byte[1024];
        while(!deflater.finished())
            outputStream.write(buffer, 0, deflater.deflate(buffer));
        outputStream.close();
        return outputStream.toByteArray();
    }

    private byte[] decompress(byte[] data) throws IOException, DataFormatException {
        final Inflater inflater = new Inflater();
        inflater.setInput(data);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        final byte[] buffer = new byte[1024];
        while(!inflater.finished())
            outputStream.write(buffer, 0, inflater.inflate(buffer));
        outputStream.close();
        return outputStream.toByteArray();
    }

}
