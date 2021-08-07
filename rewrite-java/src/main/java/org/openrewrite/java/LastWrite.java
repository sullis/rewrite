/*
 * Copyright 2021 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.marker.JavaSearchResult;
import org.openrewrite.java.tree.J;

import java.util.HashMap;
import java.util.Map;

public class LastWrite extends Recipe {
    @Override
    public String getDisplayName() {
        return "Last write data flow analysis";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            int variableCounter = 0;

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
                getCursor().dropParentUntil(J.Block.class::isInstance)
                        .computeMessageIfAbsent("variables", v -> new HashMap<String, Integer>())
                        .put(variable.getSimpleName(), ++variableCounter);
                J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, executionContext);
                v = v.withMarkers(v.getMarkers().addIfAbsent(new JavaSearchResult(LastWrite.this, "definition of " + variableCounter)));
                return v;
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext executionContext) {
                J.Assignment a = super.visitAssignment(assignment, executionContext);
                if (a.getVariable() instanceof J.Identifier) {
                    Map<String, Integer> variableIds = getCursor().getNearestMessage("variables");
                    if (variableIds != null) {
                        Integer id = variableIds.get(((J.Identifier) a.getVariable()).getSimpleName());
                        if(id != null) {
                            a = a.withAssignment(a.getAssignment().withMarkers(a.getMarkers()
                                    .addIfAbsent(new JavaSearchResult(LastWrite.this, "write of " + id))));
                        }
                    }
                }
                return a;
            }
        };
    }
}
