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

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * @author Kevims KCodeYT
 */
public class PaginationList<T> extends AbstractList<List<T>> {

    private final List<T> list;
    private final int pageSize;

    public PaginationList(List<T> list, int pageSize) {
        this.list = Collections.unmodifiableList(list);
        this.pageSize = pageSize;
    }

    @Override
    public List<T> get(int index) {
        final int start = index * this.pageSize;
        final int end = Math.min(start + this.pageSize, this.list.size());

        if(start > end)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.size());

        return this.list.subList(start, end);
    }

    @Override
    public int size() {
        return (int) Math.ceil((double) this.list.size() / (double) this.pageSize);
    }

}
