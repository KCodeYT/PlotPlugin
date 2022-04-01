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

import cn.nukkit.level.Level;
import lombok.experimental.UtilityClass;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
@UtilityClass
public class LevelUtils {

    /**
     * Returns the lowest y-coordinate blocks can be placed on, in the given dimension
     *
     * @param dimension The dimension which lowest y-coordinate needs to be returned
     * @return The lowest y-coordinate in the given dimension
     */
    public int getChunkMinY(int dimension) {
        return switch(dimension) {
            case Level.DIMENSION_OVERWORLD -> -64;
            case Level.DIMENSION_NETHER, Level.DIMENSION_THE_END -> 0;
            default -> throw new UnsupportedOperationException("Could not resolve dimension by id '" + dimension + "'");
        };
    }

    /**
     * Returns the highest y-coordinate blocks can be placed on, in the given dimension
     *
     * @param dimension The dimension which highest y-coordinate needs to be returned
     * @return The highest y-coordinate in the given dimension
     */
    public int getChunkMaxY(int dimension) {
        return switch(dimension) {
            case Level.DIMENSION_OVERWORLD -> 319;
            case Level.DIMENSION_NETHER -> 127;
            case Level.DIMENSION_THE_END -> 255;
            default -> throw new UnsupportedOperationException("Could not resolve dimension by id '" + dimension + "'");
        };
    }

}
