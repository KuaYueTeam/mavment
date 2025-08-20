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
import org.openrewrite.properties.PropertiesIsoVisitor;
import org.openrewrite.properties.tree.Properties;

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
                                    // Preserve the original quote style from the source
                                    String valueSource = l.getValueSource();
                                    if (valueSource != null) {
                                        char quoteChar = valueSource.charAt(0);
                                        return l.withValue(updatedValue).withValueSource(quoteChar + updatedValue + quoteChar);
                                    } else {
                                        return l.withValue(updatedValue).withValueSource("\"" + updatedValue + "\"");
                                    }
                                }
                            }
                        }
                        return l;
                    }
                }
            ),
            // Handle gradle.properties files
            Preconditions.check(
                new FindSourceFiles("**/gradle.properties"),
                new PropertiesIsoVisitor<ExecutionContext>() {
                    @Override
                    public Properties.Entry visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                        Properties.Entry e = super.visitEntry(entry, ctx);
                        if (e.getValue() instanceof Properties.Value) {
                            Properties.Value value = (Properties.Value) e.getValue();
                            if (value.getText() != null && value.getText().contains(currentVersion)) {
                                String updatedValue = value.getText().replace(currentVersion, targetVersion);
                                if (!updatedValue.equals(value.getText())) {
                                    return e.withValue(value.withText(updatedValue));
                                }
                            }
                        }
                        return e;
                    }
                }
            )
        );
    }

    private boolean containsMinecraftVersion(String value) {
        // Check if the string contains the current version
        if (!value.contains(currentVersion)) {
            return false;
        }
        
        // For simple cases like "1.20.1" assignments, always allow
        if (value.equals(currentVersion)) {
            return true;
        }
        
        // For strings that might contain the version, check common patterns
        return value.toLowerCase().contains("minecraft") || 
               value.toLowerCase().contains("forge") ||
               value.toLowerCase().contains("neoforge") ||
               value.toLowerCase().contains("fabric") ||
               value.toLowerCase().contains("quilt") ||
               value.toLowerCase().contains("official") ||  // mappings
               // Version patterns like "net.minecraft:server:1.20.1"
               Pattern.compile("net\\.minecraft[\\w.]*:" + Pattern.quote(currentVersion)).matcher(value).find() ||
               // Version patterns in dependency strings
               Pattern.compile(":['\"]?" + Pattern.quote(currentVersion) + "['\"]?").matcher(value).find();
    }
}