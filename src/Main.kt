package com.example.dephem

import kotlin.math.abs


fun main() {
    testLoadCoefficients()
    testEphemeris01NewYear2024()
    testEphemeris02StartBlock()
    testEphemeris03March2033()
    testEphemeris04July2027()
}

fun testEphemeris01NewYear2024() {
    println("Test EphemerisRelease 01: test date & time 2024-01-01 00:00:00")
    val datetime = DateTime(2024, 1, 1, 0, 0, 0)
    val referenceStateSun = arrayOf(24810993.259654, -133033452.131155, -57668106.240179, 29.841464, 4.703725, 2.038024)
    val referenceStateMoon = arrayOf(-367952.531142, 142774.973143, 89342.281181, -0.409768, -0.779798, -0.402679)
    testEphemeris(datetime, referenceStateSun, referenceStateMoon)
    val referenceStateMars = arrayOf(-19074781.314242, -330823495.157156, -147205952.773975, 54.503378, 2.469513, 0.347854)
    testEphemerisMars(datetime, referenceStateMars)
    val referenceStateVenus = arrayOf(	-82187748.980149, -145937135.858503, -56704820.144932, 33.354924, -27.115838, -12.501973)
    testEphemerisVenus(datetime, referenceStateVenus)
}

fun testEphemeris02StartBlock() {
    println("Test EphemerisRelease 02: test date & time 2023-12-26 00:00:00 (start a new block)")
    val datetime = DateTime(2023, 12,26,0, 0, 0)
    val referenceStateSun = arrayOf(9233761.509019, -134725387.920829, -58401381.549880, 30.202551, 1.819586, 0.789203)
    val referenceStateMoon = arrayOf(57619.183239, 339537.840455, 179141.140298, -0.999298, 0.152655, 0.118505)
    testEphemeris(datetime, referenceStateSun, referenceStateMoon)
}

fun testEphemeris03March2033() {
    println("Test EphemerisRelease 03: test date & time 2033-3-14 00:00:00")
    val datetime = DateTime(2033, 3, 14, 0, 0, 0)
    val referenceStateSun = arrayOf(147683271.310658, -16007778.360424, -6940442.404462, 3.974841, 27.236287, 11.806681)
    val referenceStateMoon = arrayOf(-352870.580480, 192077.348743, 52357.438678, -0.490098, -0.791989, -0.282740)
    testEphemeris(datetime, referenceStateSun, referenceStateMoon)
}

fun testEphemeris04July2027() {
    println("Test EphemerisRelease 03: test date & time 2027-07-04 12:00:00")
    val datetime = DateTime(2027, 7, 4, 12, 0, 0)
    val referenceStateSun = arrayOf(-31441055.778901, 136538825.805945, 59186920.359570, -28.673277, -5.550450, -2.407040)
    val referenceStateMoon = arrayOf(-105938.167887, 308597.822627, 148338.586188, -1.044174, -0.264739, -0.214517)
    testEphemeris(datetime, referenceStateSun, referenceStateMoon)
}

fun testLoadCoefficients() {
    val datetime = DateTime(2033, 11, 19, 0, 0, 0)
    val julianDate = datetime.dateTimeToJED()
    val coefficients = loadCoefficients(julianDate)
    if (coefficients.values != null) {
        for (c in coefficients.values) {
            println("coefficient = $c")
        }
        println("Coefficients loaded: ${coefficients.values.size}")
    }
    println("keys = ${ coefficients.keys.joinToString(" ") }")
    println("epoch start date = ${jedToDateTime(coefficients.epochStart)} (julian date ${coefficients.epochStart})")
    println("epoch end date = ${jedToDateTime(coefficients.epochEnd)} (julian date ${coefficients.epochEnd})")
    println("block time span = ${coefficients.blockTimeSpan}")
    println("block start date = ${jedToDateTime(coefficients.blockStartDate)} (julian date ${coefficients.blockStartDate})")
    println("block end date = ${jedToDateTime(coefficients.blockEndDate)} (julian date ${coefficients.blockEndDate})")
    println("datetime = $datetime (julian date $julianDate)")
    val normalizedTime = (julianDate - coefficients.epochStart) / coefficients.blockTimeSpan
    val offset = normalizedTime.toInt()
    println("normalizedTime = $normalizedTime")
    println("offset = $offset")
}

