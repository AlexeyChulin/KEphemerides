package com.example.dephem


enum class CelestialBody {
    MERCURY,
    VENUS,
    EARTH,
    MARS,
    JUPITER,
    SATURN,
    URANUS,
    NEPTUNE,
    PLUTO,
    MOON,
    SUN,
    SSBARY,  // Solar System Barycenter
    EMBARY // Earth - Moon Barycenter
}

enum class Other {
    EARTH_NUTATIONS,
    LUNAR_MANTLE_LIBRATION,
    LUNAR_MANTLE_ANGULAR_VELOCITY,
    TTM_TDB
}

enum class CalculationKind {
    POSITION, // Calculate position only
    STATE // Calculate total state vector i.e. position and velocity
}