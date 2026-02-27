package com.rutar.ttool_xapoc;

import java.io.*;
import java.util.*;

// ............................................................................
/// Реалізація функціоналу блоку нлопедії
/// @author Rutar_Andriy
/// 19.02.2026

public class UFOPediaBlock {
    
private byte[] starter;
private byte[] title;
private byte[] separator;
private byte[] description;
private byte[] ender;

private final byte[] endLine = new byte[] { 0x0};
private final byte[] endSep  = new byte[] { 0x1, 0x1, 0x0, 0x0 };

// ============================================================================
/// Конструктор за замовчуванням
/// @param rawData масив "сирих" даних

public UFOPediaBlock (byte[] rawData) { processBlock(rawData); }

// ============================================================================
/// Обробка "сирих" даних

private void processBlock (byte[] rawData) {

    int tmp, position = 0;
    
    // Знаходження "префікса" рядка
    starter = Arrays.copyOfRange(rawData, position, position + 12);
    position += starter.length;
    
    // Знаходження заголовку рядка
    tmp = findPosition(position, endLine, rawData);
    tmp -= 1; // Додаємо 0-байт рядка до "розділювача"
    title = Arrays.copyOfRange(rawData, position, tmp);
    position += title.length;
    
    // Знаходження "розділювача" заголовку та опису
    tmp = findPosition(position, endSep, rawData);
    
    if (tmp == -1) // Оброблення рядка з помилкою!
        { separator = new byte[0];
          description = new byte[0];
          ender = Arrays.copyOfRange(rawData, position, rawData.length);
          return; }
    
    separator = Arrays.copyOfRange(rawData, position, tmp);
    position += separator.length;
    
    // Знаходження опису рядка
    tmp = findPosition(position, endLine, rawData);    
    tmp -= 1; // Додаємо 0-байт рядка до "суфікса"
    description = Arrays.copyOfRange(rawData, position, tmp);
    position += description.length;
    
    // Знаходження "суфікса" рядка
    ender = Arrays.copyOfRange(rawData, position, rawData.length);
    
}

// ============================================================================
/// Отримання заголовку блоку нлопедії
/// @return заголовок блоку нлопедії

public String getTitle() { return CodeTable.decodeText(title); }

// ============================================================================
/// Задання нового заголовку блоку нлопедії
/// @param title новий заголовок блоку нлопедії

public void setTitle (String title)
    { this.title = CodeTable.encodeText(title); }

// ============================================================================
/// Отримання опису блоку нлопедії
/// @return опис блоку нлопедії

public String getDescription() { return CodeTable.decodeText(description); }

// ============================================================================
/// Задання новиго опису блоку нлопедії
/// @param description новий опис блоку нлопедії

public void setDescription (String description)
    { this.description = CodeTable.encodeText(description); }

// ============================================================================
/// Отримання мисиву "сирих" даних
/// @return мисив "сирих" даних

public byte[] getRawData() {
    
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
        { baos.write(starter);
          baos.write(title);
          baos.write(separator);
          baos.write(description);
          baos.write(ender);
          return baos.toByteArray(); }
    
    catch (Exception e) { return null; }

}

// ============================================================================
/// Отримання мисиву "сирих" даних

private int findPosition (int start, byte[] bytesToFind, byte[] rawBytes) {
    
    boolean find;
    
    for (int z = start; z < rawBytes.length - bytesToFind.length; z++)
        { find = true;
          for (int q = 0; q < bytesToFind.length; q++)
              { if (rawBytes[z + q] != bytesToFind[q]) { find = false; } }
          if (find) { return z + bytesToFind.length; } }
    
    return -1;

}

// Кнець класу UFOPediaBlock ==================================================

}