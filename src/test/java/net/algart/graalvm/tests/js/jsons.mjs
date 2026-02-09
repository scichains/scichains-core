/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
 */

export function toJson(o) {
    if (o == null) {
        return o;
    }
    if (isCollection(o)) {
        return collectionToJson(o);
    }
    if (isMap(o)) {
        return mapToJson(o);
    }
    if (typeof o !== 'object') {
        return o;
    }
    const result = new Object();
    for (const fieldName of Object.getOwnPropertyNames(o)) {
        const e = o[fieldName];
        result[fieldName] = toJson(e);
    }
    return result;
}

export function toJsonString(o) {
    return JSON.stringify(toJson(o), null, 2);
}

function isCollection(a) {
    return a instanceof Array || a instanceof Set;
}

function collectionToJson(a) {
    const result = [];
    for (let e of a) {
        result.push(toJson(e));
    }
    return result;
}

function isMap(m) {
    return m instanceof Map;
}

function mapToJson(m) {
    const result = {};
    for (let [key, value] of m) {
        result[key] = toJson(value);
    }
    return result;
}