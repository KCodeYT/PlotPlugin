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

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class Utils {

    public static final UUID UUID_EVERYONE = new UUID(0L, 0L);
    public static final String STRING_EVERYONE = "*";

    public static int parseInteger(String string) {
        return parseInteger(string, 0);
    }

    public static int parseInteger(String string, int defaultValue) {
        try {
            return Integer.parseInt(string);
        } catch(Throwable throwable) {
            return defaultValue;
        }
    }

    public static Integer parseIntegerWithNull(String string) {
        try {
            return Integer.parseInt(string);
        } catch(Throwable throwable) {
            return null;
        }
    }

    public static boolean parseBoolean(String string) {
        try {
            return Boolean.parseBoolean(string) || string.equalsIgnoreCase("1") || string.equalsIgnoreCase("y") || string.equalsIgnoreCase("yes");
        } catch(Throwable throwable) {
            return false;
        }
    }

    public static <K, V> Map<K, V> createMap(List<K> keys, List<V> values) {
        if(keys.size() != values.size()) throw new IllegalArgumentException("Keys and values must be the same size");
        final Map<K, V> map = new java.util.HashMap<>();
        for(int i = 0; i < keys.size(); i++) map.put(keys.get(i), values.get(i));

        return map;
    }

}
