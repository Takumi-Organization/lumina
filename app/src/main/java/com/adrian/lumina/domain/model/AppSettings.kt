package com.adrian.lumina.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Color de acento (personalización). Los valores ARGB concretos viven en la capa de UI. */
enum class AccentChoice { CORAL, ORO, MENTA, ROSA, ZAFIRO }

/** Tamaño de las miniaturas de la cuadrícula: más calidad = más memoria y carga. */
enum class ThumbQuality(val px: Int) { LOW(200), NORMAL(320), HIGH(512) }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val defaultColumns: Int = 4,
    val thumbQuality: ThumbQuality = ThumbQuality.NORMAL,
    val autoplayVideos: Boolean = false,
    val animations: Boolean = true,
    val haptics: Boolean = true,
    val albumSort: AlbumSort = AlbumSort.MANUAL,
    /** Oculta la vista en «recientes» y bloquea capturas de pantalla (FLAG_SECURE). */
    val privacyScreen: Boolean = false,
    val accent: AccentChoice = AccentChoice.ORO,
) {
    /** Valores fuera de rango (p. ej. de una versión futura) se acotan al leer. */
    fun sanitized() = copy(defaultColumns = defaultColumns.coerceIn(2, 7))
}
