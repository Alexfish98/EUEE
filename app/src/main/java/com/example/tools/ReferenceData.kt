package com.example.tools

data class ElementInfo(
    val number: Int,
    val symbol: String,
    val name: String,
    val atomicMass: Double,
    val category: String
)

data class FormulaItem(
    val title: String,
    val formula: String,
    val description: String
)

data class BioTerm(
    val term: String,
    val category: String,
    val definition: String
)

object ReferenceData {

    val elements: List<ElementInfo> = listOf(
        ElementInfo(1, "H", "Hydrogen", 1.008, "Nonmetal"),
        ElementInfo(2, "He", "Helium", 4.0026, "Noble Gas"),
        ElementInfo(3, "Li", "Lithium", 6.94, "Alkali Metal"),
        ElementInfo(4, "Be", "Beryllium", 9.0122, "Alkaline Earth"),
        ElementInfo(5, "B", "Boron", 10.81, "Metalloid"),
        ElementInfo(6, "C", "Carbon", 12.011, "Nonmetal"),
        ElementInfo(7, "N", "Nitrogen", 14.007, "Nonmetal"),
        ElementInfo(8, "O", "Oxygen", 15.999, "Nonmetal"),
        ElementInfo(9, "F", "Fluorine", 18.998, "Halogen"),
        ElementInfo(10, "Ne", "Neon", 20.180, "Noble Gas"),
        ElementInfo(11, "Na", "Sodium", 22.990, "Alkali Metal"),
        ElementInfo(12, "Mg", "Magnesium", 24.305, "Alkaline Earth"),
        ElementInfo(13, "Al", "Aluminum", 26.982, "Post-Transition"),
        ElementInfo(14, "Si", "Silicon", 28.085, "Metalloid"),
        ElementInfo(15, "P", "Phosphorus", 30.974, "Nonmetal"),
        ElementInfo(16, "S", "Sulfur", 32.06, "Nonmetal"),
        ElementInfo(17, "Cl", "Chlorine", 35.45, "Halogen"),
        ElementInfo(18, "Ar", "Argon", 39.948, "Noble Gas"),
        ElementInfo(19, "K", "Potassium", 39.098, "Alkali Metal"),
        ElementInfo(20, "Ca", "Calcium", 40.078, "Alkaline Earth"),
        ElementInfo(21, "Sc", "Scandium", 44.956, "Transition Metal"),
        ElementInfo(22, "Ti", "Titanium", 47.867, "Transition Metal"),
        ElementInfo(23, "V", "Vanadium", 50.942, "Transition Metal"),
        ElementInfo(24, "Cr", "Chromium", 51.996, "Transition Metal"),
        ElementInfo(25, "Mn", "Manganese", 54.938, "Transition Metal"),
        ElementInfo(26, "Fe", "Iron", 55.845, "Transition Metal"),
        ElementInfo(27, "Co", "Cobalt", 58.933, "Transition Metal"),
        ElementInfo(28, "Ni", "Nickel", 58.693, "Transition Metal"),
        ElementInfo(29, "Cu", "Copper", 63.546, "Transition Metal"),
        ElementInfo(30, "Zn", "Zinc", 65.38, "Transition Metal"),
        ElementInfo(31, "Ga", "Gallium", 69.723, "Post-Transition"),
        ElementInfo(32, "Ge", "Germanium", 72.630, "Metalloid"),
        ElementInfo(33, "As", "Arsenic", 74.922, "Metalloid"),
        ElementInfo(34, "Se", "Selenium", 78.971, "Nonmetal"),
        ElementInfo(35, "Br", "Bromine", 79.904, "Halogen"),
        ElementInfo(36, "Kr", "Krypton", 83.798, "Noble Gas"),
        ElementInfo(37, "Rb", "Rubidium", 85.468, "Alkali Metal"),
        ElementInfo(38, "Sr", "Strontium", 87.62, "Alkaline Earth"),
        ElementInfo(47, "Ag", "Silver", 107.87, "Transition Metal"),
        ElementInfo(50, "Sn", "Tin", 118.71, "Post-Transition"),
        ElementInfo(53, "I", "Iodine", 126.90, "Halogen"),
        ElementInfo(54, "Xe", "Xenon", 131.29, "Noble Gas"),
        ElementInfo(56, "Ba", "Barium", 137.33, "Alkaline Earth"),
        ElementInfo(74, "W", "Tungsten", 183.84, "Transition Metal"),
        ElementInfo(78, "Pt", "Platinum", 195.08, "Transition Metal"),
        ElementInfo(79, "Au", "Gold", 196.97, "Transition Metal"),
        ElementInfo(80, "Hg", "Mercury", 200.59, "Transition Metal"),
        ElementInfo(82, "Pb", "Lead", 207.2, "Post-Transition"),
        ElementInfo(92, "U", "Uranium", 238.03, "Actinide")
    )

