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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Preconditions.or;

@Value
@EqualsAndHashCode(callSuper = false)
public class MergePluginsBlocks extends Recipe {

    String displayName = "Merge `plugins` blocks";

    String description = "Gradle accepts more than one `plugins` block in the same scope, leaving the plugins a build " +
                         "applies spread over several places. Merges them into the first of them and sorts the " +
                         "declarations alphabetically by plugin id, so that the whole set can be read at once. " +
                         "Plugins are applied in the order they are declared, so a build that relies on that order " +
                         "rather than on the plugins reacting to one another can change behaviour.";

    /**
     * Plugin ids that could not be read are sorted last, keeping them in the order they were written relative to
     * one another rather than guessing at a position for them.
     */
    private static final Comparator<Statement> BY_PLUGIN_ID =
            comparing(MergePluginsBlocks::pluginId, nullsLast(naturalOrder()));

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(or(new IsBuildGradle<>(), new IsSettingsGradle<>()), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                J j = super.visit(tree, ctx);
                // A Groovy script holds its statements on the compilation unit, so they are unreachable from
                // visitBlock. A Kotlin script instead wraps them all in a single block, which visitBlock handles.
                if (j instanceof G.CompilationUnit) {
                    G.CompilationUnit c = (G.CompilationUnit) j;
                    Merged merged = merge(c.getStatements(), c.getEof());
                    // Nothing follows the end of a file, so a comment landing there needs no line break after it.
                    return merged == null ? c : c.withStatements(merged.statements).withEof(merged.trailer);
                }
                return j;
            }

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                Merged merged = merge(b.getStatements(), b.getEnd());
                return merged == null ? b : b.withStatements(merged.statements)
                        .withEnd(endLine(merged.trailer, lineStart(b.getEnd())));
            }
        });
    }

    /**
     * Folds every {@code plugins} block among a list of sibling statements into the first of them. Returns
     * {@code null} when there is nothing to merge, so that callers leave their tree untouched.
     *
     * @param trailer the space closing the enclosing scope, which receives any comment orphaned by a removed block
     *                that had nothing following it.
     */
    private static @Nullable Merged merge(List<Statement> statements, Space trailer) {
        Set<Integer> blocks = new HashSet<>();
        int first = -1;
        for (int i = 0; i < statements.size(); i++) {
            if (pluginsBlock(statements.get(i)) != null) {
                blocks.add(i);
                if (first < 0) {
                    first = i;
                }
            }
        }
        if (blocks.size() < 2) {
            return null;
        }

        Space blockPrefix = statements.get(first).getPrefix();
        J.MethodInvocation target = requireNonNull(pluginsBlock(statements.get(first)));
        J.Block body = body(target);
        List<Statement> plugins = new ArrayList<>(body.getStatements());
        // The space before a block's closing brace holds any comment trailing its last plugin, so it has to travel
        // with the plugins rather than stay behind with the brace being discarded.
        Space end = body.getEnd();
        for (int i = first + 1; i < statements.size(); i++) {
            if (!blocks.contains(i)) {
                continue;
            }
            J.Block nextBody = body(requireNonNull(pluginsBlock(statements.get(i))));
            List<Statement> next = nextBody.getStatements();
            if (next.isEmpty()) {
                end = endLine(concat(end, nextBody.getEnd()), lineStart(blockPrefix));
            } else {
                plugins.add(next.get(0).withPrefix(concat(end, next.get(0).getPrefix())));
                plugins.addAll(next.subList(1, next.size()));
                end = nextBody.getEnd();
            }
        }

        J.MethodInvocation merged = withBody(target,
                onSeparateLines(body.withStatements(sorted(plugins)).withEnd(end), blockPrefix));

        List<Statement> result = new ArrayList<>(statements.size());
        Space orphaned = null;
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            if (i != first && blocks.contains(i)) {
                // Only the prefix of a discarded block outlives it, carrying whatever comment was written above it
                // onto the next statement that survives.
                orphaned = orphaned == null ? statement.getPrefix() : concat(orphaned, statement.getPrefix());
                continue;
            }
            if (i == first) {
                statement = withPluginsBlock(statement, merged);
            }
            if (orphaned != null) {
                Space prefix = concat(orphaned, statement.getPrefix());
                statement = statement.withPrefix(endLine(prefix, lineStart(statement.getPrefix())));
                orphaned = null;
            }
            result.add(statement);
        }
        return new Merged(result, orphaned == null ? trailer : concat(orphaned, trailer));
    }

    /**
     * Sorts plugin declarations alphabetically. Each declaration keeps its own prefix, so that a comment written
     * above it travels with it. Only the leading whitespace is swapped between the declaration that was written
     * first and the one that now is, to keep the line the block opens on indented as it was.
     */
    private static List<Statement> sorted(List<Statement> plugins) {
        if (plugins.size() < 2) {
            return plugins;
        }
        List<Statement> sorted = new ArrayList<>(plugins);
        sorted.sort(BY_PLUGIN_ID);

        Statement originalFirst = plugins.get(0);
        if (sorted.get(0) == originalFirst) {
            return sorted;
        }
        // Located by identity before either prefix is rewritten, since two declarations differing only in prefix
        // are equal to one another.
        int displaced = 0;
        while (sorted.get(displaced) != originalFirst) {
            displaced++;
        }

        Space newFirstPrefix = sorted.get(0).getPrefix();
        sorted.set(0, sorted.get(0).withPrefix(newFirstPrefix.withWhitespace(originalFirst.getPrefix().getWhitespace())));
        sorted.set(displaced, originalFirst.withPrefix(originalFirst.getPrefix().withWhitespace(newFirstPrefix.getWhitespace())));
        return sorted;
    }

    /**
     * Gives every declaration but the first a line of its own. Groovy and Kotlin alike need a line break between
     * two statements, so declarations lifted out of a block written on a single line would otherwise run together
     * into one unparseable statement.
     *
     * @param blockPrefix the prefix of the {@code plugins} block itself, which supplies the indentation to fall
     *                    back on when every block was written on a single line and none has any to copy.
     */
    private static J.Block onSeparateLines(J.Block body, Space blockPrefix) {
        List<Statement> plugins = body.getStatements();
        String indent = null;
        for (Statement plugin : plugins) {
            String separator = separator(plugin.getPrefix());
            int lineBreak = separator.lastIndexOf('\n');
            if (lineBreak >= 0) {
                indent = separator.substring(lineBreak);
                break;
            }
        }

        // With no indentation anywhere to copy, the merged block is opened up rather than left as a single line,
        // indenting its declarations one step past the line the block itself starts on.
        boolean openUp = indent == null;
        String lineStart = lineStart(blockPrefix);
        if (openUp) {
            indent = lineStart + "    ";
        }

        List<Statement> onOwnLines = new ArrayList<>(plugins);
        for (int i = 0; i < onOwnLines.size(); i++) {
            Space prefix = onOwnLines.get(i).getPrefix();
            // The declaration written first may share the line the block opens on, and only has to be moved off it
            // when a comment now stands in the way.
            Space moved = openUp || i > 0 ? onNewLine(prefix, indent) : endLine(prefix, indent);
            onOwnLines.set(i, onOwnLines.get(i).withPrefix(moved));
        }
        // Every declaration now sits on a line of its own, so the closing brace takes one too. A comment already
        // holds that line open, and moving the brace off it would leave the comment dangling at the end of a
        // declaration it does not belong to.
        Space end = body.getEnd();
        if (end.getComments().isEmpty() && !end.getWhitespace().contains("\n")) {
            end = end.withWhitespace(lineStart);
        }
        return body.withStatements(onOwnLines).withEnd(end);
    }

    /**
     * The whitespace that separates a space's last comment, or the space itself when it has none, from whatever
     * follows it. This, rather than the leading whitespace, is what decides which line the following element lands on.
     */
    private static String separator(Space space) {
        List<Comment> comments = space.getComments();
        return comments.isEmpty() ? space.getWhitespace() : comments.get(comments.size() - 1).getSuffix();
    }

    private static Space onNewLine(Space prefix, String indent) {
        if (separator(prefix).contains("\n")) {
            return prefix;
        }
        if (prefix.getComments().isEmpty()) {
            return prefix.withWhitespace(indent);
        }
        List<Comment> comments = new ArrayList<>(prefix.getComments());
        int last = comments.size() - 1;
        comments.set(last, comments.get(last).withSuffix(indent));
        return Space.build(prefix.getWhitespace(), comments);
    }

    /**
     * Breaks the line after a trailing comment that would otherwise swallow whatever now follows it. Merging can
     * put a comment written at the end of one line in front of something written on another, and a line comment
     * comments out the rest of its line. Spaces holding no comment are left as they were, so that nothing merely
     * written inline gets moved.
     */
    private static Space endLine(Space space, String indent) {
        return space.getComments().isEmpty() ? space : onNewLine(space, indent);
    }

    /**
     * The line break and indentation opening the line a prefix ends on, or a bare line break when the prefix holds
     * no line break of its own because what follows it begins the file.
     */
    private static String lineStart(Space prefix) {
        String whitespace = prefix.getWhitespace();
        int lineBreak = whitespace.lastIndexOf('\n');
        return lineBreak < 0 ? "\n" : whitespace.substring(lineBreak);
    }

    /**
     * Appends {@code second} to {@code first} as one space. When {@code first} ends in a comment, that comment gives
     * up its own suffix in favour of {@code second}'s leading whitespace; keeping both would leave behind the line
     * break that used to precede a closing brace as a blank line. Callers with something following the result pass
     * it through {@link #endLine} to restore a line break the comment turns out to still need.
     */
    private static Space concat(Space first, Space second) {
        if (first.getComments().isEmpty()) {
            return second;
        }
        List<Comment> comments = new ArrayList<>(first.getComments());
        int last = comments.size() - 1;
        comments.set(last, comments.get(last).withSuffix(second.getWhitespace()));
        comments.addAll(second.getComments());
        return Space.build(first.getWhitespace(), comments);
    }

    /**
     * The plugin a declaration applies, used as its sort key. Reads through the trailing calls of
     * {@code id 'x' version '1.0'}, which nest the {@code id} call as the select of the {@code version} call, and
     * covers the {@code alias(libs.plugins.x)} and Kotlin {@code kotlin("jvm")} spellings alongside a bare
     * {@code java} accessor. Returns {@code null} for anything it cannot read.
     */
    private static @Nullable String pluginId(Statement statement) {
        J j = withoutImplicitReturn(statement);
        while (j instanceof J.MethodInvocation && ((J.MethodInvocation) j).getSelect() != null) {
            j = requireNonNull(((J.MethodInvocation) j).getSelect());
        }
        if (j instanceof J.MethodInvocation) {
            for (Expression argument : ((J.MethodInvocation) j).getArguments()) {
                String id = name(argument);
                if (id != null) {
                    return id;
                }
            }
            return null;
        }
        return name(j);
    }

    private static @Nullable String name(J j) {
        if (j instanceof J.Literal) {
            Object value = ((J.Literal) j).getValue();
            return value instanceof String ? (String) value : null;
        }
        if (j instanceof J.Identifier) {
            return ((J.Identifier) j).getSimpleName();
        }
        if (j instanceof J.FieldAccess) {
            String target = name(((J.FieldAccess) j).getTarget());
            return target == null ? null : target + '.' + ((J.FieldAccess) j).getSimpleName();
        }
        return null;
    }

    private static J. @Nullable MethodInvocation pluginsBlock(Statement statement) {
        Statement unwrapped = withoutImplicitReturn(statement);
        if (!(unwrapped instanceof J.MethodInvocation)) {
            return null;
        }
        J.MethodInvocation m = (J.MethodInvocation) unwrapped;
        if (m.getSelect() != null || !"plugins".equals(m.getSimpleName()) || m.getArguments().size() != 1) {
            return null;
        }
        Expression argument = m.getArguments().get(0);
        if (!(argument instanceof J.Lambda) || !(((J.Lambda) argument).getBody() instanceof J.Block)) {
            return null;
        }
        return m;
    }

    private static J.Block body(J.MethodInvocation pluginsBlock) {
        return (J.Block) ((J.Lambda) pluginsBlock.getArguments().get(0)).getBody();
    }

    private static J.MethodInvocation withBody(J.MethodInvocation pluginsBlock, J.Block body) {
        J.Lambda lambda = (J.Lambda) pluginsBlock.getArguments().get(0);
        return pluginsBlock.withArguments(singletonList(lambda.withBody(body)));
    }

    private static Statement withPluginsBlock(Statement statement, J.MethodInvocation pluginsBlock) {
        if (statement instanceof J.Return) {
            return ((J.Return) statement).withExpression(pluginsBlock);
        }
        return pluginsBlock;
    }

    /**
     * Groovy gives the last statement of a closure an implicit {@code return}, which wraps a {@code plugins} block
     * written there. Kotlin has no such wrapper.
     */
    private static Statement withoutImplicitReturn(Statement statement) {
        if (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof Statement) {
            return (Statement) ((J.Return) statement).getExpression();
        }
        return statement;
    }

    @Value
    private static class Merged {
        List<Statement> statements;
        Space trailer;
    }
}
