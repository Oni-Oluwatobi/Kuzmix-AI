package com.kuzmix.ai

import com.kuzmix.ai.domain.ai.MathSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the flagship requirement: solve equations in ANY format the
 * textbook uses ("1x - 2 = 32") and answer the student's own variant
 * ("2x - 3 = 34"), with teaching steps, factorisation, quadratics and
 * auto-marking of practice replies.
 */
class MathSolverTest {

    // ------------------------------------------------------------ detection

    @Test
    fun detectsEquations_ofEveryCommonShape() {
        assertTrue(MathSolver.isEquation("2x - 3 = 34"))
        assertTrue(MathSolver.isEquation("1x - 2 = 32"))
        assertTrue(MathSolver.isEquation("what is x + 7 = 12"))
        assertTrue(MathSolver.isEquation("2(x - 3) = 4x + 1"))
        assertTrue(MathSolver.isEquation("(x+1)/2 = 3"))
        assertTrue(MathSolver.isEquation("x^2 - 5x + 6 = 0"))
        assertTrue(MathSolver.isEquation("solve for y: 2y + 4 = 10"))
    }

    @Test
    fun rejectsNonEquations() {
        assertFalse(MathSolver.isEquation("12 * 8"))
        assertFalse(MathSolver.isEquation("what is photosynthesis"))
        assertFalse(MathSolver.isEquation("give me practice questions on biology"))
        assertFalse(MathSolver.isEquation("2x + 3"))          // no '='
        assertFalse(MathSolver.isEquation("the answer is 42"))
    }

    @Test
    fun factorRequestRequiresExpressionNotJustKeyword() {
        assertTrue(MathSolver.isFactorRequest("factorise x^2 + 5x + 6"))
        assertTrue(MathSolver.isFactorRequest("factor x2 + 5x + 6"))  // digit shorthand
        assertFalse(MathSolver.isFactorRequest("factorise 12"))
        assertFalse(MathSolver.isFactorRequest("expand my knowledge"))
    }

    // -------------------------------------------------------------- linear

    @Test
    fun solvesTextbookLinear_1xMinus2Equals32() {
        val s = MathSolver.solve("1x - 2 = 32")
        assertNotNull(s)
        assertEquals("x = 34", s!!.answer)
        assertEquals(34.0, s.roots[0], 1e-9)
        assertTrue(s.steps.size >= 3)                       // teaching steps, not just the root
        assertTrue(s.steps.any { it.contains("Check") })    // verification included
    }

    @Test
    fun solvesStudentVariant_2xMinus3Equals34() {
        val s = MathSolver.solve("2x - 3 = 34")
        assertNotNull(s)
        assertEquals("x = 18.5", s!!.answer)
        assertEquals(18.5, s.roots[0], 1e-9)
    }

    @Test
    fun solvesSimpleAndImplicitMultiplication() {
        assertEquals("x = 5", MathSolver.solve("x + 7 = 12")!!.answer)
        assertEquals("x = 5", MathSolver.solve("3x + 4 = 19")!!.answer)
        assertEquals("x = 4", MathSolver.solve("5x = 20")!!.answer)
    }

    @Test
    fun solvesParenthesesOnBothSides() {
        // 2(x - 3) = 4x + 1 -> 2x - 6 = 4x + 1 -> -2x = 7 -> x = -3.5
        val s = MathSolver.solve("2(x - 3) = 4x + 1")
        assertNotNull(s)
        assertEquals(-3.5, s!!.roots[0], 1e-9)
    }

    @Test
    fun solvesDivisionForm() {
        // (x + 1)/2 = 3 -> x + 1 = 6 -> x = 5
        assertEquals("x = 5", MathSolver.solve("(x+1)/2 = 3")!!.answer)
    }

    @Test
    fun solvesAnyVariable() {
        val s = MathSolver.solve("2y + 4 = 10")
        assertNotNull(s)
        assertEquals("y", s!!.variable)
        assertEquals("y = 3", s.answer)
    }

    @Test
    fun detectsIdentityAndContradiction() {
        val id = MathSolver.solve("2x + 3 = 2x + 3")
        assertNotNull(id)
        assertEquals(MathSolver.Kind.IDENTITY, id!!.kind)
        val no = MathSolver.solve("2x + 3 = 2x + 5")
        assertNotNull(no)
        assertEquals(MathSolver.Kind.CONTRADICTION, no!!.kind)
    }

    // ------------------------------------------------------------ quadratic

    @Test
    fun solvesQuadraticWithTwoRoots() {
        val s = MathSolver.solve("x^2 - 5x + 6 = 0")
        assertNotNull(s)
        assertEquals(MathSolver.Kind.QUADRATIC, s!!.kind)
        assertEquals(listOf(2.0, 3.0), s.roots.sorted())
        assertTrue(s.steps.any { it.contains("Discriminant") })
        assertTrue(s.steps.any { it.contains("Quadratic formula") })
    }

    @Test
    fun solvesQuadraticNegativeRoots() {
        val s = MathSolver.solve("x^2 + 5x + 6 = 0")
        assertNotNull(s)
        assertEquals(listOf(-3.0, -2.0), s!!.roots.sorted())
    }

    @Test
    fun factorisesMonicQuadratic() {
        val s = MathSolver.factorize("x^2 + 5x + 6")
        assertNotNull(s)
        assertEquals("(x + 2)(x + 3)", s!!.answer)
        assertEquals(listOf(-3.0, -2.0), s.roots.sorted())
    }

    @Test
    fun factorRequestRoutesThroughSolver() {
        val s = MathSolver.factorize("factorise x^2 - 5x + 6")
        assertNotNull(s)
        assertEquals(listOf(2.0, 3.0), s!!.roots.sorted())
    }

    // ------------------------------------------------------ practice + mark

    @Test
    fun generatedLinearProblem_hasIntegerAnswerAndMarksAllReplyShapes() {
        repeat(20) {
            val p = MathSolver.generateSimilar(MathSolver.Kind.LINEAR, "x")
            assertNotNull(p)
            val answerValue = Regex("-?\\d+(?:\\.\\d+)?").find(p!!.answer)!!.value.toDouble()

            assertTrue(MathSolver.isCorrectAnswer(p.answer, p))       // "x = 18"
            assertTrue(MathSolver.isCorrectAnswer("$answerValue", p)) // "18"
            assertTrue(MathSolver.isCorrectAnswer("x=$answerValue", p))
            assertTrue(MathSolver.isCorrectAnswer("the answer is $answerValue", p))
            assertFalse(MathSolver.isCorrectAnswer("${answerValue + 7}", p))
            assertFalse(MathSolver.isCorrectAnswer("I do not know", p))
        }
    }

    @Test
    fun generatedQuadraticProblem_acceptsEitherRoot() {
        val p = MathSolver.generateSimilar(MathSolver.Kind.QUADRATIC, "x")
        assertNotNull(p)
        val roots = Regex("-?\\d+").findAll(p!!.answer).map { it.value.toDouble() }.toList()
        assertEquals(2, roots.size)
        assertTrue(MathSolver.isCorrectAnswer("x = ${roots[0]}", p))
        assertTrue(MathSolver.isCorrectAnswer("x = ${roots[1]}", p))
        assertFalse(MathSolver.isCorrectAnswer("999", p))
    }

    // ------------------------------------------------------------ extraction

    @Test
    fun extractEquationKeepsRawMath() {
        assertEquals("2x - 3 = 34", MathSolver.extractEquation("what is 2x - 3 = 34"))
        assertNull(MathSolver.extractEquation("what is photosynthesis"))
    }
}
