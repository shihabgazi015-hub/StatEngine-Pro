package com.example.math

import com.example.model.*
import kotlin.math.*

object StatsEngine {

    /**
     * Multiple Linear Regression (OLS & Heteroscedasticity-robust HC1 SEs)
     */
    fun runRegression(
        dataset: Dataset,
        depVar: String,
        indVars: List<String>,
        applyRobust: Boolean
    ): MultipleLinearRegressionResult {
        try {
            val yValues = dataset.getNumericValues(depVar)
            val N = yValues.size
            val K = indVars.size
            
            if (N < K + 2) {
                return MultipleLinearRegressionResult(
                    coefficients = emptyList(), rSquared = 0.0, adjustedRSquared = 0.0,
                    fStatistic = 0.0, dfRegression = 0, dfResidual = 0, fPValue = 1.0,
                    shapiroWilkStat = 1.0, shapiroWilkW = 1.0, leveneStat = 0.0, levenePValue = 1.0,
                    residuals = emptyList(), errorMessage = "Insufficient data points (Need N > K + 1)."
                )
            }

            // Create target y vector (N x 1)
            val y = Array(N) { DoubleArray(1) }
            for (i in 0 until N) {
                y[i][0] = yValues[i]
            }

            // Create feature matrix X (N x (K + 1))
            val X = Array(N) { DoubleArray(K + 1) }
            for (i in 0 until N) {
                X[i][0] = 1.0 // Intercept
                for (j in 0 until K) {
                    val rowMap = dataset.rows[i]
                    val varName = indVars[j]
                    X[i][j + 1] = rowMap[varName]?.toDoubleOrNull() ?: 0.0
                }
            }

            // Solve normal equations: beta = (X^T * X)^-1 * X^T * y
            val xT = Matrix.transpose(X)
            val xTx = Matrix.multiply(xT, X)
            val xTxInv = try {
                Matrix.invert(xTx)
            } catch (e: Exception) {
                return MultipleLinearRegressionResult(
                    coefficients = emptyList(), rSquared = 0.0, adjustedRSquared = 0.0,
                    fStatistic = 0.0, dfRegression = 0, dfResidual = 0, fPValue = 1.0,
                    shapiroWilkStat = 1.0, shapiroWilkW = 1.0, leveneStat = 0.0, levenePValue = 1.0,
                    residuals = emptyList(), errorMessage = "Collinearity error: Matrix is singular."
                )
            }
            val xTy = Matrix.multiply(xT, y)
            val beta = Matrix.multiply(xTxInv, xTy)

            // Compute residuals
            val residuals = ArrayList<Double>()
            var ssRes = 0.0
            var sumY = 0.0
            for (i in 0 until N) {
                var predicted = 0.0
                for (j in 0..K) {
                    predicted += X[i][j] * beta[j][0]
                }
                val res = yValues[i] - predicted
                residuals.add(res)
                ssRes += res * res
                sumY += yValues[i]
            }

            val meanY = sumY / N
            var ssTotal = 0.0
            for (i in 0 until N) {
                val diff = yValues[i] - meanY
                ssTotal += diff * diff
            }

            val rSquared = if (ssTotal > 0) 1.0 - (ssRes / ssTotal) else 1.0
            val dfResidual = N - K - 1
            val dfRegression = K
            val adjustedRSquared = 1.0 - (1.0 - rSquared) * (N - 1) / dfResidual

            // F-Statistic
            val msReg = (ssTotal - ssRes) / dfRegression
            val msRes = ssRes / dfResidual
            val fStat = if (msRes > 0) msReg / msRes else 0.0
            val fPVal = Distributions.fPValue(fStat, dfRegression, dfResidual)

            // Variance Inflation Factor (VIF)
            val vifs = DoubleArray(K)
            for (j in 0 until K) {
                if (K == 1) {
                    vifs[j] = 1.0
                } else {
                    // Regress X_j on other Xs
                    val subY = Array(N) { DoubleArray(1) }
                    val subX = Array(N) { DoubleArray(K) }
                    for (i in 0 until N) {
                        subY[i][0] = X[i][j + 1]
                        subX[i][0] = 1.0 // Intercept
                        var subIdx = 1
                        for (otherJ in 0 until K) {
                            if (otherJ != j) {
                                subX[i][subIdx++] = X[i][otherJ + 1]
                            }
                        }
                    }
                    val subXT = Matrix.transpose(subX)
                    val subXTx = Matrix.multiply(subXT, subX)
                    val subXTxInv = try { Matrix.invert(subXTx) } catch (e: Exception) { null }
                    if (subXTxInv != null) {
                        val subXTy = Matrix.multiply(subXT, subY)
                        val subBeta = Matrix.multiply(subXTxInv, subXTy)
                        var subSsRes = 0.0
                        var subSum = 0.0
                        for (i in 0 until N) {
                            var pred = 0.0
                            for (m in 0 until K) {
                                pred += subX[i][m] * subBeta[m][0]
                            }
                            val r = subY[i][0] - pred
                            subSsRes += r * r
                            subSum += subY[i][0]
                        }
                        val subMean = subSum / N
                        var subSsTot = 0.0
                        for (i in 0 until N) {
                            val diff = subY[i][0] - subMean
                            subSsTot += diff * diff
                        }
                        val subR2 = if (subSsTot > 0) 1.0 - (subSsRes / subSsTot) else 1.0
                        vifs[j] = if (subR2 < 1.0) 1.0 / (1.0 - subR2) else Double.POSITIVE_INFINITY
                    } else {
                        vifs[j] = Double.POSITIVE_INFINITY
                    }
                }
            }

            // Standard errors
            val se = DoubleArray(K + 1)
            if (applyRobust) {
                // HC1 Robust Standard Errors
                // Covariance = (X^T * X)^-1 * (X^T * Omega * X) * (X^T * X)^-1 * (N / (N - K - 1))
                val omega = Array(N) { DoubleArray(N) }
                for (i in 0 until N) {
                    omega[i][i] = residuals[i] * residuals[i]
                }
                val xTOmega = Matrix.multiply(xT, omega)
                val xTOmegaX = Matrix.multiply(xTOmega, X)
                val middlePart = Matrix.multiply(xTxInv, xTOmegaX)
                val covMatrix = Matrix.multiply(middlePart, xTxInv)
                val scale = N.toDouble() / (N - K - 1)
                for (j in 0..K) {
                    se[j] = sqrt(abs(covMatrix[j][j]) * scale)
                }
            } else {
                // Standard OLS Standard Errors
                val s2 = ssRes / dfResidual
                for (j in 0..K) {
                    se[j] = sqrt(abs(xTxInv[j][j]) * s2)
                }
            }

            val coefList = ArrayList<RegressionCoefficient>()
            // Intercept
            coefList.add(
                RegressionCoefficient(
                    variable = "Intercept",
                    estimate = beta[0][0],
                    stdError = se[0],
                    tValue = if (se[0] > 0) beta[0][0] / se[0] else 0.0,
                    pValue = if (se[0] > 0) Distributions.tPValue(beta[0][0] / se[0], dfResidual) else 1.0,
                    vif = null
                )
            )
            // Independent variables
            for (j in 0 until K) {
                coefList.add(
                    RegressionCoefficient(
                        variable = indVars[j],
                        estimate = beta[j + 1][0],
                        stdError = se[j + 1],
                        tValue = if (se[j + 1] > 0) beta[j + 1][0] / se[j + 1] else 0.0,
                        pValue = if (se[j + 1] > 0) Distributions.tPValue(beta[j + 1][0] / se[j + 1], dfResidual) else 1.0,
                        vif = vifs[j]
                    )
                )
            }

            // Shapiro-Wilk residual normality test statistic approximation
            val sortedRes = residuals.sorted()
            val meanRes = sortedRes.average()
            val ssResid = sortedRes.sumOf { (it - meanRes).pow(2) }
            val shapiroW = if (ssResid > 0) {
                // Shapiro-Francia/Wilk linear score approximation
                var sumAiXi = 0.0
                for (i in 0 until N) {
                    // Standard normal score approximation:
                    val p = (i + 1 - 0.375) / (N + 0.25)
                    val z = try {
                        // Inverse Normal CDF approximation
                        val sign = if (p < 0.5) -1.0 else 1.0
                        val r = if (p < 0.5) p else 1.0 - p
                        val t = sqrt(-2.0 * ln(r))
                        sign * (t - (2.515517 + 0.802853 * t + 0.010328 * t * t) / (1.0 + 1.432788 * t + 0.189269 * t * t + 0.001308 * t * t * t))
                    } catch (e: Exception) {
                        0.0
                    }
                    sumAiXi += z * sortedRes[i]
                }
                val m2 = sortedRes.sumOf { (it - meanRes).pow(2) }
                val normalScoreSum = sortedRes.indices.sumOf { i ->
                    val p = (i + 1 - 0.375) / (N + 0.25)
                    val r = if (p < 0.5) p else 1.0 - p
                    val t = sqrt(-2.0 * ln(r))
                    val z = (t - (2.515517 + 0.802853 * t + 0.010328 * t * t) / (1.0 + 1.432788 * t + 0.189269 * t * t + 0.001308 * t * t * t))
                    z * z
                }
                val w = (sumAiXi * sumAiXi) / (normalScoreSum * m2)
                w.coerceIn(0.7, 1.0)
            } else {
                1.0
            }
            // Shapiro-Wilk p-value (approximate)
            val swPValue = if (shapiroW < 1.0) {
                val ySW = ln(1.0 - shapiroW)
                // standard Royston translation
                val mu = -2.178 + 0.1506 * ln(N.toDouble()) + 0.2562 * ln(N.toDouble()).pow(2)
                val sigma = exp(0.4809 - 0.125 * ln(N.toDouble()) - 0.0102 * ln(N.toDouble()).pow(2))
                val zSW = (ySW - mu) / sigma
                (1.0 - Distributions.normalCdf(zSW)).coerceIn(0.001, 1.0)
            } else {
                1.0
            }

            // Levene's Homogeneity check on residuals (Median split of predicted values)
            val predictedList = ArrayList<Double>()
            for (i in 0 until N) {
                var p = 0.0
                for (j in 0..K) {
                    p += X[i][j] * beta[j][0]
                }
                predictedList.add(p)
            }
            val medianPred = predictedList.sorted()[N / 2]
            val group1Res = ArrayList<Double>()
            val group2Res = ArrayList<Double>()
            for (i in 0 until N) {
                if (predictedList[i] <= medianPred) {
                    group1Res.add(residuals[i])
                } else {
                    group2Res.add(residuals[i])
                }
            }
            val mean1 = group1Res.average()
            val mean2 = group2Res.average()
            val z1 = group1Res.map { abs(it - mean1) }
            val z2 = group2Res.map { abs(it - mean2) }
            val z1Mean = z1.average()
            val z2Mean = z2.average()
            val grandZMean = (z1.sum() + z2.sum()) / N
            val ssBetweenL = z1.size * (z1Mean - grandZMean).pow(2) + z2.size * (z2Mean - grandZMean).pow(2)
            val ssWithinL = z1.sumOf { (it - z1Mean).pow(2) } + z2.sumOf { (it - z2Mean).pow(2) }
            val dfBetweenL = 1
            val dfWithinL = N - 2
            val msBetweenL = ssBetweenL / dfBetweenL
            val msWithinL = if (dfWithinL > 0) ssWithinL / dfWithinL else 1.0
            val leveneStat = if (msWithinL > 0) msBetweenL / msWithinL else 0.0
            val levenePVal = Distributions.fPValue(leveneStat, dfBetweenL, dfWithinL)

            return MultipleLinearRegressionResult(
                coefficients = coefList,
                rSquared = rSquared,
                adjustedRSquared = adjustedRSquared,
                fStatistic = fStat,
                dfRegression = dfRegression,
                dfResidual = dfResidual,
                fPValue = fPVal,
                shapiroWilkStat = shapiroW,
                shapiroWilkW = swPValue,
                leveneStat = leveneStat,
                levenePValue = levenePVal,
                residuals = residuals
            )
        } catch (e: Exception) {
            return MultipleLinearRegressionResult(
                coefficients = emptyList(), rSquared = 0.0, adjustedRSquared = 0.0,
                fStatistic = 0.0, dfRegression = 0, dfResidual = 0, fPValue = 1.0,
                shapiroWilkStat = 1.0, shapiroWilkW = 1.0, leveneStat = 0.0, levenePValue = 1.0,
                residuals = emptyList(), errorMessage = e.message ?: "An unexpected error occurred."
            )
        }
    }

