package com.github.iassign.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Tuple<K, V> {
    public K k;
    public V v;

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
}
