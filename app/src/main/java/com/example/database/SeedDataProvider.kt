package com.example.database

import com.example.core.model.AlarmCategory
import com.example.core.model.Difficulty
import com.example.core.model.QuestionType
import com.example.core.model.TaskPriority
import java.time.LocalDate

object SeedDataProvider {

    suspend fun seedIfEmpty(db: EueeDatabase) {
        val curriculum = db.curriculumDao()
        if (curriculum.getAllSubjects().isNotEmpty()) return

        // 1. Question Bank
        val builtInBankId = curriculum.insertQuestionBank(
            QuestionBankEntity(
                name = "EUEE Core Curriculum Bank",
                description = "Ethiopian University Entrance Examination (Grades 9-12) offline foundation set",
                sourceType = "BUILT_IN"
            )
        )

        // 2. Grades 9..12
        val gradeIds = mutableMapOf<Int, Long>()
        for (level in 9..12) {
            gradeIds[level] = curriculum.insertGrade(GradeEntity(level = level, label = "Grade $level"))
        }

        // 3. All 6 EUEE Subjects
        val mathId = curriculum.insertSubject(SubjectEntity(name = "Mathematics", category = "Natural Science", colorHex = 0xFF2563EB, iconName = "Calculate"))
        val physicsId = curriculum.insertSubject(SubjectEntity(name = "Physics", category = "Natural Science", colorHex = 0xFF7C3AED, iconName = "Science"))
        val chemId = curriculum.insertSubject(SubjectEntity(name = "Chemistry", category = "Natural Science", colorHex = 0xFF0D9488, iconName = "Biotech"))
        val bioId = curriculum.insertSubject(SubjectEntity(name = "Biology", category = "Natural Science", colorHex = 0xFF16A34A, iconName = "Eco"))
        val englishId = curriculum.insertSubject(SubjectEntity(name = "English", category = "Additional", colorHex = 0xFFD97706, iconName = "MenuBook"))
        val aptitudeId = curriculum.insertSubject(SubjectEntity(name = "Aptitude", category = "Additional", colorHex = 0xFFDC2626, iconName = "Psychology"))

        // Helper to insert Unit + Topics
        suspend fun addUnit(subjectId: Long, gradeLevel: Int, num: Int, title: String, topics: List<String>): Pair<Long, List<Long>> {
            val uid = curriculum.insertUnit(UnitEntity(subjectId = subjectId, gradeId = gradeIds.getValue(gradeLevel), unitNumber = num, title = title))
            val tids = topics.map { curriculum.insertTopic(TopicEntity(unitId = uid, title = it)) }
            return uid to tids
        }

        // Curriculum hierarchy across Grades 9-12 for all 6 subjects
        val (m9u1, m9t) = addUnit(mathId, 9, 1, "The Number System & Sets", listOf("Real Numbers & Surds", "Sets & Venn Diagrams"))
        val (m10u1, m10t) = addUnit(mathId, 10, 2, "Polynomial & Exponential Functions", listOf("Quadratic Equations", "Logarithmic Laws"))
        val (m11u1, m11t) = addUnit(mathId, 11, 3, "Matrices, Vectors & Conics", listOf("Determinants & Inverses", "Conic Sections"))
        val (m12u1, m12t) = addUnit(mathId, 12, 1, "Sequences, Limits & Calculus", listOf("Limits & Continuity", "Derivatives & Optimization", "Definite Integrals"))

        val (p9u1, p9t) = addUnit(physicsId, 9, 1, "Vectors & Kinematics", listOf("Uniformly Accelerated Motion", "Vector Resolution"))
        val (p10u1, p10t) = addUnit(physicsId, 10, 2, "Electrostatics & Circuits", listOf("Ohm's Law & Resistors", "Capacitors"))
        val (p11u1, p11t) = addUnit(physicsId, 11, 3, "Dynamics, Work & Energy", listOf("Newton's Laws & Friction", "Conservation of Momentum"))
        val (p12u1, p12t) = addUnit(physicsId, 12, 2, "Wave Optics & Modern Physics", listOf("Interference & Diffraction", "Photoelectric Effect"))

        val (c9u1, c9t) = addUnit(chemId, 9, 1, "Atomic Structure & Periodic Table", listOf("Electron Configuration", "Periodic Trends"))
        val (c10u1, c10t) = addUnit(chemId, 10, 2, "Chemical Bonding & Stoichiometry", listOf("Mole Concept & Limiting Reactant", "VSEPR & Hybridization"))
        val (c11u1, c11t) = addUnit(chemId, 11, 3, "Chemical Equilibrium & Kinetics", listOf("Le Chatelier's Principle", "Rate Laws"))
        val (c12u1, c12t) = addUnit(chemId, 12, 1, "Acid-Base Equilibria & Electrochemistry", listOf("pH & Buffer Solutions", "Galvanic Cells & Nernst Equation"))

        val (b9u1, b9t) = addUnit(bioId, 9, 1, "Cell Biology & Microscope", listOf("Organelles & Membranes", "Osmosis & Active Transport"))
        val (b10u1, b10t) = addUnit(bioId, 10, 2, "Enzymes & Cellular Respiration", listOf("Enzyme Kinetics", "Glycolysis & Krebs Cycle"))
        val (b11u1, b11t) = addUnit(bioId, 11, 3, "Genetics & Molecular Biology", listOf("Mendelian Inheritance", "DNA Replication & Transcription"))
        val (b12u1, b12t) = addUnit(bioId, 12, 2, "Ecology & Evolution", listOf("Natural Selection", "Biogeochemical Cycles"))

        val (e9u1, e9t) = addUnit(englishId, 9, 1, "Grammar & Sentence Structure", listOf("Tenses & Conditionals", "Modal Auxiliaries"))
        val (e11u1, e11t) = addUnit(englishId, 11, 2, "Vocabulary & Contextual Usage", listOf("Phrasal Verbs & Idioms", "Synonyms & Antonyms"))
        val (e12u1, e12t) = addUnit(englishId, 12, 3, "Reading Comprehension & Writing", listOf("Inference & Tone", "Paragraph Coherence"))

        val (a11u1, a11t) = addUnit(aptitudeId, 11, 1, "Verbal Reasoning", listOf("Analogies & Classifications", "Logical Deduction"))
        val (a12u1, a12t) = addUnit(aptitudeId, 12, 2, "Quantitative Reasoning", listOf("Ratios, Percentages & Sequences", "Data Sufficiency"))

        // 4. Rich EUEE Questions across all 6 subjects
        val questions = listOf(
            // MATHEMATICS
            QuestionEntity(
                uniqueCode = "EUEE-MATH-12-001",
                bankId = builtInBankId,
                subjectId = mathId,
                gradeId = gradeIds.getValue(12),
                unitId = m12u1,
                topicId = m12t[0],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Evaluate the limit: lim(x → 2) (x² − 4) / (x − 2)",
                correctAnswer = "4",
                acceptedAnswersJson = """["4","4.0"]""",
                optionsJson = """["0","2","4","Undefined"]""",
                hintsJson = """["Factor the numerator using difference of two squares: a² − b² = (a − b)(a + b).","Cancel (x − 2) before substituting x = 2."]""",
                explanation = "Factoring x² − 4 gives (x − 2)(x + 2). Canceling (x − 2) for x ≠ 2 leaves x + 2, which approaches 2 + 2 = 4.",
                whyCorrect = "The 0/0 indeterminate form is removable by algebraic factoring.",
                commonMistakes = "Substituting x = 2 directly and concluding the limit is undefined.",
                conceptExplanation = "A removable discontinuity at x = a does not prevent lim(x → a) f(x) from existing.",
                workedExample = "lim(x → 3) (x² − 9)/(x − 3) = lim(x → 3)(x + 3) = 6.",
                memoryTip = "0/0 means factor, rationalize, or apply L'Hôpital's rule!",
                examTrap = "Selecting 'Undefined' because the denominator is zero at x = 2.",
                referencesText = "Ethiopian Grade 12 Mathematics, Unit 2"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-MATH-12-002",
                bankId = builtInBankId,
                subjectId = mathId,
                gradeId = gradeIds.getValue(12),
                unitId = m12u1,
                topicId = m12t[1],
                difficulty = Difficulty.HARD.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "If f(x) = x³ − 3x² + 2, at what x-value does f(x) attain a local minimum?",
                correctAnswer = "x = 2",
                acceptedAnswersJson = """["x = 2","2"]""",
                optionsJson = """["x = 0","x = 1","x = 2","x = −1"]""",
                hintsJson = """["Find f'(x) and set it to 0 to locate critical points.","Use the second derivative f''(x) = 6x − 6 to classify."]""",
                explanation = "f'(x) = 3x² − 6x = 3x(x − 2) = 0 gives x = 0 and x = 2. Since f''(2) = 6(2) − 6 = 6 > 0, x = 2 is a local minimum.",
                whyCorrect = "f'(2) = 0 and f''(2) > 0 confirms concave-up local minimum.",
                commonMistakes = "Stopping after finding x = 0 and assuming the first critical point is minimum.",
                conceptExplanation = "First derivative test finds stationary points; second derivative test determines concavity.",
                workedExample = "For g(x) = x² − 4x, g'(x) = 2x − 4 = 0 at x = 2, g''(2) = 2 > 0 (min).",
                memoryTip = "f'' > 0 smiles (minimum); f'' < 0 frowns (maximum).",
                examTrap = "x = 0 is a local maximum (f''(0) = −6 < 0), placed first to trap hasty solvers.",
                referencesText = "Ethiopian Grade 12 Mathematics, Unit 3"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-MATH-11-003",
                bankId = builtInBankId,
                subjectId = mathId,
                gradeId = gradeIds.getValue(11),
                unitId = m11u1,
                topicId = m11t[0],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.NUMERICAL.name,
                questionText = "Find the determinant of the 2×2 matrix A = [[5, 3], [2, 4]].",
                correctAnswer = "14",
                acceptedAnswersJson = """["14","14.0"]""",
                optionsJson = """["14","26","7","−14"]""",
                hintsJson = """["For [[a, b], [c, d]], det(A) = ad − bc."]""",
                explanation = "det(A) = (5 × 4) − (3 × 2) = 20 − 6 = 14.",
                whyCorrect = "Product of main diagonal minus product of off-diagonal.",
                commonMistakes = "Adding ad + bc (giving 26) or reversing subtraction (giving −14).",
                conceptExplanation = "A matrix is invertible if and only if its determinant is non-zero.",
                workedExample = "det([[2, 1], [4, 3]]) = 2(3) − 1(4) = 2.",
                memoryTip = "Down-right minus down-left.",
                examTrap = "Forgetting the minus sign between ad and bc.",
                numericalTolerance = 0.001,
                referencesText = "Grade 11 Mathematics, Matrices"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-MATH-10-004",
                bankId = builtInBankId,
                subjectId = mathId,
                gradeId = gradeIds.getValue(10),
                unitId = m10u1,
                topicId = m10t[1],
                difficulty = Difficulty.EASY.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Solve for x: log₂(x) + log₂(x − 2) = 3",
                correctAnswer = "4",
                acceptedAnswersJson = """["4","x = 4"]""",
                optionsJson = """["4","−2 and 4","2","8"]""",
                hintsJson = """["Combine logs: log₂[x(x − 2)] = 3.","Remember the domain of log₂(y) requires y > 0."]""",
                explanation = "x(x − 2) = 2³ = 8 → x² − 2x − 8 = 0 → (x − 4)(x + 2) = 0. Only x = 4 satisfies x > 2.",
                whyCorrect = "x = −2 is extraneous because log₂(−2) is undefined in ℝ.",
                commonMistakes = "Accepting both −2 and 4 without checking log domain constraints.",
                conceptExplanation = "Logarithmic arguments must always be strictly positive.",
                workedExample = "log₃(x) = 2 ⇒ x = 3² = 9 (> 0).",
                memoryTip = "Always plug roots back into the original log equation!",
                examTrap = "Option '−2 and 4' catches students who skip domain verification.",
                referencesText = "Grade 10 Mathematics, Exponential & Logarithmic Functions"
            ),

            // PHYSICS
            QuestionEntity(
                uniqueCode = "EUEE-PHYS-11-001",
                bankId = builtInBankId,
                subjectId = physicsId,
                gradeId = gradeIds.getValue(11),
                unitId = p11u1,
                topicId = p11t[0],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.NUMERICAL.name,
                questionText = "A 4.0 kg block is pushed along a horizontal surface with μₖ = 0.25 by a horizontal force of 20 N. Take g = 10 m/s². Calculate its acceleration in m/s².",
                correctAnswer = "2.5",
                acceptedAnswersJson = """["2.5","2.5 m/s²","5/2"]""",
                optionsJson = """["2.5 m/s²","5.0 m/s²","7.5 m/s²","1.5 m/s²"]""",
                hintsJson = """["Compute kinetic friction: fₖ = μₖ N = μₖ m g.","Apply Newton's Second Law: F_net = F_applied − fₖ = m a."]""",
                explanation = "N = mg = 4.0 × 10 = 40 N. fₖ = 0.25 × 40 = 10 N. F_net = 20 − 10 = 10 N. a = 10 / 4.0 = 2.5 m/s².",
                whyCorrect = "Net horizontal force is reduced by kinetic friction opposing motion.",
                commonMistakes = "Dividing 20 N directly by 4.0 kg (ignoring friction) to get 5.0 m/s².",
                conceptExplanation = "Newton's Second Law applies to the vector sum of all forces acting on the mass.",
                workedExample = "If F = 30 N, f = 10 N, m = 5 kg → a = (30 − 10)/5 = 4 m/s².",
                memoryTip = "Always draw the Free Body Diagram before computing a = F/m.",
                examTrap = "Using g = 9.8 when the problem states g = 10 m/s².",
                numericalTolerance = 0.05,
                referencesText = "Grade 11 Physics, Dynamics"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-PHYS-12-002",
                bankId = builtInBankId,
                subjectId = physicsId,
                gradeId = gradeIds.getValue(12),
                unitId = p12u1,
                topicId = p12t[1],
                difficulty = Difficulty.HARD.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "In the photoelectric effect, if the frequency of incident light (above threshold f₀) is doubled while intensity remains constant, what happens to the maximum kinetic energy of emitted photoelectrons?",
                correctAnswer = "It more than doubles",
                acceptedAnswersJson = """["It more than doubles"]""",
                optionsJson = """["It stays the same","It doubles exactly","It more than doubles","It halves"]""",
                hintsJson = """["Use Einstein's photoelectric equation: K_max = hf − φ.","Compare 2hf − φ with 2(hf − φ) = 2hf − 2φ."]""",
                explanation = "Initially K₁ = hf − φ. When frequency doubles, K₂ = 2hf − φ = 2(hf − φ) + φ = 2K₁ + φ > 2K₁.",
                whyCorrect = "The work function φ is subtracted only once, so K_max > 2K₁.",
                commonMistakes = "Assuming K_max is directly proportional to frequency without the −φ intercept.",
                conceptExplanation = "K_max = hf − φ is linear with slope h and y-intercept −φ.",
                workedExample = "If hf = 3 eV and φ = 1 eV, K₁ = 2 eV. Doubling f gives hf' = 6 eV, K₂ = 5 eV (> 4 eV).",
                memoryTip = "Test with simple numbers like hf = 3, φ = 1!",
                examTrap = "'It doubles exactly' is the classic EUEE trap.",
                referencesText = "Grade 12 Physics, Modern Physics"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-PHYS-10-003",
                bankId = builtInBankId,
                subjectId = physicsId,
                gradeId = gradeIds.getValue(10),
                unitId = p10u1,
                topicId = p10t[0],
                difficulty = Difficulty.EASY.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Two resistors of 6 Ω and 3 Ω are connected in parallel across a 12 V battery. What is the total current drawn from the battery?",
                correctAnswer = "6 A",
                acceptedAnswersJson = """["6 A","6"]""",
                optionsJson = """["1.33 A","2 A","6 A","9 A"]""",
                hintsJson = """["Find equivalent resistance: 1/R_eq = 1/6 + 1/3.","Then I = V / R_eq."]""",
                explanation = "R_eq = (6 × 3) / (6 + 3) = 18 / 9 = 2 Ω. Total current I = 12 V / 2 Ω = 6 A.",
                whyCorrect = "Parallel combination lowers equivalent resistance below the smallest resistor.",
                commonMistakes = "Adding resistors in series (6 + 3 = 9 Ω → 1.33 A).",
                conceptExplanation = "In parallel circuits, voltage is shared equally and branch currents sum up.",
                workedExample = "I₁ = 12/6 = 2 A, I₂ = 12/3 = 4 A → I_total = 6 A.",
                memoryTip = "Parallel product over sum: R₁R₂ / (R₁ + R₂).",
                examTrap = "Adding resistances instead of reciprocals.",
                referencesText = "Grade 10 Physics, Electric Circuits"
            ),

            // CHEMISTRY
            QuestionEntity(
                uniqueCode = "EUEE-CHEM-12-001",
                bankId = builtInBankId,
                subjectId = chemId,
                gradeId = gradeIds.getValue(12),
                unitId = c12u1,
                topicId = c12t[0],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.NUMERICAL.name,
                questionText = "Calculate the pH of a 0.005 M aqueous solution of H₂SO₄ assuming complete dissociation of both protons at 25 °C.",
                correctAnswer = "2.0",
                acceptedAnswersJson = """["2","2.0","2.00"]""",
                optionsJson = """["2.0","2.3","1.7","3.0"]""",
                hintsJson = """["H₂SO₄ is diprotic: complete dissociation yields [H⁺] = 2 × 0.005 M.","pH = −log₁₀([H⁺])."]""",
                explanation = "[H⁺] = 2 × 0.005 M = 0.010 M = 10⁻² M. Thus pH = −log₁₀(10⁻²) = 2.0.",
                whyCorrect = "Each mole of H₂SO₄ produces 2 moles of H⁺ under full dissociation.",
                commonMistakes = "Using [H⁺] = 0.005 M directly, giving pH = 2.3.",
                conceptExplanation = "Stoichiometric normality matters for polyprotic strong acids.",
                workedExample = "0.0005 M Ca(OH)₂ → [OH⁻] = 0.001 M → pOH = 3 → pH = 11.",
                memoryTip = "Watch out for diprotic H₂SO₄ and diacidic Ca(OH)₂!",
                examTrap = "2.3 is included specifically for students who forget the factor of 2.",
                numericalTolerance = 0.05,
                referencesText = "Grade 12 Chemistry, Acid-Base Equilibria"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-CHEM-11-002",
                bankId = builtInBankId,
                subjectId = chemId,
                gradeId = gradeIds.getValue(11),
                unitId = c11u1,
                topicId = c11t[0],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "For the exothermic Haber reaction N₂(g) + 3H₂(g) ⇌ 2NH₃(g) (ΔH = −92 kJ/mol), which condition maximizes the equilibrium yield of NH₃?",
                correctAnswer = "High pressure and low temperature",
                acceptedAnswersJson = """["High pressure and low temperature"]""",
                optionsJson = """["High pressure and low temperature","Low pressure and high temperature","High pressure and high temperature","Adding a catalyst alone"]""",
                hintsJson = """["Compare moles of gas: 4 mol reactants → 2 mol products.","Exothermic forward reaction releases heat."]""",
                explanation = "Higher pressure shifts equilibrium toward fewer gas moles (2 < 4). Lower temperature favors the exothermic forward direction. Catalysts speed attainment without shifting equilibrium.",
                whyCorrect = "Le Chatelier's principle favors products under high P and low T.",
                commonMistakes = "Confusing kinetic rate (high T) with equilibrium yield (low T for exothermic).",
                conceptExplanation = "A catalyst lowers activation energy equally for forward and reverse reactions.",
                workedExample = "2SO₂(g) + O₂(g) ⇌ 2SO₃(g), ΔH < 0 also increases yield at high P, low T.",
                memoryTip = "Catalyst = faster equilibrium, NOT more product.",
                examTrap = "Industrial Haber process uses ~450 °C for rate, but equilibrium yield strictly prefers low T.",
                referencesText = "Grade 11 Chemistry, Chemical Equilibrium"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-CHEM-10-003",
                bankId = builtInBankId,
                subjectId = chemId,
                gradeId = gradeIds.getValue(10),
                unitId = c10u1,
                topicId = c10t[0],
                difficulty = Difficulty.EASY.name,
                questionType = QuestionType.SHORT_ANSWER.name,
                questionText = "Write the balanced chemical formula for the product formed when aluminum reacts completely with oxygen gas.",
                correctAnswer = "Al2O3",
                acceptedAnswersJson = """["Al2O3","Al₂O₃"]""",
                optionsJson = """["Al2O3","AlO","Al3O2","AlO2"]""",
                hintsJson = """["Al³⁺ combines with O²⁻ to achieve electrical neutrality."]""",
                explanation = "2 Al³⁺ + 3 O²⁻ → Al₂O₃ (aluminum oxide).",
                whyCorrect = "Crisscrossing valencies 3 and 2 gives Al₂O₃.",
                commonMistakes = "Writing AlO or Al₃O₂.",
                conceptExplanation = "Ionic compound formulas reflect the lowest whole-number ratio balancing total charge.",
                workedExample = "Mg²⁺ and N³⁻ → Mg₃N₂.",
                memoryTip = "4Al + 3O₂ → 2Al₂O₃.",
                examTrap = "Swapping subscripts incorrectly.",
                referencesText = "Grade 10 Chemistry, Chemical Bonding"
            ),

            // BIOLOGY
            QuestionEntity(
                uniqueCode = "EUEE-BIO-11-001",
                bankId = builtInBankId,
                subjectId = bioId,
                gradeId = gradeIds.getValue(11),
                unitId = b11u1,
                topicId = b11t[1],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "During transcription in eukaryotic cells, which enzyme synthesizes mRNA from a DNA template strand?",
                correctAnswer = "RNA polymerase II",
                acceptedAnswersJson = """["RNA polymerase II","RNA polymerase"]""",
                optionsJson = """["DNA polymerase III","RNA polymerase II","DNA helicase","DNA ligase"]""",
                hintsJson = """["Transcription makes RNA, not DNA.","Ligase joins Okazaki fragments during replication."]""",
                explanation = "RNA polymerase II binds to the promoter and synthesizes pre-mRNA in the 5' → 3' direction.",
                whyCorrect = "RNA polymerase II is dedicated to protein-coding mRNA synthesis in eukaryotes.",
                commonMistakes = "Confusing DNA replication enzymes (DNA polymerase III / helicase) with transcription.",
                conceptExplanation = "Central Dogma: DNA → (RNA polymerase) → mRNA → (Ribosome) → Protein.",
                workedExample = "Template 3'-TAC-5' transcribes to mRNA 5'-AUG-3'.",
                memoryTip = "RNA Pol II = Messenger (2 letters mR).",
                examTrap = "DNA polymerase III performs replication, not transcription.",
                referencesText = "Grade 11 Biology, Molecular Genetics"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-BIO-10-002",
                bankId = builtInBankId,
                subjectId = bioId,
                gradeId = gradeIds.getValue(10),
                unitId = b10u1,
                topicId = b10t[1],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Where in a eukaryotic cell does the Krebs (citric acid) cycle occur?",
                correctAnswer = "Mitochondrial matrix",
                acceptedAnswersJson = """["Mitochondrial matrix"]""",
                optionsJson = """["Cytoplasm (cytosol)","Mitochondrial matrix","Inner mitochondrial membrane","Chloroplast stroma"]""",
                hintsJson = """["Glycolysis happens in the cytosol; oxidative phosphorylation is on the cristae."]""",
                explanation = "Pyruvate enters the mitochondrion and is oxidized in the mitochondrial matrix via the Krebs cycle.",
                whyCorrect = "Soluble Krebs cycle enzymes reside in the mitochondrial matrix.",
                commonMistakes = "Confusing the matrix (Krebs cycle) with cristae/inner membrane (electron transport chain).",
                conceptExplanation = "Compartmentalization separates glycolysis (cytosol), Krebs (matrix), and ETC (cristae).",
                workedExample = "Each glucose yields 2 turns of the Krebs cycle in the matrix.",
                memoryTip = "Glycolysis = Cytosol, Krebs = Matrix, ETC = Cristae.",
                examTrap = "Inner mitochondrial membrane hosts the ETC, not the Krebs cycle.",
                referencesText = "Grade 10 Biology, Cellular Respiration"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-BIO-09-003",
                bankId = builtInBankId,
                subjectId = bioId,
                gradeId = gradeIds.getValue(9),
                unitId = b9u1,
                topicId = b9t[1],
                difficulty = Difficulty.EASY.name,
                questionType = QuestionType.TRUE_FALSE.name,
                questionText = "True or False: Active transport moves solutes across a plasma membrane against their concentration gradient without requiring cellular ATP.",
                correctAnswer = "False",
                acceptedAnswersJson = """["False","F"]""",
                optionsJson = """["True","False"]""",
                hintsJson = """["Moving uphill against a gradient requires energy input."]""",
                explanation = "False. Active transport requires metabolic energy (typically ATP hydrolysis, e.g., Na⁺/K⁺ pump) to move substances from low to high concentration.",
                whyCorrect = "Passive diffusion/osmosis require no ATP; active transport always requires energy.",
                commonMistakes = "Confusing facilitated diffusion (passive) with active transport.",
                conceptExplanation = "Active transport maintains electrochemical gradients across membranes.",
                workedExample = "Root hair cells absorb mineral ions via active transport.",
                memoryTip = "Active = ATP required.",
                examTrap = "Watch for the qualifier 'without requiring cellular ATP'.",
                referencesText = "Grade 9 Biology, Cell Transport"
            ),

            // ENGLISH
            QuestionEntity(
                uniqueCode = "EUEE-ENG-12-001",
                bankId = builtInBankId,
                subjectId = englishId,
                gradeId = gradeIds.getValue(12),
                unitId = e12u1,
                topicId = e12t[0],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Choose the grammatically correct completion: 'Had the students revised the past papers thoroughly, they _____ the entrance exam with greater confidence.'",
                correctAnswer = "would have approached",
                acceptedAnswersJson = """["would have approached"]""",
                optionsJson = """["will approach","would approach","would have approached","had approached"]""",
                hintsJson = """["Identify the inverted third conditional: 'Had + subject + V3' = 'If + subject + had + V3'."]""",
                explanation = "'Had the students revised...' is an inverted Type 3 conditional referring to an unreal past condition, requiring 'would have + past participle' in the main clause.",
                whyCorrect = "Third conditional structure pairs past perfect condition with 'would have + V3'.",
                commonMistakes = "Using 'would approach' (Type 2 conditional) for a past counterfactual.",
                conceptExplanation = "Inverted conditionals omit 'if' and front 'Had', 'Were', or 'Should'.",
                workedExample = "Had I known earlier, I would have called you.",
                memoryTip = "Had + V3 → would have + V3.",
                examTrap = "'would approach' looks natural at first glance but mismatches past time.",
                referencesText = "Grade 12 English, Conditional Clauses"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-ENG-11-002",
                bankId = builtInBankId,
                subjectId = englishId,
                gradeId = gradeIds.getValue(11),
                unitId = e11u1,
                topicId = e11t[0],
                difficulty = Difficulty.EASY.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Select the meaning closest to the underlined phrasal verb: 'Because of the heavy rain, the committee decided to call off the outdoor science fair.'",
                correctAnswer = "cancel",
                acceptedAnswersJson = """["cancel"]""",
                optionsJson = """["postpone","cancel","relocate","announce"]""",
                hintsJson = """["Distinguish 'call off' from 'put off'."]""",
                explanation = "'Call off' means to cancel an event entirely; 'put off' means to postpone.",
                whyCorrect = "Standard idiomatic meaning of 'call off'.",
                commonMistakes = "Confusing 'call off' (cancel) with 'put off' (postpone).",
                conceptExplanation = "Phrasal verb particles dramatically change verb meaning.",
                workedExample = "The match was put off until Sunday (postponed) vs. called off (canceled).",
                memoryTip = "Call OFF = switch OFF completely (cancel).",
                examTrap = "'Postpone' is always placed alongside 'cancel' in EUEE English papers.",
                referencesText = "Grade 11 English, Phrasal Verbs"
            ),

            // APTITUDE
            QuestionEntity(
                uniqueCode = "EUEE-APT-12-001",
                bankId = builtInBankId,
                subjectId = aptitudeId,
                gradeId = gradeIds.getValue(12),
                unitId = a12u1,
                topicId = a12t[0],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.NUMERICAL.name,
                questionText = "Find the next term in the number sequence: 3, 7, 15, 31, 63, _____",
                correctAnswer = "127",
                acceptedAnswersJson = """["127"]""",
                optionsJson = """["95","125","127","129"]""",
                hintsJson = """["Examine differences: +4, +8, +16, +32... or the rule 2n + 1."]""",
                explanation = "Each term is 2×(previous) + 1: 63 × 2 + 1 = 127 (also differences double: +64 → 63 + 64 = 127).",
                whyCorrect = "Both the recurrence aₙ = 2aₙ₋₁ + 1 and doubling differences yield 127.",
                commonMistakes = "Adding 32 again instead of doubling to 64.",
                conceptExplanation = "Notice powers of 2 minus 1: 2²−1, 2³−1, 2⁴−1, 2⁵−1, 2⁶−1, 2⁷−1 = 127.",
                workedExample = "2⁷ − 1 = 128 − 1 = 127.",
                memoryTip = "Check both differences and powers of 2!",
                examTrap = "125 and 129 sit right beside 127 to catch arithmetic slips.",
                numericalTolerance = 0.001,
                referencesText = "EUEE Scholastic Aptitude, Quantitative Reasoning"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-APT-11-002",
                bankId = builtInBankId,
                subjectId = aptitudeId,
                gradeId = gradeIds.getValue(11),
                unitId = a11u1,
                topicId = a11t[0],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "SCALPEL : SURGEON :: _____ : _____",
                correctAnswer = "CHISEL : SCULPTOR",
                acceptedAnswersJson = """["CHISEL : SCULPTOR"]""",
                optionsJson = """["CHISEL : SCULPTOR","PATIENT : HOSPITAL","WOOD : CARPENTER","POEM : AUTHOR"]""",
                hintsJson = """["Identify the bridge: [Precision cutting/shaping tool] : [Profession that uses it]."]""",
                explanation = "A scalpel is the primary precision hand tool used by a surgeon, just as a chisel is the precision tool used by a sculptor.",
                whyCorrect = "Preserves exact Tool : Practitioner relationship and word order.",
                commonMistakes = "Choosing 'WOOD : CARPENTER' (material : practitioner) or 'POEM : AUTHOR' (product : creator).",
                conceptExplanation = "Always verify both relationship type and directionality in analogies.",
                workedExample = "BRUSH : PAINTER :: TELESCOPE : ASTRONOMER.",
                memoryTip = "Build a crisp sentence bridge: 'A [X] is a tool wielded by a [Y].'",
                examTrap = "Distractors match the domain (carpenter, author) while reversing or shifting the bridge.",
                referencesText = "EUEE Scholastic Aptitude, Verbal Analogies"
            ),

            // ADDITIONAL CURATED EUEE QUESTIONS (Grades 9-12 across all 6 subjects)
            QuestionEntity(
                uniqueCode = "EUEE-MATH-12-005",
                bankId = builtInBankId,
                subjectId = mathId,
                gradeId = gradeIds.getValue(12),
                unitId = m12u1,
                topicId = m12t[2],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.NUMERICAL.name,
                questionText = "Evaluate the definite integral: ∫₀² (3x² + 2x) dx",
                correctAnswer = "12",
                acceptedAnswersJson = """["12","12.0"]""",
                optionsJson = """["8","10","12","16"]""",
                hintsJson = """["Find an antiderivative: F(x) = x³ + x².","Evaluate F(2) − F(0)."]""",
                explanation = "∫(3x² + 2x) dx = x³ + x². At x = 2: 2³ + 2² = 8 + 4 = 12. At x = 0: 0.",
                whyCorrect = "Fundamental Theorem of Calculus yields F(2) − F(0) = 12.",
                commonMistakes = "Differentiating instead of integrating (getting 6x + 2).",
                conceptExplanation = "Power rule for integration: ∫xⁿ dx = xⁿ⁺¹/(n+1) for n ≠ −1.",
                workedExample = "∫₀¹ 4x³ dx = [x⁴]₀¹ = 1.",
                memoryTip = "Add 1 to exponent, divide by new exponent.",
                examTrap = "Plugging x = 2 directly into 3x² + 2x gives 16.",
                numericalTolerance = 0.01,
                referencesText = "Grade 12 Mathematics, Integral Calculus"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-MATH-09-006",
                bankId = builtInBankId,
                subjectId = mathId,
                gradeId = gradeIds.getValue(9),
                unitId = m9u1,
                topicId = m9t[1],
                difficulty = Difficulty.EASY.name,
                questionType = QuestionType.NUMERICAL.name,
                questionText = "In a class of 40 students, 24 study Physics, 22 study Chemistry, and 10 study both. How many students study neither subject?",
                correctAnswer = "4",
                acceptedAnswersJson = """["4"]""",
                optionsJson = """["4","6","10","14"]""",
                hintsJson = """["Use inclusion-exclusion: n(P ∪ C) = n(P) + n(C) − n(P ∩ C)."]""",
                explanation = "n(P ∪ C) = 24 + 22 − 10 = 36. Neither = 40 − 36 = 4 students.",
                whyCorrect = "Inclusion-exclusion avoids double counting the 10 students in the intersection.",
                commonMistakes = "Adding 24 + 22 = 46 without subtracting 10.",
                conceptExplanation = "Complement of union: n((A ∪ B)') = n(U) − n(A ∪ B).",
                workedExample = "If n(A)=15, n(B)=12, n(A∩B)=5, then n(A∪B)=22.",
                memoryTip = "Add both sets, subtract overlap once!",
                examTrap = "Option 6 comes from subtracting incorrectly or confusing 'only Physics'.",
                numericalTolerance = 0.01,
                referencesText = "Grade 9 Mathematics, Sets"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-PHYS-09-004",
                bankId = builtInBankId,
                subjectId = physicsId,
                gradeId = gradeIds.getValue(9),
                unitId = p9u1,
                topicId = p9t[0],
                difficulty = Difficulty.EASY.name,
                questionType = QuestionType.NUMERICAL.name,
                questionText = "A car accelerates uniformly from rest at 3.0 m/s² for 4.0 s. What distance (in meters) does it travel during this interval?",
                correctAnswer = "24",
                acceptedAnswersJson = """["24","24 m","24.0"]""",
                optionsJson = """["12 m","18 m","24 m","48 m"]""",
                hintsJson = """["Use Δx = v₀t + ½at² with v₀ = 0."]""",
                explanation = "Δx = 0 + ½(3.0)(4.0)² = 0.5 × 3 × 16 = 24 m.",
                whyCorrect = "Average velocity is (0 + 12)/2 = 6 m/s over 4 s → 24 m.",
                commonMistakes = "Forgetting the ½ factor (yielding 48 m) or forgetting to square t (yielding 12 m).",
                conceptExplanation = "Distance under uniform acceleration grows quadratically with time.",
                workedExample = "At a = 2 m/s² for 5 s from rest: Δx = ½(2)(25) = 25 m.",
                memoryTip = "v_avg × t = (v₀ + v)/2 × t works whenever acceleration is constant.",
                examTrap = "12 m is the final velocity (v = at), not the distance!",
                numericalTolerance = 0.1,
                referencesText = "Grade 9 Physics, Kinematics"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-PHYS-11-005",
                bankId = builtInBankId,
                subjectId = physicsId,
                gradeId = gradeIds.getValue(11),
                unitId = p11u1,
                topicId = p11t[1],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.NUMERICAL.name,
                questionText = "A 2 kg trolley moving right at 6 m/s collides and sticks to a stationary 4 kg trolley. What is their common velocity (in m/s) immediately after collision?",
                correctAnswer = "2",
                acceptedAnswersJson = """["2","2.0","2 m/s"]""",
                optionsJson = """["1 m/s","2 m/s","3 m/s","4 m/s"]""",
                hintsJson = """["Apply conservation of linear momentum: m₁v₁ = (m₁ + m₂)v_f."]""",
                explanation = "Total initial momentum = 2 × 6 + 4 × 0 = 12 kg·m/s. Total mass = 6 kg → v_f = 12 / 6 = 2 m/s.",
                whyCorrect = "Momentum is conserved in an isolated inelastic collision even though kinetic energy is not.",
                commonMistakes = "Equating kinetic energy (½m₁v₁² = ½(m₁+m₂)v²) in a perfectly inelastic collision.",
                conceptExplanation = "Sticking together defines a completely inelastic collision.",
                workedExample = "3 kg at 4 m/s + 1 kg at 0 → v_f = 12 / 4 = 3 m/s.",
                memoryTip = "Sticky collision? Conserve p, NOT KE!",
                examTrap = "Dividing momentum by 4 kg instead of total mass (2 + 4 = 6 kg).",
                numericalTolerance = 0.05,
                referencesText = "Grade 11 Physics, Momentum"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-PHYS-10-006",
                bankId = builtInBankId,
                subjectId = physicsId,
                gradeId = gradeIds.getValue(10),
                unitId = p10u1,
                topicId = p10t[1],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "A parallel-plate capacitor is connected to a constant-voltage battery while the plate separation d is halved. What happens to the stored charge Q?",
                correctAnswer = "It doubles",
                acceptedAnswersJson = """["It doubles"]""",
                optionsJson = """["It halves","It remains constant","It doubles","It quadruples"]""",
                hintsJson = """["C = ε₀A/d and Q = C·V with V held fixed."]""",
                explanation = "Halving d doubles capacitance C = ε₀A/d. Because the battery keeps V constant, Q = CV doubles.",
                whyCorrect = "Both C and Q scale inversely with plate separation at constant potential.",
                commonMistakes = "Assuming Q stays constant (which happens only when the battery is disconnected).",
                conceptExplanation = "Battery connected ⇒ constant V; battery disconnected ⇒ constant Q.",
                workedExample = "If C doubles from 4 μF to 8 μF at 10 V, Q rises from 40 μC to 80 μC.",
                memoryTip = "Connected = fixed V; Isolated = fixed Q.",
                examTrap = "Exam questions swap 'connected to battery' and 'disconnected'.",
                referencesText = "Grade 10 Physics, Electrostatics"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-CHEM-09-004",
                bankId = builtInBankId,
                subjectId = chemId,
                gradeId = gradeIds.getValue(9),
                unitId = c9u1,
                topicId = c9t[0],
                difficulty = Difficulty.EASY.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Which ground-state electron configuration corresponds to Copper (Cu, Z = 29)?",
                correctAnswer = "[Ar] 4s¹ 3d¹⁰",
                acceptedAnswersJson = """["[Ar] 4s1 3d10","[Ar] 4s¹ 3d¹⁰"]""",
                optionsJson = """["[Ar] 4s² 3d⁹","[Ar] 4s¹ 3d¹⁰","[Ar] 4s² 3d¹⁰","[Ar] 4s⁰ 3d¹⁰"]""",
                hintsJson = """["Remember the half-filled and filled d-subshell stability exceptions (Cr and Cu)."]""",
                explanation = "Copper promotes one 4s electron to achieve a completely filled 3d¹⁰ subshell: [Ar] 4s¹ 3d¹⁰.",
                whyCorrect = "Filled 3d¹⁰ lowers electron-electron repulsion and increases exchange stability.",
                commonMistakes = "Writing the naive Aufbau prediction [Ar] 4s² 3d⁹.",
                conceptExplanation = "Cr (Z=24: 4s¹3d⁵) and Cu (Z=29: 4s¹3d¹⁰) are mandatory EUEE exceptions.",
                workedExample = "Chromium (Z=24) = [Ar] 4s¹ 3d⁵.",
                memoryTip = "Cu & Cr steal 1 from 4s to stabilize 3d!",
                examTrap = "[Ar] 4s² 3d⁹ is always option A.",
                referencesText = "Grade 9 Chemistry, Atomic Structure"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-CHEM-12-005",
                bankId = builtInBankId,
                subjectId = chemId,
                gradeId = gradeIds.getValue(12),
                unitId = c12u1,
                topicId = c12t[1],
                difficulty = Difficulty.HARD.name,
                questionType = QuestionType.NUMERICAL.name,
                questionText = "Given E°(Zn²⁺/Zn) = −0.76 V and E°(Cu²⁺/Cu) = +0.34 V, calculate the standard cell potential E°_cell (in Volts) for the spontaneous Daniell cell.",
                correctAnswer = "1.10",
                acceptedAnswersJson = """["1.1","1.10","1.10 V"]""",
                optionsJson = """["0.42 V","1.10 V","−1.10 V","−0.42 V"]""",
                hintsJson = """["E°_cell = E°_cathode − E°_anode. The higher reduction potential is the cathode."]""",
                explanation = "Cu²⁺/Cu (+0.34 V) is reduced at the cathode; Zn (−0.76 V) is oxidized at the anode. E°_cell = 0.34 − (−0.76) = +1.10 V.",
                whyCorrect = "Spontaneous galvanic cells always have E°_cell > 0.",
                commonMistakes = "Adding 0.34 + (−0.76) = −0.42 V without flipping the anode sign.",
                conceptExplanation = "Cathode (reduction) − Anode (oxidation) using standard reduction potentials.",
                workedExample = "Ag⁺/Ag (+0.80 V) vs Cu²⁺/Cu (+0.34 V): E° = 0.80 − 0.34 = 0.46 V.",
                memoryTip = "RED CAT (Reduction at Cathode), AN OX (Oxidation at Anode).",
                examTrap = "0.42 V comes from 0.76 − 0.34.",
                numericalTolerance = 0.02,
                referencesText = "Grade 12 Chemistry, Electrochemistry"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-CHEM-11-006",
                bankId = builtInBankId,
                subjectId = chemId,
                gradeId = gradeIds.getValue(11),
                unitId = c11u1,
                topicId = c11t[1],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "For the reaction 2A + B → C, doubling [A] quadruples the rate, and doubling [B] leaves the rate unchanged. What is the rate law?",
                correctAnswer = "Rate = k[A]²",
                acceptedAnswersJson = """["Rate = k[A]^2","Rate = k[A]²"]""",
                optionsJson = """["Rate = k[A]²[B]","Rate = k[A]²","Rate = k[A][B]","Rate = k[A]⁴"]""",
                hintsJson = """["2^m = 4 gives order in A; 2^n = 1 gives order in B."]""",
                explanation = "Since 2² = 4, the reaction is 2nd order in A. Since 2⁰ = 1, it is zero order in B. Thus Rate = k[A]².",
                whyCorrect = "Reaction orders must be determined experimentally, not from stoichiometric coefficients.",
                commonMistakes = "Copying the stoichiometric coefficients 2 and 1 into Rate = k[A]²[B].",
                conceptExplanation = "Zero-order reactants do not appear in the rate expression.",
                workedExample = "If tripling [X] multiplies rate by 9, order in X is 2 (3² = 9).",
                memoryTip = "Stoichiometry ≠ Rate Law (unless elementary step)!",
                examTrap = "Rate = k[A]²[B] matches the balanced equation coefficients as a deliberate trap.",
                referencesText = "Grade 11 Chemistry, Chemical Kinetics"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-BIO-11-004",
                bankId = builtInBankId,
                subjectId = bioId,
                gradeId = gradeIds.getValue(11),
                unitId = b11u1,
                topicId = b11t[0],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "In a Mendelian dihybrid cross between two heterozygous tall, round-seeded pea plants (TtRr × TtRr), what phenotypic ratio is expected in the F₂ generation assuming independent assortment?",
                correctAnswer = "9:3:3:1",
                acceptedAnswersJson = """["9:3:3:1"]""",
                optionsJson = """["3:1","1:2:1","9:3:3:1","1:1:1:1"]""",
                hintsJson = """["Combine two independent monohybrid ratios: (3:1) × (3:1)."]""",
                explanation = "Independent assortment of two heterozygous traits produces 9/16 dominant-dominant, 3/16 dominant-recessive, 3/16 recessive-dominant, and 1/16 double-recessive (9:3:3:1).",
                whyCorrect = "Product rule of independent probabilities gives (3/4)(3/4) : (3/4)(1/4) : (1/4)(3/4) : (1/4)(1/4).",
                commonMistakes = "Confusing 9:3:3:1 (dihybrid F₂) with 1:1:1:1 (dihybrid test cross TtRr × ttrr).",
                conceptExplanation = "Independent assortment holds when genes lie on different chromosomes or far apart.",
                workedExample = "Probability of ttRr = (1/4)(1/2) = 1/8.",
                memoryTip = "Dihybrid F₂ = 9:3:3:1; Dihybrid test cross = 1:1:1:1.",
                examTrap = "1:1:1:1 is only for TtRr × ttrr.",
                referencesText = "Grade 11 Biology, Mendelian Genetics"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-BIO-12-005",
                bankId = builtInBankId,
                subjectId = bioId,
                gradeId = gradeIds.getValue(12),
                unitId = b12u1,
                topicId = b12t[0],
                difficulty = Difficulty.EASY.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Wings of bats (mammals) and wings of insects perform the same flight function but evolved from completely different ancestral structures. What are these structures called?",
                correctAnswer = "Analogous structures",
                acceptedAnswersJson = """["Analogous structures","Analogous"]""",
                optionsJson = """["Homologous structures","Analogous structures","Vestigial organs","Co-evolved traits"]""",
                hintsJson = """["Convergent evolution produces similar function from different origins."]""",
                explanation = "Analogous structures arise through convergent evolution due to similar environmental pressures despite different anatomical origins.",
                whyCorrect = "Different anatomy + similar function = analogous (convergent evolution).",
                commonMistakes = "Confusing analogous (convergent) with homologous (divergent evolution from a common ancestor).",
                conceptExplanation = "Human arm, bat wing, and whale flipper are homologous; bat wing and butterfly wing are analogous.",
                workedExample = "Streamlined body of dolphins (mammals) and sharks (fish) is analogous.",
                memoryTip = "Homo = same ancestor; Ana = different origin, same job.",
                examTrap = "Bat wings vs bird/human forelimbs are homologous, but bat vs insect are analogous.",
                referencesText = "Grade 12 Biology, Evolution"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-BIO-10-006",
                bankId = builtInBankId,
                subjectId = bioId,
                gradeId = gradeIds.getValue(10),
                unitId = b10u1,
                topicId = b10t[0],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "How does a competitive inhibitor alter the rate of an enzyme-catalyzed reaction?",
                correctAnswer = "Binds the active site and can be overcome by increasing substrate concentration",
                acceptedAnswersJson = """["Binds the active site and can be overcome by increasing substrate concentration"]""",
                optionsJson = """["Binds an allosteric site permanently","Denatures the enzyme immediately","Binds the active site and can be overcome by increasing substrate concentration","Lowers activation energy even further"]""",
                hintsJson = """["Competitive inhibitors resemble the substrate and compete for the same site."]""",
                explanation = "Competitive inhibitors reversibly occupy the active site; flooding the system with high substrate concentration outcompetes the inhibitor and restores V_max.",
                whyCorrect = "V_max remains unchanged while apparent K_m increases.",
                commonMistakes = "Confusing competitive inhibition with non-competitive allosteric inhibition.",
                conceptExplanation = "Non-competitive inhibitors bind elsewhere and cannot be overcome by excess substrate.",
                workedExample = "Malonate competitively inhibits succinate dehydrogenase in the Krebs cycle.",
                memoryTip = "Competitive = Active site race (more substrate wins!).",
                examTrap = "Allosteric site binding belongs to non-competitive inhibition.",
                referencesText = "Grade 10 Biology, Enzymes"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-ENG-09-003",
                bankId = builtInBankId,
                subjectId = englishId,
                gradeId = gradeIds.getValue(9),
                unitId = e9u1,
                topicId = e9t[1],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Choose the best modal expression: 'The laboratory floor is completely wet; someone _____ the distilled water tap open last night.'",
                correctAnswer = "must have left",
                acceptedAnswersJson = """["must have left"]""",
                optionsJson = """["should leave","must have left","could leave","would leave"]""",
                hintsJson = """["Strong logical deduction about a past event uses 'must have + V3'."]""",
                explanation = "'Must have + past participle' expresses near-certainty about a past cause based on present evidence (wet floor).",
                whyCorrect = "Matches past time reference ('last night') with strong logical deduction.",
                commonMistakes = "Picking 'should leave' or present modals for a past action.",
                conceptExplanation = "Modals of deduction: must have (certain), might/could have (possible), can't have (impossible).",
                workedExample = "She isn't answering; she must have gone to sleep.",
                memoryTip = "Evidence now + past cause = must have + V3.",
                examTrap = "Do not confuse 'should have left' (regret/duty) with 'must have left' (deduction).",
                referencesText = "Grade 9 English, Modal Auxiliaries"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-ENG-12-004",
                bankId = builtInBankId,
                subjectId = englishId,
                gradeId = gradeIds.getValue(12),
                unitId = e12u1,
                topicId = e12t[1],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Identify the grammatically correct sentence regarding subject-verb agreement:",
                correctAnswer = "Neither the biology teacher nor the students were informed of the schedule change.",
                acceptedAnswersJson = """["Neither the biology teacher nor the students were informed of the schedule change."]""",
                optionsJson = """["Each of the candidates have submitted their admission card.","Neither the biology teacher nor the students were informed of the schedule change.","The number of scholarship applicants are rising rapidly.","Physics, along with Chemistry, are required for engineering."]""",
                hintsJson = """["With 'neither...nor', the verb agrees with the nearer noun ('students')."]""",
                explanation = "Under the proximity rule for 'neither...nor', 'students' is plural so 'were' is correct. 'Each', 'The number of', and 'Physics along with...' all take singular verbs.",
                whyCorrect = "Proximity agreement matches plural 'students' with plural 'were'.",
                commonMistakes = "Treating 'The number of...' as plural (only 'A number of...' is plural).",
                conceptExplanation = "Prepositional phrases like 'along with' do not compound a singular subject.",
                workedExample = "Neither the students nor the teacher was late.",
                memoryTip = "Or / Nor → match the closer noun!",
                examTrap = "'A number of' vs 'The number of' appears frequently on the EUEE English paper.",
                referencesText = "Grade 12 English, Grammar & Usage"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-ENG-11-005",
                bankId = builtInBankId,
                subjectId = englishId,
                gradeId = gradeIds.getValue(11),
                unitId = e11u1,
                topicId = e11t[1],
                difficulty = Difficulty.EASY.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Select the word most nearly OPPOSITE in meaning to 'MITIGATE' in: 'Terracing hillsides helps mitigate severe soil erosion.'",
                correctAnswer = "aggravate",
                acceptedAnswersJson = """["aggravate"]""",
                optionsJson = """["alleviate","aggravate","prevent","measure"]""",
                hintsJson = """["'Mitigate' means to make less severe or intense."]""",
                explanation = "Mitigate means to lessen or ease harm; its antonym is aggravate (to worsen or intensify).",
                whyCorrect = "'Alleviate' is a synonym, while 'aggravate' is the direct antonym.",
                commonMistakes = "Misreading the prompt and choosing the synonym ('alleviate').",
                conceptExplanation = "Always underline 'OPPOSITE' / 'ANTONYM' vs 'CLOSEST' / 'SYNONYM' in vocabulary questions.",
                workedExample = "Mitigate pain ↔ aggravate pain.",
                memoryTip = "Watch the question stem word: OPPOSITE ≠ SIMILAR!",
                examTrap = "Option A ('alleviate') is the #1 synonym trap.",
                referencesText = "Grade 11 English, Vocabulary"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-APT-12-003",
                bankId = builtInBankId,
                subjectId = aptitudeId,
                gradeId = gradeIds.getValue(12),
                unitId = a12u1,
                topicId = a12t[0],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.NUMERICAL.name,
                questionText = "A laptop's price is increased by 25% and then the new price is discounted by 20%. What is the net percentage change from the original price?",
                correctAnswer = "0",
                acceptedAnswersJson = """["0","0%","no change"]""",
                optionsJson = """["0%","+5%","−5%","+2.5%"]""",
                hintsJson = """["Test starting from 100 ETB, or use a + b + (ab)/100."]""",
                explanation = "Start at 100 → +25% = 125 → −20% of 125 is −25 → 125 − 25 = 100. Net change = 0%.",
                whyCorrect = "25 − 20 + (25 × −20)/100 = 5 − 5 = 0%.",
                commonMistakes = "Subtracting percentages linearly: 25% − 20% = +5%.",
                conceptExplanation = "Successive percentage changes apply to different base amounts.",
                workedExample = "+10% then −10% yields 100 → 110 → 99 (−1%).",
                memoryTip = "Pick 100 as your test base for percentage problems!",
                examTrap = "+5% is the linear subtraction trap.",
                numericalTolerance = 0.01,
                referencesText = "EUEE Aptitude, Quantitative Reasoning"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-APT-11-004",
                bankId = builtInBankId,
                subjectId = aptitudeId,
                gradeId = gradeIds.getValue(11),
                unitId = a11u1,
                topicId = a11t[1],
                difficulty = Difficulty.HARD.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Statements: (1) All laboratories in the school are well-equipped. (2) Some rooms on the second floor are laboratories. Which conclusion follows logically?",
                correctAnswer = "Some rooms on the second floor are well-equipped.",
                acceptedAnswersJson = """["Some rooms on the second floor are well-equipped."]""",
                optionsJson = """["All rooms on the second floor are well-equipped.","Some rooms on the second floor are well-equipped.","No well-equipped rooms are on the first floor.","All well-equipped rooms are laboratories."]""",
                hintsJson = """["The rooms that are laboratories on the second floor must inherit the property of all laboratories."]""",
                explanation = "Since those second-floor rooms are laboratories, and all laboratories are well-equipped, at least those rooms on the second floor are well-equipped.",
                whyCorrect = "Valid existential syllogism ('Some S are M; All M are P ⇒ Some S are P').",
                commonMistakes = "Over-generalizing 'Some' to 'All' or reversing 'All A are B' to 'All B are A'.",
                conceptExplanation = "Never assume universal converse from 'All A are B'.",
                workedExample = "Some fruits are apples; all apples have seeds ⇒ some fruits have seeds.",
                memoryTip = "'Some + All' can only yield a 'Some' conclusion!",
                examTrap = "Option A upgrades 'Some' to 'All'.",
                referencesText = "EUEE Aptitude, Logical Deduction"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-APT-12-005",
                bankId = builtInBankId,
                subjectId = aptitudeId,
                gradeId = gradeIds.getValue(12),
                unitId = a12u1,
                topicId = a12t[0],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.NUMERICAL.name,
                questionText = "Pipe A can fill a water tank in 6 hours and Pipe B can fill it in 3 hours. Working together, how many hours will it take to fill the tank?",
                correctAnswer = "2",
                acceptedAnswersJson = """["2","2 hours","2.0"]""",
                optionsJson = """["1.5 hours","2 hours","4.5 hours","9 hours"]""",
                hintsJson = """["Combine rates: 1/6 + 1/3 = 1/T."]""",
                explanation = "Combined rate = 1/6 + 1/3 = 3/6 = 1/2 tank per hour → T = 2 hours.",
                whyCorrect = "Shortcut (6 × 3) / (6 + 3) = 18 / 9 = 2 hours.",
                commonMistakes = "Taking the arithmetic average (6 + 3)/2 = 4.5 hours.",
                conceptExplanation = "Rates (work per unit time) add directly; times do not.",
                workedExample = "4 hr and 12 hr together: (4×12)/(4+12) = 48/16 = 3 hr.",
                memoryTip = "Product over sum: (T₁ × T₂)/(T₁ + T₂).",
                examTrap = "4.5 hours is the naive mean of 6 and 3.",
                numericalTolerance = 0.01,
                referencesText = "EUEE Aptitude, Work-Rate Problems"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-MATH-11-007",
                bankId = builtInBankId,
                subjectId = mathId,
                gradeId = gradeIds.getValue(11),
                unitId = m11u1,
                topicId = m11t[1],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.NUMERICAL.name,
                questionText = "Find the radius of the circle given by x² + y² − 6x + 8y = 0.",
                correctAnswer = "5",
                acceptedAnswersJson = """["5","5.0"]""",
                optionsJson = """["3","4","5","25"]""",
                hintsJson = """["Complete the square for x and y: (x − 3)² + (y + 4)² = r²."]""",
                explanation = "(x² − 6x + 9) + (y² + 8y + 16) = 9 + 16 = 25 ⇒ (x − 3)² + (y + 4)² = 5². Radius r = 5.",
                whyCorrect = "r² = 25 ⇒ r = 5 (3-4-5 right triangle center distance).",
                commonMistakes = "Reporting r² = 25 instead of taking √25 = 5.",
                conceptExplanation = "For x² + y² + Dx + Ey + F = 0, r = ½√(D² + E² − 4F).",
                workedExample = "x² + y² − 2x − 4y = 4 ⇒ (x−1)² + (y−2)² = 9 ⇒ r = 3.",
                memoryTip = "Always take the square root of the right-hand side!",
                examTrap = "25 is r², placed as option D.",
                numericalTolerance = 0.01,
                referencesText = "Grade 11 Mathematics, Conic Sections"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-CHEM-10-007",
                bankId = builtInBankId,
                subjectId = chemId,
                gradeId = gradeIds.getValue(10),
                unitId = c10u1,
                topicId = c10t[1],
                difficulty = Difficulty.MEDIUM.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "According to VSEPR theory, what is the molecular geometry and approximate bond angle of ammonia (NH₃)?",
                correctAnswer = "Trigonal pyramidal, ~107°",
                acceptedAnswersJson = """["Trigonal pyramidal, ~107°","Trigonal pyramidal"]""",
                optionsJson = """["Trigonal planar, 120°","Tetrahedral, 109.5°","Trigonal pyramidal, ~107°","Bent, ~104.5°"]""",
                hintsJson = """["Nitrogen has 3 bonding pairs and 1 lone pair (AX₃E)."]""",
                explanation = "With 4 electron domains (3 bonds + 1 lone pair), the electron geometry is tetrahedral, while the molecular shape is trigonal pyramidal with lone-pair compression to ~107°.",
                whyCorrect = "Lone pair repulsion is greater than bonding-pair repulsion, compressing 109.5° to ~107°.",
                commonMistakes = "Confusing electron-domain geometry (tetrahedral) with molecular geometry (trigonal pyramidal).",
                conceptExplanation = "Compare CH₄ (AX₄, 109.5°), NH₃ (AX₃E, 107°), and H₂O (AX₂E₂, 104.5°).",
                workedExample = "BF₃ (AX₃, no lone pair) is trigonal planar at 120°.",
                memoryTip = "Each lone pair squeezes the bond angle by ~2.5°!",
                examTrap = "Trigonal planar applies to BF₃, not NH₃.",
                referencesText = "Grade 10 Chemistry, VSEPR & Bonding"
            ),
            QuestionEntity(
                uniqueCode = "EUEE-BIO-09-007",
                bankId = builtInBankId,
                subjectId = bioId,
                gradeId = gradeIds.getValue(9),
                unitId = b9u1,
                topicId = b9t[0],
                difficulty = Difficulty.EASY.name,
                questionType = QuestionType.MULTIPLE_CHOICE.name,
                questionText = "Which organelle modifies, sorts, and packages proteins into vesicles for secretion outside the eukaryotic cell?",
                correctAnswer = "Golgi apparatus",
                acceptedAnswersJson = """["Golgi apparatus","Golgi body"]""",
                optionsJson = """["Smooth ER","Golgi apparatus","Lysosome","Peroxisome"]""",
                hintsJson = """["Think of the cell's post office receiving vesicles from the Rough ER."]""",
                explanation = "The Golgi apparatus receives proteins from the Rough ER, attaches carbohydrate/phosphate tags, and packages them into secretory vesicles.",
                whyCorrect = "Golgi stacks (cisternae) handle sorting and secretory packaging.",
                commonMistakes = "Confusing synthesis on Rough ER with sorting/packaging in the Golgi apparatus.",
                conceptExplanation = "Endomembrane flow: Rough ER → Transport Vesicle → Golgi → Secretory Vesicle → Plasma Membrane.",
                workedExample = "Pancreatic acinar cells have extensive Rough ER and Golgi bodies for enzyme secretion.",
                memoryTip = "Golgi = Gift wrapper & shipping center.",
                examTrap = "Smooth ER synthesizes lipids and detoxifies rather than packaging proteins.",
                referencesText = "Grade 9 Biology, Cell Organelles"
            )
        )

        db.questionDao().insertQuestions(questions)

        // 5. Starter Notebook Notes & Flashcards
        val note1Id = db.notebookDao().upsertNote(
            NoteEntity(
                subjectId = chemId,
                topicId = c12t[0],
                title = "Strong Polyprotic Acids & pH Shortcuts",
                content = "Remember: H₂SO₄ yields 2 H⁺ per formula unit in dilute complete-dissociation problems. Always compute [H⁺] = n × Molarity before taking −log₁₀([H⁺]).",
                isPinned = true
            )
        )
        db.notebookDao().upsertNote(
            NoteEntity(
                subjectId = mathId,
                topicId = m12t[1],
                title = "Calculus Local Extrema Checklist",
                content = "1. Compute f'(x) and solve f'(x) = 0.\n2. Check domain endpoints if interval is closed.\n3. Evaluate f''(x): positive = local min, negative = local max.",
                isPinned = false
            )
        )
        db.notebookDao().upsertNote(
            NoteEntity(
                subjectId = physicsId,
                topicId = p11t[0],
                title = "Kinematics & Dynamics Problem Blueprint",
                content = "1. Sketch a Free Body Diagram first.\n2. Resolve forces perpendicular (N = mg cosθ) and parallel to motion.\n3. Watch whether the problem specifies g = 9.8 or g = 10 m/s².",
                isPinned = false
            )
        )
        db.notebookDao().upsertNote(
            NoteEntity(
                subjectId = englishId,
                topicId = e12t[0],
                title = "EUEE Conditional & Agreement Rules",
                content = "• Type 3 inverted: 'Had + S + V3, S + would have + V3'.\n• 'The number of' takes a singular verb; 'A number of' takes a plural verb.\n• With 'Neither A nor B', the verb matches B.",
                isPinned = false
            )
        )

        db.notebookDao().upsertFlashcard(
            FlashcardEntity(
                subjectId = chemId,
                topicId = c12t[0],
                front = "What is the relationship between [H⁺] and molarity for completely dissociated H₂SO₄?",
                back = "[H⁺] = 2 × M (diprotic acid). Example: 0.005 M H₂SO₄ → 0.01 M H⁺ → pH = 2.0.",
                hint = "Count ionizable protons.",
                sourceNoteId = note1Id
            )
        )
        db.notebookDao().upsertFlashcard(
            FlashcardEntity(
                subjectId = physicsId,
                topicId = p12t[1],
                front = "Einstein's Photoelectric Equation",
                back = "K_max = h·f − φ (where φ = h·f₀ is the work function of the metal).",
                hint = "Energy of photon minus binding threshold."
            )
        )
        db.notebookDao().upsertFlashcard(
            FlashcardEntity(
                subjectId = bioId,
                topicId = b10t[1],
                front = "Where do Glycolysis, Krebs Cycle, and ETC take place?",
                back = "Glycolysis → Cytosol\nKrebs Cycle → Mitochondrial Matrix\nETC → Inner Mitochondrial Membrane (Cristae)",
                hint = "Three compartments from outside to inside."
            )
        )
        db.notebookDao().upsertFlashcard(
            FlashcardEntity(
                subjectId = mathId,
                topicId = m11t[0],
                front = "When does a 2×2 matrix [[a, b], [c, d]] have an inverse?",
                back = "Whenever det(A) = ad − bc ≠ 0. Then A⁻¹ = (1/det(A)) · [[d, −b], [−c, a]].",
                hint = "Swap main diagonal, negate off-diagonal, divide by determinant."
            )
        )
        db.notebookDao().upsertFlashcard(
            FlashcardEntity(
                subjectId = aptitudeId,
                topicId = a12t[0],
                front = "Fast formula for two workers / pipes finishing a job together in times T₁ and T₂",
                back = "T_total = (T₁ × T₂) / (T₁ + T₂), derived from 1/T = 1/T₁ + 1/T₂.",
                hint = "Product over sum."
            )
        )
        db.notebookDao().upsertFlashcard(
            FlashcardEntity(
                subjectId = englishId,
                topicId = e11t[0],
                front = "Distinguish 'call off' vs. 'put off'",
                back = "'call off' = cancel completely\n'put off' = postpone to a later time",
                hint = "Switch OFF vs delay."
            )
        )

        // 6. Starter Today's Study Tasks
        val today = LocalDate.now().toEpochDay()
        db.plannerAndAlarmDao().upsertTask(
            StudyTaskEntity(
                title = "Mathematics: Limits & Derivatives Drill",
                notes = "Complete 15 Smart Study questions and review calculus traps",
                subjectId = mathId,
                unitId = m12u1,
                topicId = m12t[0],
                scheduledDateEpochDay = today,
                startTimeMinutes = 16 * 60, // 4:00 PM
                durationMinutes = 45,
                priority = TaskPriority.HIGH.name
            )
        )
        db.plannerAndAlarmDao().upsertTask(
            StudyTaskEntity(
                title = "Chemistry: Acid-Base pH & Equilibrium",
                notes = "Review Henderson-Hasselbalch and Le Chatelier notes",
                subjectId = chemId,
                unitId = c12u1,
                topicId = c12t[0],
                scheduledDateEpochDay = today,
                startTimeMinutes = 18 * 60 + 30, // 6:30 PM
                durationMinutes = 40,
                priority = TaskPriority.MEDIUM.name
            )
        )

        // 7. Starter disabled Alarm template (user enables when ready)
        db.plannerAndAlarmDao().upsertAlarm(
            AlarmEntity(
                label = "Morning EUEE Revision",
                hour = 6,
                minute = 30,
                daysOfWeekMask = 0b0111110, // Mon-Fri
                isRepeating = true,
                isEnabled = false,
                category = AlarmCategory.STUDY_REMINDER.name,
                subjectId = mathId,
                vibrate = true,
                snoozeMinutes = 10
            )
        )
    }
}
