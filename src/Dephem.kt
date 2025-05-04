// Ephemeris calculation module

package com.example.dephem

import java.io.BufferedReader
import java.io.FileReader

//data class EphemerisBlockInfo(val blockNum: Int, val blockStartDate: Double)

data class InterpolationCoefficients(val epochStart: Double = 0.0,
                                     val epochEnd: Double = 0.0,
                                     val blockTimeSpan: Double = 0.0,
                                     val blockStartDate: Double = 0.0,
                                     val blockEndDate: Double = 0.0,
                                     val keys: Array<Array<Int>> = Array(15) { Array(3) {0}},
                                     val values: DoubleArray? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InterpolationCoefficients

        if (epochStart != other.epochStart) return false
        if (epochEnd != other.epochEnd) return false
        if (blockTimeSpan != other.blockTimeSpan) return false
        if (blockStartDate != other.blockStartDate) return false
        if (blockEndDate != other.blockEndDate) return false
        if (!keys.contentEquals(other.keys)) return false
        if (values != null) {
            if (other.values == null) return false
            if (!values.contentEquals(other.values)) return false
        } else if (other.values != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = epochStart.hashCode()
        result = 31 * result + epochEnd.hashCode()
        result = 31 * result + blockTimeSpan.hashCode()
        result = 31 * result + blockStartDate.hashCode()
        result = 31 * result + blockEndDate.hashCode()
        result = 31 * result + keys.hashCode()
        result = 31 * result + (values?.contentHashCode() ?: 0)
        return result
    }
}

class EphemerisRelease(private val julianDate: Double) {
    val coefficients: InterpolationCoefficients = loadCoefficients(this.julianDate)
    private val emrat2 = 0.01215058440 // Отношение массы Луны к массе Земля-Луна

    fun calculateBody(calculationKind: CalculationKind, targetBody: CelestialBody, centerBody: CelestialBody): DoubleArray? {
        var bodyVector : DoubleArray? = null
        if (coefficients.values == null) {
            return null
        }
        val componentsCount = if (calculationKind == CalculationKind.STATE) {
            6 // length of state vector
        } else {
            3 // length of radius vector
        }
        //println("componentsCount = $componentsCount") // DEBUG
        if (targetBody == centerBody) {
            // if targetBody is centerBody, its state vector is all-zeroes
            //println("targetBody == centerBody") // DEBUG
            return DoubleArray(componentsCount) { 0.0 }
        } else if (targetBody == CelestialBody.SSBARY || centerBody == CelestialBody.SSBARY) {
            // Either targetBody or centerBody is the Solar System barycenter
            val body = if (targetBody == CelestialBody.SSBARY)  centerBody else targetBody
            //println("body = $body") // DEBUG
            bodyVector = calculateBodyVector(body, julianDate, calculationKind)
            if (targetBody == CelestialBody.SSBARY) {
                for (i in bodyVector.indices) {
                    bodyVector[i] = -bodyVector[i]
                }
            }
        } else if ((targetBody == CelestialBody.EARTH && centerBody == CelestialBody.MOON)
            || (targetBody == CelestialBody.MOON && centerBody == CelestialBody.EARTH)) {
            // Earth - Moon relative vector calculation
            bodyVector = calculateBaseItem(CelestialBody.MOON.ordinal, julianDate, calculationKind)
            if (targetBody == CelestialBody.EARTH) {
                for (i in bodyVector.indices) {
                    bodyVector[i] = -bodyVector[i]
                }
            }
        }
        else {
            // All other cases: calculate both center body and target body vectors relative to Solar System barycenter
            // Then calculate their difference
            //println("DEBUG: calculate centerBodyVector")// DEBUG
            var centerBodyVector = calculateBodyVector(centerBody, julianDate, calculationKind)
            //println("DEBUG: calculate bodyVector")// DEBUG
            bodyVector = calculateBodyVector(targetBody, julianDate, calculationKind)
            for (i in bodyVector.indices) {
                bodyVector[i] -= centerBodyVector[i]
            }
        }
        return bodyVector
    }

