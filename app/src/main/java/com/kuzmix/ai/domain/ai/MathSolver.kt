package com.kuzmix.ai.domain.ai

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Offline symbolic solver for the equations a Nigerian secondary-school
 * student actually meets (JSS1 - SS3): linear equations in one variable,
 * simple quadratics, factorisation and plain arithmetic.
 *
 * Design goals:
 *  - FORMAT-INSENSITIVE: "1x - 2 = 32", "2x - 3 = 34", "x + 7 = 12",
 *    "2(x - 3) = 4x + 1", "(x+1)/2 = 3", "3x^2 - 5x - 2 = 0" and
 *    "solve for y: 2y + 4 = 10" all parse to the same polynomial form.
 *  - TEACHING-FIRST: [solve] returns numbered steps written the way a
 *    teacher works on a board (move constants, then divide, then check),
 *    not just the root.
 *  - PURE: no Android dependencies so it is fully unit-testable.
 *
 * Internals: tokenize -> recursive-descent parse with implicit
 * multiplication -> polynomial (degree -> coefficient map) arithmetic ->
 * degree-based solution strategy.
 */
object MathSolver {

    // ---------------------------------------------------------------- kinds
    enum class Kind { LINEAR, QUADRATIC, IDENTITY, CONTRADICTION, FACTOR, UNSUPPORTED }

    data class Solution(
        val kind: Kind,
        val steps: List<String>,
        val answer: String,
        val variable: String,
        /** best-guess numeric root(s), for auto-marking typed answers */
        val roots: List<Double> = emptyList()
    )

    data class PracticeProblem(
        val question: String,
        val answer: String,
        val root: Double
    )

    // ------------------------------------------------------------ detection
    /** True when [text] looks like an equation we can solve (has '=' + a variable). */
    fun isEquation(text: String): Boolean {
        val cleaned = stripInstruction(text)
        if ('=' !in cleaned) return false
        if (!cleaned.any { it.isLetter() && it in "xyzabcdefghijklmnopqrstuvw" }) return false
        return try {
            val eq = parseEquation(cleaned) ?: return false
            val diff = eq.first - eq.second
            diff.degree in 1..2
        } catch (_: Exception) {
            false
        }
    }

    /** True when the user typed something like "factorise x^2 + 5x + 6". */
    fun isFactorRequest(text: String): Boolean {
        val t = text.lowercase()
        val keyword = Regex("\\b(factori[sz]e|factor|expand)\\b").containsMatchIn(t)
        if (!keyword) return false
        val expr = extractExpression(t) ?: return false
        // "^" (x^2) or the digit shorthand textbooks use (x2)
        return '^' in expr || Regex("[a-z][0-9]").containsMatchIn(expr)
    }

    // --------------------------------------------------------------- solving
    fun solve(raw: String): Solution? {
        val cleaned = stripInstruction(raw)
        val equation = try {
            parseEquation(cleaned)
        } catch (_: Exception) {
            null
        } ?: return null

        val variable = detectVariable(cleaned) ?: "x"
        val (left, right) = equation
        val diff = left - right
        val degree = diff.degree

        return when (degree) {
            1 -> solveLinear(variable, left, right, diff)
            2 -> solveQuadratic(variable, diff)
            0 -> if (diff.isZero())
                Solution(
                    Kind.IDENTITY,
                    listOf(
                        "Both sides are identical expressions.",
                        "Every value of $variable makes the two sides equal."
                    ),
                    "True for all values of $variable (identity)",
                    variable
                )
            else Solution(
                Kind.CONTRADICTION,
                listOf(
                    "Simplify both sides: ${renderPoly(diff, variable)} = 0",
                    "A number can never equal a different number."
                ),
                "No solution (contradiction)",
                variable
            )
            else -> null
        }
    }

