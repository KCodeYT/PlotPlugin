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

package ms.kevi.plotplugin.schematic.format;

import cn.nukkit.blockstate.BlockState;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.BinaryStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ms.kevi.plotplugin.schematic.Schematic;
import ms.kevi.plotplugin.schematic.SchematicBlock;

/**
 * @author Kevims KCodeYT
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SchematicSerializerV1 implements SchematicSerializer {

    public static final SchematicSerializer INSTANCE = new SchematicSerializerV1();

    @Override
    public void serialize(Schematic schematic, BinaryStream binaryStream) {
        throw new UnsupportedOperationException("V1 schematic serialization is not supported");
    }

    @Override
    public void deserialize(Schematic schematic, BinaryStream binaryStream) {
        final int blocks = binaryStream.getVarInt();
        for(int i = 0; i < blocks; i++) {
            final int blockX = binaryStream.getVarInt();
            final int blockY = binaryStream.getVarInt();
            final int blockZ = binaryStream.getVarInt();

            final int layer0Id = binaryStream.getVarInt();
            final int layer0Meta = binaryStream.getVarInt();
            final int layer1Id = binaryStream.getVarInt();
            final int layer1Meta = binaryStream.getVarInt();

            schematic.addBlock(new Vector3(blockX, blockY, blockZ), new SchematicBlock(BlockState.of(layer0Id, layer0Meta), BlockState.of(layer1Id, layer1Meta)));
        }

        final int blockEntities = binaryStream.getVarInt();
        for(int i = 0; i < blockEntities; i++) {
            final int x = binaryStream.getVarInt();
            final int y = binaryStream.getVarInt();
            final int z = binaryStream.getVarInt();

            final String type = binaryStream.getString();
            final CompoundTag compoundTag = binaryStream.getTag();

            schematic.addBlockEntity(new BlockVector3(x, y, z), type, compoundTag);
        }
    }

    @Override
    public int version() {
        return 1;
    }

}
