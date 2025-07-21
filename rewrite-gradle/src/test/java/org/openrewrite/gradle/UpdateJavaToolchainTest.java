/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

@SuppressWarnings("GroovyUnusedAssignment")
class UpdateJavaToolchainTest implements RewriteTest {

    @DocumentExample
    @Test
    void default_does_not_add_if_missing() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(11, null, null, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      1.8,11
      '1.8','11'
      "1.8","11"
      JavaVersion.VERSION_1_8,JavaVersion.VERSION_11
      """, quoteCharacter = '`')
    void beforeAndAfterJavaToolchain(String beforeJavaToolchain, String afterJavaToolchain) {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(11, null, null, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              java {
                  toolchain {
                    languageVersion = %s
                  }
              }
              """.formatted(beforeJavaToolchain),
            """
              plugins {
                  id "java"
              }
              
              java {
                  toolchain {
                    languageVersion = %s
                  }
              }
              """.formatted(afterJavaToolchain)
          )
        );
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      Enum,1.8,JavaVersion.VERSION_1_8
      Enum,'1.8',JavaVersion.VERSION_1_8
      Enum,"1.8",JavaVersion.VERSION_1_8
      Enum,JavaVersion.toVersion("1.8"),JavaVersion.VERSION_1_8
      Number,'1.8',1.8
      Number,"1.8",1.8
      Number,JavaVersion.VERSION_1_8,1.8
      Number,JavaVersion.toVersion("1.8"),1.8
      String,1.8,'1.8'
      String,JavaVersion.VERSION_1_8,'1.8'
      String,JavaVersion.toVersion("1.8"),'1.8'
      """, quoteCharacter = '`')
    void styleChange(String declarationStyle, String beforeJavaToolchainVersion, String afterJavaToolchainVersion) {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(8, UpdateJavaToolchain.DeclarationStyle.valueOf(declarationStyle), null, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              java {
                  toolchain = %s
              }
              """.formatted(beforeJavaToolchainVersion),
            """
              plugins {
                  id "java"
              }
              
              java {
                  toolchain = %s
              }
              """.formatted(afterJavaToolchainVersion)
          )
        );
    }

    @Test
    void javaToolchain_upgrade_8_to_11() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(11, null, false, false)),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(8)
                  }
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(11)
                  }
              }
              """
          )
        );
    }

    @Test
    void javaToolchain_added_when_addIfMissingTrue() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(21, null, null, true)),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              sourceCompatibility = 21
              targetCompatibility = 21
              """,
            """
              plugins {
                  id "java"
              }
              sourceCompatibility = 21
              targetCompatibility = 21

              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(21)
                  }
              }
              """
          )
        );
    }

    @Test
    void javaToolchain_addIfMissingTrue_toolchainAddedToExistingJavaBlock() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(21, null, null, true)),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              java {
                  withJavadocJar()
                  withSourcesJar()
              }
              """,
            """
              plugins {
                  id "java"
              }

              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(21)
                  }
                  withJavadocJar()
                  withSourcesJar()
              }
              """
          )
        );
    }

    @Test
    void javaToolchain_isPresent_noModificationsNeeded() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(21, null, null, true)),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(21)
                  }
                  withJavadocJar()
                  withSourcesJar()
              }
              """
          )
        );
    }

    @Test
    void javaToolchain_version_downgrade_applied() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(17, null, true, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(21)
                  }
                  withJavadocJar()
                  withSourcesJar()
              }
              """,
            """
               plugins {
                   id "java"
               }
 
               java {
                   toolchain {
                       languageVersion = JavaLanguageVersion.of(17)
                   }
                   withJavadocJar()
                   withSourcesJar()
               }
               """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      Enum,JavaVersion.VERSION_11,1.8
      Number,11,1.8
      String,'11',1.8
      Enum,1.8,JavaVersion.VERSION_11
      Number,1.8,11
      String,1.8,'11'
      """, quoteCharacter = '`')
    void allOptions(String declarationStyle, String expectedSourceCompatibility, String expectedTargetCompatibility) {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(11, UpdateJavaToolchain.DeclarationStyle.valueOf(declarationStyle), null, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              sourceCompatibility = 1.8
              targetCompatibility = 1.8
              """,
            """
              plugins {
                  id "java"
              }
              
              sourceCompatibility = %s
              targetCompatibility = %s
              """.formatted(expectedSourceCompatibility, expectedTargetCompatibility)
          )
        );
    }

    @Test
    void onlyModifyJavaToolchainAssignment() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(11, null, null, null)),
          buildGradle(
            """
              version = "0.1.0-SNAPSHOT"
              group = "com.example"
              java {
                  toolchain = JavaVersion.toVersion(1.8)
              }
              """,
            """
              version = "0.1.0-SNAPSHOT"
              group = "com.example"
              java {
                  toolchain = JavaVersion.toVersion(11)
              }
              """
          )
        );
    }

    @Test
    void doNotDowngradeByDefault() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(17, null, null, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              java {
                  sourceCompatibility = 21
                  targetCompatibility = 21
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(21)
                  }
              }
              """
          )
        );
    }

    @Test
    void doDowngradeWhenRequested() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(17, null, true, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              java {
                  bogusProperty = 8
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(21)
                  }
              }
              """,

            """
              plugins {
                  id "java"
              }
              
              java {
                  bogusProperty = 8
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(17)
                  }
              }
              """
          )
        );
    }

    @Test
    void upgradeExistingJavaToolchainInKotlinDSL() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(11, null, null, null)),
          buildGradleKts(
            """
              plugins {
                  java
              }
              
              java {
                  toolchain {
                      languageVersion.set(JavaLanguageVersion.of(8))
                  }
              }
              """,
            """
              plugins {
                  java
              }
              
              java {
                  toolchain {
                      languageVersion.set(JavaLanguageVersion.of(11))
                  }
              }
              """
          )
        );
    }

    @Test
    void addMissingJavaToolchainInKotlinDSL() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaToolchain(21, null, null, true)),
          buildGradleKts(
            """
              plugins {
                  java
              }
              """,
            """
              plugins {
                  java
              }
              
              java {
                  toolchain {
                      languageVersion = JavaVersion.VERSION_21
                  }
              }
              """
          )
        );
    }
}
