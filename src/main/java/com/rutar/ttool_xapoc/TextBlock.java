package com.rutar.ttool_xapoc;

import java.io.*;
import java.util.*;
import java.nio.charset.*;

// ............................................................................
/// Реалізація функціоналу текстового блоку
/// @author Rutar_Andriy
/// 19.02.2026

public class TextBlock {

private final int position;
private final byte[] rawData;

private static ArrayList<Code> codes;

// ============================================================================

static { initCodeTable(); }

// ============================================================================
/// Конструктор за замовчуванням
/// @param position позиція даних
/// @param rawData масив "сирих" даних

public TextBlock (int position, byte[] rawData) {
    
    this.position = position;
    this.rawData = rawData;

}

// ============================================================================
/// Повертає позицію текстового блоку
/// @return позиція текстового блоку

public int getPosition() { return position; }

// ============================================================================
/// Повертає масив "сирих" даних
/// @return масив "сирих" даних

public byte[] getRawData() { return rawData; }

// ============================================================================
/// Перетворення масиву байт у текстовий рядок
/// @param encodedText масив для перетворення
/// @return перетворений текст

public static String decodeText (byte[] encodedText) {

StringBuilder builder = new StringBuilder();

for (byte b : encodedText) { builder.append(decodeChar(b)); }

return builder.toString();

}

// ============================================================================
/// Перетворення типу byte у тип char

private static char decodeChar (byte key) {
    
    for (Code code : codes)
        { if (code.getKey() == key) { return code.getValue(); } }
    
    return '_';
    //throw new Error("Unknown key: " + String.format("%02X", key));

}

// ============================================================================
/// Перетворення текстового рядка у масив байт
/// @param decodedText текстовий рядок для перетворення
/// @return перетворений масив байт

public static byte[] encodeText (String decodedText) {

ByteArrayOutputStream baos = new ByteArrayOutputStream();

for (char c : decodedText.toCharArray()) { baos.write(encodeChar(c)); }

return baos.toByteArray();

}

// ============================================================================
/// Перетворення типу char у тип byte

private static byte encodeChar (char value) {

    for (Code code : codes)
        { if (code.getValue() == value) { return code.getKey(); } }

    return 0x3f; // ?
    
    //throw new Error("Unknown value: " + value);

}

// ============================================================================
/// Ініціалізація таблиці кодування символів

private static void initCodeTable() {

codes = new ArrayList<>();

try (InputStream is = TextBlock.class.getClassLoader()
                     .getResourceAsStream("com/rutar/ttool_xapoc/"
                                        + "others/charCodes.txt");
     InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
     BufferedReader br = new BufferedReader(isr)) {
    
    String line;
    while ((line = br.readLine()) != null) {
        String[] kayAndValue = line.split("-");
        String key = kayAndValue[0];
        String value = key.equals("2D") ? "-" : kayAndValue[1];
        
        byte bKey = (byte) Integer.parseInt(key, 16);
        char cValue = value.charAt(0);
        
        codes.add(new Code(bKey, cValue));
    }
}

catch (Exception e) { IO.println("Init code table error!"); }

}

// ============================================================================
/// Реалізація об'єкту, який представляє код ігрового символу
/// @author Rutar_Andriy
/// 19.02.2026

private static class Code {
 
private final byte key;
private final char value;

// ============================================================================
/// Конструктор за замовчуванням

public Code (byte key, char value) {
    this.key = key;
    this.value = value;
}

// ============================================================================
/// Повертає ключ об'єкту
/// @return ключ об'єкту

public byte getKey() { return key; }
    
// ============================================================================
/// Повертає значення об'єкту
/// @return значення об'єкту
    
public char getValue() { return value; }
    
// Кнець класу Code ===========================================================
    
}

// Кнець класу TextBlock ======================================================

}