    private fun calculateBodyVector(body: CelestialBody,
                                    julianDate: Double,
                                    calculationKind: CalculationKind) : DoubleArray {
        return when (body) {
            CelestialBody.EARTH -> calculateBaseEarth(julianDate, calculationKind)
            CelestialBody.MOON -> calculateBaseMoon(julianDate, calculationKind)
            CelestialBody.EMBARY -> calculateBaseItem(2, julianDate, calculationKind)
            else -> calculateBaseItem(body.ordinal, julianDate, calculationKind)
        }

    }

    private fun calculateBaseItem(baseItemIndex : Int, julianDate : Double, calculationKind: CalculationKind) : DoubleArray {
        // baseItemIndex: index of element:
        //		0		Mercury
        //		1		Venus
        //		2		Earth-Moon barycenter
        //		3		Mars
        //		4		Jupiter
        //		5		Saturn
        //		6		Uranus
        //		7		Neptune
        //		8		Pluto
        //		9		Moon (geocentric)
        //		10		Sun
        //		11		Earth Nutations in longitude and obliquity (IAU 1980 model)
        //		12		Lunar mantle libration
        //		13		Lunar mantle angular velocity
        //		14		TT-TDB (at geocenter)
        //println("DEBUG: running calculateBaseItem (baseItemIndex = $baseItemIndex)") // DEBUG
        val normalizedTime0 = (julianDate - coefficients.epochStart) / coefficients.blockTimeSpan
        val offset0 = normalizedTime0.toInt()
        val normalizedTime1 = (normalizedTime0 - offset0) * coefficients.keys[baseItemIndex][2]
        val offset1 = normalizedTime1.toInt()
        val normalizedTime = 2 * (normalizedTime1 - offset1) - 1
        val componentsCount = when (baseItemIndex) {
            11 -> 2
            14 -> 1
            else -> 3
        }
        val startPos = coefficients.keys[baseItemIndex][0] - 1 +
                componentsCount * offset1 * coefficients.keys[baseItemIndex][1]
        return if (calculationKind == CalculationKind.POSITION) {
            //print("DEBUG: normalizedTime = $normalizedTime, ") // DEBUG
            //print("DEBUG: startPos = ${startPos}, ") // DEBUG
            //println("DEBUG: componentsCount = ${componentsCount}, ") // DEBUG
            interpolatePosition(baseItemIndex, normalizedTime, coefficients.keys, coefficients.values!!, startPos, componentsCount)
        } else {
            interpolateState(baseItemIndex, normalizedTime, coefficients.keys, coefficients.values!!, coefficients.blockTimeSpan, startPos, componentsCount)
        }
    }
    private fun calculateBaseEarth(julianDate : Double, calculationKind: CalculationKind) : DoubleArray {
        // calculate Earth vector relative to Solar System barycenter
        // println("DEBUG: running calculateBaseEarth") // DEBUG
        var baseEarthVector = calculateBaseItem(2, julianDate, calculationKind)
        var baseMoonVector = calculateBaseItem(9, julianDate, calculationKind)
        for (i in baseEarthVector.indices) {
            baseEarthVector[i] -= baseMoonVector[i] * emrat2
        }
        return baseEarthVector
    }
    private fun calculateBaseMoon(julianDate : Double, calculationKind: CalculationKind) : DoubleArray {
        // calculate Moon vector relative to Solar System barycenter
        var baseVector = calculateBaseItem(2, julianDate, calculationKind)
        var baseMoonVector = calculateBaseItem(9, julianDate, calculationKind)
        for (i in baseVector.indices) {
            baseVector[i] += baseMoonVector[i] * (1 - emrat2)
        }
        return baseVector
    }
    private fun interpolatePosition(baseItemIndex : Int,
                                    normalizedTime : Double,
                                    coefficientKeys : Array<Array<Int>>,
                                    coefficientValues : DoubleArray,
                                    startPos : Int,
                                    componentsCount : Int) :  DoubleArray {
        //print("DEBUG: running interpolatePosition (baseItemIndex = $baseItemIndex") // DEBUG
        val cpec = coefficientKeys[baseItemIndex][1] // Копирование значения количества коэффициентов на компоненту
        //println(", cpec = $cpec)") // DEBUG
        // Заполнение полиномов
        val poly : Array<Double> = Array(cpec) { 0.0 }
        poly[0]  = 1.0
        poly[1] = normalizedTime
        for (i in 2..<cpec) {
            poly[i] = 2 * normalizedTime * poly[i - 1] - poly[i - 2]
        }
        /*print("DEBUG: poly = ") // DEBUG
        for (i in poly.indices) {
            print("${poly[i]} ") // DEBUG
        }
        println() // DEBUG */
        // Coordinate calculation:
        val result : Array<Double> = Array(componentsCount) { 0.0 }
        for (i in 0..<componentsCount) {
            for (j in 0..<cpec) {
                result[i] += poly[j] * coefficientValues[startPos + i * cpec + j]
            }
        }
        return result.toDoubleArray()
    }
    private fun interpolateState(baseItemIndex : Int,
                                 normalizedTime : Double,
                                 coefficientKeys : Array<Array<Int>>,
                                 coefficientValues : DoubleArray,
                                 blockTimeSpan : Double,
                                 startPos : Int,
                                 componentsCount : Int) :  DoubleArray {
        //print("DEBUG: running interpolateState (baseItemIndex = $baseItemIndex") // DEBUG
        val cpec = coefficientKeys[baseItemIndex][1] // Копирование значения количества коэффициентов на компоненту
        //println(", cpec = $cpec)") // DEBUG
        // Заполнение полиномов
        val poly : Array<Double> = Array(cpec) { 0.0 }
        val dpoly : Array<Double> = Array(cpec) { 0.0 }
        poly[0]  = 1.0
        dpoly[0] = 0.0
        dpoly[1] = 1.0
        // TODO: from here
        poly[1] = normalizedTime
        poly[2]  = 2 * normalizedTime * normalizedTime - 1;
        dpoly[2] = 4 * normalizedTime;
        for (i in 3..<cpec) {
            poly[i] = 2 * normalizedTime * poly[i - 1] - poly[i - 2]
            dpoly[i] = 2 * poly[i - 1] + 2 * normalizedTime * dpoly[i - 1] - dpoly[i - 2]
        }
        /*print("DEBUG: poly = ") // DEBUG
        for (i in poly.indices) {
            print("${poly[i]} ") // DEBUG
        }
        println() // DEBUG
        print("DEBUG: dpoly = ") // DEBUG
        for (i in dpoly.indices) {
            print("${dpoly[i]} ") // DEBUG
        }
        println() // DEBUG
        print("DEBUG: coefficientValues = ") // DEBUG
        for (i in 0..<componentsCount) {
            for (j in 0..<cpec)
            print("${coefficientValues[startPos + i * cpec + j]} ") // DEBUG
        }
        println() // DEBUG */

        // Defining a variable for unit conversion:
        val dimensionFit: Double =  1.0 / (43200.0 * blockTimeSpan)
        val derivativeUnits: Double = coefficientKeys[baseItemIndex][2] * dimensionFit
        // Coordinate calculation:
        val result : Array<Double> = Array(componentsCount*2) { 0.0 }
        for (i in 0..<componentsCount) {
            for (j in 0..<cpec) {
                result[i] += poly[j] * coefficientValues[startPos + i * cpec + j]
                result[i + componentsCount] += dpoly[j] * coefficientValues[startPos + i * cpec + j]
            }
            result[i + componentsCount] *= derivativeUnits
        }
        /*print("DEBUG: result = ") // DEBUG
        for (i in result.indices) {
            print("${result[i]} ") // DEBUG
        }
        println() // DEBUG */
        return result.toDoubleArray()
    }
}

