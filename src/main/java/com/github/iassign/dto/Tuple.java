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