    /** Factorise a quadratic expression, e.g. "x^2 + 5x + 6" -> "(x + 2)(x + 3)". */
    fun factorize(raw: String): Solution? {
        val expr = extractExpression(stripInstruction(raw)) ?: return null
        val variable = detectVariable(expr) ?: "x"
        val poly = try {
            parseExpression(expr)
        } catch (_: Exception) {
            null
        } ?: return null
        if (poly.degree != 2) return null
        val a = poly.coeff(2); val b = poly.coeff(1); val c = poly.coeff(0)
        if (a == 0.0) return null

        val steps = mutableListOf<String>()
        // monic integer factorisation first: x^2 + (p+q)x + pq
        if (a == 1.0 && b.isWhole() && c.isWhole()) {
            val bb = b.toInt(); val cc = c.toInt()
            val pair = findFactorPair(bb, cc)
            if (pair != null) {
                val (p, q) = pair
                steps += "Find two numbers that multiply to $cc and add to $bb: $p and $q."
                steps += "Write the factors: (${variable} + ${fmt(p)})(${variable} + ${fmt(q)}) = 0."
                return Solution(
                    Kind.FACTOR, steps,
                    "($variable + ${fmt(p)})($variable + ${fmt(q)})",
                    variable,
                    listOf(-p.toDouble(), -q.toDouble())
                )
            }
        }
        // general: a*x^2 + bx + c = a(x - r1)(x - r2) when roots are real
        val roots = quadraticRoots(a, b, c)
        if (roots == null) {
            steps += "Discriminant b^2 - 4ac = ${fmt(b * b - 4 * a * c)} is negative, so this quadratic does not factorise over the real numbers."
            return Solution(Kind.FACTOR, steps, "Does not factorise over real numbers", variable)
        }
        steps += "Solve ${renderPoly(poly, variable)} = 0 first to find the roots."
        return when (roots.size) {
            1 -> {
                steps += "Double root: $variable = ${fmt(roots[0])}."
                if (a == 1.0) {
                    val r = roots[0]
                    val sign = if (r >= 0) "+" else "-"
                    Solution(Kind.FACTOR, steps, "($variable - ${fmt(r)})($variable - ${fmt(r)})", variable, roots)
                        .let { it.copy(answer = "($variable ${sign} ${fmt(abs(r))})\u00b2".replace("()", "($variable ${sign} ${fmt(abs(r))})")) }
                } else Solution(Kind.FACTOR, steps, "${fmt(a)}($variable - ${fmt(r)})\u00b2", variable, roots)
            }
            else -> {
                val terms = roots.map { r ->
                    val sign = if (r >= 0) "-" else "+"
                    "$variable $sign ${fmt(abs(r))}"
                }
                val prefix = if (a == 1.0) "" else "${fmt(a)}"
                steps += "The roots are $variable = ${roots.joinToString(" and ") { fmt(it) }}."
                Solution(Kind.FACTOR, steps, "$prefix(${terms[0]})(${terms[1]})", variable, roots)
            }
        }
    }

    // --------------------------------------------------------------- linear
    private fun solveLinear(
        variable: String,
        left: Poly,
        right: Poly,
        diff: Poly
    ): Solution {
        val steps = mutableListOf<String>()
        steps += "Equation: ${renderPoly(left, variable)} = ${renderPoly(right, variable)}"

        // running state of each side: coefficient of the variable, constant
        var lv = left.coeff(1)
        var lc = left.coeff(0)
        var rv = right.coeff(1)
        var rc = right.coeff(0)

        // Step 1 - bring every variable term to the left side.
        if (rv != 0.0) {
            val term = renderTerm(abs(rv), variable)
            steps += if (rv > 0) {
                "Subtract $term from both sides (keep the variable on one side):"
            } else {
                "Add $term to both sides (keep the variable on one side):"
            }
            lv -= rv          // right side loses its variable term entirely
            rv = 0.0
            steps += "${renderPolySigned(lv, variable, lc)} = ${renderPolySigned(0.0, variable, rc)}"
        }

        // Step 2 - move the plain number away from the variable.
        if (lc != 0.0) {
            val term = fmt(abs(lc))
            steps += if (lc > 0) {
                "Subtract $term from both sides (move the number away from $variable):"
            } else {
                "Add $term to both sides (move the number away from $variable):"
            }
            rc -= lc          // subtract the left constant from BOTH sides
            lc = 0.0
            steps += "${renderPolySigned(lv, variable, lc)} = ${renderPolySigned(0.0, variable, rc)}"
        }

        // Step 3 - divide by the variable coefficient.
        if (lv != 1.0) {
            steps += "Divide both sides by ${fmt(lv)} (the coefficient of $variable):"
        }
        val root = rc / lv
        steps += "$variable = ${fmt(root)}"

        // Verification - substitute back.
        val checkL = left.eval(root)
        val checkR = right.eval(root)
        steps += "Check: substitute $variable = ${fmt(root)} into the original equation."
        steps += "${fmt(checkL)} = ${fmt(checkR)} \u2713"
        steps += "\u2705 It works - whatever you do to one side, you must do to the other."

        return Solution(Kind.LINEAR, steps, "$variable = ${fmt(root)}", variable, listOf(root))
    }

