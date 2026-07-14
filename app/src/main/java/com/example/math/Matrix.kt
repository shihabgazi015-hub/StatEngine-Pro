package com.example.math

import kotlin.math.abs

object Matrix {

    fun transpose(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val rows = matrix.size
        val cols = matrix[0].size
        val result = Array(cols) { DoubleArray(rows) }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[j][i] = matrix[i][j]
            }
        }
        return result
    }

    fun multiply(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val rowsA = a.size
        val colsA = a[0].size
        val colsB = b[0].size
        val result = Array(rowsA) { DoubleArray(colsB) }
        for (i in 0 until rowsA) {
            for (j in 0 until colsB) {
                var sum = 0.0
                for (k in 0 until colsA) {
                    sum += a[i][k] * b[k][j]
                }
                result[i][j] = sum
            }
        }
        return result
    }

    /**
     * Invert a square matrix using Gaussian Elimination with partial pivoting.
     * Throws an ArithmeticException if the matrix is singular.
     */
    fun invert(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val n = matrix.size
        // Create augmented matrix [A | I]
        val augmented = Array(n) { DoubleArray(2 * n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                augmented[i][j] = matrix[i][j]
            }
            augmented[i][n + i] = 1.0
        }

        for (i in 0 until n) {
            // Find pivot row
            var pivotRow = i
            var maxVal = abs(augmented[i][i])
            for (r in i + 1 until n) {
                if (abs(augmented[r][i]) > maxVal) {
                    maxVal = abs(augmented[r][i])
                    pivotRow = r
                }
            }

            if (maxVal < 1e-12) {
                throw ArithmeticException("Matrix is singular and cannot be inverted.")
            }

            // Swap pivot row
            if (pivotRow != i) {
                val temp = augmented[i]
                augmented[i] = augmented[pivotRow]
                augmented[pivotRow] = temp
            }

            // Normalize pivot row
            val pivotVal = augmented[i][i]
            for (j in 0 until 2 * n) {
                augmented[i][j] /= pivotVal
            }

            // Eliminate column values in other rows
            for (r in 0 until n) {
                if (r != i) {
                    val factor = augmented[r][i]
                    for (j in 0 until 2 * n) {
                        augmented[r][j] -= factor * augmented[i][j]
                    }
                }
            }
        }

        // Extract inverse matrix
        val inverse = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                inverse[i][j] = augmented[i][n + j]
            }
        }
        return inverse
    }
}
