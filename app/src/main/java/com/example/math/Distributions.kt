package com.example.math

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

object Distributions {

    /**
     * Standard Normal (Z) Cumulative Distribution Function.
     * Uses a highly accurate Abramowitz and Stegun approximation (error < 1.5e-7).
     */
    fun normalCdf(x: Double): Double {
        val sign = if (x < 0) -1.0 else 1.0
        val absX = abs(x)
        
        // erf approximation
        val p = 0.3275911
        val a1 = 0.254829592
        val a2 = -0.284496736
        val a3 = 1.421413741
        val a4 = -1.453152027
        val a5 = 1.061405429
        
        val t = 1.0 / (1.0 + p * absX)
        val erf = 1.0 - ((((a5 * t + a4) * t + a3) * t + a2) * t + a1) * t * exp(-absX * absX)
        
        val cdf = 0.5 * (1.0 + sign * erf)
        return cdf.coerceIn(0.0, 1.0)
    }

    /**
     * Two-tailed p-value for Student's t-distribution.
     */
    fun tPValue(t: Double, df: Int): Double {
        if (df <= 0) return 1.0
        val absT = abs(t)
        // Normal approximation for Student's t-distribution
        val term1 = 1.0 - 1.0 / (4.0 * df)
        val term2 = sqrt(1.0 + (absT * absT) / (2.0 * df))
        val z = (absT * term1) / term2
        val cdf = normalCdf(z)
        val p = 2.0 * (1.0 - cdf)
        return p.coerceIn(0.0, 1.0)
    }

    /**
     * p-value for Chi-Square distribution (one-tailed).
     */
    fun chiSquarePValue(chiSq: Double, df: Int): Double {
        if (chiSq <= 0.0 || df <= 0) return 1.0
        
        if (df == 1) {
            // Chi-Square with 1 df is the square of standard normal
            val z = sqrt(chiSq)
            val p = 2.0 * (1.0 - normalCdf(z))
            return p.coerceIn(0.0, 1.0)
        }
        
        // Wilson-Hilferty transformation of Chi-Square to Normal
        val ratio = chiSq / df
        val term1 = 2.0 / (9.0 * df)
        val num = ratio.pow(1.0 / 3.0) - (1.0 - term1)
        val den = sqrt(term1)
        val z = num / den
        
        val p = 1.0 - normalCdf(z)
        return p.coerceIn(0.0, 1.0)
    }

    /**
     * p-value for F-distribution (one-tailed).
     */
    fun fPValue(f: Double, df1: Int, df2: Int): Double {
        if (f <= 0.0 || df1 <= 0 || df2 <= 0) return 1.0
        
        // Paulson approximation for F-distribution
        val fPow = f.pow(1.0 / 3.0)
        val term1 = 2.0 / (9.0 * df1)
        val term2 = 2.0 / (9.0 * df2)
        
        val num = fPow * (1.0 - term1) - (1.0 - term2)
        val den = sqrt(fPow * fPow * term1 + term2)
        val z = num / den
        
        val p = 1.0 - normalCdf(z)
        return p.coerceIn(0.0, 1.0)
    }
}