    // ------------------------------------------------------------ quadratic
    private fun solveQuadratic(variable: String, poly: Poly): Solution? {
        val a = poly.coeff(2); val b = poly.coeff(1); val c = poly.coeff(0)
        if (a == 0.0) return null
        val steps = mutableListOf<String>()
        steps += "Quadratic equation: ${renderPoly(poly, variable)} = 0"

        val disc = b * b - 4 * a * c
        steps += "Identify a = ${fmt(a)}, b = ${fmt(b)}, c = ${fmt(c)} (standard form ax\u00b2 + bx + c = 0)."
        steps += "Discriminant: b\u00b2 - 4ac = (${fmt(b)})\u00b2 - 4(${fmt(a)})(${fmt(c)}) = ${fmt(disc)}."

        val roots = quadraticRoots(a, b, c) ?: return Solution(
            Kind.QUADRATIC,
            steps + "Because the discriminant is negative, there is no real solution.",
            "No real solution",
            variable
        )

        if (roots.size == 1) {
            steps += "The discriminant is 0, so there is one repeated root."
            steps += "x = -b / 2a = ${fmt(-b)} / ${fmt(2 * a)} = ${fmt(roots[0])}"
            steps += "\u2705 Answer: $variable = ${fmt(roots[0])} (double root)"
            return Solution(Kind.QUADRATIC, steps, "$variable = ${fmt(roots[0])}", variable, roots)
        }

        steps += "Because the discriminant is positive, there are two real roots."
        steps += "Quadratic formula: $variable = (\u2212b \u00b1 \u221a(b\u00b2 \u2212 4ac)) / 2a"
        steps += "$variable = (\u2212(${fmt(b)}) \u00b1 \u221a${fmt(disc)}) / (2 \u00d7 ${fmt(a)})"
        steps += "\u221a${fmt(disc)} = ${fmt(kotlin.math.sqrt(disc))}"
        steps += "$variable = ${fmt(roots[0])}  or  $variable = ${fmt(roots[1])}"
        val check = roots.map { r ->
            val l = poly.eval(r)
            "$variable=${fmt(r)} \u2192 ${fmt(l)} \u2248 0 \u2713"
        }
        steps += "Check: ${check.joinToString(",  ")}"
        return Solution(
            Kind.QUADRATIC, steps,
            "$variable = ${fmt(roots[0])} or $variable = ${fmt(roots[1])}",
            variable, roots.sorted()
        )
    }

    // ------------------------------------------------- practice generation
    /**
     * Builds a fresh problem of the same shape as what the student just
     * solved, so the chat can say "Now try this one: ..." and mark it.
     */
    fun generateSimilar(kind: Kind, variable: String = "x"): PracticeProblem? {
        if (kind != Kind.LINEAR && kind != Kind.QUADRATIC) return null
        val rng = Random(System.nanoTime())
        if (kind == Kind.LINEAR) {
            // a*x + b = c, with integer solution
            val a = rng.nextInt(2, 10)
            val x = rng.nextInt(-9, 13)
            val b = rng.nextInt(-12, 13)
            val c = a * x + b
            val question = "Solve: ${fmt(a.toDouble())}$variable" +
                (if (b >= 0) " + ${fmt(b.toDouble())}" else " - ${fmt(-b.toDouble())}") +
                " = ${fmt(c.toDouble())}"
            return PracticeProblem(question, "$variable = ${fmt(x.toDouble())}", x.toDouble())
        }
        // quadratic with integer roots: (x - r1)(x - r2) = 0
        val r1 = rng.nextInt(-7, 8)
        var r2 = rng.nextInt(-7, 8)
        if (r2 == r1) r2 = r1 + rng.nextInt(1, 5)
        val sum = r1 + r2
        val prod = r1 * r2
        val question = "Solve: $variable\u00b2" +
            (if (sum >= 0) " + ${fmt(sum.toDouble())}$variable" else " - ${fmt(-sum.toDouble())}$variable") +
            (if (prod >= 0) " + ${fmt(prod.toDouble())}" else " - ${fmt(-prod.toDouble())}") + " = 0"
        return PracticeProblem(
            question,
            "$variable = ${fmt(r1.toDouble())} or $variable = ${fmt(r2.toDouble())}",
            r1.toDouble()
        )
    }

