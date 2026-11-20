package com.example.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ui.SectionHeader
import com.example.ui.theme.MonoFontFamily
import kotlin.math.*

enum class SciToolCategory(val title: String) {
    CALC("Scientific Calc"),
    GRAPH("Graph Plotter"),
    EQUATIONS("Equation Solvers"),
    CONVERTERS("Units & Sci Notation"),
    MATH_SUITE("Geom / Trig / Log / %"),
    MATRIX_STATS("Matrix & Statistics"),
    CHEMISTRY("Periodic / Molar / Balance"),
    REFERENCES("Phys / Chem / Bio Ref")
}

@Composable
fun ScientificToolsScreen(modifier: Modifier = Modifier) {
    var activeCategory by remember { mutableStateOf(SciToolCategory.CALC) }

    Column(modifier = modifier.fillMaxSize().testTag("scientific_tools_screen")) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SciToolCategory.entries) { cat ->
                FilterChip(
                    selected = activeCategory == cat,
                    onClick = { activeCategory = cat },
                    label = { Text(cat.title) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeCategory) {
                SciToolCategory.CALC -> item { ScientificCalculatorCard() }
                SciToolCategory.GRAPH -> item { GraphPlotterCard() }
                SciToolCategory.EQUATIONS -> item { EquationSolversCard() }
                SciToolCategory.CONVERTERS -> item { ConvertersCard() }
                SciToolCategory.MATH_SUITE -> item { MathSuiteCard() }
                SciToolCategory.MATRIX_STATS -> item { MatrixAndStatsCard() }
                SciToolCategory.CHEMISTRY -> item { ChemistryToolsCard() }
                SciToolCategory.REFERENCES -> item { ReferencesCard() }
            }
        }
    }
}

