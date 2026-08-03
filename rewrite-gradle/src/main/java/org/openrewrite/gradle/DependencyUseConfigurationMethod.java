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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;

import java.util.List;
import java.util.regex.Pattern;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class DependencyUseConfigurationMethod extends Recipe {

    private static final Pattern CONFIGURATION_NAME = Pattern.compile("[a-zA-Z_$][a-zA-Z0-9_$]*");

    final String displayName = "Use the configuration method to declare a dependency";

    final String description = "Replaces `DependencyHandler.add(configuration, notation)` calls inside a " +
            "`dependencies` block with the equivalent configuration method call, so that " +
            "`add('testImplementation', platform(\"org.junit:junit-bom:6.1.2\"))` becomes " +
            "`testImplementation platform(\"org.junit:junit-bom:6.1.2\")`. The configuration method form is the " +
            "idiomatic way to declare dependencies, and it is what dependency-aware recipes and Gradle's own " +
            "documentation expect. Whether the resulting call is parenthesized follows the style of the other " +
            "declarations in the same block.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                // Cheap rejections first; only walk ancestors for something already shaped like `add(name, notation)`
                if (m.getSelect() != null || !"add".equals(m.getSimpleName())) {
                    return m;
                }
                List<Expression> args = m.getArguments();
                // `add(configuration, notation)`, optionally with a trailing configuration closure
                if (args.size() < 2 || args.size() > 3 || !(args.get(0) instanceof J.Literal)) {
                    return m;
                }
                if (args.size() == 3 && !(args.get(2) instanceof J.Lambda)) {
                    return m;
                }
                Object configuration = ((J.Literal) args.get(0)).getValue();
                // A configuration name that is not a plain identifier cannot be called as a method
                if (!(configuration instanceof String) || !CONFIGURATION_NAME.matcher((String) configuration).matches()) {
                    return m;
                }
                if (!withinDependencies(getCursor())) {
                    return m;
                }
                if (getCursor().firstEnclosing(K.CompilationUnit.class) != null && withinProjectIteration(getCursor())) {
                    return m;
                }

                String configurationName = (String) configuration;
                JavaType.Method methodType = m.getMethodType() == null ? null : m.getMethodType().withName(configurationName);
                boolean omitParentheses = blockOmitsParentheses(getCursor());
                return m.withName(m.getName().withSimpleName(configurationName).withType(methodType))
                        .withMethodType(methodType)
                        .withArguments(ListUtils.map(args.subList(1, args.size()), (i, arg) -> {
                            if (i > 0) {
                                // The trailing closure keeps whatever spacing it already had
                                return arg;
                            }
                            // `OmitParentheses` lives on the argument element rather than the invocation, and the
                            // notation needs a leading space only when it is not wrapped in parentheses
                            return omitParentheses ?
                                    arg.withPrefix(Space.SINGLE_SPACE)
                                            .withMarkers(arg.getMarkers().addIfAbsent(new OmitParentheses(randomId()))) :
                                    arg.withPrefix(Space.EMPTY)
                                            .withMarkers(arg.getMarkers().removeByType(OmitParentheses.class));
                        }));
            }
        });
    }

    private static boolean withinDependencies(Cursor cursor) {
        return withinBlockNamed(cursor, "dependencies");
    }

    /**
     * The Kotlin DSL generates the type-safe configuration accessors from the plugins applied to the project the
     * script belongs to, and only for that project. Inside `subprojects`/`allprojects` the `DependencyHandler` the
     * block configures has no `testImplementation(...)` method, so `add("testImplementation", ...)` is the only form
     * that compiles. Groovy resolves the configuration method dynamically, so it is unaffected.
     */
    private static boolean withinProjectIteration(Cursor cursor) {
        return withinBlockNamed(cursor, "subprojects") || withinBlockNamed(cursor, "allprojects");
    }

    private static boolean withinBlockNamed(Cursor cursor, String name) {
        Cursor c = cursor.getParent();
        while (c != null) {
            if (c.getValue() instanceof J.MethodInvocation && name.equals(((J.MethodInvocation) c.getValue()).getSimpleName())) {
                return true;
            }
            c = c.getParent();
        }
        return false;
    }

    /**
     * Groovy build scripts customarily declare dependencies in the parenthesis-free command syntax, while the Kotlin
     * DSL cannot express it at all. Rather than impose one style, match the sibling declarations of the block being
     * modified, defaulting to parentheses when the block offers no example to follow.
     */
    private static boolean blockOmitsParentheses(Cursor cursor) {
        J.Block block = cursor.firstEnclosing(J.Block.class);
        if (block == null) {
            return false;
        }
        for (Statement statement : block.getStatements()) {
            if (statement instanceof J.MethodInvocation) {
                J.MethodInvocation sibling = (J.MethodInvocation) statement;
                if (sibling.getSelect() == null && !sibling.getArguments().isEmpty() &&
                        sibling.getArguments().get(0).getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }
}