    /** Marks a student's typed reply against a problem (handles "x = 5", "5", "x=5 or x=2"). */
    fun isCorrectAnswer(reply: String, problem: PracticeProblem): Boolean {
        val numbers = Regex("-?\\d+(?:\\.\\d+)?").findAll(reply.replace("−", "-"))
            .map { it.value.toDoubleOrNull() }.filterNotNull().toList()
        if (numbers.isEmpty()) return false
        val rootMatches = numbers.any { n -> problem.root.isClose(n) }
        // quadratic: also accept the other root if mentioned
        val otherRoots = problem.answer.split(" or ").mapNotNull { part ->
            Regex("-?\\d+(?:\\.\\d+)?").find(part.replace("−", "-"))?.value?.toDoubleOrNull()
        }
        return rootMatches || (otherRoots.isNotEmpty() && otherRoots.any { o ->
            numbers.any { n -> o.isClose(n) }
        })
    }

    // ------------------------------------------------------------ extraction
    /** Pulls the bare equation/expression out of a chatty message. */
    fun extractEquation(text: String): String? {
        val cleaned = stripInstruction(text)
        if ('=' !in cleaned) return null
        return cleaned
    }

    private fun stripInstruction(text: String): String {
        var t = text.trim().lowercase()
        t = t.replace('−', '-')          // unicode minus
            .replace('–', '-')
            .replace('×', '*')
            .replace('÷', '/')
            .replace('＝', '=')
        t = t.replace(Regex("^\\s*(?:what\\s+is|whats|calculate|compute|work\\s+out|evaluate)\\s*"), "")
        t = t.replace(Regex("^\\s*solve(?:\\s+for\\s+[a-z])?\\s*[:\-]?\\s*"), "")
        t = t.replace(Regex("^\\s*(?:find|determine|simplify)\\s+(?:the\\s+value\\s+of\\s+)?"), "")
        t = t.replace(Regex("\\s*=\\s*what\\s*\\??$"), " =")
        t = t.replace(Regex("^\\s*the\\s+equation\\s+is\\s*"), "")
        t = t.replace("?", "")
        // keep letters (variables), digits and math symbols only
        t = t.replace(Regex("[^a-z0-9=+\\-*/^().\\s]"), "")
        return t.replace(Regex("\\s+"), " ").trim()
    }

    private fun extractExpression(text: String): String? {
        val t = stripInstruction(text)
        val noEq = t.substringBefore('=').trim()
        return noEq.ifBlank { null }
    }

    private fun detectVariable(text: String): String? {
        // prefer classic unknowns, then any stray letter that is not in a word
        val candidates = "xyzabcdefghijklmnopqrstuvw".toSet()
        val found = text.filter { it in candidates }.toSet()
        return found.firstOrNull() ?: text.firstOrNull { it.isLetter() }
    }

    // ---------------------------------------------------------------- algebra
    /** degree -> coefficient map, e.g. {1: 2.0, 0: -3.0} for 2x - 3. */
    internal class Poly(private val terms: Map<Int, Double>) {

        val degree: Int get() = terms.filterValues { it != 0.0 }.keys.maxOrNull() ?: -1

        fun coeff(d: Int): Double = terms[d] ?: 0.0

        fun isZero(): Boolean = terms.values.all { abs(it) < 1e-12 }

        operator fun plus(o: Poly): Poly {
            val out = terms.toMutableMap()
            for ((d, c) in o.terms) out[d] = (out[d] ?: 0.0) + c
            return Poly(out)
        }

