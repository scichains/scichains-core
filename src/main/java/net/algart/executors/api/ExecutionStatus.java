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

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import net.algart.json.Jsons;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ExecutionStatus {
    public enum DataKind {
        FULL(1, status -> status.joinAll(false)),
        INFORMATION(2, status -> status.joinAll(true)),
        MESSAGES(3, ExecutionStatus::joinMessages),
        COMMENTS(4, ExecutionStatus::joinComments),
        JSON(-1, ExecutionStatus::toJsonString),
        CUSTOM_JSON(-2, ExecutionStatus::getCustomJsonString);

        private static final Map<Integer, DataKind> CODE_TO_KIND =
                Arrays.stream(values()).collect(Collectors.toMap(DataKind::code, kind -> kind));
        private static final Map<String, DataKind> NAME_TO_KIND =
                Arrays.stream(values()).collect(Collectors.toMap(DataKind::name, kind -> kind));

        private final int code;
        private final Function<ExecutionStatus, String> getter;

        DataKind(int code, Function<ExecutionStatus, String> getter) {
            this.code = code;
            this.getter = getter;
        }

        public String data(ExecutionStatus status) {
            Objects.requireNonNull(status, "Null status");
            return getter.apply(status);
        }

        public int code() {
            return code;
        }

        public static DataKind ofCode(int code) {
            return fromCode(code).orElseThrow(() -> new IllegalArgumentException("Unknown status data code: " + code));
        }

        /**
         * Returns an {@link Optional} containing the {@link DataKind} with the given {@link #name()}.
         * <p>If no data kind with the specified name exists or if the argument is {@code null},
         * an empty optional is returned.
         *
         * @param name the enum name; may be {@code null}.
         * @return an optional data kind.
         */
        public static Optional<DataKind> from(String name) {
            return Optional.ofNullable(NAME_TO_KIND.get(name));
        }

        public static Optional<DataKind> fromCode(int code) {
            return Optional.ofNullable(CODE_TO_KIND.get(code));
        }
    }

    private static final int MAX_POSSIBLE_NUMBER_OF_PARENTS = 10000;
    private static final int MAX_SHOWN_NUMBER_OF_PARENTS = 256;

    private static final String LOGGING_STATUS_LEVEL = net.algart.arrays.Arrays.SystemSettings.getStringProperty(
            "net.algart.executors.api.loggingStatusLevel", null);
    private static final DataKind LOGGING_STATUS_KIND = DataKind.from(
            net.algart.arrays.Arrays.SystemSettings.getStringProperty(
                    "net.algart.executors.api.loggingStatusKind", null)).orElse(null);
    private static final long MIN_TIME_BETWEEN_LOGGING_IN_NANOSECONDS = 500_000_000;

    private final Supplier<String> ownerName;

    private ExecutionStatus parent = null;
    private ExecutionStatus root = null;
    private ExecutionStatus child = null;
    private volatile boolean opened = false;

    private Supplier<String> message = null;
    private Supplier<String> comment = null;
    private String executorFullClassName = null;
    private String executorSimpleClassName = null;
    private String executorClassId = null;
    private String executorInstanceId = null;
    private JsonObject custom = null;
    private Long startProcessingTimeStamp = null;
    private boolean classInformationIncluded = false;

    private ExecutionStatus(Supplier<String> ownerName) {
        this.ownerName = ownerName;
    }

    public static ExecutionStatus newInstance() {
        return newNamedInstance((Supplier<String>) null);
    }

    public static ExecutionStatus newNamedInstance(String ownerName) {
        return newNamedInstance(ownerName == null ? null : () -> ownerName);
    }

    public static ExecutionStatus newNamedInstance(Supplier<String> ownerName) {
        return LOGGING_STATUS_LEVEL == null ? new ExecutionStatus(ownerName) : new LoggingExecutionStatus(ownerName);
    }

    public String ownerName() {
        return ownerName == null ? null : ownerName.get();
    }

    public boolean isOpened() {
        return opened;
    }

    public ExecutionStatus parent() {
        return parent;
    }

    /**
     * Returns root status in the stack of statuses. Will be <code>null</code> for closed status
     * and non-<code>null</code> for opened one.
     *
     * @return root status.
     */
    public ExecutionStatus root() {
        assert !opened || root != null;
        return root;
    }

    public void open(ExecutionStatus parentStatus) {
        if (parentStatus == this) {
            throw new IllegalArgumentException("Parent status cannot be identical to this one");
        }
        parent = parentStatus;
        if (parent != null) {
            parent.child = this;
        }
        root = findRoot();
        assert root != null;
        clearInformation();
        opened = true;
        onOpen();
    }

    public void close() {
        onClose();
        // - note: must be called before destroying fields
        opened = false;
        if (parent != null) {
            parent.child = null;
        }
        parent = null;
        root = null;
        child = null;
    }

    public boolean isEmpty() {
        return !isNonEmpty();
    }

    public boolean isNonEmpty() {
        return hasMessage() || hasComment() || (classInformationIncluded && executorSimpleClassName != null);
    }

    public void clear() {
        clearInformation();
        onUpdate(true, true);
    }

    public boolean hasMessage() {
        return message != null;
    }

    public String message() {
        return message == null ? null : message.get();
    }

    public Supplier<String> getMessage() {
        return message;
    }

    /**
     * Sets the current message to the specified parameter.
     * Please <b>remember</b>: making a message for this function usually requires some time!
     * Usually it is much better to call {@link #setMessage(Supplier)} method.
     *
     * @param message new message.
     */
    public ExecutionStatus setMessageString(String message) {
        return setMessage(() -> message);
    }

    public ExecutionStatus setMessage(Supplier<String> message) {
        if (!opened) {
            return this;
        }
        this.message = message;
        onUpdate(message != null, false);
        return this;
    }

    public boolean hasComment() {
        return comment != null;
    }

    public String comment() {
        return comment == null ? null : comment.get();
    }

    public Supplier<String> getComment() {
        return comment;
    }

    public ExecutionStatus setComment(Supplier<String> comment) {
        if (!opened) {
            return this;
        }
        this.comment = comment;
        onUpdate(false, comment != null);
        return this;
    }

    public String getExecutorFullClassName() {
        return executorFullClassName;
    }

    public ExecutionStatus setExecutorFullClassName(String executorFullClassName) {
        this.executorFullClassName = executorFullClassName;
        return this;
    }

    public String getExecutorSimpleClassName() {
        return executorSimpleClassName;
    }

    public ExecutionStatus setExecutorSimpleClassName(String executorSimpleClassName) {
        this.executorSimpleClassName = executorSimpleClassName;
        return this;
    }

    public ExecutionStatus setExecutorClass(Class<?> executorClass) {
        setExecutorSimpleClassName(executorClass.getSimpleName());
        setExecutorFullClassName(executorClass.getName());
        return this;
    }


    public String getExecutorClassId() {
        return executorClassId;
    }

    public ExecutionStatus setExecutorClassId(String executorClassId) {
        this.executorClassId = executorClassId;
        return this;
    }

    public String getExecutorInstanceId() {
        return executorInstanceId;
    }

    public ExecutionStatus setExecutorInstanceId(String executorInstanceId) {
        this.executorInstanceId = executorInstanceId;
        return this;
    }

    public JsonObject getCustom() {
        return custom;
    }

    public String getCustomJsonString() {
        return custom == null ? null : Jsons.toPrettyString(custom);
    }

    public ExecutionStatus setCustom(JsonObject custom) {
        this.custom = custom;
        return this;
    }

    public Long getStartProcessingTimeStamp() {
        return startProcessingTimeStamp;
    }

    public ExecutionStatus setStartProcessingTimeStamp(Long startProcessingTimeStamp) {
        this.startProcessingTimeStamp = startProcessingTimeStamp;
        return this;
    }

    public ExecutionStatus setStartProcessingTimeStamp() {
        this.startProcessingTimeStamp = System.nanoTime();
        return this;
    }

    public boolean isClassInformationIncluded() {
        return classInformationIncluded;
    }

    public ExecutionStatus setClassInformationIncluded(boolean classInformationIncluded) {
        this.classInformationIncluded = classInformationIncluded;
        return this;
    }

    public List<ExecutionStatus> stack() {
        return stack(Integer.MAX_VALUE);
    }

    public List<ExecutionStatus> stack(int maxLength) {
        final List<ExecutionStatus> stack = new ArrayList<>();
        int counter = 0;
        for (ExecutionStatus status = this; status != null; status = status.child) {
            counter++;
            if (counter > MAX_POSSIBLE_NUMBER_OF_PARENTS) {
                throw new IllegalStateException("Too large child nesting level of status hierarchy, " +
                        "probably infinite loop (starting from the parent \""
                        + toSingleLevelString(false) + "\")");
            }
            if (counter <= maxLength) {
                stack.add(status);
            }
        }
        return stack;
    }

    public String toSingleLevelString(boolean onlyMainInformation) {
        final String message = emptyToNull(message());
        final String comment = emptyToNull(comment());
        final StringBuilder sb = new StringBuilder();
        if (classInformationIncluded && executorSimpleClassName != null) {
            sb.append(executorSimpleClassName);
            if (!executorSimpleClassName.isEmpty()) {
                sb.append(' ');
            }
            if (startProcessingTimeStamp != null) {
                final long elapsedTime = System.nanoTime() - startProcessingTimeStamp;
                sb.append(elapsedTime < 100_000_000 ?
                        String.format(Locale.US, "(%.2f ms)", elapsedTime * 1e-6) :
                        String.format(Locale.US, "(%.2f sec)", elapsedTime * 1e-9));
            }
        }
        if (message != null) {
            if (sb.length() > 0) {
                sb.append(": ");
            }
            sb.append(message);
        }
        if (!onlyMainInformation) {
            if (comment != null) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("[").append(comment).append("]");
            }
        }
        return sb.toString();
    }

    public String joinMessages() {
        return joinStack(stackPlusOne(), ExecutionStatus::hasMessage, status -> nullToEmpty(status.message()));
    }

    public String joinComments() {
        return joinStack(stackPlusOne(), ExecutionStatus::hasComment, status -> nullToEmpty(status.comment()));
    }

    public String joinAll(boolean onlyMainInformation) {
        return joinStack(stackPlusOne(),
                ExecutionStatus::isNonEmpty,
                status -> status.toSingleLevelString(onlyMainInformation));
    }

    public JsonObject toSingleLevelJson() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final String owner = ownerName();
        if (owner != null) {
            builder.add("owner", owner);
        }
        final String message = message();
        if (message != null) {
            builder.add("message", message);
        }
        final String comment = comment();
        if (comment != null) {
            builder.add("comment", comment);
        }
        if (executorFullClassName != null) {
            builder.add("executorFullClassName", executorFullClassName);
        }
        if (executorSimpleClassName != null) {
            builder.add("executorSimpleClassName", executorSimpleClassName);
        }
        if (executorClassId != null) {
            builder.add("executorClassId", executorClassId);
        }
        if (executorInstanceId != null) {
            builder.add("executorInstanceId", executorInstanceId);
        }
        if (startProcessingTimeStamp != null) {
            builder.add("startProcessingTimeStamp", startProcessingTimeStamp);
            builder.add("processingTimeInSeconds", (System.nanoTime() - startProcessingTimeStamp) * 1e-9);
        }
        if (custom != null) {
            builder.add("custom", custom);
        }
        return builder.build();
    }

    public JsonObject toJson() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (ExecutionStatus status : stack()) {
            arrayBuilder.add(status.toSingleLevelJson());
        }
        builder.add("stack", arrayBuilder.build());
        return builder.build();
    }

    public String toJsonString() {
        return Jsons.toPrettyString(toJson());
    }

    @Override
    public String toString() {
        if (!opened) {
            return "<closed status>";
        }
        // - important to avoid possible infinite loop while calling in invalid state,
        // for example, while throwing exceptions
        return joinAll(false);
    }

    void onOpen() {
    }

    void onClose() {
    }

    void onUpdate(boolean message, boolean comment) {
    }

    private void clearInformation() {
        message = null;
        comment = null;
    }

    private List<ExecutionStatus> stackPlusOne() {
        return stack(MAX_SHOWN_NUMBER_OF_PARENTS + 1);
    }

    private ExecutionStatus findRoot() {
        int counter = 0;
        ExecutionStatus status = this;
        for (; status.parent != null; status = status.parent) {
            counter++;
            if (counter > MAX_POSSIBLE_NUMBER_OF_PARENTS) {
                throw new IllegalStateException("Too large parent nesting level of status hierarchy, " +
                        "probably infinite loop (starting from the child \""
                        + toSingleLevelString(false) + "\")");
            }
        }
        // assert status != null;
        return status;
    }

    private String joinStack(
            List<ExecutionStatus> stack,
            Predicate<ExecutionStatus> included,
            Function<ExecutionStatus, String> statusToString) {
        if (!opened) {
            return "";
            // - no sense to show anything in closed status
        }
        final String result = stack.stream().limit(MAX_SHOWN_NUMBER_OF_PARENTS)
                .filter(included).map(statusToString).collect(Collectors.joining(" / "));
        return stack.size() > MAX_SHOWN_NUMBER_OF_PARENTS ? result + "..." : result;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        return s != null && s.isEmpty() ? null : s;
    }

    private static class LoggingExecutionStatus extends ExecutionStatus {
        private volatile boolean needToFinishStage = false;

        private LoggingExecutionStatus(Supplier<String> ownerName) {
            super(ownerName);
        }

        @Override
        void onOpen() {
            needToFinishStage = false;
        }

        @Override
        void onClose() {
            if (needToFinishStage) {
                DelayingLogger.INSTANCE.log(this::makeStatus, true);
                needToFinishStage = false;
            }
        }

        @Override
        void onUpdate(boolean message, boolean comment) {
            DelayingLogger.INSTANCE.log(this::makeStatus, false);
            if (message) {
                // - if the user tried to show something, it is important and must be shown on closong
                needToFinishStage = true;
            }
        }

        private String makeStatus() {
            final ExecutionStatus root = root();
            assert root != null;
            return LOGGING_STATUS_KIND == null ? root.toString() : LOGGING_STATUS_KIND.data(root);
        }
    }

    private static class DelayingLogger {
        static final DelayingLogger INSTANCE = new DelayingLogger(LogLevel.of(LOGGING_STATUS_LEVEL));

        private final LogLevel level;
        private long lastTime = Long.MAX_VALUE;
        // - note: it is actual only while the very beginning usage of this class in the whole JVM
        private String lastString = null;

        private DelayingLogger(LogLevel level) {
            this.level = Objects.requireNonNull(level);
        }

        public synchronized boolean log(Supplier<String> supplier, boolean forceFinishStage) {
            long last = lastTime;
            final long t = System.nanoTime();
            boolean result = false;
            if (forceFinishStage || last == Long.MAX_VALUE || t - last > MIN_TIME_BETWEEN_LOGGING_IN_NANOSECONDS) {
                final String s = supplier.get();
                if (s != null && !s.isEmpty()) {
                    level.removeMessage(lastString);
                    level.log(s);
                    result = true;
                }
                lastString = s;
                lastTime = t;
                if (forceFinishStage) {
                    level.finishStage();
                }
            }
            return result;
        }
    }

//    public static void main(String[] args) {
//        System.out.println(DataKind.valueOfOrNull("FULL"));
//        System.out.println(DataKind.valueOfOrNull(null));
//    }
}
