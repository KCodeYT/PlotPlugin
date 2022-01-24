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

package de.kcodeyt.plotplugin.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlotVector {

    private final int x;
    private final int z;

    public PlotVector add(int x, int z) {
        return new PlotVector(this.x + x, this.z + z);
    }

    public PlotVector subtract(int x, int z) {
        return this.add(-x, -z);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + this.x + ";" + this.z + ")";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlotVector && ((PlotVector) obj).x == this.x && ((PlotVector) obj).z == this.z;
    }

}
