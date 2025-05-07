# KEphemerides

## Общие сведения

Библиотека Kotlin для работы с эфемеридами JPL NASA.

Основана на C++ библиотеке [Dephem](https://github.com/SpaceCalc/dephem)

Совместимость: Kotlin language v.1.9

Зависимости:

```
package com.example.dephem

import java.io.BufferedReader
import java.io.FileReader
```

## Особенности и ограничения

Так как библиотека KEphemerides предназначена для мобильной разработки, то использует собственный формат файлов вместо бинарного формата JPL NASA; при этом размер каждого файла ограничен интервалом 10 лет.

## Структура проекта

- Dephem.kt -  модуль вычисления эфемерид

- DephemEnums.kt -  модуль определения Enums для настроек вычислений 

- JEDConverter.kt - модуль преобразования даты/времени из структуры DateTime в юлианскую дату (Double) и обратно


## Использование 

1. Создать объект **EphemerisRelease**

Пример:

```
    val datetime = DateTime(2024, 1, 1, 0, 0, 0) // задать дату и время, на которую требуется рассчитать эфемериды
    val julianDate = datetime.dateTimeToJED() // преобразовать дату и время в юлианскую дату
    val ephemeris = EphemerisRelease(julianDate) // Создать объект EphemerisRelease на требуемую юлианскую дату
```
При этом автоматически загружаются интерполяционные коэффициенты полинома Чебышева из файла

2. Вычислить эфемериды вызовом метода **calculateBody** объекта **EphemerisRelease**:

```
    fun calculateBody(calculationKind: CalculationKind, targetBody: CelestialBody, centerBody: CelestialBody): DoubleArray? 

```
где 

**calculationKind**``` - перечисление типов вычислений:

```
enum class CalculationKind {
    POSITION, // Вычислить только радиус-вектор
    STATE // Вычислить полный вектор состояния, т.е радиус-вектор и вектор скорости
}
```

**targetBody** - тело, для которого определяются эфемериды;

**centerBody** - тело, служащее началом системы координат:

**CelestialBody** - перечисление доступных тел:

```
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
```

**Возвращаемое значение**: требуемый вектор (радиус-вектор или вектор состояния) или **null**, если вычислить требуемый вектор не удалось

