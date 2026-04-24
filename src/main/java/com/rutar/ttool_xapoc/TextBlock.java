package com.rutar.ttool_xapoc;

// ............................................................................
/// Реалізація функціоналу текстового блоку
/// @author Rutar_Andriy
/// 19.02.2026

public class TextBlock {

private final int position;
private final byte[] rawData;

// ============================================================================
/// Конструктор за замовчуванням
/// @param position позиція даних
/// @param rawData масив "сирих" даних

public TextBlock (int position, byte[] rawData)
  { this.position = position;
    this.rawData  = rawData; }

// ============================================================================
/// Повертає позицію текстового блоку
/// @return позиція текстового блоку

public int getPosition() { return position; }

// ============================================================================
/// Повертає масив "сирих" даних
/// @return масив "сирих" даних

public byte[] getRawData() { return rawData; }

// Кнець класу TextBlock ======================================================

}