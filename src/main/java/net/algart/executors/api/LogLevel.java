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

package net.algart.executors.api;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public enum LogLevel {
    OFF(Level.OFF),
    ERROR(Level.ERROR),
    WARNING(Level.WARNING),
    INFO(Level.INFO),
    DEBUG(Level.DEBUG),
    CONFIG(Level.DEBUG, "CONFIG"),
    // - for compatibility with java.util.logging.Level
    TRACE(Level.TRACE),
    PRINTLN_TO_CONSOLE(null, "println") {
        @Override
        public void log(String message) {
            System.out.println(message);
        }

        @Override
        public void finishStage() {
            System.out.println();
        }
    },
    PRINT_TO_CONSOLE(null, "print") {
        @Override
        public void log(String message) {
            if (message.length() > MAX_PRINT_TO_CONSOLE_LENGTH) {
                message = message.substring(0, MAX_PRINT_TO_CONSOLE_LENGTH - 3) + "...";
            }
            System.out.print("        \r" + message + "\r");
            // - last \r helps to overwrite useless message after the end of using this function
        }

        @Override
        public void removeMessage(String previousMessage) {
            if (previousMessage != null && !previousMessage.isEmpty()) {
                System.out.print("\r"
                        + repeat(' ', Math.min(MAX_PRINT_TO_CONSOLE_LENGTH, previousMessage.length())));
            }
        }

        @Override
        public void finishStage() {
            System.out.println();
        }
    };

    private static final int MAX_PRINT_TO_CONSOLE_LENGTH = 190;
    // - typical width of usual console on many monitors is >= 190 characters (190*8 = 1520 pixels)
    private static final Map<String, LogLevel> ALL_TYPES =
            Arrays.stream(values()).collect(Collectors.toMap(LogLevel::levelName, level -> level));

    private final Level level;
    private final String name;

    LogLevel(Level level) {
        this(level, level.getName());
    }

    LogLevel(Level level, String name) {
        this.level = level;
        this.name = name;
    }

    public Level level() {
        return level;
    }

    public String levelName() {
        return name;
    }

    public boolean isLoggable() {
        return isLoggable(Executor.LOG);
    }

    public boolean isLoggable(Logger logger) {
        Objects.requireNonNull(logger, "Null logger");
        return level == null || logger.isLoggable(level);
    }

    public void log(String message) {
        log(Executor.LOG, message);
    }

    public void log(Logger logger, String message) {
        Objects.requireNonNull(logger, "Null logger");
        if (level == null) {
            throw new AssertionError("Invalid usage");
        }
        if (message != null) {
            logger.log(level, message);
        }
    }

    public void removeMessage(String previousMessage) {
    }

    public void finishStage() {
    }

    public static LogLevel of(String levelName) {
        final LogLevel result = ALL_TYPES.get(levelName);
        if (result == null) {
            throw new IllegalArgumentException("Unknown logging level name: " + levelName);
        }
        return result;
    }

    private static String repeat(char c, int count) {
        final char[] result = new char[count];
        Arrays.fill(result, c);
        return String.valueOf(result);
    }

    public static void main(String[] args) {
        System.out.println(of("print"));
        System.out.println(of("println"));
        System.out.println(of("OFF"));
    }
}
