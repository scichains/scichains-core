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

package net.algart.executors.modules.core.logic.compiler.settings.executable;

import net.algart.executors.api.system.Chain;
import net.algart.executors.api.system.ChainSpecification;
import net.algart.executors.api.system.ExecutorFactory;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombinerSpecification;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public final class SubChainToSettingsCombiner {
    public static final String SESSION_ID = "~~DUMMY_SESSION";

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.printf("Usage: %s sub_chain.json " +
                            "new_resulting_settings_combiner_specification.json executor_name%n",
                    SubChainToSettingsCombiner.class);
            return;
        }

        final Path chainSpecificationFile = Paths.get(args[0]);
        final Path settingsCombinerSpecificationFile = Paths.get(args[1]);
        final String executorName = args[2];

        final ExecutorFactory executorFactory = ExecutorFactory.newDefaultInstance(SESSION_ID);
        // - ExecutorFactory is necessary:
        // 1) to find input/output/data blocks;
        // 2) to find default values of parameters in data blocks.
        final ChainSpecification chainSpecification = ChainSpecification.read(chainSpecificationFile);
        final Chain chain = Chain.valueOf(null, executorFactory, chainSpecification);
        final ExecutorSpecification executorSpecification = new ExecutorSpecification();
        executorSpecification.setTo(chain);
        final SettingsCombinerSpecification settingsCombinerSpecification = new SettingsCombinerSpecification();
        settingsCombinerSpecification.setId(UUID.randomUUID().toString());
        settingsCombinerSpecification.setCombineName(
                SettingsCombinerSpecification.DEFAULT_SETTINGS_COMBINE_PREFIX + executorName);
        settingsCombinerSpecification.setControls(executorSpecification.getControls());
        settingsCombinerSpecification.write(settingsCombinerSpecificationFile);
    }
}
