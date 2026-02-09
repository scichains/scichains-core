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

package net.algart.json;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EscapingJsonStringTest {
    // Clone of the method JsonGeneratorImpl.writeEscapedString
    private static void writeEscapedString(StringBuilder sb, CharSequence string) {
        int len = string.length();
        for (int i = 0; i < len; i++) {
            int begin = i;
            int end = i;
            char c = string.charAt(i);
            // find all the characters that need not be escaped
            // unescaped = %x20-21 | %x23-5B | %x5D-10FFFF
            while (c >= 0x20 && c != 0x22 && c != 0x5c) {
                i++;
                end = i;
                if (i < len) {
                    c = string.charAt(i);
                } else {
                    break;
                }
            }
            // Write characters without escaping
            if (begin < end) {
                sb.append(string, begin, end);
                if (i == len) {
                    break;
                }
            }

            switch (c) {
                case '"':
                case '\\':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '\b':
                    sb.append('\\');
                    sb.append('b');
                    break;
                case '\f':
                    sb.append('\\');
                    sb.append('f');
                    break;
                case '\n':
                    sb.append('\\');
                    sb.append('n');
                    break;
                case '\r':
                    sb.append('\\');
                    sb.append('r');
                    break;
                case '\t':
                    sb.append('\\');
                    sb.append('t');
                    break;
                default:
                    String hex = "000" + Integer.toHexString(c);
                    sb.append("\\u").append(hex.substring(hex.length() - 4));
            }
        }
    }


    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.printf("Usage: %s some_file_with_large_string%n", EscapingJsonStringTest.class.getName());
            return;
        }

        Path file = Paths.get(args[0]);
        byte[] bytes = Files.readAllBytes(file);
        String s = new String(bytes, StandardCharsets.UTF_8);
        System.out.println(s);
        System.out.println();
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("value", s);
        JsonObject json = builder.build();
        String escapedJson = Jsons.toPrettyString(json);
        System.out.println(escapedJson);
        System.out.println();
        StringBuilder sb = new StringBuilder();
        writeEscapedString(sb, s);
        String escaped = "\"" + sb + "\"";
        System.out.println(sb);
        System.out.println();
        if (!escapedJson.contains(escaped)) {
            throw new AssertionError("Invalid escaping");
        }
    }
}