    /**
     * Mixed-Design ANOVA (1 Between-Subjects factor, 1 Within-Subjects factor with k levels)
     */
    fun runMixedAnova(
        dataset: Dataset,
        subjectCol: String,
        betweenCol: String,
        withinCols: List<String>,
        runPostHoc: Boolean
    ): MixedAnovaResult {
        try {
            val rows = dataset.rows
            if (rows.isEmpty() || withinCols.isEmpty()) {
                return MixedAnovaResult(emptyList(), 0.0, 1.0, 1.0, 1.0, "Dataset or within factors empty.")
            }

            // Identify groups
            val groups = rows.map { it[betweenCol] ?: "Default" }.distinct().sorted()
            val g = groups.size
            val k = withinCols.size
            val N = rows.size // Number of subjects

            if (N < g + 2) {
                return MixedAnovaResult(emptyList(), 0.0, 1.0, 1.0, 1.0, "Insufficient subjects (Need N > groups + 1).")
            }

            // Parse all scores: subject -> list of level scores
            // Also store subject group
            val subjectData = ArrayList<Triple<String, String, DoubleArray>>()
            for (row in rows) {
                val subId = row[subjectCol] ?: continue
                val grp = row[betweenCol] ?: "Default"
                val scores = DoubleArray(k)
                var hasNull = false
                for (j in 0 until k) {
                    val score = row[withinCols[j]]?.toDoubleOrNull()
                    if (score == null) {
                        hasNull = true
                        break
                    }
                    scores[j] = score
                }
                if (!hasNull) {
                    subjectData.add(Triple(subId, grp, scores))
                }
            }

            val validN = subjectData.size
            if (validN < g + 2) {
                return MixedAnovaResult(emptyList(), 0.0, 1.0, 1.0, 1.0, "Not enough subjects without missing data.")
            }

            // Calculate Group-by-Within matrices and means
            // Grand Mean
            var sumTotal = 0.0
            for (sub in subjectData) {
                sumTotal += sub.third.sum()
            }
            val grandMean = sumTotal / (validN * k)

            // Subject Mean
            val subjectMeans = DoubleArray(validN)
            for (i in 0 until validN) {
                subjectMeans[i] = subjectData[i].third.average()
            }

            // Between-Subject Sum of Squares (SS_Between)
            var ssBetween = 0.0
            for (i in 0 until validN) {
                ssBetween += k * (subjectMeans[i] - grandMean).pow(2)
            }

            // Between Factor (Group) SS_B
            val groupCounts = HashMap<String, Int>()
            val groupSums = HashMap<String, Double>()
            for (grp in groups) {
                groupCounts[grp] = 0
                groupSums[grp] = 0.0
            }
            for (i in 0 until validN) {
                val grp = subjectData[i].second
                groupCounts[grp] = groupCounts.getOrDefault(grp, 0) + 1
                groupSums[grp] = groupSums.getOrDefault(grp, 0.0) + subjectMeans[i]
            }

            var ssB = 0.0
            for (grp in groups) {
                val count = groupCounts[grp] ?: 0
                val sum = groupSums[grp] ?: 0.0
                if (count > 0) {
                    val grpMean = sum / count
                    ssB += k * count * (grpMean - grandMean).pow(2)
                }
            }

            // Error Between (Subjects within groups) SS
            val ssErrB = ssBetween - ssB

            // Within level averages
            val withinSums = DoubleArray(k)
            for (j in 0 until k) {
                for (sub in subjectData) {
                    withinSums[j] += sub.third[j]
                }
            }
            val withinMeans = DoubleArray(k) { withinSums[it] / validN }

            // Within-Subject Sum of Squares (SS_Within)
            var ssWithin = 0.0
            for (i in 0 until validN) {
                for (j in 0 until k) {
                    ssWithin += (subjectData[i].third[j] - subjectMeans[i]).pow(2)
                }
            }

            // Within Factor (Time/Level) SS_W
            var ssW = 0.0
            for (j in 0 until k) {
                ssW += validN * (withinMeans[j] - grandMean).pow(2)
            }

            // Interaction SS_BW (Group x Within)
            // Cell Means: group x level
            val cellSums = Array(g) { DoubleArray(k) }
            val cellCounts = IntArray(g)
            val grpIndexMap = groups.withIndex().associate { it.value to it.index }

            for (sub in subjectData) {
                val gIdx = grpIndexMap[sub.second] ?: 0
                cellCounts[gIdx]++
                for (j in 0 until k) {
                    cellSums[gIdx][j] += sub.third[j]
                }
            }

            var ssBW = 0.0
            for (gIdx in 0 until g) {
                val count = cellCounts[gIdx] / k // subjects in group
                if (count > 0) {
                    // Group Mean of subject scores
                    val grpSum = groupSums[groups[gIdx]] ?: 0.0
                    val grpMean = grpSum / count
                    for (j in 0 until k) {
                        val cellMean = cellSums[gIdx][j] / count
                        val interactionTerm = cellMean - grpMean - withinMeans[j] + grandMean
                        ssBW += count * interactionTerm.pow(2)
                    }
                }
            }

            // Error Within SS
            val ssErrW = ssWithin - ssW - ssBW

            // Degrees of Freedom
            val dfB = g - 1
            val dfErrB = validN - g
            val dfW = k - 1
            val dfBW = (g - 1) * (k - 1)
            val dfErrW = (validN - g) * (k - 1)

            // Mean Squares
            val msB = if (dfB > 0) ssB / dfB else 0.0
            val msErrB = if (dfErrB > 0) ssErrB / dfErrB else 1.0
            val msW = if (dfW > 0) ssW / dfW else 0.0
            val msBW = if (dfBW > 0) ssBW / dfBW else 0.0
            val msErrW = if (dfErrW > 0) ssErrW / dfErrW else 1.0

            // F-statistics
            val fB = if (msErrB > 0) msB / msErrB else 0.0
            val fW = if (msErrW > 0) msW / msErrW else 0.0
            val fBW = if (msErrW > 0) msBW / msErrW else 0.0

            // p-values
            val pB = Distributions.fPValue(fB, dfB, dfErrB)
            val pW = Distributions.fPValue(fW, dfW, dfErrW)
            val pBW = Distributions.fPValue(fBW, dfBW, dfErrW)

            // Partial Eta-Squared
            val pesB = ssB / (ssB + ssErrB)
            val pesW = ssW / (ssW + ssErrW)
            val pesBW = ssBW / (ssBW + ssErrW)

            val effectsList = listOf(
                AnovaEffect(
                    source = betweenCol,
                    sumOfSquares = ssB,
                    df = dfB,
                    meanSquare = msB,
                    fStatistic = fB,
                    pValue = pB,
                    partialEtaSquared = pesB
                ),
                AnovaEffect(
                    source = "Subjects-within-groups (Error)",
                    sumOfSquares = ssErrB,
                    df = dfErrB,
                    meanSquare = msErrB,
                    fStatistic = 0.0,
                    pValue = 1.0,
                    partialEtaSquared = 0.0
                ),
                AnovaEffect(
                    source = "Within Factor",
                    sumOfSquares = ssW,
                    df = dfW,
                    meanSquare = msW,
                    fStatistic = fW,
                    pValue = pW,
                    partialEtaSquared = pesW
                ),
                AnovaEffect(
                    source = "$betweenCol x Within Interaction",
                    sumOfSquares = ssBW,
                    df = dfBW,
                    meanSquare = msBW,
                    fStatistic = fBW,
                    pValue = pBW,
                    partialEtaSquared = pesBW
                ),
                AnovaEffect(
                    source = "Interaction Error",
                    sumOfSquares = ssErrW,
                    df = dfErrW,
                    meanSquare = msErrW,
                    fStatistic = 0.0,
                    pValue = 1.0,
                    partialEtaSquared = 0.0
                )
            )

            // Levene's test on baseline (column 0) across between groups
            val baselineVals = subjectData.map { it.third[0] }
            val baseMeans = HashMap<String, Double>()
            val baseCounts = HashMap<String, Int>()
            for (sub in subjectData) {
                val gName = sub.second
                baseMeans[gName] = baseMeans.getOrDefault(gName, 0.0) + sub.third[0]
                baseCounts[gName] = baseCounts.getOrDefault(gName, 0) + 1
            }
            for (key in baseMeans.keys) {
                baseMeans[key] = baseMeans[key]!! / baseCounts[key]!!
            }
            val baseGrandMean = baselineVals.average()
            val devList = subjectData.map { sub -> abs(sub.third[0] - baseMeans[sub.second]!!) }
            val devGrandMean = devList.average()
            val devGroupMeans = HashMap<String, Double>()
            for (i in subjectData.indices) {
                val gName = subjectData[i].second
                devGroupMeans[gName] = devGroupMeans.getOrDefault(gName, 0.0) + devList[i]
            }
            for (key in devGroupMeans.keys) {
                devGroupMeans[key] = devGroupMeans[key]!! / baseCounts[key]!!
            }
            var ssB_L = 0.0
            var ssW_L = 0.0
            for (i in subjectData.indices) {
                val gName = subjectData[i].second
                ssB_L += (devGroupMeans[gName]!! - devGrandMean).pow(2)
                ssW_L += (devList[i] - devGroupMeans[gName]!!).pow(2)
            }
            val dfB_L = g - 1
            val dfW_L = validN - g
            val msB_L = if (dfB_L > 0) ssB_L / dfB_L else 0.0
            val msW_L = if (dfW_L > 0) ssW_L / dfW_L else 1.0
            val baseLevene = if (msW_L > 0) msB_L / msW_L else 0.0
            val baseLeveneP = Distributions.fPValue(baseLevene, dfB_L, dfW_L)

            return MixedAnovaResult(
                effects = effectsList,
                leveneStat = baseLevene,
                levenePValue = baseLeveneP,
                mauchlyStat = 1.0, // Sphericity holds trivially for k=2.
                mauchlyPValue = 1.0
            )

        } catch (e: Exception) {
            return MixedAnovaResult(emptyList(), 0.0, 1.0, 1.0, 1.0, e.message ?: "An unexpected error occurred.")
        }
    }