@Composable
private fun ScientificCalculatorCard() {
    var expr by remember { mutableStateOf("sin(30) + log(100) + sqrt(16)") }
    var useDegrees by remember { mutableStateOf(true) }
    var output by remember { mutableStateOf("Result: 6.5") }

    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("1. Scientific Calculator", "Supports sin, cos, tan, asin, acos, atan, log, ln, sqrt, abs, exp, pi, e, ^")
            OutlinedTextField(
                value = expr,
                onValueChange = { expr = it },
                label = { Text("Expression") },
                textStyle = LocalTextStyle.current.copy(fontFamily = MonoFontFamily),
                modifier = Modifier.fillMaxWidth().testTag("sci_calc_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = useDegrees, onClick = { useDegrees = true }, label = { Text("DEG") })
                FilterChip(selected = !useDegrees, onClick = { useDegrees = false }, label = { Text("RAD") })
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        output = when (val res = MathEngine.evaluateExpression(expr, useDegrees = useDegrees)) {
                            is EvalResult.Success -> "Result: ${res.formatted}"
                            is EvalResult.Error -> "Error: ${res.message}"
                        }
                    },
                    modifier = Modifier.testTag("sci_calc_eval_btn")
                ) { Text("Evaluate") }
            }
            Text(
                output,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("sci_calc_result")
            )
            val buttons = listOf(
                listOf("sin(", "cos(", "tan(", "sqrt("),
                listOf("log(", "ln(", "^", "pi"),
                listOf("(", ")", "abs(", "e")
            )
            buttons.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { token ->
                        FilledTonalButton(onClick = { expr += token }, modifier = Modifier.weight(1f)) {
                            Text(token, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphPlotterCard() {
    var expr by remember { mutableStateOf("x^2 - 4") }
    var xMinStr by remember { mutableStateOf("-5") }
    var xMaxStr by remember { mutableStateOf("5") }
    var useDegrees by remember { mutableStateOf(false) }
    var points by remember { mutableStateOf<List<Pair<Double, Double?>>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        MathEngine.sampleGraphPoints(expr, -5.0, 5.0, useDegrees = false)
            .onSuccess { points = it; errorMsg = null }
            .onFailure { errorMsg = it.message }
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outline

    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("2. Graph Plotter", "Plot y = f(x) offline")
            OutlinedTextField(value = expr, onValueChange = { expr = it }, label = { Text("f(x) =") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = xMinStr, onValueChange = { xMinStr = it }, label = { Text("xMin") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = xMaxStr, onValueChange = { xMaxStr = it }, label = { Text("xMax") }, modifier = Modifier.weight(1f))
                Button(onClick = {
                    val min = xMinStr.toDoubleOrNull()
                    val max = xMaxStr.toDoubleOrNull()
                    if (min == null || max == null) {
                        errorMsg = "Enter valid numeric bounds."
                    } else {
                        MathEngine.sampleGraphPoints(expr, min, max, useDegrees = useDegrees)
                            .onSuccess { points = it; errorMsg = null }
                            .onFailure { errorMsg = it.message }
                    }
                }) { Text("Plot") }
            }
            errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                val valid = points.mapNotNull { it.second }
                if (valid.isEmpty()) return@Canvas
                val xMin = points.first().first
                val xMax = points.last().first
                val yMin = (valid.minOrNull() ?: -1.0).let { if (it == valid.maxOrNull()) it - 1.0 else it }
                val yMax = (valid.maxOrNull() ?: 1.0).let { if (it == valid.minOrNull()) it + 1.0 else it }

                fun mapX(x: Double) = ((x - xMin) / (xMax - xMin)).toFloat() * size.width
                fun mapY(y: Double) = size.height - ((y - yMin) / (yMax - yMin)).toFloat() * size.height

                if (0.0 in xMin..xMax) {
                    val ax = mapX(0.0)
                    drawLine(axisColor, Offset(ax, 0f), Offset(ax, size.height), strokeWidth = 2f)
                }
                if (0.0 in yMin..yMax) {
                    val ay = mapY(0.0)
                    drawLine(axisColor, Offset(0f, ay), Offset(size.width, ay), strokeWidth = 2f)
                }

                val path = Path()
                var started = false
                for ((x, y) in points) {
                    if (y == null) {
                        started = false
                    } else {
                        val px = mapX(x)
                        val py = mapY(y)
                        if (!started) {
                            path.moveTo(px, py)
                            started = true
                        } else {
                            path.lineTo(px, py)
                        }
                    }
                }
                drawPath(path, lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun EquationSolversCard() {
    // 3. Linear, 4. Quadratic, 5. Simultaneous
    var linA by remember { mutableStateOf("2") }; var linB by remember { mutableStateOf("-6") }; var linC by remember { mutableStateOf("10") }
    var linResult by remember { mutableStateOf("") }

    var qa by remember { mutableStateOf("1") }; var qb by remember { mutableStateOf("-5") }; var qc by remember { mutableStateOf("6") }
    var qResult by remember { mutableStateOf("") }

    var a1 by remember { mutableStateOf("2") }; var b1 by remember { mutableStateOf("3") }; var c1 by remember { mutableStateOf("12") }
    var a2 by remember { mutableStateOf("1") }; var b2 by remember { mutableStateOf("-1") }; var c2 by remember { mutableStateOf("1") }
    var simResult by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("3. Linear Equation Solver", "ax + b = c")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(linA, { linA = it }, label = { Text("a") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(linB, { linB = it }, label = { Text("b") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(linC, { linC = it }, label = { Text("c") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = {
                    val a = linA.toDoubleOrNull(); val b = linB.toDoubleOrNull(); val c = linC.toDoubleOrNull()
                    linResult = if (a == null || b == null || c == null) "Invalid numeric input." else MathEngine.solveLinear(a, b, c)
                }) { Text("Solve Linear") }
                if (linResult.isNotEmpty()) Text(linResult, fontWeight = FontWeight.Bold)
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("4. Quadratic Equation Solver", "ax² + bx + c = 0")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(qa, { qa = it }, label = { Text("a") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(qb, { qb = it }, label = { Text("b") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(qc, { qc = it }, label = { Text("c") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = {
                    val a = qa.toDoubleOrNull(); val b = qb.toDoubleOrNull(); val c = qc.toDoubleOrNull()
                    qResult = if (a == null || b == null || c == null) "Invalid numeric input." else {
                        MathEngine.solveQuadratic(a, b, c).fold(
                            onSuccess = { "${it.description}\nΔ = ${MathEngine.formatNumber(it.discriminant)} • Vertex (${MathEngine.formatNumber(it.vertexX)}, ${MathEngine.formatNumber(it.vertexY)})" },
                            onFailure = { "Error: ${it.message}" }
                        )
                    }
                }) { Text("Solve Quadratic") }
                if (qResult.isNotEmpty()) Text(qResult, fontWeight = FontWeight.Bold)
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("5. Simultaneous Equations (2×2)", "a₁x + b₁y = c₁  and  a₂x + b₂y = c₂")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(a1, { a1 = it }, label = { Text("a₁") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(b1, { b1 = it }, label = { Text("b₁") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(c1, { c1 = it }, label = { Text("c₁") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(a2, { a2 = it }, label = { Text("a₂") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(b2, { b2 = it }, label = { Text("b₂") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(c2, { c2 = it }, label = { Text("c₂") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = {
                    val nums = listOf(a1, b1, c1, a2, b2, c2).map { it.toDoubleOrNull() }
                    simResult = if (nums.any { it == null }) "Invalid numeric coefficients." else {
                        MathEngine.solveSimultaneous2x2(nums[0]!!, nums[1]!!, nums[2]!!, nums[3]!!, nums[4]!!, nums[5]!!)
                    }
                }) { Text("Solve System") }
                if (simResult.isNotEmpty()) Text(simResult, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ConvertersCard() {
    // 6. Unit Converter & 7. Scientific Notation Converter
    var inputVal by remember { mutableStateOf("100") }
    var unitPair by remember { mutableStateOf("°C → K") }
    val unitPairs = listOf("°C → K", "K → °C", "m → km", "km/h → m/s", "atm → kPa", "cal → J", "g → kg", "L → mL")

    var sciInput by remember { mutableStateOf("0.00045") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("6. Scientific Unit Converter")
                OutlinedTextField(inputVal, { inputVal = it }, label = { Text("Value") }, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(unitPairs) { p ->
                        FilterChip(selected = unitPair == p, onClick = { unitPair = p }, label = { Text(p) })
                    }
                }
                val num = inputVal.toDoubleOrNull()
                val converted = if (num == null) "Enter a valid number." else when (unitPair) {
                    "°C → K" -> "${MathEngine.formatNumber(num + 273.15)} K"
                    "K → °C" -> "${MathEngine.formatNumber(num - 273.15)} °C"
                    "m → km" -> "${MathEngine.formatNumber(num / 1000.0)} km"
                    "km/h → m/s" -> "${MathEngine.formatNumber(num / 3.6)} m/s"
                    "atm → kPa" -> "${MathEngine.formatNumber(num * 101.325)} kPa"
                    "cal → J" -> "${MathEngine.formatNumber(num * 4.184)} J"
                    "g → kg" -> "${MathEngine.formatNumber(num / 1000.0)} kg"
                    "L → mL" -> "${MathEngine.formatNumber(num * 1000.0)} mL"
                    else -> ""
                }
                Text("Converted: $converted", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("7. Scientific Notation Converter")
                OutlinedTextField(sciInput, { sciInput = it }, label = { Text("Decimal or E-notation (e.g. 6.022e23)") }, modifier = Modifier.fillMaxWidth())
                val v = sciInput.toDoubleOrNull()
                val sciFormatted = if (v == null) "Enter a valid number." else if (v == 0.0) "0 × 10⁰" else {
                    val exp = floor(log10(abs(v))).toInt()
                    val mantissa = v / 10.0.pow(exp.toDouble())
                    "${MathEngine.formatNumber(mantissa)} × 10^$exp  (Standard: ${MathEngine.formatNumber(v)})"
                }
                Text(sciFormatted, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MathSuiteCard() {
    // 8. Percentage, 9. Geometry, 10. Trigonometry, 11. Logarithm
    var pctPart by remember { mutableStateOf("45") }; var pctWhole by remember { mutableStateOf("60") }
    var radiusStr by remember { mutableStateOf("7") }
    var angleDeg by remember { mutableStateOf("30") }
    var logBase by remember { mutableStateOf("2") }; var logVal by remember { mutableStateOf("32") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("8. Percentage & Yield Calculator")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(pctPart, { pctPart = it }, label = { Text("Part") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(pctWhole, { pctWhole = it }, label = { Text("Whole") }, modifier = Modifier.weight(1f))
                }
                val p = pctPart.toDoubleOrNull(); val w = pctWhole.toDoubleOrNull()
                val text = if (p == null || w == null || w == 0.0) "Enter non-zero whole." else {
                    "${MathEngine.formatNumber((p / w) * 100.0)}% • Percent change from Whole→Part: ${MathEngine.formatNumber(((p - w) / w) * 100.0)}%"
                }
                Text(text, fontWeight = FontWeight.Bold)
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("9. Geometry Calculator (Circle & Sphere)")
                OutlinedTextField(radiusStr, { radiusStr = it }, label = { Text("Radius r") }, modifier = Modifier.fillMaxWidth())
                val r = radiusStr.toDoubleOrNull()
                val gOut = if (r == null || r < 0) "Enter non-negative radius." else {
                    val area = Math.PI * r * r
                    val circ = 2 * Math.PI * r
                    val vol = (4.0 / 3.0) * Math.PI * r * r * r
                    "Circle Area: ${MathEngine.formatNumber(area)} | Circumference: ${MathEngine.formatNumber(circ)}\nSphere Volume: ${MathEngine.formatNumber(vol)}"
                }
                Text(gOut, fontWeight = FontWeight.Bold)
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("10. Trigonometry Calculator")
                OutlinedTextField(angleDeg, { angleDeg = it }, label = { Text("Angle (Degrees)") }, modifier = Modifier.fillMaxWidth())
                val deg = angleDeg.toDoubleOrNull()
                val tOut = if (deg == null) "Enter valid angle." else {
                    val rad = Math.toRadians(deg)
                    val tanVal = if (abs(cos(rad)) < 1e-10) "Undefined" else MathEngine.formatNumber(tan(rad))
                    "sin = ${MathEngine.formatNumber(sin(rad))} | cos = ${MathEngine.formatNumber(cos(rad))} | tan = $tanVal\nRadians = ${MathEngine.formatNumber(rad)} rad"
                }
                Text(tOut, fontWeight = FontWeight.Bold)
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("11. Logarithm Calculator", "log_b(x)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(logBase, { logBase = it }, label = { Text("Base b") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(logVal, { logVal = it }, label = { Text("Value x") }, modifier = Modifier.weight(1f))
                }
                val b = logBase.toDoubleOrNull(); val x = logVal.toDoubleOrNull()
                val lOut = if (b == null || x == null || b <= 0 || b == 1.0 || x <= 0) {
                    "Domain error: require b > 0, b ≠ 1, and x > 0."
                } else {
                    "log_${MathEngine.formatNumber(b)}(${MathEngine.formatNumber(x)}) = ${MathEngine.formatNumber(ln(x) / ln(b))}"
                }
                Text(lOut, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MatrixAndStatsCard() {
    // 12. Matrix Calculator & 13. Statistics Calculator
    var m00 by remember { mutableStateOf("4") }; var m01 by remember { mutableStateOf("7") }
    var m10 by remember { mutableStateOf("2") }; var m11 by remember { mutableStateOf("6") }
    var dataStr by remember { mutableStateOf("12, 15, 18, 18, 22, 25, 30") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("12. Matrix Calculator (2×2)", "Determinant, Transpose & Inverse")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(m00, { m00 = it }, label = { Text("a₁₁") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(m01, { m01 = it }, label = { Text("a₁₂") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(m10, { m10 = it }, label = { Text("a₂₁") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(m11, { m11 = it }, label = { Text("a₂₂") }, modifier = Modifier.weight(1f))
                }
                val vals = listOf(m00, m01, m10, m11).map { it.toDoubleOrNull() }
                if (vals.any { it == null }) {
                    Text("Enter valid numbers.", color = MaterialTheme.colorScheme.error)
                } else {
                    val res = MathEngine.analyzeMatrix2x2(listOf(listOf(vals[0]!!, vals[1]!!), listOf(vals[2]!!, vals[3]!!)))
                    Text("det(A) = ${MathEngine.formatNumber(res.determinant)}", fontWeight = FontWeight.Bold)
                    val invText = res.inverse?.let { inv ->
                        "Inverse: [[${MathEngine.formatNumber(inv[0][0])}, ${MathEngine.formatNumber(inv[0][1])}], [${MathEngine.formatNumber(inv[1][0])}, ${MathEngine.formatNumber(inv[1][1])}]]"
                    } ?: "Singular matrix (det = 0): Inverse does not exist."
                    Text(invText, fontFamily = MonoFontFamily)
                }
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("13. Statistics Calculator", "Comma or space separated values")
                OutlinedTextField(dataStr, { dataStr = it }, label = { Text("Dataset") }, modifier = Modifier.fillMaxWidth())
                val nums = dataStr.split(Regex("[,\\s]+")).mapNotNull { it.toDoubleOrNull() }
                MathEngine.computeStats(nums).fold(
                    onSuccess = { st ->
                        Text("n=${st.count} | Mean=${MathEngine.formatNumber(st.mean)} | Median=${MathEngine.formatNumber(st.median)}\nMode=${st.mode} | Range=${MathEngine.formatNumber(st.range)}\nPop σ=${MathEngine.formatNumber(st.popStdDev)} | Sample s=${MathEngine.formatNumber(st.sampleStdDev)}")
                    },
                    onFailure = { Text(it.message ?: "Error", color = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}

@Composable
private fun ChemistryToolsCard() {
    // 14. Periodic Table, 15. Molar Mass, 16. Equation Balancer
    var elementQuery by remember { mutableStateOf("") }
    var formulaStr by remember { mutableStateOf("Ca(NO3)2") }
    var eqStr by remember { mutableStateOf("Fe + O2 -> Fe2O3") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("15. Chemical Molar-Mass Calculator")
                OutlinedTextField(formulaStr, { formulaStr = it }, label = { Text("Formula (e.g., H2SO4, Ca(NO3)2)") }, modifier = Modifier.fillMaxWidth())
                MathEngine.calculateMolarMass(formulaStr).fold(
                    onSuccess = { (mass, counts) ->
                        Text("Molar Mass: ${"%.3f".format(mass)} g/mol", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Composition: ${counts.entries.joinToString { "${it.key}: ${it.value}" }}", style = MaterialTheme.typography.bodySmall)
                    },
                    onFailure = { Text("Error: ${it.message}", color = MaterialTheme.colorScheme.error) }
                )
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("16. Chemical Equation Balancer")
                OutlinedTextField(eqStr, { eqStr = it }, label = { Text("Unbalanced equation (use -> or =)") }, modifier = Modifier.fillMaxWidth())
                MathEngine.balanceChemicalEquation(eqStr).fold(
                    onSuccess = { balanced ->
                        Text("Balanced: $balanced", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    },
                    onFailure = { Text("Note: ${it.message}", color = MaterialTheme.colorScheme.error) }
                )
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("14. Periodic Table Reference")
                OutlinedTextField(elementQuery, { elementQuery = it }, label = { Text("Filter by symbol, name, or atomic #") }, modifier = Modifier.fillMaxWidth())
                val filtered = ReferenceData.elements.filter {
                    elementQuery.isBlank() || it.name.contains(elementQuery, true) ||
                        it.symbol.equals(elementQuery.trim(), true) || it.number.toString() == elementQuery.trim()
                }.take(16)
                filtered.forEach { el ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${el.number}. ${el.symbol} — ${el.name} (${el.category})", style = MaterialTheme.typography.bodyMedium)
                        Text("${el.atomicMass}", fontFamily = MonoFontFamily, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferencesCard() {
    // 17. Physics Formulas, 18. Chemistry Formulas, 19. Biology Terminology
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("17. Physics Formula Reference")
                ReferenceData.physicsFormulas.forEach { f ->
                    Text(f.title, fontWeight = FontWeight.Bold)
                    Text(f.formula, fontFamily = MonoFontFamily, color = MaterialTheme.colorScheme.primary)
                    Text(f.description, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("18. Chemistry Formula Reference")
                ReferenceData.chemistryFormulas.forEach { f ->
                    Text(f.title, fontWeight = FontWeight.Bold)
                    Text(f.formula, fontFamily = MonoFontFamily, color = MaterialTheme.colorScheme.secondary)
                    Text(f.description, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("19. Biology Terminology Reference")
                ReferenceData.biologyTerms.forEach { b ->
                    Text("${b.term} (${b.category})", fontWeight = FontWeight.Bold)
                    Text(b.definition, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("20. Mathematics & Calculus Reference")
                ReferenceData.mathFormulas.forEach { f ->
                    Text(f.title, fontWeight = FontWeight.Bold)
                    Text(f.formula, fontFamily = MonoFontFamily, color = MaterialTheme.colorScheme.primary)
                    Text(f.description, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
            }
        }

        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("21. Physical Constants, English & Aptitude")
                (ReferenceData.physicalConstants + ReferenceData.englishGrammarRules + ReferenceData.aptitudeShortcuts).forEach { f ->
                    Text(f.title, fontWeight = FontWeight.Bold)
                    Text(f.formula, fontFamily = MonoFontFamily, color = MaterialTheme.colorScheme.tertiary)
                    Text(f.description, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
            }
        }
    }
}
