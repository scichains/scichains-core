/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.api.system.tests;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.executors.api.system.ControlEditionType;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreateExecutorSpecificationTest {
    public static class TestExecutor extends Executor {
        @Override
        public void process() {
            System.out.println("    Hi from TestExecutor!");
            System.out.println("    My ID: " + getExecutorId());
            System.out.println("    Current session ID: " + getSessionId());
            System.out.println("    My mode (int): " + parameters().getInteger("modeInt"));
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.out.printf("Usage: %s result.json%n", CreateExecutorSpecificationTest.class.getName());
            return;
        }
        final Path resultFile = Paths.get(args[0]);

        final ExecutorSpecification specification = new ExecutorSpecification();
        specification.setVersion("0.0.11");
        specification.setPlatformId("~~~SOME_PLATFORM");
        specification.setCategory("some.category");
        specification.setName("some executor");
        specification.setId("144f8656-91c7-45a8-9e32-c19b179b9d34");
        specification.setLanguage("java");
        final ExecutorSpecification.JavaConf javaConf = new ExecutorSpecification.JavaConf();
        javaConf.setJson("{\"class\":\"" + TestExecutor.class.getName() + "\"}");
        specification.setJava(javaConf);
        final ExecutorSpecification.Options options = new ExecutorSpecification.Options();
        final ExecutorSpecification.Options.Behavior behavior = new ExecutorSpecification.Options.Behavior();
        options.setBehavior(behavior);
        specification.setOptions(options);
        final Map<String, ExecutorSpecification.PortConf> outputPorts = new LinkedHashMap<>();
        outputPorts.put("output", new ExecutorSpecification.PortConf()
                .setName("output").setValueType(DataType.SCALAR));
        specification.setOutputPorts(outputPorts);
        final Map<String, ExecutorSpecification.ControlConf> controls = new LinkedHashMap<>();
        controls.put("width", new ExecutorSpecification.ControlConf()
                .setName("width").setValueType(ParameterValueType.INT));
        List<ExecutorSpecification.ControlConf.EnumItem> items = new ArrayList<>();
        items.add(new ExecutorSpecification.ControlConf.EnumItem().setValue("MODE_1").setCaption("mode 2"));
        items.add(new ExecutorSpecification.ControlConf.EnumItem().setValue("MODE_2").setCaption("mode 2"));
        controls.put("mode", new ExecutorSpecification.ControlConf()
                .setName("mode").setValueType(ParameterValueType.STRING)
                .setCaption("Mode").setEditionType(ControlEditionType.ENUM)
                .setItems(items).setDefaultStringValue("MODE_1"));
        items = new ArrayList<>();
        items.add(new ExecutorSpecification.ControlConf.EnumItem()
                .setValue(Jsons.toJsonIntValue(1)).setCaption("m_1"));
        items.add(new ExecutorSpecification.ControlConf.EnumItem()
                .setValue(Jsons.toJsonIntValue(2)).setCaption("m_2"));
        controls.put("modeInt", new ExecutorSpecification.ControlConf()
                .setName("modeInt").setValueType(ParameterValueType.INT)
                .setCaption("Mode (int)").setEditionType(ControlEditionType.ENUM)
                .setItems(items).setDefaultJsonValue(Jsons.toJsonIntValue(2)));
        specification.setControls(controls);

        final String jsonString = specification.jsonString();
        java.nio.file.Files.writeString(resultFile, jsonString);
        System.out.printf("Minimal configuration:%n");
        System.out.println(specification.minimalSpecification());
        System.out.printf("%nFull specification:%n");
        System.out.println(specification);
        if (specification.isJavaExecutor()) {
            System.out.printf("%nExecutor object:%n");
            Thread.sleep(100);
            final Executor executor;
            try {
                executor = (Executor) ExecutionBlock.newExecutor("some_session",
                        specification.minimalSpecification());
                executor.disableOnChangeParametersAutomatic();
                // - suppressing warning on setIntParameter
                executor.setIntParameter("modeInt", 1);
                System.out.println();
                System.out.println("Full specification:");
                System.out.println(specification.jsonString());
                System.out.println("Minimal specification for creation:");
                System.out.println(executor.getSpecification().jsonString());
                System.out.println("Parameters:");
                System.out.println(executor.parameters());
                System.out.println("Executor:");
                System.out.println(executor);
                System.out.println();
                executor.execute();
            } catch (ClassNotFoundException e) {
                System.out.printf("Cannot load required class: %s%n", e);
            }
        }
    }
}
