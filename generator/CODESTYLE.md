# Код-стайл генератора

Правила проверяет detekt при `./gradlew check`: готовые правила и правила оформления ktlint — `detekt.yml`, свои —
набор `codestyle` в `detekt-rules/` (имя правила — в скобках у пункта). На ревью остаются исключения из правил по
согласию владельца, отсутствие спек у моделей, пакеты по роли, спека по файлу с вызовом, отсутствие логики в тестах,
вход из `src/test/resources` и `decode`/`encode` в разных `withJson`.

## Файлы

1. Один класс на файл (`OneClassPerFile`), файл называется по классу: модели DTO (`ContractFile.kt`,
   `ReleaseStatus.kt`) и модели логики (`ObjectContract.kt`, `PropertyType.kt`). Подтипы `sealed`-типа объявляются
   внутри него и лежат в его файле.
2. Настройки и сериализаторы тоже лежат каждый в своём файле: `ContractJson.kt`, `VersionedStatusSerializer.kt`.
3. Закрытые служебные классы, нужные только одному классу, объявляются внутри него: `Loaded` и `Resolution` внутри
   `ContractResolver`.
4. Исключения из правила обсуждаются с владельцем до того, как появятся в коде.
5. Пакеты делятся по роли класса, роли не смешиваются: `format/` — DTO формата, `format/json/` — настройка разбора и
   сериализаторы, `model/` — разрешённая модель, `resolve/` — резолвер и его исключение. Спека лежит в пакете своего
   класса.

## Код

1. Тело функции — блок с `return`, у всех функций, включая `override fun toString()` (`BlockBody`). Тело-выражение
   `fun x() = …` не пишется.
2. Литерал в аргументе вызова — число, `Boolean`, `Char`, строка, `null` — с именем параметра
   (`NamedLiteralArgument`): `Property(name = "size", default = null)`. У вызова Java-метода имена параметров не
   стабильны: перед литералом комментарий `/* имя = */`, например `System.err.println(/* x = */ "ошибка")`. Не
   затрагиваются аргументы аннотаций, `vararg` и инфиксные вызовы (`"a" to 1`).
3. Больше трёх аргументов в вызове — все с именами (`NamedArguments`).
4. Параметр лямбды именуется, неявный `it` не пишется нигде, включая лямбды KotestKit (`NoImplicitIt`):
   `names.map { name -> name.uppercase() }`.
5. `run` не используется (`ForbiddenMethodCall`): блок — обычная функция или локальная переменная.
6. Пределы: строка — 120 символов, функция — 40 строк, класс — 250 строк, вложенность `if`/`when`/циклов — 2, `return`
   в функции — 2 без ранних выходов (`MaxLineLength`, `LongMethod`, `LargeClass`, `NestedBlockDepth`, `ReturnCount`).
7. Параметры класса — каждый на своей строке, и у класса с одним параметром (`ClassSignature`). Оформление (переносы,
   отступы, пустые строки) исправляется автоматически: `./gradlew detektMain detektTest --auto-correct`.

```kotlin
@JvmInline
value class ContractId(
    val value: String,
) {
    override fun toString(): String {
        return value
    }
}
```

## Модели DTO

Модели DTO — классы с `@Serializable`, которые разбираются из JSON или записываются в JSON: классы формата контрактов
(`format/`) и любые будущие.

1. Каждое поле класса и каждое значение `enum` помечено `@SerialName` с именем в JSON, даже если оно совпадает с именем
   в Kotlin (`SerialNameRequired`). Имя в JSON не зависит от переименования в коде.
2. Каждая аннотация — на своей строке, каждое поле — на своей строке. Аннотация и поле в одну строку не пишутся
   (`SerialAnnotationOnOwnLine`).
3. Значения `enum` разделяются пустой строкой (`SerialEnumBlankLine`; обычные `enum` вне DTO не затрагиваются).
4. Сначала обязательные поля, затем необязательные (`SerialRequiredFirst`). Необязательное поле — nullable со
   значением по умолчанию `null`.