        operator fun minus(o: Poly): Poly = this + (o * Poly(mapOf(0 to -1.0)))

        operator fun times(o: Poly): Poly {
            val out = HashMap<Int, Double>()
            for ((d1, c1) in terms) for ((d2, c2) in o.terms) {
                out[d1 + d2] = (out[d1 + d2] ?: 0.0) + c1 * c2
            }
            return Poly(out)
        }

        fun divideByConstant(k: Double): Poly = Poly(terms.mapValues { it.value / k })

        fun pow(n: Int): Poly {
            var result = Poly(mapOf(0 to 1.0))
            repeat(n) { result = result * this }
            return result
        }

        fun eval(x: Double): Double = terms.entries.sumOf { (d, c) -> c * Math.pow(x, d.toDouble()) }
    }

    private fun parseEquation(text: String): Pair<Poly, Poly>? {
        val parts = text.split('=')
        if (parts.size != 2) return null
        val left = parseExpression(parts[0]) ?: return null
        val right = parseExpression(parts[1]) ?: return null
        return left to right
    }

    internal fun parseExpression(text: String): Poly? = try {
        Parser(tokenize(text)).parseFull()
    } catch (_: Exception) {
        null
    }

    // -------------------------------------------------------------- tokenizer
    private sealed class Tok {
        data class Num(val v: Double) : Tok()
        data class Var(val name: String) : Tok()
        data class Op(val c: Char) : Tok()
    }