    val atomicMassBySymbol: Map<String, Double> = elements.associate { it.symbol to it.atomicMass }

    val physicsFormulas = listOf(
        FormulaItem("Kinematics (Constant a)", "v = v₀ + a·t;  Δx = v₀·t + ½a·t²;  v² = v₀² + 2a·Δx", "Uniformly accelerated linear motion."),
        FormulaItem("Newton's Second Law", "F_net = m · a", "Net force equals mass times acceleration."),
        FormulaItem("Kinetic Friction", "fₖ = μₖ · N", "Opposes sliding motion on a surface."),
        FormulaItem("Work & Kinetic Energy", "W = F·d·cos(θ) = ΔK = ½m·v² − ½m·v₀²", "Work-energy theorem."),
        FormulaItem("Ohm's Law & Power", "V = I · R;  P = I·V = I²·R = V²/R", "DC circuits and resistive dissipation."),
        FormulaItem("Coulomb's Law", "F = k·|q₁·q₂| / r²  (k ≈ 8.99×10⁹ N·m²/C²)", "Electrostatic force between point charges."),
        FormulaItem("Wave & Optics", "v = f · λ;  n₁·sin(θ₁) = n₂·sin(θ₂)", "Wave speed and Snell's law of refraction."),
        FormulaItem("Photoelectric Effect", "K_max = h·f − φ", "Maximum photoelectron kinetic energy (h = 6.626×10⁻³⁴ J·s).")
    )

    val chemistryFormulas = listOf(
        FormulaItem("Ideal Gas Law", "P·V = n·R·T  (R = 0.0821 L·atm/(mol·K) = 8.314 J/(mol·K))", "Relates pressure, volume, moles, and Kelvin temperature."),
        FormulaItem("Molarity & Dilution", "M = n / V(L);  M₁·V₁ = M₂·V₂", "Solution concentration and dilution."),
        FormulaItem("pH & pOH", "pH = −log₁₀[H⁺];  pOH = −log₁₀[OH⁻];  pH + pOH = 14 (at 25 °C)", "Aqueous acid-base scale."),
        FormulaItem("Henderson-Hasselbalch", "pH = pKₐ + log₁₀([A⁻] / [HA])", "Buffer solution pH calculation."),
        FormulaItem("Gibbs Free Energy", "ΔG = ΔH − T·ΔS;  ΔG° = −R·T·ln(K)", "Spontaneity criterion (ΔG < 0 is spontaneous)."),
        FormulaItem("Nernst Equation (25 °C)", "E_cell = E°_cell − (0.0592 / n)·log₁₀(Q)", "Electrochemical cell potential under non-standard conditions.")
    )

    val biologyTerms = listOf(
        BioTerm("ATP (Adenosine Triphosphate)", "Bioenergetics", "Primary energy currency of cells; releases energy upon hydrolysis to ADP + Pᵢ."),
        BioTerm("Enzyme Active Site", "Biochemistry", "Specific 3D pocket where substrate binds and activation energy is lowered."),
        BioTerm("Osmosis", "Cell Transport", "Net passive movement of water across a selectively permeable membrane toward higher solute concentration."),
        BioTerm("Transcription", "Molecular Biology", "Synthesis of complementary mRNA from a DNA template catalyzed by RNA polymerase."),
        BioTerm("Translation", "Molecular Biology", "Decoding of mRNA codons by tRNA anticodons on ribosomes to assemble polypeptides."),
        BioTerm("Krebs Cycle", "Cellular Respiration", "Cyclic pathway in the mitochondrial matrix generating NADH, FADH₂, GTP/ATP, and CO₂."),
        BioTerm("Crossing Over", "Genetics", "Exchange of genetic material between non-sister chromatids during Prophase I of meiosis."),
        BioTerm("Natural Selection", "Evolution", "Differential survival and reproduction of individuals with heritable advantageous traits.")
    )

