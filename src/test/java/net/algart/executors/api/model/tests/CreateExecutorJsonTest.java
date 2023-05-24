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

package net.algart.executors.api.model.tests;

import net.algart.json.Jsons;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.data.ParameterValueType;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.Executor;
import net.algart.executors.api.model.ControlEditionType;
import net.algart.executors.api.model.ExecutorJson;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CreateExecutorJsonTest {
    public static class TestExecutor extends Executor {
        @Override
        public void process() {
            System.out.println("    Hi from TestExecutor!");
            System.out.println("    My ID: " + getExecutorId());
            System.out.println("    Current session ID: " + getSessionId());
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.out.printf("Usage: %s result.json%n", CreateExecutorJsonTest.class.getName());
            return;
        }
        final Path resultFile = Paths.get(args[0]);

        final ExecutorJson model = new ExecutorJson();
        model.setVersion("0.0.11");
        model.setPlatformId("~~~SOME_PLATFORM");
        model.setCategory("some.category");
        model.setName("some executor");
        model.setExecutorId("11933940-d53b-495b-94ef-bb85d5b5402e");
        model.setLanguage("java");
        final ExecutorJson.JavaConf javaConf = new ExecutorJson.JavaConf();
        javaConf.setJson("{\"class\":\"" + TestExecutor.class.getName() + "\"}");
        model.setJava(javaConf);
        final ExecutorJson.Options options = new ExecutorJson.Options();
        final ExecutorJson.Options.Behavior behavior = new ExecutorJson.Options.Behavior();
        options.setBehavior(behavior);
        model.setOptions(options);
        final Map<String, ExecutorJson.PortConf> outPorts = new LinkedHashMap<>();
        outPorts.put("output", new ExecutorJson.PortConf().setName("output").setValueType(DataType.SCALAR));
        model.setOutPorts(outPorts);
        final Map<String, ExecutorJson.ControlConf> controls = new LinkedHashMap<>();
        controls.put("width", new ExecutorJson.ControlConf().setName("width").setValueType(ParameterValueType.INT));
        List<ExecutorJson.ControlConf.EnumItem> items = new ArrayList<>();
        items.add(new ExecutorJson.ControlConf.EnumItem().setValue("MODE_1").setCaption("mode 2"));
        items.add(new ExecutorJson.ControlConf.EnumItem().setValue("MODE_2").setCaption("mode 2"));
        controls.put("mode", new ExecutorJson.ControlConf().setName("mode").setValueType(ParameterValueType.STRING)
            .setCaption("Mode").setEditionType(ControlEditionType.ENUM)
                .setItems(items).setDefaultStringValue("MODE_1"));
        items = new ArrayList<>();
        items.add(new ExecutorJson.ControlConf.EnumItem().setValue(Jsons.toJsonIntValue(1)).setCaption("m_1"));
        items.add(new ExecutorJson.ControlConf.EnumItem().setValue(Jsons.toJsonIntValue(2)).setCaption("m_2"));
        controls.put("modeInt", new ExecutorJson.ControlConf().setName("modeInt").setValueType(ParameterValueType.INT)
                .setCaption("Mode (int)").setEditionType(ControlEditionType.ENUM)
                .setItems(items).setDefaultJsonValue(Jsons.toJsonIntValue(2)));
        model.setControls(controls);

        final String jsonString = model.jsonString();
        java.nio.file.Files.writeString(resultFile, jsonString);
        System.out.printf("Minimal configuration:%n");
        System.out.println(model.minimalConfigurationJsonString());
        System.out.printf("%nFull model:%n");
        System.out.println(model);
        if (model.isJavaExecutor()) {
            System.out.printf("%nExecutor object:%n");
            Thread.sleep(100);
            final ExecutionBlock executionBlock;
            try {
                executionBlock = ExecutionBlock.newExecutionBlock(
                        "some_session", model.getExecutorId(), model.minimalConfigurationJsonString());
                Thread.sleep(100);
                System.out.println(executionBlock);
                executionBlock.execute();
            } catch (ClassNotFoundException e) {
                System.out.printf("Cannot load required class: %s%n", e);
            }
        }
    }
}
