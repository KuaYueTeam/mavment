/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yourorg;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.properties.ChangePropertyValue;

import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class MinecraftVersionMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "Minecraft version migration";
    }

    @Override
    public String getDescription() {
        return "Migrates Minecraft project from current version to target version by updating build.gradle and gradle.properties files.";
    }

    @Option(displayName = "Current version",
            description = "The current Minecraft version to migrate from.",
            example = "1.20.1")
    String currentVersion;

    @Option(displayName = "Target version", 
            description = "The target Minecraft version to migrate to.",
            example = "1.21")
    String targetVersion;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.or(
            // Handle build.gradle files
            Preconditions.check(
                new FindSourceFiles("**/build.gradle"),
                new GroovyIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                        J.Literal l = super.visitLiteral(literal, ctx);
                        if (l.getValue() instanceof String) {
                            String value = (String) l.getValue();
                            if (containsMinecraftVersion(value)) {
                                String updatedValue = value.replace(currentVersion, targetVersion);
                                if (!updatedValue.equals(value)) {
                                    return l.withValue(updatedValue).withValueSource("\"" + updatedValue + "\"");
                                }
                            }
                        }
                        return l;
                    }
                }
            ),
            // Handle gradle.properties files - basic version properties
            Preconditions.check(
                new FindSourceFiles("**/gradle.properties"),
                new ChangePropertyValue("minecraft_version", targetVersion, null, null, null).getVisitor()
            ),
            // Handle more gradle.properties patterns
            Preconditions.check(
                new FindSourceFiles("**/gradle.properties"),
                new ChangePropertyValue("mc_version", targetVersion, null, null, null).getVisitor()
            )
        );
    }

    private boolean containsMinecraftVersion(String value) {
        // Check if the string contains version patterns commonly used in Minecraft projects
        return value.contains(currentVersion) && (
            value.toLowerCase().contains("minecraft") || 
            value.toLowerCase().contains("forge") ||
            value.toLowerCase().contains("neoforge") ||
            value.toLowerCase().contains("fabric") ||
            value.toLowerCase().contains("quilt") ||
            // Version patterns like "net.minecraft:server:1.20.1"
            Pattern.compile("net\\.minecraft[\\w.]*:" + Pattern.quote(currentVersion)).matcher(value).find() ||
            // Version patterns in dependency strings
            Pattern.compile(":['\"]?" + Pattern.quote(currentVersion) + "['\"]?").matcher(value).find()
        );
    }
}