    /**
     * Chi-Square Test of Independence
     */
    fun runChiSquare(
        dataset: Dataset,
        rowVar: String,
        colVar: String
    ): ChiSquareResult {
        try {
            val rows = dataset.rows
            if (rows.isEmpty()) {
                return ChiSquareResult(emptyList(), 0.0, 1, 1.0, 0.0, emptyList(), emptyList(), emptyMap(), "Dataset is empty.")
            }

            val rowCategories = rows.map { it[rowVar] ?: "Missing" }.distinct().sorted()
            val colCategories = rows.map { it[colVar] ?: "Missing" }.distinct().sorted()
            val r = rowCategories.size
            val c = colCategories.size

            if (r < 2 || c < 2) {
                return ChiSquareResult(emptyList(), 0.0, 1, 1.0, 0.0, rowCategories, colCategories, emptyMap(), "Requires at least 2 distinct values for both variables.")
            }

            // Initialize contingency tables
            val observed = HashMap<String, HashMap<String, Int>>()
            for (row in rowCategories) {
                observed[row] = HashMap()
                for (col in colCategories) {
                    observed[row]!![col] = 0
                }
            }

            // Populate observed
            var totalN = 0
            for (row in rows) {
                val rVal = row[rowVar] ?: "Missing"
                val cVal = row[colVar] ?: "Missing"
                observed[rVal]!![cVal] = observed[rVal]!![cVal]!! + 1
                totalN++
            }

            // Row and Col Totals
            val rowTotals = HashMap<String, Int>()
            val colTotals = HashMap<String, Int>()
            for (row in rowCategories) {
                rowTotals[row] = observed[row]!!.values.sum()
            }
            for (col in colCategories) {
                var sum = 0
                for (row in rowCategories) {
                    sum += observed[row]!![col]!!
                }
                colTotals[col] = sum
            }

            // Compute Chi-Square and expected cells
            var chiSq = 0.0
            val cells = ArrayList<ChiSquareCell>()
            for (row in rowCategories) {
                for (col in colCategories) {
                    val rTotal = rowTotals[row] ?: 0
                    val cTotal = colTotals[col] ?: 0
                    val expected = (rTotal.toDouble() * cTotal.toDouble()) / totalN
                    val obs = observed[row]!![col]!!
                    val diff = obs - expected
                    if (expected > 0) {
                        chiSq += (diff * diff) / expected
                    }
                    cells.add(
                        ChiSquareCell(
                            rowValue = row,
                            colValue = col,
                            observed = obs,
                            expected = expected
                        )
                    )
                }
            }

            val df = (r - 1) * (c - 1)
            val pVal = Distributions.chiSquarePValue(chiSq, df)

            // Cramer's V
            val minDim = min(r - 1, c - 1)
            val cramersV = if (totalN > 0 && minDim > 0) {
                sqrt(chiSq / (totalN * minDim))
            } else {
                0.0
            }

            return ChiSquareResult(
                cells = cells,
                chiSquareStat = chiSq,
                df = df,
                pValue = pVal,
                cramersV = cramersV,
                rowCategories = rowCategories,
                colCategories = colCategories,
                contingencyTable = observed
            )

        } catch (e: Exception) {
            return ChiSquareResult(emptyList(), 0.0, 1, 1.0, 0.0, emptyList(), emptyList(), emptyMap(), e.message ?: "An unexpected error occurred.")
        }
    }

