/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.zonky.test.db.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class LinuxUtils {

    private static final Logger logger = LoggerFactory.getLogger(LinuxUtils.class);

    private static final String DISTRIBUTION_NAME = resolveDistributionName();

    private LinuxUtils() {}

    public static String getDistributionName() {
        return DISTRIBUTION_NAME;
    }

    private static String resolveDistributionName() {
        if (!SystemUtils.IS_OS_LINUX) {
            return null;
        }

        try {
            InputStream source = new ClassPathResource("/sh/detect_linux_distribution.sh").getInputStream();
            Path target = Files.createTempFile("detect_linux_distribution_", ".sh");
            Files.copy(source, target, REPLACE_EXISTING);

            ProcessBuilder builder = new ProcessBuilder();
            builder.command("sh", target.toFile().getAbsolutePath());

            Process process = builder.start();
            process.waitFor();

            if (process.exitValue() != 0) {
                throw new IOException("Execution of the script to detect the Linux distribution failed with error code: '" + process.exitValue() + "'");
            }

            BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String distributionName = outputReader.readLine();

            if (StringUtils.isBlank(distributionName)) {
                logger.warn("It's not possible to detect the name of the Linux distribution, the detection script returned empty output");
                return null;
            }

            if (distributionName.startsWith("Debian")) {
                distributionName = "Debian";
            }
            if (distributionName.equals("openSUSE project")) {
                distributionName = "openSUSE";
            }

            return distributionName;
        } catch (Exception e) {
            logger.error("It's not possible to detect the name of the Linux distribution", e);
            return null;
        }
    }
}
