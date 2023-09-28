/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

public class ReplaceConstantWithAnotherConstantTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("guava"));
    }

    @Test
    void replaceConstantInAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("com.google.common.base.Charsets.UTF_8", "com.constant.B.UTF_8")),
          java(
            """
              package com.constant;
              public class B {
                  public static final String UTF_8 = "UTF_8";
              }
              """
          ),
          java(
            """
              import com.google.common.base.Charsets;
              
              @SuppressWarnings(Charsets.UTF_8)
              class Test {
                  @SuppressWarnings(value = Charsets.UTF_8)
                  void foo() {
                      System.out.println("Annotation");
                  }
              }
              """,
            """
              import com.constant.B;
    
              @SuppressWarnings(B.UTF_8)
              class Test {
                  @SuppressWarnings(value = B.UTF_8)
                  void foo() {
                      System.out.println("Annotation");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("com.google.common.base.Charsets.UTF_8", "java.io.File.separator")),
          java(
            """
              import com.google.common.base.Charsets;

              import static com.google.common.base.Charsets.UTF_8;

              class Test {
                  Object o = Charsets.UTF_8;
                  void foo() {
                      System.out.println(UTF_8);
                  }
              }
              """,
            """
              import java.io.File;

              import static java.io.File.separator;

              class Test {
                  Object o = File.separator;
                  void foo() {
                      System.out.println(separator);
                  }
              }
              """
          )
        );
    }

    @Test
    void removeTopLevelClassImport() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo.Bar.Baz.QUX", "Test.FOO")),
          java(
            """
              package foo;

              public class Bar {
                  public enum Baz {
                      QUX
                  }
              }
              """
          ),
          java(
            """
              import foo.Bar;

              class Test {
                  static final String FOO = "foo";
                  Object o = Bar.Baz.QUX;
              }
              """,
            """
              class Test {
                  static final String FOO = "foo";
                  Object o = Test.FOO;
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3555")
    void replaceConstantForAnnotatedParameter() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("com.google.common.base.Charsets.UTF_8", "com.constant.B.UTF_8"))
            .typeValidationOptions(TypeValidation.builder().identifiers(false).build()),
          java(
            """
              package com.constant;
              public class B {
                  public static final String UTF_8 = "UTF_8";
              }
              """
          ),
          java(
            """
              import com.google.common.base.Charsets;
              
              class Test {
                  void foo(@SuppressWarnings(value = Charsets.UTF_8) String param) {
                      System.out.println(param);
                  }
              }
              """,
            """
              import com.constant.B;
              
              class Test {
                  void foo(@SuppressWarnings(value = B.UTF_8) String param) {
                      System.out.println(param);
                  }
              }
              """
          )
        );
    }
}
