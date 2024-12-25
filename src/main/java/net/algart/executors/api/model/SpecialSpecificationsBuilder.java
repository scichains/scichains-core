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

package net.algart.executors.api.model;

import net.algart.executors.api.CommonPlatformInformation;

import java.util.Objects;
import java.util.UUID;

class SpecialSpecificationsBuilder {
    public static final String PLATFORM_LANGUAGE = "system";

    private final ExecutorJsonSet specifications;

    public SpecialSpecificationsBuilder(ExecutorJsonSet specifications) {
        this.specifications = Objects.requireNonNull(specifications, "Null specifications");
    }

    public boolean addSpecifications() {
        final ExecutorJson pattern = findCommonPlatformInformationPattern();
        if (pattern != null) {
            specifications.remove(pattern.getExecutorId());
            for (ExtensionJson.Platform platform : InstalledExtensions.allInstalledPlatforms()) {
                specifications.add(newCommonPlatformInformationSpecification(pattern, platform));
            }
            return true;
        }
        return false;
    }

    private ExecutorJson findCommonPlatformInformationPattern() {
        for (ExecutorJson model : specifications.all()) {
            if (model.isJavaExecutor()) {
                final ExecutorJson.JavaConf javaConf = model.getJava();
                if (javaConf != null) {
                    final String className = javaConf.getClassName();
                    if (Objects.equals(className, CommonPlatformInformation.class.getName())) {
                        return model;
                    }
                }
            }
        }
        return null;
    }

    private ExecutorJson newCommonPlatformInformationSpecification(
            ExecutorJson pattern,
            ExtensionJson.Platform platform) {
        Objects.requireNonNull(pattern, "Null pattern");
        Objects.requireNonNull(platform, "Null platform");
        ExecutorJson result = new ExecutorJson();
        result.setExecutorId(makeId(pattern.getExecutorId(), platform));
        result.setPlatformId(platform.getId());
        result.setName(replacePlatformName(pattern.getName(), platform));
        result.setCategory(pattern.getCategory());
        result.setDescription(replacePlatformName(pattern.getDescription(), platform));
        result.setLanguage(PLATFORM_LANGUAGE);
        result.setJava(new ExecutorJson.JavaConf().setJson(
                ExecutorJson.JavaConf.standardJson(CommonPlatformInformation.class.getName())));
        result.setInPorts(pattern.getInPorts());
        result.setOutPorts(pattern.getOutPorts());
        result.setControls(pattern.getControls());
        // - note: it is not absolutely safe, because ports will be shared in all created models;
        // but it is maximally quick
        return result;
    }

    private static String replacePlatformName(String s, ExtensionJson.Platform platform) {
        return s == null ? null : s.replace("$$$", platform.getName());
    }

    private static String makeId(String commonPlatformInformationId, ExtensionJson.Platform platform) {
        UUID uuid;
        try {
            uuid = UUID.fromString(platform.getId());
        } catch (Exception e) {
            // - not UUID
            return platform.getId() + "~~" + CommonPlatformInformation.class.getSimpleName();
        }
        final long correction = commonPlatformInformationId.hashCode() & 0xFFFFFFFFL;
        // - We modify only 4 bytes from 6 low UUID bytes.
        // Note that we do not require commonPlatformInformationId to be UUID, but it must be stable!
        // (Changing it will lead to changing all IDs of created models.)
        uuid = new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() ^ correction);
        return uuid.toString();
    }
}
