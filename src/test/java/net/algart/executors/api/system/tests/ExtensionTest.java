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

import net.algart.executors.api.system.ExtensionSpecification;
import net.algart.executors.api.system.InstalledExtensions;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class ExtensionTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.printf("Usage: %s extensionFolder result.json%n",
                    ExtensionTest.class.getName());
            return;
        }
        final Path extensionFolder = Paths.get(args[0]);
        final Path resultFile = Paths.get(args[1]);
        System.out.printf("Analysing extension folder %s...%n", extensionFolder);
        final ExtensionSpecification extension = ExtensionSpecification.readFromFolder(extensionFolder);
        System.out.printf("Writing %s...%n", resultFile);
        extension.write(resultFile);
        System.out.printf("Extension:%n%s%n", extension);
        System.out.printf("Its platforms:%n");
        for (ExtensionSpecification.Platform platform : extension.getPlatforms()) {
            System.out.printf("    Technology: %s%n", platform.getTechnology());
            System.out.printf("    Folders: %s%n", platform.getFolders());
            if (platform.isJvmTechnology()) {
                final ExtensionSpecification.Platform.Configuration configuration = platform.getConfiguration();
                System.out.printf("        classpath: %s%n", configuration.getClasspath());
                System.out.printf("        VM options: %s%n", configuration.getVmOptions());
            }
        }

        final ExtensionSpecification.Platform empty = new ExtensionSpecification.Platform()
                .setId("some id")
                .setCategory("some category")
                .setName("some name")
                .setTechnology("jvm");
        System.out.printf("%nEmpty JVM platform:%n    %s%n%s%n", empty, empty.jsonString());
        System.out.printf("%n%n****************%nInstalled extensions:%n");
        for (ExtensionSpecification e : InstalledExtensions.allInstalledExtensions()) {
            System.out.println(e.jsonString());
        }
        System.out.printf("%n%n****************%nInstalled platforms:%n");
        for (Map.Entry<String, ExtensionSpecification.Platform> entry
                : InstalledExtensions.allInstalledPlatformsMap().entrySet()) {
            final ExtensionSpecification.Platform platform = entry.getValue();
            System.out.println(entry.getKey() + ": "
                    + platform.jsonString() + " ["
                    + (platform.isBuiltIn() ? "supported, " : "")
                    + platform.getFolders().getRoot() + "]");
        }
    }
}
