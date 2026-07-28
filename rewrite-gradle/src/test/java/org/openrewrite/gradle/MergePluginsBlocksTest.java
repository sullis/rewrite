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

class MergePluginsBlocksTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MergePluginsBlocks());
    }

    @Test
    void emptyPluginsBlock() {
        rewriteRun(buildGradle("""
          plugins {
            // hello world
          }
          """
        ));
    }

    @Test
    void onePluginsBlock() {
        rewriteRun(buildGradle("""
          plugins {
             id 'java'
          }
          """
        ));
    }

    @Test
    void multiplePluginsBlocks() {
        rewriteRun(buildGradle("""
          plugins {
             id 'java'
          }
          plugins {
             id 'scala'
          }
          """, """
          plugins {
             id 'java'
             id 'scala'
          }
          """)
        );
    }

    @Test
    void preserveComments() {
        rewriteRun(buildGradle("""
          plugins {
             id 'java' // we love java
          }
          plugins {
             id 'scala' // we love scala
          }
          """, """
          plugins {
             id 'java' // we love java
             id 'scala' // we love scala
          }
          """)
        );
    }

    @Test
    void sortPluginsAlphabetically() {
        rewriteRun(buildGradle("""
          plugins {
             id 'java'
             id 'aaa'
          }
          plugins {
             id 'zzz'
          }
          plugins {
             id 'scala'
          }
          """, """
          plugins {
             id 'aaa'
             id 'java'
             id 'scala'
             id 'zzz'
          }
          """)
        );
    }

    @Test
    void sortByPluginIdRatherThanByVersion() {
        rewriteRun(buildGradle("""
          plugins {
             id 'com.zzz' version '1.0'
          }
          plugins {
             id 'com.aaa'
          }
          """, """
          plugins {
             id 'com.aaa'
             id 'com.zzz' version '1.0'
          }
          """)
        );
    }

    @Test
    void emptyFirstBlock() {
        rewriteRun(buildGradle("""
          plugins {
          }
          plugins {
             id 'java'
          }
          """, """
          plugins {
             id 'java'
          }
          """)
        );
    }

    /**
     * A comment written above a discarded block has nothing left to describe once that block is gone, so it
     * transfers to whatever takes the block's place, here the end of the file.
     */
    @Test
    void keepCommentWrittenAboveADiscardedBlock() {
        rewriteRun(buildGradle("""
          plugins {
             id 'java'
          }
          // the scala plugins
          plugins {
             id 'scala'
          }
          """, """
          plugins {
             id 'java'
             id 'scala'
          }
          // the scala plugins
          """)
        );
    }

    @Test
    void blocksNestedInAnotherBlock() {
        rewriteRun(buildGradle("""
          subprojects {
             plugins {
                id 'java'
             }
             plugins {
                id 'scala'
             }
          }
          """, """
          subprojects {
             plugins {
                id 'java'
                id 'scala'
             }
          }
          """)
        );
    }

    @Test
    void leaveBlocksInDifferentScopesAlone() {
        rewriteRun(buildGradle("""
          plugins {
             id 'java'
          }
          subprojects {
             plugins {
                id 'scala'
             }
          }
          """
        ));
    }

    /**
     * Two statements cannot share a line, so blocks written on one line each have to be opened up rather than run
     * together into a single unparseable statement.
     */
    @Test
    void blocksWrittenOnASingleLine() {
        rewriteRun(buildGradle("""
          plugins { id 'java' }
          plugins { id 'scala' }
          """, """
          plugins {
              id 'java'
              id 'scala'
          }
          """)
        );
    }

    @Test
    void oneBlockWrittenOnASingleLine() {
        rewriteRun(buildGradle("""
          plugins {
             id 'java'
          }
          plugins { id 'scala' }
          """, """
          plugins {
             id 'java'
             id 'scala'
          }
          """)
        );
    }

    /**
     * A line comment comments out the rest of its line, so a declaration merged in behind one has to start a line
     * of its own rather than disappear into it.
     */
    @Test
    void declarationMergedInBehindALineComment() {
        rewriteRun(buildGradle("""
          plugins { // none yet
          }
          plugins { id 'java' }
          """, """
          plugins { // none yet
              id 'java'
          }
          """)
        );
    }

    @Test
    void settingsScript() {
        rewriteRun(settingsGradle("""
          plugins {
             id 'com.zzz'
          }
          plugins {
             id 'com.aaa'
          }
          """, """
          plugins {
             id 'com.aaa'
             id 'com.zzz'
          }
          """)
        );
    }

    @Test
    void blocksNestedInPluginManagement() {
        rewriteRun(settingsGradle("""
          pluginManagement {
             plugins {
                id 'com.zzz' version '1.0'
             }
             plugins {
                id 'com.aaa' version '2.0'
             }
          }
          """, """
          pluginManagement {
             plugins {
                id 'com.aaa' version '2.0'
                id 'com.zzz' version '1.0'
             }
          }
          """)
        );
    }

    @Test
    void onePluginsBlockKotlin() {
        rewriteRun(buildGradleKts("""
          plugins {
              java
          }
          """
        ));
    }

    @Test
    void multiplePluginsBlocksKotlin() {
        rewriteRun(buildGradleKts("""
          plugins {
              java
          }
          plugins {
              id("com.example")
          }
          """, """
          plugins {
              id("com.example")
              java
          }
          """)
        );
    }

    @Test
    void sortByPluginIdRatherThanByVersionKotlin() {
        rewriteRun(buildGradleKts("""
          plugins {
              id("com.zzz") version "1.0"
          }
          plugins {
              id("com.aaa")
          }
          """, """
          plugins {
              id("com.aaa")
              id("com.zzz") version "1.0"
          }
          """)
        );
    }

    /**
     * The Kotlin shorthands sort under the name they are written with rather than under the id of the plugin they
     * stand for, {@code kotlin("jvm")} being {@code org.jetbrains.kotlin.jvm}.
     */
    @Test
    void kotlinShorthandDeclaration() {
        rewriteRun(buildGradleKts("""
          plugins {
              kotlin("jvm") version "1.9.0"
          }
          plugins {
              id("com.aaa")
          }
          """, """
          plugins {
              id("com.aaa")
              kotlin("jvm") version "1.9.0"
          }
          """)
        );
    }

    @Test
    void threeBlocksWithCommentsKotlin() {
        rewriteRun(buildGradleKts("""
          plugins {
              // the zzz plugin
              id("com.zzz")
          }
          // the aaa plugins
          plugins {
              id("com.aaa")
          }
          // the mmm plugins
          plugins {
              id("com.mmm")
          }

          repositories {
              mavenCentral()
          }
          """, """
          plugins {
              id("com.aaa")
              id("com.mmm")
              // the zzz plugin
              id("com.zzz")
          }
          // the aaa plugins
          // the mmm plugins

          repositories {
              mavenCentral()
          }
          """)
        );
    }
}
