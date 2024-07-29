/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import jakarta.json.JsonException;
import net.algart.executors.api.model.ChainJson;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReplaceChainsInFolder {
    boolean doChanges = false;
    int count = 0;

    private void replace(Path f) throws IOException {
        ChainJson model = null;
        try {
            model = ChainJson.readIfValid(f);
        } catch (JsonException e) {
            e.printStackTrace();
            return;
        }
        if (model == null) {
            System.out.printf("%s is not a chain%n", f);
            return;
        }
        // Debugging:
        final String jsonString = model.jsonString();
        final ChainJson reverse = ChainJson.valueOf(Jsons.toJson(jsonString));
        if (!reverse.jsonString().equals(jsonString)) {
            throw new AssertionError("Cannot reproduce JSON structre for " + f);
        }
        if (doChanges) {
            model.rewriteChainSection(f);
            System.out.printf("%s successfully rewritten%n", f);
        }
        count++;
    }

    private void replaceAll(Path folder) throws IOException {
        try (final DirectoryStream<Path> files = Files.newDirectoryStream(folder)) {
            for (Path file : files) {
                if (Files.isDirectory(file)) {
                    replaceAll(file);
                } else if (ChainJson.isChainJsonFile(file)) {
                    replace(file);
                }
            }
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        final ReplaceChainsInFolder replacer = new ReplaceChainsInFolder();
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equals("-f")) {
            replacer.doChanges = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 1) {
            System.out.printf("Usage: %s [-f] folder_with_chains%n"
                            + "Warning: with -f flag, this utility will replace "
                            + "all chains in this folder and its subfolders!",
                    ReplaceChainsInFolder.class.getName());
            return;
        }
        final Path folder = Paths.get(args[startArgIndex]);
        replacer.replaceAll(folder);
    }
}
