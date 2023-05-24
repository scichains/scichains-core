/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.build;

import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import net.algart.json.Jsons;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Port;
import net.algart.executors.api.SystemEnvironment;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.Executor;
import net.algart.executors.api.model.ChainJson;
import net.algart.executors.api.model.ExecutorJson;
import net.algart.executors.api.model.ExtensionJson;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombinerJson;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class ExecutorJsonVerifier {
    private static final List<String> POSSIBLE_BLOCK_KINDS = Collections.unmodifiableList(Arrays.asList(
            "function", "input", "output", "data"));

    private final Set<String> ids = new HashSet<>();
    private final Set<String> instantiationNames = new HashSet<>();

    private boolean checkClasses = false;
    private boolean thoroughWarnings = false;

    static JsonObject readExecutorJson(Path f, boolean ignoreOtherApps) throws IOException {
        if (f.getFileName().toString().startsWith(".")) {
            // Skip special files
            return null;
        }
        final JsonObject json;
        try {
            json = Jsons.readJson(f);
        } catch (Exception e) {
            throw new IOException("Exception while parsing " + f, e);
        }
        final String app = json.getString("app", null);
        if (app == null) {
            // It is not a json for execution block
            return null;
        }
        if (!app.equals(ExecutorJson.APP_NAME)) {
            if (ignoreOtherApps) {
                return null;
            }
            if (app.equals(SettingsCombinerJson.APP_NAME)
                    || app.equals(SettingsCombinerJson.APP_NAME_FOR_MAIN)
                    || app.equals(ExtensionJson.APP_NAME)
                    || ChainJson.isChainJsonContainer(json)) {
                // - not an error, just another known model type
                return null;
            }
            throw new JsonException("Invalid app " + app + " in " + f);
        }
        return json;
    }

    void verify(Path f) throws IOException {
        final JsonObject json = readExecutorJson(f, false);
        if (json == null) {
            return;
        }

//        if (json.getString("caption", null) != null) {
//            throw new JsonException("OBSOLETE in " + f);
//        }
        String id = json.getString("id", null);
        if (id == null) {
            id = json.getString("uuid", null);
            // - for compatibility
        }
        if (id == null) {
            throw new JsonException("ID is not specified in " + f);
        }
        if (!id.equals(id.toLowerCase())) {
            throw new JsonException("ID " + id + " is not in lower case in " + f);
        }
        if (!ids.add(id)) {
            throw new JsonException("Duplicate of ID " + id + " in " + f);
        }
        final String kind = json.getString("kind", POSSIBLE_BLOCK_KINDS.get(0));
        if (!POSSIBLE_BLOCK_KINDS.contains(kind)) {
            throw new JsonException("Unsupported execution block kind \"" + kind + "\" in " + f);
        }
        final boolean javaExecutor = json.getString("language", "").equals("java");
        final boolean checkClasses = this.checkClasses && javaExecutor;
        final String instantiationName;
        final ExecutionBlock executionBlock;
        if (checkClasses) {
            final JsonObject javaSection = json.getJsonObject(ExecutorJson.JavaConf.JAVA_CONF_NAME);
            final String className = javaSection.getString(ExecutorJson.JavaConf.CLASS_PROPERTY_NAME);
            final String newInstanceMethodName = javaSection.getString(
                    ExecutorJson.JavaConf.NEW_INSTANCE_METHOD_PROPERTY_NAME, null);
            instantiationName = newInstanceMethodName != null ?
                    className + "." + newInstanceMethodName + "()" :
                    className;
            final Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new JsonException("Java execution block class " + className + " not found in " + f, e);
            }
            if (newInstanceMethodName != null) {
                final Method newInstanceMethod;
                try {
                    newInstanceMethod = clazz.getMethod(newInstanceMethodName);
                } catch (NoSuchMethodException e) {
                    throw new JsonException("Instantiation method "
                            + newInstanceMethodName + " not found in class " + clazz + " in ", e);
                }
                try {
                    executionBlock = (ExecutionBlock) newInstanceMethod.invoke(null);
                } catch (Exception e) {
                    throw new JsonException("Executor " + clazz.getName() + " cannot be created by "
                            + newInstanceMethodName + " in " + f, e);
                }
            } else {
                try {
                    executionBlock = (ExecutionBlock) clazz.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassCastException |
                         InvocationTargetException | NoSuchMethodException e) {
                    throw new JsonException("Executor " + clazz.getName() + " cannot be created in " + f, e);
                }
            }
        } else {
            executionBlock = null;
            instantiationName = null;
        }
        if (instantiationName != null && !instantiationNames.add(instantiationName)) {
            if (thoroughWarnings) {
                // - usually not a problem: many executors have aliases
                System.out.printf("Duplicate Java class / instantiation method %s in %s%n", instantiationName, f);
            }
        }
        final Map<String, DataType> inPorts = new LinkedHashMap<>();
        final Map<String, DataType> outPorts = new LinkedHashMap<>();
        try {
            readPorts(inPorts, json, "in_ports", f);
            if (json.containsKey("in_ports_hidden")) {
                readPorts(inPorts, json, "in_ports_hidden", f);
            }
            readPorts(outPorts, json, "out_ports", f);
            if (json.containsKey("out_ports_hidden")) {
                readPorts(outPorts, json, "out_ports_hidden", f);
            }
        } catch (RuntimeException e) {
            throw new JsonException("Error in ports specification in " + f, e);
        }
        if (checkClasses) {
            assert executionBlock != null;
            for (Port port : executionBlock.allInputPorts()) {
                final DataType dataType = inPorts.get(port.getName());
                if (dataType == null) {
                    throw new JsonException("Built-in input " + port + " is not specified in " + f);
                }
                if (dataType != port.getDataType()) {
                    throw new JsonException("Type of built-in input " + port + " is incorrectly specified in "
                            + f + " (" + dataType + ")");
                }
            }
            for (Port port : executionBlock.allOutputPorts()) {
                final DataType dataType = outPorts.get(port.getName());
                if (dataType == null) {
                    throw new JsonException("Built-in output " + port + " is not specified in " + f);
                }
                if (dataType != port.getDataType()) {
                    throw new JsonException("Type of built-in output " + port + " is incorrectly specified in "
                            + f + " (" + dataType + ")");
                }
            }
        }
        final JsonArray controls = json.getJsonArray("controls");
        if (controls == null) {
            throw new JsonException("No controls in " + f);
        }
        final Set<String> controlNames = new HashSet<>();
        for (JsonValue jsonValue : controls) {
            if (!(jsonValue instanceof final JsonObject control)) {
                throw new JsonException("One of controls is not Json object in " + f + " (" + jsonValue + ")");
            }
            final String name = control.getString("name", null);
            if (name == null) {
                throw new JsonException("One of controls has no \"name\" "
                        + "or has non-string  \"name\" in " + f + " (" + control + ")");
            }
            if (!controlNames.add(name)) {
                throw new JsonException("Duplicate control with name " + name + " in " + f);
            }
            final String valueType = control.getString("value_type", null);
            if (valueType == null) {
                throw new JsonException("One of controls has no \"value_type\" "
                        + "or has non-string  \"value_type\" in " + f + " (" + control + ")");
            }
            final String editionType = control.getString("edition_type", null);
            if (editionType == null) {
                throw new JsonException("One of controls has no \"edition_type\" "
                        + "or has non-string  \"edition_type\" in " + f + " (" + control + ")");
            }
            if (editionType.equals("enum")) {
                final JsonArray items = control.getJsonArray("items");
                if (items == null) {
                    throw new JsonException("Enum control has no \"items\" in " + f + " (" + control + ")");
                }
                for (JsonValue item : items) {
                    if (!(item instanceof JsonObject)) {
                        throw new JsonException("One of items is not Json object in " + f + " (" + item + ")");
                    }
                }
                if (valueType.equals("String")) {
                    final Set<String> values = new HashSet<>();
                    for (JsonValue item : items) {
                        final String value = ((JsonObject) item).getString("value", null);
                        if (value == null) {
                            throw new JsonException("One of items has no \"value\" or has non-string \"value\" in "
                                    + f + " (" + item + ")");
                        }
                        if (!values.add(value)) {
                            throw new JsonException("Several items have identical \"value\":\""
                                    + value + "\" in " + f);
                        }
                    }
                    final String defaultValue = control.getString("default", null);
                    if (defaultValue == null) {
                        throw new JsonException("Enum string control has no \"default\" "
                                + "or has non-string  \"default\" in " + f + " (" + control + ")");
                    }
                    if (!values.contains(defaultValue)) {
                        throw new JsonException("Enum string control has unknown \"default\":\"" + defaultValue
                                + "\" (not listed among items) " + f + " (" + control + ")");
                    }
                    if (checkClasses) {
                        if (executionBlock instanceof Executor) {
                            final Class<?> type = ((Executor) executionBlock).parameterJavaType(name);
                            if (type == null) {
                                if (!Executor.STANDARD_VISIBLE_RESULT_PARAMETER_NAME.equals(name)) {
                                    // - visible result property usually has no setter, it is normal
                                    System.out.printf("There is no automatic property setter " +
                                            "for enum string control %s in %s%n", name, f);
                                }
                            } else if (type.isEnum()) {
                                // Json enums may be used for elementType, visibleResult etc. - not enums in Java
                                assert Enum.class.isAssignableFrom(type);
                                final Set<String> enumNames = new HashSet<>();
                                for (Enum<?> e : type.asSubclass(Enum.class).getEnumConstants()) {
                                    if (thoroughWarnings && !(values.contains(e.name()))) {
                                        // - only in thorough mode:
                                        // many executors provide not all options, provided by enum class
                                        System.out.println("Enum string control has no item with value \""
                                                + e.name() + "\" (one of possible values of "
                                                + type.getSimpleName() + ") in " + f);
                                    }
                                    enumNames.add(e.name());
                                }
                                for (String value : values) {
                                    if (!enumNames.contains(value)) {
                                        throw new JsonException("Enum string control has item with "
                                                + "unknown value \"" + value + "\" (not one of possible values of "
                                                + type + ") in " + f + " (" + control + ")");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        try {
            ExecutorJson.valueOf(json);
            // - check for all other problems, possible while usage in sub-chains
        } catch (Exception e) {
            throw new JsonException("Some problem detected while parsing " + f, e);
        }

        if (checkClasses) {
            executionBlock.close();
        }
    }

    private void verifyAll(Path folder) throws IOException {
        try (final DirectoryStream<Path> files = Files.newDirectoryStream(folder)) {
            for (Path file : files) {
                if (Files.isDirectory(file)) {
                    verifyAll(file);
                } else if (file.getFileName().toString().endsWith(".json")) {
                    verify(file);
                }
            }
        }
    }

    private static void readPorts(Map<String, DataType> result, JsonObject conf, String portArrayName, Path f) {
        final JsonArray ports = conf.getJsonArray(portArrayName);
        if (ports == null) {
            throw new JsonException("No \"" + portArrayName + "\" section in " + f);
        }
        for (JsonValue value : ports) {
            if (!(value instanceof JsonObject)) {
                throw new JsonException("One of ports is not Json object: " + value);
            }
            final JsonObject port = (JsonObject) value;
            final String name = port.getString("name");
            final DataType dataType = DataType.valueOfTypeName(port.getString("value_type"));
            if (result.put(name, dataType) != null) {
                throw new JsonException("Duplicate port with name " + name + " in " + f);
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ExecutorJsonVerifier verifier = new ExecutorJsonVerifier();
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equals("-check_classes")) {
            verifier.checkClasses = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("-thorough")) {
            verifier.thoroughWarnings = true;
            startArgIndex++;
        }
        if (args.length == startArgIndex) {
            System.out.printf("Usage: %s [-check_classes] [-thorough] " +
                            "folder1_with_json_files folder2_with_json_files...s%n",
                    ExecutorJsonVerifier.class.getName());
            return;
        }
        if (!verifier.checkClasses) {
            System.out.printf("Java classes of executors will NOT be checked%n");
        }
        try {
            for (int k = startArgIndex; k < args.length; k++) {
                final String path = SystemEnvironment.replaceHomeEnvironmentVariable(args[k]);
                final Path folder = Paths.get(path);
                System.out.printf("Verifying folder %s...%n", folder);
                verifier.verifyAll(folder);
            }
        } catch (RuntimeException | IOException e) {
            System.out.println();
            Thread.sleep(300);
            // for better behaviour from IntelliJ IDEA
            throw e;
        }
        System.out.printf("O'k%n");
    }

}