fun testEphemeris(datetime: DateTime, referenceStateSun: Array<Double>, referenceStateMoon: Array<Double>) {
    val julianDate = datetime.dateTimeToJED()
    val ephemeris = EphemerisRelease(julianDate)
    println("datetime = $ (julian date $julianDate)")
    println("block start date = ${jedToDateTime(ephemeris.coefficients.blockStartDate)} (julian date ${ephemeris.coefficients.blockStartDate})")
    println("block end date =  ${jedToDateTime(ephemeris.coefficients.blockEndDate)} (julian date ${ephemeris.coefficients.blockEndDate})")
    if (ephemeris.coefficients.values == null) {
        println("ephemeris.coefficients.values == null")
    }
    val resultArraySun = ephemeris.calculateBody(CalculationKind.POSITION, CelestialBody.SUN, CelestialBody.EARTH)
    println("Sun position relative to Earth: [${resultArraySun!![0]}, ${resultArraySun[1]}, ${resultArraySun[2]}] km")
    for (i in resultArraySun.indices) {
        assertDoubleEqual(resultArraySun[i], referenceStateSun[i], 1.0)
    }
    val resultArrayMoon = ephemeris.calculateBody(CalculationKind.POSITION, CelestialBody.MOON, CelestialBody.EARTH)
    println("Moon position relative to Earth: [${resultArrayMoon!![0]}, ${resultArrayMoon[1]}, ${resultArrayMoon[2]}] km")
    for (i in resultArraySun.indices) {
        assertDoubleEqual(resultArrayMoon[i], referenceStateMoon[i], 1e-3)
    }
    val resultStateSun = ephemeris.calculateBody(CalculationKind.STATE, CelestialBody.SUN, CelestialBody.EARTH)
    //println("resultStateSun.size = ${resultStateSun!!.size}")
    println("Sun position relative to Earth: [${resultStateSun!![0]}, ${resultStateSun[1]}, ${resultStateSun[2]}] km")
    println("Sun velocity relative to Earth: [${resultStateSun[3]}, ${resultStateSun[4]}, ${resultStateSun[5]}] km/s")
    for (i in 0..2) {
        assertDoubleEqual(resultStateSun[i], referenceStateSun[i], 1e-2)
    }
    for (i in 3..5) {
        assertDoubleEqual(resultStateSun[i], referenceStateSun[i], 1e-6)
    }
    val resultStateMoon = ephemeris.calculateBody(CalculationKind.STATE, CelestialBody.MOON, CelestialBody.EARTH)
    //println("resultStateMoon.size = ${resultStateMoon!!.size}")
    println("Moon position relative to Earth: [${resultStateMoon!![0]}, ${resultStateMoon[1]}, ${resultStateMoon[2]}] km")
    println("Moon velocity relative to Earth: [${resultStateMoon[3]}, ${resultStateMoon[4]}, ${resultStateMoon[5]}] km/s")
    for (i in 0..2) {
        assertDoubleEqual(resultStateMoon[i], referenceStateMoon[i], 1e-5)
    }
    for (i in 3..5) {
        assertDoubleEqual(resultStateMoon[i], referenceStateMoon[i], 1e-6)
    }
}

fun testEphemerisMars(datetime: DateTime, referenceStateMars: Array<Double>) {
    val julianDate = datetime.dateTimeToJED()
    val ephemeris = EphemerisRelease(julianDate)
    println("datetime = $ (julian date $julianDate)")
    println("block start date = ${jedToDateTime(ephemeris.coefficients.blockStartDate)} (julian date ${ephemeris.coefficients.blockStartDate})")
    println("block end date =  ${jedToDateTime(ephemeris.coefficients.blockEndDate)} (julian date ${ephemeris.coefficients.blockEndDate})")
    if (ephemeris.coefficients.values == null) {
        println("ephemeris.coefficients.values == null")
    }
    val resultArrayMars = ephemeris.calculateBody(CalculationKind.POSITION, CelestialBody.MARS, CelestialBody.EARTH)
    println("Mars position relative to Earth: [${resultArrayMars!![0]}, ${resultArrayMars[1]}, ${resultArrayMars[2]}] km")
    for (i in resultArrayMars.indices) {
        assertDoubleEqual(resultArrayMars[i], referenceStateMars[i], 1.0)
    }
    val resultStateMars = ephemeris.calculateBody(CalculationKind.STATE, CelestialBody.MARS, CelestialBody.EARTH)
    //println("resultStateSun.size = ${resultStateSun!!.size}")
    println("Mars position relative to Earth: [${resultStateMars!![0]}, ${resultStateMars[1]}, ${resultStateMars[2]}] km")
    println("Mars velocity relative to Earth: [${resultStateMars[3]}, ${resultStateMars[4]}, ${resultStateMars[5]}] km/s")
    for (i in 0..2) {
        assertDoubleEqual(resultStateMars[i], referenceStateMars[i], 1e-2)
    }
    for (i in 3..5) {
        assertDoubleEqual(resultStateMars[i], referenceStateMars[i], 1e-6)
    }
}

fun testEphemerisVenus(datetime: DateTime, referenceStateVenus: Array<Double>) {
    val julianDate = datetime.dateTimeToJED()
    val ephemeris = EphemerisRelease(julianDate)
    println("datetime = $ (julian date $julianDate)")
    println("block start date = ${jedToDateTime(ephemeris.coefficients.blockStartDate)} (julian date ${ephemeris.coefficients.blockStartDate})")
    println("block end date =  ${jedToDateTime(ephemeris.coefficients.blockEndDate)} (julian date ${ephemeris.coefficients.blockEndDate})")
    if (ephemeris.coefficients.values == null) {
        println("ephemeris.coefficients.values == null")
    }
    val resultArrayVenus = ephemeris.calculateBody(CalculationKind.POSITION, CelestialBody.VENUS, CelestialBody.EARTH)
    println("Venus position relative to Earth: [${resultArrayVenus!![0]}, ${resultArrayVenus[1]}, ${resultArrayVenus[2]}] km")
    for (i in resultArrayVenus.indices) {
        assertDoubleEqual(resultArrayVenus[i], referenceStateVenus[i], 1.0)
    }
    val resultStateVenus = ephemeris.calculateBody(CalculationKind.STATE, CelestialBody.VENUS, CelestialBody.EARTH)
    //println("resultStateSun.size = ${resultStateSun!!.size}")
    println("Venus position relative to Earth: [${resultStateVenus!![0]}, ${resultStateVenus[1]}, ${resultStateVenus[2]}] km")
    println("Venus velocity relative to Earth: [${resultStateVenus[3]}, ${resultStateVenus[4]}, ${resultStateVenus[5]}] km/s")
    for (i in 0..2) {
        assertDoubleEqual(resultStateVenus[i], referenceStateVenus[i], 1e-2)
    }
    for (i in 3..5) {
        assertDoubleEqual(resultStateVenus[i], referenceStateVenus[i], 1e-6)
    }
}

fun assertDoubleEqual(left: Double, right: Double, tol: Double) {
    if(abs(left - right) > tol) {
        throw (AssertionError("Assertion abs($left - $right) <= $tol failed"))
    }
}