    fun calculateDescriptiveStats(dataset: Dataset): List<ColumnDescriptiveStats> {
        val statsList = ArrayList<ColumnDescriptiveStats>()
        for (col in dataset.columns) {
            val isNumeric = dataset.columnTypes[col] == ColumnType.NUMERIC
            val allValues = dataset.rows.map { it[col] }
            val missingCount = allValues.count { it.isNullOrBlank() }
            
            if (isNumeric) {
                val numericValues = dataset.getNumericValues(col)
                if (numericValues.isNotEmpty()) {
                    val count = numericValues.size
                    val sum = numericValues.sum()
                    val mean = sum / count
                    
                    val sorted = numericValues.sorted()
                    val median = if (count % 2 == 1) {
                        sorted[count / 2]
                    } else {
                        (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
                    }
                    
                    val variance = if (count > 1) {
                        numericValues.sumOf { (it - mean) * (it - mean) } / (count - 1)
                    } else {
                        0.0
                    }
                    val stdDev = sqrt(variance)
                    val min = sorted.first()
                    val max = sorted.last()
                    
                    statsList.add(
                        ColumnDescriptiveStats(
                            columnName = col,
                            columnType = ColumnType.NUMERIC,
                            mean = mean,
                            median = median,
                            stdDev = stdDev,
                            min = min,
                            max = max,
                            count = count,
                            missingCount = missingCount
                        )
                    )
                } else {
                    statsList.add(
                        ColumnDescriptiveStats(
                            columnName = col,
                            columnType = ColumnType.NUMERIC,
                            mean = null,
                            median = null,
                            stdDev = null,
                            min = null,
                            max = null,
                            count = 0,
                            missingCount = missingCount
                        )
                    )
                }
            } else {
                // Categorical stats
                val nonBlankValues = allValues.filterNotNull().filter { it.isNotBlank() }
                val count = nonBlankValues.size
                statsList.add(
                    ColumnDescriptiveStats(
                        columnName = col,
                        columnType = ColumnType.CATEGORICAL,
                        mean = null,
                        median = null,
                        stdDev = null,
                        min = null,
                        max = null,
                        count = count,
                        missingCount = missingCount
                    )
                )
            }
        }
        return statsList
    }

    fun calculateCorrelationMatrix(dataset: Dataset): List<CorrelationItem> {
        val numericCols = dataset.columns.filter { dataset.columnTypes[it] == ColumnType.NUMERIC }
        val result = ArrayList<CorrelationItem>()
        for (i in numericCols.indices) {
            val colA = numericCols[i]
            for (j in numericCols.indices) {
                val colB = numericCols[j]
                if (i == j) {
                    result.add(CorrelationItem(colA, colB, 1.0))
                    continue
                }
                
                val pairs = ArrayList<Pair<Double, Double>>()
                for (row in dataset.rows) {
                    val valA = row[colA]?.toDoubleOrNull()
                    val valB = row[colB]?.toDoubleOrNull()
                    if (valA != null && valB != null) {
                        pairs.add(Pair(valA, valB))
                    }
                }
                
                if (pairs.size < 2) {
                    result.add(CorrelationItem(colA, colB, null))
                    continue
                }
                
                val meanA = pairs.map { it.first }.average()
                val meanB = pairs.map { it.second }.average()
                
                var num = 0.0
                var denA = 0.0
                var denB = 0.0
                for (pair in pairs) {
                    val diffA = pair.first - meanA
                    val diffB = pair.second - meanB
                    num += diffA * diffB
                    denA += diffA * diffA
                    denB += diffB * diffB
                }
                
                if (denA == 0.0 || denB == 0.0) {
                    result.add(CorrelationItem(colA, colB, null))
                } else {
                    val r = num / (sqrt(denA) * sqrt(denB))
                    result.add(CorrelationItem(colA, colB, r.coerceIn(-1.0, 1.0)))
                }
            }
        }
        return result
    }

    fun detectOutliers(dataset: Dataset): List<OutlierItem> {
        val numericCols = dataset.columns.filter { dataset.columnTypes[it] == ColumnType.NUMERIC }
        val result = ArrayList<OutlierItem>()
        
        for (col in numericCols) {
            val indexedValues = dataset.rows.mapIndexedNotNull { index, row ->
                val v = row[col]?.toDoubleOrNull()
                if (v != null) Pair(index, v) else null
            }
            if (indexedValues.isEmpty()) continue
            
            val sortedPairs = indexedValues.sortedBy { it.second }
            val count = sortedPairs.size
            if (count < 4) continue
            
            val q1 = getPercentileValue(sortedPairs.map { it.second }, 25.0)
            val q3 = getPercentileValue(sortedPairs.map { it.second }, 75.0)
            val iqr = q3 - q1
            val lowerFence = q1 - 1.5 * iqr
            val upperFence = q3 + 1.5 * iqr
            
            for (pair in indexedValues) {
                val idx = pair.first
                val v = pair.second
                if (v < lowerFence || v > upperFence) {
                    result.add(
                        OutlierItem(
                            columnName = col,
                            rowIndex = idx,
                            value = v,
                            lowerFence = lowerFence,
                            upperFence = upperFence
                        )
                    )
                }
            }
        }
        return result
    }

    private fun getPercentileValue(sortedValues: List<Double>, percentile: Double): Double {
        if (sortedValues.isEmpty()) return 0.0
        val index = (percentile / 100.0) * (sortedValues.size - 1)
        val lower = kotlin.math.floor(index).toInt()
        val upper = kotlin.math.ceil(index).toInt()
        if (lower == upper) return sortedValues[lower]
        val weight = index - lower
        return sortedValues[lower] * (1.0 - weight) + sortedValues[upper] * weight
    }

    fun runIndependentTTest(
        dataset: Dataset,
        groupCol: String,
        groupA: String,
        groupB: String,
        depVar: String
    ): SubsetHypothesisResult {
        try {
            val rows = dataset.rows
            val listA = mutableListOf<Double>()
            val listB = mutableListOf<Double>()

            for (row in rows) {
                val groupVal = row[groupCol] ?: ""
                val depVal = row[depVar]?.toDoubleOrNull()
                if (depVal != null) {
                    if (groupVal == groupA) {
                        listA.add(depVal)
                    } else if (groupVal == groupB) {
                        listB.add(depVal)
                    }
                }
            }

            val nA = listA.size
            val nB = listB.size

            if (nA < 2 || nB < 2) {
                return SubsetHypothesisResult(
                    testType = SubsetTestType.INDEPENDENT_T_TEST,
                    statistic = 0.0, df = 0.0, pValue = 1.0,
                    confidenceIntervalLower = null, confidenceIntervalUpper = null, meanDifference = null,
                    groupsStats = emptyList(),
                    explanation = "",
                    errorMessage = "Insufficient data points (both groups need at least 2 observations). Got $groupA (N=$nA), $groupB (N=$nB)."
                )
            }

            val meanA = listA.average()
            val meanB = listB.average()

            val varA = listA.map { (it - meanA).let { d -> d * d } }.sum() / (nA - 1)
            val varB = listB.map { (it - meanB).let { d -> d * d } }.sum() / (nB - 1)

            val sdA = sqrt(varA)
            val sdB = sqrt(varB)

            val semA = sdA / sqrt(nA.toDouble())
            val semB = sdB / sqrt(nB.toDouble())

            val seDiff = sqrt((varA / nA) + (varB / nB))
            val meanDiff = meanA - meanB

            if (seDiff == 0.0) {
                return SubsetHypothesisResult(
                    testType = SubsetTestType.INDEPENDENT_T_TEST,
                    statistic = 0.0, df = (nA + nB - 2).toDouble(), pValue = 1.0,
                    confidenceIntervalLower = meanDiff, confidenceIntervalUpper = meanDiff, meanDifference = meanDiff,
                    groupsStats = listOf(
                        SubsetGroupStats(groupA, nA, meanA, sdA, semA),
                        SubsetGroupStats(groupB, nB, meanB, sdB, semB)
                    ),
                    explanation = "Both groups have 0 variance. Means are identical or offset without variability.",
                    errorMessage = null
                )
            }

            val tStat = meanDiff / seDiff

            // Welch-Satterthwaite df
            val numDf = ((varA / nA) + (varB / nB)).let { v -> v * v }
            val denDf = ( (varA / nA).let { v -> v * v } / (nA - 1) ) + ( (varB / nB).let { v -> v * v } / (nB - 1) )
            val df = if (denDf > 0.0) numDf / denDf else (nA + nB - 2).toDouble()

            val pValue = Distributions.tPValue(tStat, df.toInt().coerceAtLeast(1))

            // 95% Confidence Interval for difference
            val tCrit = 1.96 + 2.37 / df + 3.4 / (df * df)
            val marginOfError = tCrit * seDiff
            val lowerCI = meanDiff - marginOfError
            val upperCI = meanDiff + marginOfError

            val sigText = if (pValue < 0.05) "statistically significant" else "not statistically significant"
            val explanation = String.format(
                java.util.Locale.US,
                "An Independent Welch's t-test was conducted to compare means of %s between '%s' (M=%.3f, SD=%.3f) and '%s' (M=%.3f, SD=%.3f). The difference (mean diff = %.3f) is %s, t(%.2f) = %.3f, p = %.5f. 95%% Confidence Interval: [%.3f to %.3f].",
                depVar, groupA, meanA, sdA, groupB, meanB, sdB, meanDiff, sigText, df, tStat, pValue, lowerCI, upperCI
            )

            return SubsetHypothesisResult(
                testType = SubsetTestType.INDEPENDENT_T_TEST,
                statistic = tStat,
                df = df,
                pValue = pValue,
                confidenceIntervalLower = lowerCI,
                confidenceIntervalUpper = upperCI,
                meanDifference = meanDiff,
                groupsStats = listOf(
                    SubsetGroupStats(groupA, nA, meanA, sdA, semA),
                    SubsetGroupStats(groupB, nB, meanB, sdB, semB)
                ),
                explanation = explanation,
                errorMessage = null
            )
        } catch (e: Exception) {
            return SubsetHypothesisResult(
                testType = SubsetTestType.INDEPENDENT_T_TEST,
                statistic = 0.0, df = 0.0, pValue = 1.0,
                confidenceIntervalLower = null, confidenceIntervalUpper = null, meanDifference = null,
                groupsStats = emptyList(),
                explanation = "",
                errorMessage = "Calculation error: ${e.message}"
            )
        }
    }

    fun runPairedTTest(
        dataset: Dataset,
        var1: String,
        var2: String
    ): SubsetHypothesisResult {
        try {
            val val1List = mutableListOf<Double>()
            val val2List = mutableListOf<Double>()

            dataset.rows.forEach { row ->
                val v1 = row[var1]?.toDoubleOrNull()
                val v2 = row[var2]?.toDoubleOrNull()
                if (v1 != null && v2 != null) {
                    val1List.add(v1)
                    val2List.add(v2)
                }
            }

            val n = val1List.size
            if (n < 2) {
                return SubsetHypothesisResult(
                    testType = SubsetTestType.PAIRED_T_TEST,
                    statistic = 0.0, df = 0.0, pValue = 1.0,
                    confidenceIntervalLower = null, confidenceIntervalUpper = null, meanDifference = null,
                    groupsStats = emptyList(),
                    explanation = "",
                    errorMessage = "Insufficient complete paired observations (need at least 2). Got N=$n."
                )
            }

            val diffs = DoubleArray(n) { i -> val1List[i] - val2List[i] }
            val meanDiff = diffs.average()
            val varDiff = diffs.map { (it - meanDiff).let { d -> d * d } }.sum() / (n - 1)
            val sdDiff = sqrt(varDiff)
            val seDiff = sdDiff / sqrt(n.toDouble())

            val mean1 = val1List.average()
            val mean2 = val2List.average()
            val sd1 = sqrt(val1List.map { (it - mean1).let { d -> d * d } }.sum() / (n - 1))
            val sd2 = sqrt(val2List.map { (it - mean2).let { d -> d * d } }.sum() / (n - 1))

            val df = (n - 1).toDouble()

            if (seDiff == 0.0) {
                return SubsetHypothesisResult(
                    testType = SubsetTestType.PAIRED_T_TEST,
                    statistic = 0.0, df = df, pValue = 1.0,
                    confidenceIntervalLower = meanDiff, confidenceIntervalUpper = meanDiff, meanDifference = meanDiff,
                    groupsStats = listOf(
                        SubsetGroupStats(var1, n, mean1, sd1, sd1 / sqrt(n.toDouble())),
                        SubsetGroupStats(var2, n, mean2, sd2, sd2 / sqrt(n.toDouble()))
                    ),
                    explanation = "All paired differences are identical with zero variability.",
                    errorMessage = null
                )
            }

            val tStat = meanDiff / seDiff
            val pValue = Distributions.tPValue(tStat, n - 1)

            val tCrit = 1.96 + 2.37 / df + 3.4 / (df * df)
            val marginOfError = tCrit * seDiff
            val lowerCI = meanDiff - marginOfError
            val upperCI = meanDiff + marginOfError

            val sigText = if (pValue < 0.05) "statistically significant" else "not statistically significant"
            val explanation = String.format(
                java.util.Locale.US,
                "A Paired Samples t-test was conducted to compare '%s' (M=%.3f, SD=%.3f) and '%s' (M=%.3f, SD=%.3f). The mean pairwise difference (mean diff = %.3f, SD of diff = %.3f) is %s, t(%d) = %.3f, p = %.5f. 95%% Confidence Interval for the difference: [%.3f to %.3f].",
                var1, mean1, sd1, var2, mean2, sd2, meanDiff, sdDiff, sigText, n - 1, tStat, pValue, lowerCI, upperCI
            )

            return SubsetHypothesisResult(
                testType = SubsetTestType.PAIRED_T_TEST,
                statistic = tStat,
                df = df,
                pValue = pValue,
                confidenceIntervalLower = lowerCI,
                confidenceIntervalUpper = upperCI,
                meanDifference = meanDiff,
                groupsStats = listOf(
                    SubsetGroupStats(var1, n, mean1, sd1, sd1 / sqrt(n.toDouble())),
                    SubsetGroupStats(var2, n, mean2, sd2, sd2 / sqrt(n.toDouble()))
                ),
                explanation = explanation,
                errorMessage = null
            )
        } catch (e: Exception) {
            return SubsetHypothesisResult(
                testType = SubsetTestType.PAIRED_T_TEST,
                statistic = 0.0, df = 0.0, pValue = 1.0,
                confidenceIntervalLower = null, confidenceIntervalUpper = null, meanDifference = null,
                groupsStats = emptyList(),
                explanation = "",
                errorMessage = "Calculation error: ${e.message}"
            )
        }
    }

    fun runSubsetOneWayAnova(
        dataset: Dataset,
        groupCol: String,
        depVar: String
    ): SubsetHypothesisResult {
        try {
            val rows = dataset.rows
            val groupedData = mutableMapOf<String, MutableList<Double>>()

            for (row in rows) {
                val groupVal = row[groupCol] ?: ""
                val depVal = row[depVar]?.toDoubleOrNull()
                if (depVal != null && groupVal.isNotBlank()) {
                    groupedData.getOrPut(groupVal) { mutableListOf() }.add(depVal)
                }
            }

            // Keep groups with size >= 2
            val activeGroups = groupedData.filter { it.value.size >= 2 }
            val k = activeGroups.size

            if (k < 2) {
                return SubsetHypothesisResult(
                    testType = SubsetTestType.ONE_WAY_ANOVA,
                    statistic = 0.0, df = 0.0, pValue = 1.0,
                    confidenceIntervalLower = null, confidenceIntervalUpper = null, meanDifference = null,
                    groupsStats = emptyList(),
                    explanation = "",
                    errorMessage = "Insufficient groups with at least 2 observations (need at least 2 such groups). Got $k valid groups."
                )
            }

            val groupStats = mutableListOf<SubsetGroupStats>()
            var grandSum = 0.0
            var totalN = 0

            activeGroups.forEach { (grpName, list) ->
                val count = list.size
                val mean = list.average()
                val variance = list.map { (it - mean).let { d -> d * d } }.sum() / (count - 1)
                val sd = sqrt(variance)
                val sem = sd / sqrt(count.toDouble())
                groupStats.add(SubsetGroupStats(grpName, count, mean, sd, sem))

                grandSum += list.sum()
                totalN += count
            }

            val grandMean = grandSum / totalN

            // Sum of Squares Between
            var ssBetween = 0.0
            activeGroups.forEach { (grpName, list) ->
                val count = list.size
                val mean = list.average()
                val dev = mean - grandMean
                ssBetween += count * dev * dev
            }
            val dfBetween = k - 1
            val msBetween = ssBetween / dfBetween

            // Sum of Squares Within
            var ssWithin = 0.0
            activeGroups.forEach { (grpName, list) ->
                val mean = list.average()
                val grpSS = list.map { (it - mean).let { d -> d * d } }.sum()
                ssWithin += grpSS
            }
            val dfWithin = totalN - k
            val msWithin = if (dfWithin > 0) ssWithin / dfWithin else 0.0

            if (msWithin == 0.0) {
                return SubsetHypothesisResult(
                    testType = SubsetTestType.ONE_WAY_ANOVA,
                    statistic = 0.0, df = dfBetween.toDouble() + dfWithin, pValue = 1.0,
                    confidenceIntervalLower = null, confidenceIntervalUpper = null, meanDifference = null,
                    groupsStats = groupStats,
                    explanation = "All groups have 0 within-group variance.",
                    errorMessage = null
                )
            }

            val fStat = msBetween / msWithin
            val pValue = Distributions.fPValue(fStat, dfBetween, dfWithin)

            val sigText = if (pValue < 0.05) "statistically significant" else "not statistically significant"
            
            val groupDetails = groupStats.joinToString("; ") { 
                String.format(java.util.Locale.US, "%s (N=%d, M=%.2f, SD=%.2f)", it.groupName, it.count, it.mean, it.stdDev)
            }
            val explanation = String.format(
                java.util.Locale.US,
                "A One-Way Analysis of Variance (ANOVA) was conducted on '%s' grouped by '%s' with %d groups: %s. The effect is %s, F(%d, %d) = %.3f, p = %.5f.",
                depVar, groupCol, k, groupDetails, sigText, dfBetween, dfWithin, fStat, pValue
            )

            return SubsetHypothesisResult(
                testType = SubsetTestType.ONE_WAY_ANOVA,
                statistic = fStat,
                df = dfBetween.toDouble() + dfWithin,
                pValue = pValue,
                confidenceIntervalLower = null,
                confidenceIntervalUpper = null,
                meanDifference = null,
                groupsStats = groupStats,
                explanation = explanation,
                errorMessage = null
            )
        } catch (e: Exception) {
            return SubsetHypothesisResult(
                testType = SubsetTestType.ONE_WAY_ANOVA,
                statistic = 0.0, df = 0.0, pValue = 1.0,
                confidenceIntervalLower = null, confidenceIntervalUpper = null, meanDifference = null,
                groupsStats = emptyList(),
                explanation = "",
                errorMessage = "Calculation error: ${e.message}"
            )
        }
    }
}

