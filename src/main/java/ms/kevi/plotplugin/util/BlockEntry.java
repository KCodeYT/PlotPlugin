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

package ms.kevi.plotplugin.util;

import cn.nukkit.blockstate.BlockState;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
@Builder
@Value
public class BlockEntry {

    boolean isDefault;
    String name;
    BlockState blockState;
    String imageType;
    String imageData;
    String permission;

    public static BlockEntry of(Map<?, ?> map) {
        return BlockEntry.builder().
                isDefault(map.get("name").equals("reset_to_default")).
                name((String) map.get("name")).
                blockState(map.containsKey("block_id") ? BlockState.of((int) map.get("block_id"), (int) map.get("block_data")) : null).
                imageType((String) map.get("image_type")).
                imageData((String) map.get("image_data")).
                permission((String) map.get("permission")).
                build();
    }

}
