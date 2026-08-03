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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.properties.Assertions.properties;

class GradleBestPracticesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.gradle.GradleBestPractices");
    }

    @Test
    void addsBuildCacheAndParallelProperties() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              """,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void addsMissingParallelWhenCachingAlreadyEnabled() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              org.gradle.caching=true
              """,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void addsMissingCachingWhenParallelAlreadyEnabled() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              org.gradle.parallel=true
              """,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void noChangeWhenBothAlreadyEnabled() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void noChangeWhenPropertiesAlreadySetToFalse() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              org.gradle.caching=false
              org.gradle.parallel=false
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void preservesExistingProperties() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          properties(
            //language=properties
            """
              org.gradle.jvmargs=-Xmx2g
              project.name=myproject
              """,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.jvmargs=-Xmx2g
              org.gradle.parallel=true
              project.name=myproject
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void usePropertyAssignmentSyntaxForDistributionUrl() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }

              wrapper {
                  distributionUrl("https://example.com/files/example.zip")
              }
              """,
            """
              plugins { id 'java' }

              wrapper {
                  distributionUrl = "https://example.com/files/example.zip"
              }
              """),
          properties(
            //language=properties
            """
              """,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void removeEmptyBuildscriptBlock() {
        rewriteRun(
          buildGradle(
            """
              buildscript { }
              plugins { id 'java' }
              """,
            """
              plugins { id 'java' }
              """),
          properties(
            //language=properties
            """
              """,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void shouldNormalizeDependencies_no_parens() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  implementation "com.example:json:1.2.3"
                  implementation group: 'com.example', name: 'xml', version: '2.3.4'
                  implementation group: 'io.netty', name: 'netty-transport-native-epoll', classifier: 'linux-aarch_64'
                  testImplementation "com.example:testlib:0.1.0"
                  add('testImplementation', platform("org.junit:junit-bom:6.1.2"))
              }
              """,
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  implementation "com.example:json:1.2.3"
                  implementation "com.example:xml:2.3.4"
                  implementation "io.netty:netty-transport-native-epoll::linux-aarch_64"
                  testImplementation "com.example:testlib:0.1.0"
                  testImplementation platform("org.junit:junit-bom:6.1.2")
              }
              """),
          properties(
            null,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void shouldNormalizeDependencies_with_parens() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  implementation("com.example:json:1.2.3")
                  implementation(group: 'com.example', name: 'xml', version: '2.3.4')
                  implementation(group: 'io.netty', name: 'netty-transport-native-epoll', classifier: 'linux-aarch_64')
                  testImplementation("com.example:testlib:0.1.0")
                  add('testImplementation', platform("org.junit:junit-bom:6.1.2"))
              }
              """,
            """
              plugins {
                  id 'java'
              }
              dependencies {
                  implementation("com.example:json:1.2.3")
                  implementation("com.example:xml:2.3.4")
                  implementation("io.netty:netty-transport-native-epoll::linux-aarch_64")
                  testImplementation("com.example:testlib:0.1.0")
                  testImplementation(platform("org.junit:junit-bom:6.1.2"))
              }
              """),
          properties(
            null,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void shouldNormalizeDependencies_kotlinDsl() {
        rewriteRun(
          // The Kotlin DSL has no type attribution, so the `GradleDependency` trait can only recognize these
          // declarations from the configurations on the `GradleProject` marker the tooling API supplies
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("com.example:json:1.2.3")
                  implementation(group = "com.example", name = "xml", version = "2.3.4")
                  implementation(group = "io.netty", name = "netty-transport-native-epoll", classifier = "linux-aarch_64")
                  testImplementation("com.example:testlib:0.1.0")
                  add("testImplementation", platform("org.junit:junit-bom:6.1.2"))
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("com.example:json:1.2.3")
                  implementation("com.example:xml:2.3.4")
                  implementation("io.netty:netty-transport-native-epoll::linux-aarch_64")
                  testImplementation("com.example:testlib:0.1.0")
                  testImplementation(platform("org.junit:junit-bom:6.1.2"))
              }
              """),
          properties(
            null,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void shouldNormalizeDependencies_subprojects_kotlinDsl() {
        rewriteRun(
          // The Kotlin DSL has no type attribution, so the `GradleDependency` trait can only recognize these
          // declarations from the configurations on the `GradleProject` marker the tooling API supplies
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              subprojects {
                  dependencies {
                      implementation("com.example:json:1.2.3")
                      implementation(group = "com.example", name = "xml", version = "2.3.4")
                      implementation(group = "io.netty", name = "netty-transport-native-epoll", classifier = "linux-aarch_64")
                      testImplementation("com.example:testlib:0.1.0")
                      add("testImplementation", platform("org.junit:junit-bom:6.1.2"))
                      "testImplementation"(platform("org.mockito:mockito-bom:5.23.0"))
                  }
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              subprojects {
                  dependencies {
                      implementation("com.example:json:1.2.3")
                      implementation("com.example:xml:2.3.4")
                      implementation("io.netty:netty-transport-native-epoll::linux-aarch_64")
                      testImplementation("com.example:testlib:0.1.0")
                      add("testImplementation", platform("org.junit:junit-bom:6.1.2"))
                      "testImplementation"(platform("org.mockito:mockito-bom:5.23.0"))
                  }
              }
              """),
          properties(
            null,
            //language=properties
            """
              org.gradle.caching=true
              org.gradle.parallel=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }
}