fun loadCoefficients(julianDate : Double) : InterpolationCoefficients {
    var keys : Array<Array<Int>> = Array(15) { Array(3) {0}}
    var coefficients : DoubleArray? = null // DoubleArray(1018) { 0.0 }
    val filePath = "chebyshev.txt" // Поместить в каталог проекта
    var reader: BufferedReader? = null
    var epochStart = 0.0
    var epochEnd = 0.0
    var blockTimeSpan = 0.0
    var startDate = 0.0
    var endDate = 0.0

    try {
        reader = BufferedReader(FileReader(filePath))
        var line: String?
        var nline = 0
        var blockStartDate : Double? = null
        var blockEndDate : Double? = null
        while (reader.readLine().also { line = it } != null) {
            // Process each line
            if (nline == 0) {
                // read NASA database epoch start
                epochStart = getDoubleFromLine(line)
            } else if (nline == 1) {
                // read NASA database epoch end
                epochEnd = getDoubleFromLine(line)
            } else if (nline == 2) {
                // read block time span
                blockTimeSpan = getDoubleFromLine(line)
            } else if (nline == 3) {
                // read block time span
                keys = getKeys(line)
            } else {
                val index = (nline - 4) % 1021
                if (index == 0) {
                    // read block number
                    val regex = Regex("[0-9]+")
                    val match = regex.find(line!!) // !! needed to convert nullable type to non-nullable
                    val blockNum = match?.value?.toIntOrNull()
                } else if (index == 1) {
                    // read block start date
                    val regex = Regex("[0-9]+([.][0-9]*)?")
                    val match = regex.find(line!!) // !! needed to convert nullable type to non-nullable
                    blockStartDate = match?.value?.toDoubleOrNull()
                    if (blockStartDate != null) {
                        blockEndDate = blockStartDate + 32.0
                        startDate = blockStartDate
                        endDate = blockEndDate
                    }
                } else if ((blockStartDate != null)
                    && (blockEndDate != null)
                    && (julianDate >= blockStartDate)
                    && (julianDate < blockEndDate)
                    && (index > 2)) {
                    // read coefficient if input date between block_start_date & block_end_date
                    if (coefficients == null) {
                        coefficients = DoubleArray(1018) { 0.0 }
                    }
                    coefficients[index-3] = line!!.toDouble()
                    if (index == 1020) { break }
                }
            }
            nline++
        }
    } catch (e: Exception) {
        println("An error occurred: ${e.message}")
        coefficients = null
    } finally {
        try {
            reader?.close()
        } catch (e: Exception) {
            println("An error occurred while closing the file: ${e.message}")
        }
    }
    return InterpolationCoefficients(epochStart=epochStart,
        epochEnd=epochEnd,
        blockTimeSpan=blockTimeSpan,
        blockStartDate=startDate,
        blockEndDate=endDate,
        keys=keys,
        values=coefficients)
}

fun getKeys(line: String?) : Array<Array<Int>> {
    val list = line!!.split(" ")
    val filtered = list.slice(1..list.size - 2) // One empty string is on the end after split
    val lKeys = filtered.map { it.toInt() }.toIntArray()
    val keys : Array<Array<Int>> = Array(15) { Array(3) {0}}
    for (i in 0..14) {
        keys[i] = arrayOf(lKeys[3*i], lKeys[3*i + 1], lKeys[3*i + 2])
    }

    return keys
}
fun getDoubleFromLine(line: String?) : Double {
    var x = 0.0
    var maybeX : Double? = null
    val regex = Regex("-?[0-9]+([.][0-9]*)?")
    val match = regex.find(line!!) // !! needed to convert nullable type to non-nullable
    maybeX = match?.value?.toDoubleOrNull()
    if (maybeX != null) {
        x = maybeX
    }
    return x
}