    private fun tokenize(s: String): List<Tok> {
        val out = mutableListOf<Tok>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || (c == '.' && i + 1 < s.length && s[i + 1].isDigit()) -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    out += Tok.Num(s.substring(start, i).toDouble())
                }
                c in "abcdefghijklmnopqrstuvwxyz" -> {
                    out += Tok.Var(c.toString())
                    i++
                }
                c in "+-*/^()" -> {
                    out += Tok.Op(c)
                    i++
                }
                else -> i++          // ignore unknown characters
            }
        }
        return out
    }

    private class Parser(private val toks: List<Tok>) {
        private var pos = 0

        fun parseFull(): Poly? {
            if (toks.isEmpty()) return null
            val v = parseExpr()
            return if (pos >= toks.size) v else null
        }

        private fun peek(): Tok? = toks.getOrNull(pos)

        private fun parseExpr(): Poly {
            var left = parseTerm()
            while (true) {
                val t = peek()
                if (t is Tok.Op && (t.c == '+' || t.c == '-')) {
                    pos++
                    val right = parseTerm()
                    left = if (t.c == '+') left + right else left - right
                } else break
            }
            return left
        }

        private fun parseTerm(): Poly {
            var left = parseUnary()
            while (true) {
                val t = peek()
                when {
                    t is Tok.Op && t.c == '*' -> {
                        pos++
                        left = left * parseUnary()
                    }
                    t is Tok.Op && t.c == '/' -> {
                        pos++
                        val right = parseUnary()
                        left = if (right.degree == 0 && right.coeff(0) != 0.0)
                            left.divideByConstant(right.coeff(0))
                        else return left   // variable divisor: stop (not polynomial)
                    }
                    // implicit multiplication: 2x, 3(x+1), (x+1)(x-1), x y
                    t is Tok.Num || t is Tok.Var || (t is Tok.Op && t.c == '(') -> {
                        left = left * parseUnary()
                    }
                    else -> return left
                }
            }
        }

        private fun parseUnary(): Poly {
            val t = peek()
            if (t is Tok.Op && t.c == '-') {
                pos++
                return parseUnary() * Poly(mapOf(0 to -1.0))
            }
            if (t is Tok.Op && t.c == '+') {
                pos++
                return parseUnary()
            }
            return parsePower()
        }

        private fun parsePower(): Poly {
            var base = parseAtom()
            val t = peek()
            if (t is Tok.Op && t.c == '^') {
                pos++
                val exponent = parseUnary()
                if (exponent.degree == 0) {
                    val n = exponent.coeff(0)
                    if (n >= 0 && n == Math.floor(n) && n <= 6) {
                        base = base.pow(n.toInt())
                    }
                }
            }
            return base
        }

        private fun parseAtom(): Poly {
            val t = peek() ?: throw IllegalStateException("unexpected end")
            when (t) {
                is Tok.Num -> {
                    pos++
                    return Poly(mapOf(0 to t.v))
                }
                is Tok.Var -> {
                    pos++
                    // 'e' or pi-free: every letter is a plain unknown
                    return Poly(mapOf(1 to 1.0))
                }
                is Tok.Op -> {
                    if (t.c == '(') {
                        pos++
                        val inner = parseExpr()
                        val close = peek()
                        if (close is Tok.Op && close.c == ')') pos++
                        return inner
                    }
                    throw IllegalStateException("bad token")
                }
            }
        }
    }

    // ---------------------------------------------------------------- helpers
    private fun quadraticRoots(a: Double, b: Double, c: Double): List<Double>? {
        val disc = b * b - 4 * a * c
        if (disc < -1e-9) return null
        val sqrtD = kotlin.math.sqrt(abs(disc))
        return if (abs(disc) < 1e-9) {
            listOf(round6(-b / (2 * a))).distinct()
        } else {
            listOf(round6((-b + sqrtD) / (2 * a)), round6((-b - sqrtD) / (2 * a)))
        }
    }

    private fun findFactorPair(p: Int, q: Int): Pair<Int, Int>? {
        for (i in -Math.abs(q).coerceAtLeast(1)..Math.abs(q).coerceAtLeast(1)) {
            if (i != 0 && q % i == 0) {
                val j = q / i
                if (i + j == p) return i to j
            }
        }
        if (q == 0) {
            if (p != 0) return 0 to p
        }
        return null
    }

    private fun renderPoly(p: Poly, variable: String): String {
        if (p.isZero()) return "0"
        val sb = StringBuilder()
        for (d in p.degree downTo 0) {
            val c = p.coeff(d)
            if (abs(c) < 1e-12) continue
            val sign = when {
                sb.isEmpty() -> if (c < 0) "-" else ""
                c < 0 -> " - "
                else -> " + "
            }
            sb.append(sign).append(renderTermBody(abs(c), d, variable))
        }
        return sb.toString().ifBlank { "0" }
    }

    private fun renderPolySigned(varCoef: Double, variable: String, const: Double): String {
        if (varCoef == 0.0 && const == 0.0) return "0"
        val sb = StringBuilder()
        if (varCoef != 0.0) sb.append(renderTerm(varCoef, variable))
        if (const != 0.0) {
            if (sb.isNotEmpty()) sb.append(if (const < 0) " - " else " + ")
            sb.append(fmt(abs(const)))
        } else if (sb.isEmpty()) {
            sb.append("0")
        }
        return sb.toString()
    }

    private fun renderTerm(coef: Double, variable: String): String = when {
        coef == 1.0 -> variable
        coef == -1.0 -> "-$variable"
        else -> renderTermBody(abs(coef), 1, variable).let { if (coef < 0) "-$it" else it }
    }

    private fun renderTermBody(absCoef: Double, degree: Int, variable: String): String = when (degree) {
        0 -> fmt(absCoef)
        1 -> if (absCoef == 1.0) variable else "${fmt(absCoef)}$variable"
        else -> if (absCoef == 1.0) "$variable\u00b2"
        else if (degree == 3) "${fmt(absCoef)}$variable\u00b3"
        else "${fmt(absCoef)}$variable^$degree"
    }

    /** 12 -> "12", 12.5 -> "12.5", 1/3 -> "0.333333" (bounded). */
    fun fmt(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return v.toString()
        val r = Math.round(v * 1_000_000.0) / 1_000_000.0
        return if (r == Math.floor(r) && abs(r) < 1e15) r.toLong().toString()
        else {
            val trimmed = r.toString()
            if (trimmed.length > 10) String.format("%.4f", r).trimEnd('0').trimEnd('.')
            else trimmed
        }
    }

    private fun Double.isWhole(): Boolean = abs(this - Math.floor(this)) < 1e-9

    private fun round6(v: Double): Double = Math.round(v * 1e6) / 1e6

    private infix fun Double.isClose(other: Double): Boolean = abs(this - other) < 0.01 || abs(this - other) < abs(this) * 1e-6
}
