package com.rutar.ttool_xapoc;

import java.io.*;
import java.util.*;
import java.nio.charset.*;

// ............................................................................
/// Таблиця кодувань ігрових символів
/// @author Rutar_Andriy
/// 27.02.2026

public class CodeTable {

// Таблиця кодування - ключі та значення
private static final Map<Byte, Character> codes = new HashMap<>();

// ============================================================================
// Статична ініціалізація

static { initCodeTable(); }

// ============================================================================
/// Перетворення масиву байт у текстовий рядок
/// @param encodedText масив для перетворення
/// @return перетворений текст

public static String decodeText (byte[] encodedText)
  { StringBuilder builder = new StringBuilder();
    for (byte b : encodedText) { builder.append(decodeChar(b)); }
    return builder.toString(); }

// ============================================================================
/// Перетворення типу byte у тип char
/// @param key байт для перетворення
/// @return результат перетворення

public static char decodeChar (byte key)
  { Optional<Character> value = Optional.of(codes.get(key));
    return value.orElseThrow(); }

// ============================================================================
/// Перетворення текстового рядка у масив байт
/// @param decodedText текстовий рядок для перетворення
/// @return перетворений масив байт

public static byte[] encodeText (String decodedText)
  { ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (char c : decodedText.toCharArray()) { baos.write(encodeChar(c)); }
    return baos.toByteArray(); }

// ============================================================================
/// Перетворення типу char у тип byte
/// @param value символ для перетворення
/// @return результат перетворення

public static byte encodeChar (char value)
  { for (var entry : codes.entrySet())
      { if (entry.getValue().equals(value)) { return entry.getKey(); } }
    // Якщо ключа не існує - повертаємо код символу ?
    return 0x3f; }

// ============================================================================
/// Перевірка, чи заданий байт є допустимим
/// @param key байт для перевірки
/// @return якщо true - байт є допустимим

public static boolean isValidByte (byte key)
  { return codes.containsKey(key); }

// ============================================================================
/// Ініціалізація таблиці кодування символів

private static void initCodeTable() {

try (var is  = CodeTable.class.getResourceAsStream("others/charCodes.txt");
     var isr = new InputStreamReader(is, StandardCharsets.UTF_8);
     var br  = new BufferedReader(isr)) {

String line;
while ((line = br.readLine()) != null)
  { // Зчитування наступного рядка
    String[] keyAndValue = line.split("-");
    String key = keyAndValue[0];
    String value = key.equals("2D") ? "-" : keyAndValue[1];
    // Перетворення даних
    byte bKey = (byte) Integer.parseInt(key, 16);
    char cValue = value.charAt(0);
    // Додавання даних у таблицю
    codes.put(bKey, cValue); } }

catch (Exception e) { IO.println("Init code table error!"); }

}

// Кінець класу CodeTable =====================================================

}