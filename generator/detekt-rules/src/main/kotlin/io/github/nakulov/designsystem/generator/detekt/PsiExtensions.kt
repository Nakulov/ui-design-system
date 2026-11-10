package io.github.nakulov.designsystem.generator.detekt

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtParameter

/** Аннотации элемента с коротким именем [name]. */
fun KtAnnotated.annotations(name: String): List<KtAnnotationEntry> {
    return annotationEntries.filter { entry -> entry.shortName?.asString() == name }
}

/** Класс помечен `@Serializable`: модель DTO. */
fun KtClass.isSerializable(): Boolean {
    return annotations(name = "Serializable").isNotEmpty()
}

/** Поля модели DTO: параметры главного конструктора `@Serializable`-класса, кроме `enum`. */
fun KtClass.serialFields(): List<KtParameter> {
    if (!isSerializable() || isEnum()) {
        return emptyList()
    }
    return primaryConstructorParameters
}

/** Значения `enum` среди объявлений класса. */
fun List<KtDeclaration>.enumEntries(): List<KtEnumEntry> {
    return filterIsInstance<KtEnumEntry>()
}

/** После элемента — перенос строки: пробелы до следующего элемента содержат `\n`. */
fun PsiElement.isFollowedByLineBreak(): Boolean {
    val next = PsiTreeUtil.nextLeaf(this)
    return next is PsiWhiteSpace && '\n' in next.text
}

/** Короткое имя супертипа-спеки Kotest (оканчивается на `Spec`), если он есть. */
fun KtClass.specSuperType(): String? {
    val names = superTypeListEntries.map { entry -> entry.typeReference?.text.orEmpty() }
    val simpleNames = names.map { name -> name.substringBefore(delimiter = '<').substringAfterLast(delimiter = '.') }
    return simpleNames.firstOrNull { name -> name.endsWith(suffix = "Spec") }
}
