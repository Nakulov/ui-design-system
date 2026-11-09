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
