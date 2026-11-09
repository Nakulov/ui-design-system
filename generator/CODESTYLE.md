# Код-стайл генератора

## Модели DTO

Модели DTO — классы с `@Serializable`, которые разбираются из JSON или записываются в JSON: классы формата контрактов
(`format/`) и любые будущие.

1. Каждое поле класса и каждое значение `enum` помечено `@SerialName` с именем в JSON, даже если оно совпадает с именем
   в Kotlin. Имя в JSON не зависит от переименования в коде.
2. Каждая аннотация — на своей строке, каждое поле — на своей строке. Аннотация и поле в одну строку не пишутся.
3. Значения `enum` разделяются пустой строкой.
4. Сначала обязательные поля, затем необязательные. Необязательное поле — nullable со значением по умолчанию `null`.

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

1. Тесты пишутся только как спеки Kotest, только `BehaviorSpec`.
2. Спека соответствует файлу, где объявлен публичный вызов, пакет тот же: `ContractResolver.kt` →
   `ContractResolverSpec.kt`, `ContractFile.kt` → `ContractFileSpec.kt`. Спека заводится, когда на файл появляются
   первые тесты. Вызов вместе с его параметрами проверяется там же.
3. Файлы без вызовов для пользователя (модели и настройки, например `ContractModel.kt`) своих спек не получают: они
   проверяются через публичные вызовы.
4. Тестовые данные, только если нужны, лежат в `<Имя>SpecData.kt`. Именованных классов-спек там нет: такой класс Kotest
   запустит как настоящий тест.
5. Тестовые данные объявляются функциями с префиксом `make` и названием по контексту: `makeBadgeText()`,
   `makeObjectContractFile()`. Коротких безымянных помощников вроде `fun id()` нет.
6. `val` в тестовых данных — только константы, и они объявляются как `const val`.
7. В тестах и тестовых данных нет логики: ни `map`, `filter`, `single`, `let`, ни приведений и вычислений. Тест вызывает
   проверяемый метод и сравнивает результат с ожидаемым значением целиком; ожидаемое — литерал в `make…`.
8. Вход метода, который читает файлы, — образец в `src/test/resources`; код под тесты не меняется (зависимости ради
   моков не вводятся).

```kotlin
// ContractResolverSpec.kt
shouldReturnTest(makeEnumModel()) { ContractResolver.load(makeContractsRoot("enum")) }

// ContractResolverSpecData.kt
fun makeEnumModel(): ContractModel = ContractModel(mapOf(ContractId("components/text/TextAlignment") to EnumContract(...)))
```

Не так:

```kotlin
(model[ContractId(BADGE)] as ObjectContract).properties.single { it.name == name }.let { it.declaredIn to it.type }
```
