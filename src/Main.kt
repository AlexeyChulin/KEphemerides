package com.example.dephem

import kotlin.math.abs


fun main() {
    testJEDConverter()
    test1()
}

fun testJEDConverter() {
    val datetime = DateTime(2033, 11, 19, 0, 0, 0)
    val julianDate = datetime.dateTimeToJED()
    val coeffs = loadCoefficients(julianDate)
    if (coeffs.values != null) {
        for (c in coeffs.values) {
            println("coefficient = $c")
        }
        println("Coefficients loaded: ${coeffs.values.size}")
    }
    println("keys = ${ coeffs.keys.joinToString(" ") }")
    println("epoch start date = ${jedToDateTime(coeffs.epochStart)} (julian date ${coeffs.epochStart})")
    println("epoch end date = ${jedToDateTime(coeffs.epochEnd)} (julian date ${coeffs.epochEnd})")
    println("block time span = ${coeffs.blockTimeSpan}")
    println("block start date = ${jedToDateTime(coeffs.blockStartDate)} (julian date ${coeffs.blockStartDate})")
    println("block end date = ${jedToDateTime(coeffs.blockEndDate)} (julian date ${coeffs.blockEndDate})")
    println("datetime = $datetime (julian date $julianDate)")
    val normalizedTime = (julianDate - coeffs.epochStart) / coeffs.blockTimeSpan
    val offset = normalizedTime.toInt()
    println("normalizedTime = $normalizedTime")
    println("offset = $offset")
}

fun test1() {
    val datetimeBegin = DateTime(2024, 1, 1, 0, 0, 0)
    val julianDateBegin = datetimeBegin.dateTimeToJED()
    val ephemeris = EphemerisRelease(julianDateBegin)
    println("datetimeBegin = $ (julian date $julianDateBegin)")
    println("block start date = ${jedToDateTime(ephemeris.coefficients.blockStartDate)} (julian date ${ephemeris.coefficients.blockStartDate})")
    println("block end date =  ${jedToDateTime(ephemeris.coefficients.blockEndDate)} (julian date ${ephemeris.coefficients.blockEndDate})")
    if (ephemeris.coefficients.values == null) {
        println("ephemeris.coefficients.values == null")
    }
    val resultArraySun = ephemeris.calculateBody(CalculationKind.POSITION, CelestialBody.SUN, CelestialBody.EARTH)
    println("Sun position relative to Earth: [${resultArraySun!![0]}, ${resultArraySun[1]}, ${resultArraySun[2]}] km")
    val referenceArraySun = arrayOf(24810993.259654, -133033452.131155, -57668106.240179)
    for (i in resultArraySun.indices) {
        assertDoubleEqual(resultArraySun[i], referenceArraySun[i], 1.0)
    }
    val resultArrayMoon = ephemeris.calculateBody(CalculationKind.POSITION, CelestialBody.MOON, CelestialBody.EARTH)
    println("Moon position relative to Earth: [${resultArrayMoon!![0]}, ${resultArrayMoon[1]}, ${resultArrayMoon[2]}] km")
    val referenceArrayMoon = arrayOf(-367952.531142, 142774.973143, 89342.281181)
    for (i in resultArraySun.indices) {
        assertDoubleEqual(resultArrayMoon[i], referenceArrayMoon[i], 1e-3)
    }
    val resultStateSun = ephemeris.calculateBody(CalculationKind.STATE, CelestialBody.SUN, CelestialBody.EARTH)
    //println("resultStateSun.size = ${resultStateSun!!.size}")
    println("Sun position relative to Earth: [${resultStateSun!![0]}, ${resultStateSun[1]}, ${resultStateSun[2]}] km")
    println("Sun velocity relative to Earth: [${resultStateSun[3]}, ${resultStateSun[4]}, ${resultStateSun[5]}] km")
    val referenceStateSun = arrayOf(24810993.259654, -133033452.131155, -57668106.240179, 29.841464, 4.703725, 2.038024)
    for (i in 0..2) {
        assertDoubleEqual(resultStateSun[i], referenceStateSun[i], 1.0)
    }
    for (i in 3..5) {
        assertDoubleEqual(resultStateSun[i], referenceStateSun[i], 1e-3)
    }
    val resultStateMoon = ephemeris.calculateBody(CalculationKind.STATE, CelestialBody.MOON, CelestialBody.EARTH)
    //println("resultStateMoon.size = ${resultStateMoon!!.size}")
    println("Moon position relative to Earth: [${resultStateMoon!![0]}, ${resultStateMoon[1]}, ${resultStateMoon[2]}] km")
    println("Moon velocity relative to Earth: [${resultStateMoon[3]}, ${resultStateMoon[4]}, ${resultStateMoon[5]}] km")
    val referenceStateMoon = arrayOf(-367952.531142, 142774.973143, 89342.281181,
        -0.409768, -0.779798, -0.402679)
    for (i in 0..2) {
        assertDoubleEqual(resultStateMoon[i], referenceStateMoon[i], 1e-3)
    }
    for (i in 3..5) {
        assertDoubleEqual(resultStateMoon[i], referenceStateMoon[i], 1e-3)
    }
}

fun assertDoubleEqual(left: Double, right: Double, tol: Double) {
    if(abs(left - right) > tol) {
        throw (AssertionError("Assertion abs($left - $right) <= $tol failed"))
    }
}