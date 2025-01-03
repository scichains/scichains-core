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

package net.algart.executors.api.system;

import net.algart.executors.api.CommonPlatformInformation;

import java.util.Objects;
import java.util.UUID;

class SpecialSpecificationsBuilder {
    public static final String PLATFORM_LANGUAGE = "system";

    private final ExecutorSpecificationSet specifications;

    public SpecialSpecificationsBuilder(ExecutorSpecificationSet specifications) {
        this.specifications = Objects.requireNonNull(specifications, "Null specifications");
    }

    public boolean addSpecifications() {
        final ExecutorSpecification pattern = findCommonPlatformInformationPattern();
        if (pattern != null) {
            specifications.remove(pattern.getExecutorId());
            for (ExtensionSpecification.Platform platform : InstalledExtensions.allInstalledPlatforms()) {
                specifications.add(newCommonPlatformInformationSpecification(pattern, platform));
            }
            return true;
        }
        return false;
    }

    private ExecutorSpecification findCommonPlatformInformationPattern() {
        for (ExecutorSpecification model : specifications.all()) {
            if (model.isJavaExecutor()) {
                final ExecutorSpecification.JavaConf javaConf = model.getJava();
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

    private ExecutorSpecification newCommonPlatformInformationSpecification(
            ExecutorSpecification pattern,
            ExtensionSpecification.Platform platform) {
        Objects.requireNonNull(pattern, "Null pattern");
        Objects.requireNonNull(platform, "Null platform");
        ExecutorSpecification result = new ExecutorSpecification();
        result.setExecutorId(makeId(pattern.getExecutorId(), platform));
        result.setPlatformId(platform.getId());
        result.setName(replacePlatformName(pattern.getName(), platform));
        result.setCategory(pattern.getCategory());
        result.setDescription(replacePlatformName(pattern.getDescription(), platform));
        result.setLanguage(PLATFORM_LANGUAGE);
        result.setJava(new ExecutorSpecification.JavaConf().setJson(
                ExecutorSpecification.JavaConf.standardJson(CommonPlatformInformation.class.getName())));
        result.setInPorts(pattern.getInPorts());
        result.setOutPorts(pattern.getOutPorts());
        result.setControls(pattern.getControls());
        // - note: it is not absolutely safe, because ports will be shared in all created specifications;
        // but it is maximally quick
        return result;
    }

    private static String replacePlatformName(String s, ExtensionSpecification.Platform platform) {
        return s == null ? null : s.replace("$$$", platform.getName());
    }

    private static String makeId(String commonPlatformInformationId, ExtensionSpecification.Platform platform) {
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
        // (Changing it will lead to changing all IDs of created specifications.)
        uuid = new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() ^ correction);
        return uuid.toString();
    }
}