    val mathFormulas = listOf(
        FormulaItem("Quadratic Formula & Discriminant", "x = (−b ± √(b² − 4ac)) / (2a);  Δ = b² − 4ac", "Δ > 0 two real roots; Δ = 0 one repeated root; Δ < 0 complex roots."),
        FormulaItem("Logarithm & Exponent Laws", "log_b(xy) = log_b(x)+log_b(y);  log_b(x^k) = k·log_b(x);  log_b(x) = ln(x)/ln(b)", "Arguments of logarithms must always satisfy x > 0."),
        FormulaItem("Trigonometric Identities", "sin²θ + cos²θ = 1;  1 + tan²θ = sec²θ;  sin(2θ) = 2sinθcosθ;  cos(2θ) = cos²θ − sin²θ", "Core identities for simplification and equations."),
        FormulaItem("Derivatives (Power, Product, Chain)", "d/dx(xⁿ) = n·xⁿ⁻¹;  (fg)' = f'g + fg';  d/dx[f(g(x))] = f'(g(x))·g'(x)", "d/dx(sin x)=cos x, d/dx(cos x)=−sin x, d/dx(eˣ)=eˣ, d/dx(ln x)=1/x."),
        FormulaItem("Definite Integral & FTC", "∫[a..b] f(x) dx = F(b) − F(a)  where F'(x) = f(x)", "Area between curve and x-axis (watch sign below axis)."),
        FormulaItem("2×2 Matrix Inverse", "A⁻¹ = (1 / (ad − bc)) · [[d, −b], [−c, a]]  (ad − bc ≠ 0)", "Matrix is singular when determinant det(A) = ad − bc = 0."),
        FormulaItem("Conic Sections (Standard Forms)", "Circle: (x−h)²+(y−k)²=r² | Ellipse: (x−h)²/a² + (y−k)²/b² = 1 | Parabola: y−k = a(x−h)²", "Hyperbola: x²/a² − y²/b² = 1 with asymptotes y = ±(b/a)x.")
    )

    val englishGrammarRules = listOf(
        FormulaItem("Conditionals (Types 1, 2, 3)", "Type 1: If + Present → will + V1 | Type 2: If + Past → would + V1 | Type 3: If + had V3 → would have V3", "Inverted forms: 'Had + S + V3...', 'Were + S + to V1...'."),
        FormulaItem("Subject-Verb Agreement Traps", "Each / Every / Neither / Either + singular verb | 'The number of' → singular | 'A number of' → plural", "Ignore prepositional phrases between subject and verb."),
        FormulaItem("Modals of Past Deduction", "must have + V3 (near certainty) | might/could have + V3 (possibility) | should have + V3 (unfulfilled duty)", "Distinguish past regret from past deduction."),
        FormulaItem("High-Yield EUEE Phrasal Verbs", "call off = cancel | put off = postpone | look into = investigate | put up with = tolerate | run out of = exhaust", "Always verify context clues for idiomatic particle meaning.")
    )

    val aptitudeShortcuts = listOf(
        FormulaItem("Arithmetic & Geometric Progressions", "AP: aₙ = a₁ + (n−1)d,  Sₙ = n/2(a₁ + aₙ) | GP: aₙ = a₁·rⁿ⁻¹,  S∞ = a₁/(1−r) (|r|<1)", "Check first differences, second differences, ratios, and 2ⁿ ± k."),
        FormulaItem("Successive Percentage Change", "Net % = a + b + (a·b)/100", "A 20% increase followed by 20% decrease = +20 − 20 − 4 = −4%."),
        FormulaItem("Work & Combined Rate", "1/T_total = 1/T₁ + 1/T₂  ⇒  T_total = (T₁·T₂)/(T₁ + T₂)", "Fast two-worker combined completion shortcut."),
        FormulaItem("Verbal Analogy & Syllogism Method", "Write exact bridge sentence 'X is [relation] to Y'; check word order & part of speech", "For 'All A are B; Some B are C', draw overlapping Euler circles—never assume 'Some A are C'.")
    )

    val physicalConstants = listOf(
        FormulaItem("Fundamental Constants", "g ≈ 9.81 (or 10) m/s² | c = 3.00×10⁸ m/s | h = 6.626×10⁻³⁴ J·s | e = 1.602×10⁻¹⁹ C", "Avogadro Nₐ = 6.022×10²³ mol⁻¹ | k_e = 8.99×10⁹ N·m²/C² | R = 8.314 J/(mol·K).")
    )
}
