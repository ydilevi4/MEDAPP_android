package com.medapp.domain.usecase

import com.medapp.data.model.EqualDistanceRule
import kotlin.math.abs

class DoseCalculationUseCase {
    data class Result(
        val pillCount: Double,
        val realDoseMg: Int,
        val showDeviationAlert: Boolean
    )

    operator fun invoke(
        targetDoseMg: Int,
        pillDoseMg: Int,
        divisibleHalf: Boolean,
        tieRule: EqualDistanceRule
    ): Result {
        val step = if (divisibleHalf) 0.5 else 1.0
        val maxCount = (targetDoseMg.toDouble() / pillDoseMg + 4).toInt().coerceAtLeast(1)
        var bestCount = step
        var bestDiff = Double.MAX_VALUE

        var candidate = step
        while (candidate <= maxCount) {
            val realDose = candidate * pillDoseMg
            val diff = abs(realDose - targetDoseMg)
            if (diff < bestDiff) {
                bestDiff = diff
                bestCount = candidate
            } else if (diff == bestDiff) {
                bestCount = when (tieRule) {
                    EqualDistanceRule.PREFER_HIGHER -> maxOf(bestCount, candidate)
                    EqualDistanceRule.PREFER_LOWER -> minOf(bestCount, candidate)
                }
            }
            candidate += step
        }

        val finalDose = (bestCount * pillDoseMg).toInt()
        val deviation = abs(finalDose - targetDoseMg).toDouble() / targetDoseMg.toDouble()
        return Result(
            pillCount = bestCount,
            realDoseMg = finalDose,
            showDeviationAlert = deviation > 0.10
        )
    }
}
