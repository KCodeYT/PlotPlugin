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

import java.util.Objects;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class Allowed<C> {

    private final Object[] allowed;

    @SafeVarargs
    public Allowed(C... allowed) {
        this.allowed = allowed;
    }

    public boolean isDisallowed(C c) {
        for(Object allowed : this.allowed)
            if(Objects.equals(allowed, c))
                return false;
        return true;
    }

}
