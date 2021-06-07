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
package org.openrewrite.java

import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.DebugOnly
import org.openrewrite.java.cleanup.*
import org.openrewrite.java.format.*
import org.openrewrite.java.search.*
import org.openrewrite.java.security.SecureTempFileCreationTest
import org.openrewrite.java.security.XmlParserXXEVulnerabilityTest
import org.openrewrite.java.style.AutodetectTest
import org.openrewrite.java.tree.TypeTreeTest

//----------------------------------------------------------------------------------------------
// If test classes are added here, they should also be added to JavaVisitorCompatibilityKit.kt
// Tests are in alphabetical order.
//----------------------------------------------------------------------------------------------
@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AddImportTest : Java17Test, AddImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AddLicenseHeaderTest : Java17Test, AddLicenseHeaderTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AnnotationTemplateGeneratorTest : Java17Test, AnnotationTemplateGeneratorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17AutodetectTest : Java17Test, AutodetectTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17BigDecimalRoundingConstantsToEnumsTestTest : Java17Test, BigDecimalRoundingConstantsToEnumsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17BlankLinesTest : Java17Test, BlankLinesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17BlockStatementTemplateGeneratorTest : Java17Test, BlockStatementTemplateGeneratorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeFieldNameTest : Java17Test, ChangeFieldNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeFieldTypeTest : Java17Test, ChangeFieldTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeLiteralTest : Java17Test, ChangeLiteralTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeMethodNameTest : Java17Test, ChangeMethodNameTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeMethodTargetToStaticTest : Java17Test, ChangeMethodTargetToStaticTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeMethodTargetToVariableTest : Java17Test, ChangeMethodTargetToVariableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangePackageTest : Java17Test, ChangePackageTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ChangeTypeTest : Java17Test, ChangeTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17CovariantEqualsTest : Java17Test, CovariantEqualsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17DefaultComesLastTest : Java17Test, DefaultComesLastTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17DeleteMethodArgumentTest : Java17Test, DeleteMethodArgumentTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17DeleteStatementTest : Java17Test, DeleteStatementTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17EmptyBlockTest : Java17Test, EmptyBlockTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17EqualsAvoidsNullTest : Java17Test, EqualsAvoidsNullTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ExplicitInitializationTest : Java17Test, ExplicitInitializationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FallThroughTest : Java17Test, FallThroughTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FinalizeLocalVariablesTest : Java17Test, FinalizeLocalVariablesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17StaticMethodNotFinalTest : Java17Test, StaticMethodNotFinalTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindAnnotationsTest : Java17Test, FindAnnotationsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindFieldsTest : Java17Test, FindFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindInheritedFieldsTest : Java17Test, FindInheritedFieldsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindMethodsTest : Java17Test, FindMethodsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindTextTest : Java17Test, FindTextTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17FindTypesTest : Java17Test, FindTypesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17HiddenFieldTest : Java17Test, HiddenFieldTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17HideUtilityClassConstructorTest : Java17Test, HideUtilityClassConstructorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ImplementInterfaceTest : Java17Test, ImplementInterfaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17JavaTemplateTest : Java17Test, JavaTemplateTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17JavaTemplateSubstitutionsTest : Java17Test, JavaTemplateSubstitutionsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17JavaVisitorTest : Java17Test, JavaVisitorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MaybeUsesImportTest : Java17Test, MaybeUsesImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MethodNameCasingTest : Java17Test, MethodNameCasingTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17MinimumViableSpacingTest : Java17Test, MinimumViableSpacingTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ModifierOrderTest : Java17Test, ModifierOrderTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17NormalizeFormatTest : Java17Test, NormalizeFormatTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17OrderImportsTest : Java17Test, OrderImportsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17PrimitiveWrapperClassConstructorToValueOfTest : Java17Test, PrimitiveWrapperClassConstructorToValueOfTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RedundantFileCreationTest : Java17Test, RedundantFileCreationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveAnnotationTest : Java17Test, RemoveAnnotationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveTrailingWhitespaceTest : Java17Test, RemoveTrailingWhitespaceTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RemoveUnusedImportsTest : Java17Test, RemoveUnusedImportsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RenameVariableTest : Java17Test, RenameVariableTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ReorderMethodArgumentsTest : Java17Test, ReorderMethodArgumentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17ResultOfMethodCallIgnoredTest : Java17Test, ResultOfMethodCallIgnoredTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SecureTempFileCreationTest : Java17Test, SecureTempFileCreationTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SemanticallyEqualTest : Java17Test, SemanticallyEqualTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SimplifyBooleanExpressionTest : Java17Test, SimplifyBooleanExpressionTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SimplifyBooleanReturnTest : Java17Test, SimplifyBooleanReturnTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17RenameLocalVariablesToCamelCaseTest : Java17Test, RenameLocalVariablesToCamelCaseTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17SpacesTest : Java17Test, SpacesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17TabsAndIndentsTest : Java17Test, TabsAndIndentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17TypeTreeTest : Java17Test, TypeTreeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UnnecessaryExplicitTypeArgumentsTest : Java17Test, UnnecessaryExplicitTypeArgumentsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UnnecessaryParenthesesTest : Java17Test, UnnecessaryParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UnnecessaryThrowsTest : Java17Test, UnnecessaryThrowsTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UnwrapParenthesesTest : Java17Test, UnwrapParenthesesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UseDiamondOperatorTest : Java17Test, UseDiamondOperatorTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UseStaticImportTest : Java17Test, UseStaticImportTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UsesMethodTest : Java17Test, UsesMethodTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17UsesTypeTest : Java17Test, UsesTypeTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17WrappingAndBracesTest : Java17Test, WrappingAndBracesTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java17XmlParserXXEVulnerabilityTest : Java17Test, XmlParserXXEVulnerabilityTest
