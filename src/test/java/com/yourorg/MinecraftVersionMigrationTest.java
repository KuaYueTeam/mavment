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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.properties.Assertions.properties;

class MinecraftVersionMigrationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MinecraftVersionMigration("1.20.1", "1.21"));
    }

    @DocumentExample
    @Test
    void updateMinecraftVersionInBuildGradle() {
        rewriteRun(
            buildGradle(
                """
                plugins {
                    id 'java'
                    id 'net.neoforged.gradle' version '7.0.80'
                }
                
                group 'com.example.mod'
                version '1.0.0'
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation 'net.neoforged:neoforge:1.20.1-47.1.0'
                    implementation 'net.minecraft:server:1.20.1'
                }
                """,
                """
                plugins {
                    id 'java'
                    id 'net.neoforged.gradle' version '7.0.80'
                }
                
                group 'com.example.mod'
                version '1.0.0'
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation 'net.neoforged:neoforge:1.21-47.1.0'
                    implementation 'net.minecraft:server:1.21'
                }
                """
            )
        );
    }

    @Test
    void updateMinecraftVersionInGradleProperties() {
        rewriteRun(
            properties(
                """
                minecraft_version=1.20.1
                forge_version=47.1.0
                neoforge_version=1.20.1-47.1.0
                mod_version=1.0.0
                """,
                """
                minecraft_version=1.21
                forge_version=47.1.0
                neoforge_version=1.21-47.1.0
                mod_version=1.0.0
                """,
                spec -> spec.path(Path.of("gradle.properties"))
            )
        );
    }

    @Test
    void updateMultipleVersionsInBuildGradle() {
        rewriteRun(
            buildGradle(
                """
                plugins {
                    id 'java'
                    id 'net.minecraftforge.gradle' version '5.1.+'
                }
                
                minecraft {
                    version = '1.20.1'
                    mappings = 'official_1.20.1'
                }
                
                dependencies {
                    minecraft 'net.minecraftforge:forge:1.20.1-47.1.0'
                    implementation 'net.minecraft:client:1.20.1'
                }
                """,
                """
                plugins {
                    id 'java'
                    id 'net.minecraftforge.gradle' version '5.1.+'
                }
                
                minecraft {
                    version = '1.21'
                    mappings = 'official_1.21'
                }
                
                dependencies {
                    minecraft 'net.minecraftforge:forge:1.21-47.1.0'
                    implementation 'net.minecraft:client:1.21'
                }
                """
            )
        );
    }

    @Test
    void noChangeWhenVersionNotFound() {
        rewriteRun(
            buildGradle(
                """
                plugins {
                    id 'java'
                }
                
                dependencies {
                    implementation 'com.example:lib:1.0.0'
                }
                """
            )
        );
    }

    @Test
    void updateFabricVersionInGradleProperties() {
        rewriteRun(
            spec -> spec.recipe(new MinecraftVersionMigration("1.19.2", "1.20.1")),
            properties(
                """
                # Fabric Properties
                minecraft_version=1.19.2
                fabric_loader_version=0.14.9
                fabric_version=0.67.0+1.19.2
                """,
                """
                # Fabric Properties
                minecraft_version=1.20.1
                fabric_loader_version=0.14.9
                fabric_version=0.67.0+1.20.1
                """,
                spec -> spec.path(Path.of("gradle.properties"))
            )
        );
    }
}