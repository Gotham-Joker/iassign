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
