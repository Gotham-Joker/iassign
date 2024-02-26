/*
 * MIT License
 *
 * Copyright (c) 2024 Hongtao Liu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.iassign.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Tuple<K, V> implements Comparable<Tuple<K, V>> {
    public K k;
    public V v;

    public Tuple() {

    }

    public Tuple(K key, V val) {
        this.k = key;
        this.v = val;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Tuple)) {
            return false;
        }
        Tuple<?, ?> other = (Tuple<?, ?>) obj;
        if (Objects.equals(k, other.k) && Objects.equals(v, other.v)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(k, v);
    }

    /**
     * 排序的时候跟进key值排序，前提是它们的key都必须是String
     *
     * @param other the object to be compared.
     * @return
     */
    @Override
    public int compareTo(Tuple<K, V> other) {
        if (k instanceof String && other.k instanceof String) {
            return Objects.compare((String) k, (String) other.k, String::compareTo);
        }
        return 0;
    }
}
