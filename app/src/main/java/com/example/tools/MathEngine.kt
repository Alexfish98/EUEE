package com.example.tools

import kotlin.math.*

sealed class EvalResult {
    data class Success(val value: Double, val formatted: String) : EvalResult()
    data class Error(val message: String) : EvalResult()
}

data class QuadraticSolution(
    val discriminant: Double,
    val description: String,
    val vertexX: Double,
    val vertexY: Double
)

data class MatrixResult(
    val determinant: Double,
    val transpose: List<List<Double>>,
    val inverse: List<List<Double>>?
)

data class StatsSummary(
    val count: Int,
    val mean: Double,
    val median: Double,
    val mode: String,
    val min: Double,
    val max: Double,
    val range: Double,
    val popStdDev: Double,
    val sampleStdDev: Double,
    val variance: Double
)

object MathEngine {

    fun formatNumber(v: Double): String {
        if (v.isNaN()) return "NaN"
        if (v.isInfinite()) return if (v > 0) "∞" else "-∞"
        if (abs(v) < 1e-12) return "0"
        val rounded = v.toLong()
        if (abs(v - rounded.toDouble()) < 1e-10) return rounded.toString()
        return "%.6g".format(v).trimEnd('0').trimEnd('.')
    }

    /**
     * Evaluates a scientific expression string using recursive descent parsing.
     * Supports: +, -, *, /, ^, %, parentheses, variable x, constants pi/e,
     * and functions sin, cos, tan, asin, acos, atan, log, ln, sqrt, abs, exp.
     */
    fun evaluateExpression(expr: String, xValue: Double = 0.0, useDegrees: Boolean = true): EvalResult {
        val cleaned = expr.trim()
        if (cleaned.isEmpty()) return EvalResult.Error("Enter an expression.")
        return runCatching {
            val parser = ExprParser(cleaned, xValue, useDegrees)
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) {
                EvalResult.Error("Undefined result (division by zero or out of domain).")
            } else {
                EvalResult.Success(result, formatNumber(result))
            }
        }.getOrElse { ex ->
            EvalResult.Error(ex.message ?: "Invalid mathematical expression.")
        }
    }

    fun sampleGraphPoints(
        expr: String,
        xMin: Double,
        xMax: Double,
        steps: Int = 160,
        useDegrees: Boolean = false
    ): Result<List<Pair<Double, Double?>>> {
        if (xMin >= xMax) return Result.failure(IllegalArgumentException("xMin must be less than xMax"))
        val points = mutableListOf<Pair<Double, Double?>>()
        val dx = (xMax - xMin) / steps
        for (i in 0..steps) {
            val x = xMin + i * dx
            val y = when (val res = evaluateExpression(expr, xValue = x, useDegrees = useDegrees)) {
                is EvalResult.Success -> if (abs(res.value) > 1e6) null else res.value
                is EvalResult.Error -> null
            }
            points.add(x to y)
        }
        return if (points.all { it.second == null }) {
            Result.failure(IllegalArgumentException("Expression could not be evaluated over [$xMin, $xMax]."))
        } else {
            Result.success(points)
        }
    }

    fun solveLinear(a: Double, b: Double, c: Double): String {
        // ax + b = c
        return if (abs(a) < 1e-12) {
            if (abs(b - c) < 1e-12) "Infinite solutions (identity: 0 = 0)."
            else "No solution (contradiction: ${formatNumber(b)} ≠ ${formatNumber(c)})."
        } else {
            val x = (c - b) / a
            "x = ${formatNumber(x)}"
        }
    }

    fun solveQuadratic(a: Double, b: Double, c: Double): Result<QuadraticSolution> {
        if (abs(a) < 1e-12) {
            return Result.failure(IllegalArgumentException("Coefficient 'a' must be non-zero for a quadratic equation."))
        }
        val d = b * b - 4.0 * a * c
        val vx = -b / (2.0 * a)
        val vy = a * vx * vx + b * vx + c
        val desc = when {
            d > 1e-12 -> {
                val r1 = (-b + sqrt(d)) / (2.0 * a)
                val r2 = (-b - sqrt(d)) / (2.0 * a)
                "Two real roots: x₁ = ${formatNumber(r1)}, x₂ = ${formatNumber(r2)}"
            }
            abs(d) <= 1e-12 -> {
                val r = -b / (2.0 * a)
                "One repeated real root: x = ${formatNumber(r)}"
            }
            else -> {
                val real = -b / (2.0 * a)
                val imag = sqrt(-d) / (2.0 * a)
                "Complex conjugate roots: ${formatNumber(real)} ± ${formatNumber(abs(imag))}i"
            }
        }
        return Result.success(QuadraticSolution(d, desc, vx, vy))
    }

    fun solveSimultaneous2x2(
        a1: Double, b1: Double, c1: Double,
        a2: Double, b2: Double, c2: Double
    ): String {
        val det = a1 * b2 - a2 * b1
        if (abs(det) < 1e-12) {
            val detX = c1 * b2 - c2 * b1
            val detY = a1 * c2 - a2 * c1
            return if (abs(detX) < 1e-12 && abs(detY) < 1e-12) {
                "Infinitely many solutions (coincident lines)."
            } else {
                "No solution (parallel lines)."
            }
        }
        val x = (c1 * b2 - c2 * b1) / det
        val y = (a1 * c2 - a2 * c1) / det
        return "x = ${formatNumber(x)},  y = ${formatNumber(y)}"
    }

    fun analyzeMatrix2x2(m: List<List<Double>>): MatrixResult {
        val a = m[0][0]; val b = m[0][1]
        val c = m[1][0]; val d = m[1][1]
        val det = a * d - b * c
        val transpose = listOf(listOf(a, c), listOf(b, d))
        val inverse = if (abs(det) < 1e-12) null else listOf(
            listOf(d / det, -b / det),
            listOf(-c / det, a / det)
        )
        return MatrixResult(det, transpose, inverse)
    }

    fun computeStats(values: List<Double>): Result<StatsSummary> {
        if (values.isEmpty()) return Result.failure(IllegalArgumentException("Enter at least one number."))
        val sorted = values.sorted()
        val n = sorted.size
        val mean = sorted.sum() / n
        val median = if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        val freq = sorted.groupingBy { it }.eachCount()
        val maxFreq = freq.values.maxOrNull() ?: 1
        val mode = if (maxFreq == 1) "None (all unique)" else freq.filterValues { it == maxFreq }.keys.joinToString(", ") { formatNumber(it) }
        val sumSq = sorted.sumOf { (it - mean).pow(2) }
        val popVar = sumSq / n
        val sampleStd = if (n > 1) sqrt(sumSq / (n - 1)) else 0.0
        return Result.success(
            StatsSummary(
                count = n,
                mean = mean,
                median = median,
                mode = mode,
                min = sorted.first(),
                max = sorted.last(),
                range = sorted.last() - sorted.first(),
                popStdDev = sqrt(popVar),
                sampleStdDev = sampleStd,
                variance = popVar
            )
        )
    }

    /**
     * Parses a chemical formula (e.g., H2SO4, Ca(NO3)2, Al2(SO4)3) and computes molar mass in g/mol.
     */
    fun calculateMolarMass(formula: String): Result<Pair<Double, Map<String, Int>>> {
        val clean = formula.trim().replace(" ", "")
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Enter a chemical formula (e.g., H2SO4)."))
        return runCatching {
            val counts = parseFormulaCounts(clean)
            var total = 0.0
            for ((el, qty) in counts) {
                val mass = ReferenceData.atomicMassBySymbol[el]
                    ?: throw IllegalArgumentException("Unknown element symbol '$el'.")
                total += mass * qty
            }
            total to counts
        }
    }

    private fun parseFormulaCounts(formula: String): Map<String, Int> {
        val stack = ArrayDeque<MutableMap<String, Int>>()
        stack.addLast(mutableMapOf())
        var i = 0
        while (i < formula.length) {
            when (val ch = formula[i]) {
                '(' -> {
                    stack.addLast(mutableMapOf())
                    i++
                }
                ')' -> {
                    i++
                    val numStart = i
                    while (i < formula.length && formula[i].isDigit()) i++
                    val mult = if (i > numStart) formula.substring(numStart, i).toInt() else 1
                    if (stack.size <= 1) throw IllegalArgumentException("Mismatched parentheses.")
                    val popped = stack.removeLast()
                    val top = stack.last()
                    for ((k, v) in popped) top[k] = (top[k] ?: 0) + v * mult
                }
                in 'A'..'Z' -> {
                    val start = i
                    i++
                    while (i < formula.length && formula[i].isLowerCase()) i++
                    val symbol = formula.substring(start, i)
                    val numStart = i
                    while (i < formula.length && formula[i].isDigit()) i++
                    val count = if (i > numStart) formula.substring(numStart, i).toInt() else 1
                    val top = stack.last()
                    top[symbol] = (top[symbol] ?: 0) + count
                }
                else -> throw IllegalArgumentException("Unexpected character '$ch' in formula.")
            }
        }
        if (stack.size != 1) throw IllegalArgumentException("Unclosed parenthesis in formula.")
        return stack.last()
    }

    /**
     * Balances a chemical equation such as "Fe + O2 -> Fe2O3" or "C3H8 + O2 = CO2 + H2O"
     * using exact rational Gaussian elimination over the stoichiometric matrix.
     */
    fun balanceChemicalEquation(raw: String): Result<String> {
        val normalized = raw.replace("→", "->").replace("⇌", "->").replace("=", "->")
        val sides = normalized.split("->")
        if (sides.size != 2) return Result.failure(IllegalArgumentException("Use '->' or '=' to separate reactants and products."))
        val reactants = sides[0].split("+").map { it.trim() }.filter { it.isNotEmpty() }
        val products = sides[1].split("+").map { it.trim() }.filter { it.isNotEmpty() }
        if (reactants.isEmpty() || products.isEmpty()) {
            return Result.failure(IllegalArgumentException("Both reactants and products are required."))
        }

        return runCatching {
            val allSpecies = reactants + products
            val parsedSpecies = allSpecies.map { parseFormulaCounts(it.trimStart { ch -> ch.isDigit() }) }
            val elements = parsedSpecies.flatMap { it.keys }.distinct()
            val nCols = allSpecies.size
            val nRows = elements.size

            // Build rational matrix A * x = 0 (reactants +, products -)
            val mat = Array(nRows) { r ->
                Array(nCols) { c ->
                    val count = parsedSpecies[c][elements[r]] ?: 0
                    val sign = if (c < reactants.size) 1L else -1L
                    Fraction(sign * count, 1L)
                }
            }

            // RREF
            var pivotRow = 0
            val pivotCols = mutableListOf<Int>()
            for (col in 0 until nCols) {
                if (pivotRow >= nRows) break
                var sel = (pivotRow until nRows).firstOrNull { !mat[it][col].isZero() } ?: continue
                if (sel != pivotRow) {
                    val tmp = mat[sel]; mat[sel] = mat[pivotRow]; mat[pivotRow] = tmp
                }
                val pivotVal = mat[pivotRow][col]
                for (c in col until nCols) mat[pivotRow][c] = mat[pivotRow][c] / pivotVal
                for (r in 0 until nRows) {
                    if (r != pivotRow && !mat[r][col].isZero()) {
                        val factor = mat[r][col]
                        for (c in col until nCols) {
                            mat[r][c] = mat[r][c] - factor * mat[pivotRow][c]
                        }
                    }
                }
                pivotCols.add(col)
                pivotRow++
            }

            val freeCols = (0 until nCols).filter { it !in pivotCols }
            if (freeCols.size != 1) {
                throw IllegalArgumentException("Equation must have a unique integer balance (found ${freeCols.size} degrees of freedom).")
            }
            val freeCol = freeCols.first()
            val sol = Array(nCols) { Fraction(0, 1) }
            sol[freeCol] = Fraction(1, 1)
            for (r in pivotCols.indices) {
                sol[pivotCols[r]] = -mat[r][freeCol]
            }

            // Scale to positive integers
            var lcmDen = 1L
            for (f in sol) lcmDen = lcm(lcmDen, f.den)
            val ints = sol.map { (it.num * (lcmDen / it.den)) }
            val adjusted = if (ints.all { it < 0 }) ints.map { -it } else ints
            if (adjusted.any { it <= 0 }) {
                throw IllegalArgumentException("Could not find positive stoichiometric coefficients. Verify chemical formulas.")
            }
            val g = adjusted.reduce { acc, v -> gcd(acc, v) }
            val coeffs = adjusted.map { (it / g).toInt() }

            val left = reactants.indices.joinToString(" + ") { i ->
                val c = coeffs[i]
                (if (c == 1) "" else "$c") + reactants[i]
            }
            val right = products.indices.joinToString(" + ") { j ->
                val c = coeffs[reactants.size + j]
                (if (c == 1) "" else "$c") + products[j]
            }
            "$left → $right"
        }
    }

    private data class Fraction(val num: Long, val den: Long) {
        val normalized: Fraction by lazy {
            if (num == 0L) Fraction(0L, 1L)
            else {
                val sign = if (den < 0) -1L else 1L
                val g = gcd(abs(num), abs(den))
                Fraction((num * sign) / g, abs(den) / g)
            }
        }
        fun isZero() = num == 0L
        operator fun unaryMinus() = Fraction(-num, den)
        operator fun plus(o: Fraction) = Fraction(num * o.den + o.num * den, den * o.den).normalized
        operator fun minus(o: Fraction) = Fraction(num * o.den - o.num * den, den * o.den).normalized
        operator fun times(o: Fraction) = Fraction(num * o.num, den * o.den).normalized
        operator fun div(o: Fraction) = Fraction(num * o.den, den * o.num).normalized
    }

    private fun gcd(a: Long, b: Long): Long = if (b == 0L) abs(a) else gcd(b, a % b)
    private fun lcm(a: Long, b: Long): Long = if (a == 0L || b == 0L) 0L else abs(a / gcd(a, b) * b)

    private class ExprParser(
        private val input: String,
        private val xValue: Double,
        private val useDegrees: Boolean
    ) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            while (ch == ' '.code) nextChar()
            if (pos < input.length) throw IllegalArgumentException("Unexpected '${input[pos]}'")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) || eat('−'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) || eat('×'.code) -> x *= parseFactor()
                    eat('/'.code) || eat('÷'.code) -> {
                        val d = parseFactor()
                        if (d == 0.0) throw ArithmeticException("Division by zero.")
                        x /= d
                    }
                    eat('%'.code) -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code) || eat('−'.code)) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing closing ')'")
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code || ch == 'E'.code) {
                    if (ch == 'E'.code) {
                        nextChar()
                        if (ch == '+'.code || ch == '-'.code) nextChar()
                    } else nextChar()
                }
                x = input.substring(startPos, pos).toDouble()
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                val name = input.substring(startPos, pos).lowercase()
                x = when (name) {
                    "pi" -> Math.PI
                    "e" -> Math.E
                    "x" -> xValue
                    else -> {
                        val arg = parseFactor()
                        applyFunction(name, arg)
                    }
                }
            } else {
                throw IllegalArgumentException("Unexpected character in expression.")
            }

            if (eat('^'.code)) x = x.pow(parseFactor())
            return x
        }

        private fun applyFunction(name: String, arg: Double): Double {
            val angle = if (useDegrees) Math.toRadians(arg) else arg
            return when (name) {
                "sqrt" -> if (arg < 0) throw ArithmeticException("Square root of negative number") else sqrt(arg)
                "abs" -> abs(arg)
                "sin" -> sin(angle)
                "cos" -> cos(angle)
                "tan" -> tan(angle)
                "asin" -> if (useDegrees) Math.toDegrees(asin(arg)) else asin(arg)
                "acos" -> if (useDegrees) Math.toDegrees(acos(arg)) else acos(arg)
                "atan" -> if (useDegrees) Math.toDegrees(atan(arg)) else atan(arg)
                "log" -> if (arg <= 0) throw ArithmeticException("Logarithm domain error") else log10(arg)
                "ln" -> if (arg <= 0) throw ArithmeticException("Ln domain error") else ln(arg)
                "exp" -> exp(arg)
                else -> throw IllegalArgumentException("Unknown function '$name'")
            }
        }
    }
}
