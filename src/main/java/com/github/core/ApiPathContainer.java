package com.github.core;

import org.springframework.http.server.PathContainer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;

public class ApiPathContainer implements PathContainer {
    private String value;
    private List<Element> elements = new ArrayList<>();

    public ApiPathContainer(String value) {
        this.value = value;
        char[] chars = value.toCharArray();
        int count = 0;
        int i = 0;
        while (i < chars.length){
            char c = value.charAt(i);
            if (c == '/') {
                if (count != 0) {
                    elements.add(new ApiPathSegment(new String(chars, i - count, count)));
                    count = 0;
                }
                elements.add(new ApiPathSeparator(new String(chars, i, 1)));
            } else {
                count++;
            }
            i++;
        }
        if (count != 0) {
            elements.add(new ApiPathSegment(new String(chars, i - count, count)));
        }
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public List<Element> elements() {
        return elements;
    }

    private class ApiPathSeparator implements Separator {

        private final String value;

        ApiPathSeparator(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    private class ApiPathSegment implements PathSegment {
        private final String value;

        ApiPathSegment(String value) {
            this.value = value;
        }

        @Override
        public String valueToMatch() {
            return value;
        }

        @Override
        public char[] valueToMatchAsChars() {
            return this.value.toCharArray();
        }

        @Override
        public MultiValueMap<String, String> parameters() {
            return new LinkedMultiValueMap<>();
        }

        @Override
        public String value() {
            return value;
        }
    }
}
