/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class DependencyUseConfigurationMethodTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DependencyUseConfigurationMethod());
    }

    @DocumentExample
    @Test
    void addWithPlatform() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  testImplementation "com.example:testlib:0.1.0"
                  add('testImplementation', platform("org.junit:junit-bom:6.1.2"))
              }
              """,
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  testImplementation "com.example:testlib:0.1.0"
                  testImplementation platform("org.junit:junit-bom:6.1.2")
              }
              """
          )
        );
    }

    @Test
    void parenthesizedWhenSiblingDeclarationsAreParenthesized() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  testImplementation("com.example:testlib:0.1.0")
                  add('testImplementation', platform("org.junit:junit-bom:6.1.2"))
              }
              """,
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  testImplementation("com.example:testlib:0.1.0")
                  testImplementation(platform("org.junit:junit-bom:6.1.2"))
              }
              """
          )
        );
    }

    @Test
    void parenthesizedWhenBlockOffersNoExampleToFollow() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  add('implementation', "com.example:json:1.2.3")
              }
              """,
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  implementation("com.example:json:1.2.3")
              }
              """
          )
        );
    }

    @Test
    void kotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
              }
              dependencies {
                  add("implementation", "com.example:json:1.2.3")
              }
              """,
            """
              plugins {
                  java
              }
              dependencies {
                  implementation("com.example:json:1.2.3")
              }
              """
          )
        );
    }

    @Test
    void constraints() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  constraints {
                      add('implementation', "com.example:json:1.2.3")
                  }
              }
              """,
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  constraints {
                      implementation("com.example:json:1.2.3")
                  }
              }
              """
          )
        );
    }

    @Test
    void trailingClosureIsPreserved() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  add('implementation', "com.example:json:1.2.3") {
                      transitive = false
                  }
              }
              """,
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  implementation("com.example:json:1.2.3") {
                      transitive = false
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenConfigurationIsNotALiteral() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              def conf = 'implementation'
              dependencies {
                  add(conf, "com.example:json:1.2.3")
              }
              """
          )
        );
    }

    @Test
    void noChangeForProviderVariants() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  addProvider('implementation', providers.provider { "com.example:json:1.2.3" })
              }
              """
          )
        );
    }

    @Test
    void noChangeOutsideDependenciesBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              def coordinates = []
              coordinates.add('implementation', "com.example:json:1.2.3")
              """
          )
        );
    }
}