5. Сериализатор не указывается у поля (`@Serializable(with = ...)` на поле запрещён, `NoFieldSerializer`).
   Нестандартный сериализатор регистрирует `Json` формата (`ContractJson`) в `serializersModule` через `contextual`, а
   файл модели с полем такого типа объявляет его в `@file:UseContextualSerialization(...)`. Пример —
   `VersionedStatus`: статус строкой или объектом.

```kotlin
@Serializable
data class VersionedStatus(
    @SerialName("status")
    val status: ReleaseStatus,
    @SerialName("version")
    val version: String? = null,
)

@Serializable
enum class ReleaseStatus {
    @SerialName("released")
    RELEASED,

    @SerialName("willNotBeReleased")
    WILL_NOT_BE_RELEASED,
}
```

Не так:

```kotlin
data class VersionedStatus(val version: String? = null, val status: ReleaseStatus)

enum class ReleaseStatus { @SerialName("released") RELEASED, @SerialName("willNotBeReleased") WILL_NOT_BE_RELEASED }
```

## Тесты

Форму теста выбирает стандарт KotestKit для проектов-потребителей (`CLAUDE.md` модуля); здесь — устройство спек.

1. Тесты пишутся только как спеки Kotest, только `BehaviorSpec` (`BehaviorSpecOnly`). Спека — с переносом:
   `class XSpec :` ⏎ `BehaviorSpec({`.
2. Спека соответствует файлу, где объявлен публичный вызов, пакет тот же: `ContractResolver.kt` →
   `ContractResolverSpec.kt`, `ContractFile.kt` → `ContractFileSpec.kt`. Спека заводится, когда на файл появляются
   первые тесты. Вызов вместе с его параметрами проверяется там же.
3. Файлы без вызовов для пользователя (модели и настройки, например `ContractModel.kt`) своих спек не получают: они
   проверяются через публичные вызовы.
4. Тестовые данные, только если нужны, лежат в `<Имя>SpecData.kt`. Именованных классов-спек там нет: такой класс Kotest
   запустит как настоящий тест (`SpecDataContent`).
5. Тестовые данные объявляются функциями с префиксом `make` и названием по контексту: `makeBadgeText()`,
   `makeObjectContractFile()`. Коротких безымянных помощников вроде `fun id()` нет (`SpecDataContent`). `make…`
   принимает данные параметрами; повторы выносятся в закрытые `make…` по контексту: `makeComponent(releaseVersions,
   properties)`, `makeEntityProperty(name, description, type)`. Длинный `make…` (больше 40 строк) делится так же.
6. `val` в тестовых данных — только константы, и они объявляются как `const val` (`SpecDataContent`).
7. В тестах и тестовых данных нет логики: ни `map`, `filter`, `single`, `let`, ни приведений и вычислений. Тест вызывает
   проверяемый метод и сравнивает результат с ожидаемым значением целиком; ожидаемое — литерал в `make…`.
8. Вход метода, который читает файлы, — образец в `src/test/resources`; код под тесты не меняется (зависимости ради
   моков не вводятся).
9. Тесты соответствия JSON и DTO (`shouldDecodeTest`, `shouldEncodeTest`) можно собрать в одну спеку на модуль.
   Разбор и запись разделяются: `shouldDecodeTest` — в одном блоке `withJson`, `shouldEncodeTest` — в другом.

```kotlin
// ContractResolverSpec.kt
shouldReturnTest(makeEnumModel()) { ContractResolver.load(makeContractsRoot(case = "enum")) }

// ContractResolverSpecData.kt
fun makeEnumModel(): ContractModel {
    return ContractModel(mapOf(ContractId(value = "components/text/TextAlignment") to makeTextAlignment()))
}
```

```kotlin
// ContractFileSpec.kt
withJson(ContractJson) {
    shouldDecodeTest(resource = "format/object.json", expected = makeObjectContractFile())
}
withJson(ContractJson) {
    shouldEncodeTest(value = makeObjectContractFile(), resource = "format/object.json")
}
```

Не так:

```kotlin
(model[ContractId(BADGE)] as ObjectContract).properties.single { it.name == name }.let { it.declaredIn to it.type }
```
