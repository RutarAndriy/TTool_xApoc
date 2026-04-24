package com.rutar.ttool_xapoc;

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.nio.file.*;
import javax.swing.table.*;

import static com.rutar.ttool_xapoc.TToolxApoc.*;

// ............................................................................
/// Обробка "сирих" даних *.exe файлів
/// @author Rutar_Andriy
/// 28.02.2026

public class ExeProcessor {

private Range range;                            // допустимий ряд обробки даних
private int pos = -1;           // позиція читання даних, якщо (-1) - не задана
private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

private static byte[] allBytes;                            // усі зчитані байти

// ============================================================================
// Задання допустимих діапазонів обробки даних, 
// необхідні для фільтрування недопустимих текстових блоків

private final String ufo2p =
  "1346004..1353019,1353324..1360206,1360742..1368755," +
  "1368895..1376455,1376755..1392187,1392289..1396269";

private final String ufo2p4 =
  "1349588..1356603,1356908..1363790,1364326..1380039," +
  "1380339..1394373,1394509..1395771,1395873..1399853";

private final String tacp =
  "1254172..1254400,3005388..3010255," +
  "3010307..3015444,3015521..3021497,3023446..3086216";

private final String tacp4 =
  "1245468..1245696,2996684..3001551," +
  "3001603..3002764,3002912..3012793,3014742..3077512";

// ============================================================================
/// Читання "сирих" байт *.exe файлу
/// @param inputFile вхідний *.exe файл
/// @param filterRows ручне фільтрування рядків
/// @param strictRules строгі правила фільтрування
/// @param table головна таблиця із даними
/// @throws IOException якщо відбулася помилка обробки файлу

public void read (File inputFile,
                  boolean filterRows,
                  boolean strictRules,
                  JTable table) throws IOException {

// Доступ до моделі даних головної таблиці
DefaultTableModel tModel = (DefaultTableModel) table.getModel();

// Очищення попередніх даних
textBlocks.clear();
editedList.clear();

// Зчитування всіх байт
allBytes = Files.readAllBytes(inputFile.toPath());

// Ручне задавання діапазонів, у межах яких є дані для обробки
switch (inputFile.getName())
  { case "UFO2P.EXE"  -> { range = new Range(ufo2p);  }
    case "UFO2P4.EXE" -> { range = new Range(ufo2p4); }
    case "TACP.EXE"   -> { range = new Range(tacp);   }
    case "TACP4.EXE"  -> { range = new Range(tacp4);  }
    default           -> { throw new IOException(); } }

// Якщо true - використовувати діапазони для обробки
if (!filterRows) { range = new Range("-"); }

// ............................................................................
// Пошук послідовностей, які потенційно можуть бути текстом для перекладу

for (int z = 0; z < allBytes.length; z++) {

  // Пропуск даних поза робочим діапазоном
  if (!range.contains(z)) { resetExeData();
                            continue; }
  
  // Виявлено потенційний кінець текстового рядка
  if (allBytes[z] == 0) {
    // Отримання байтового масиву
    byte[] bytes = baos.toByteArray();
    // Перевірка коректності обробленого тексту
    if (bytes.length < 2 || (strictRules &&
       !isValidText(CodeTable.decodeText(bytes)))) { resetExeData();
                                                     continue; }
    // Додавання нового текстового блоку до загального масиву
    textBlocks.add(new TextBlock(pos, bytes));
    // Очищення даних
    resetExeData(); }
  
  // Виявлено допустимий символ
  else if (CodeTable.isValidByte(allBytes[z])) {
    // Якщо позиція обробки не задана - задаємо її
    if (pos == -1) { pos = z; }
    // Запис допустимого символу в буфер
    baos.write(allBytes[z]); }
  
  // Виявлено недопустимий символ
  else { resetExeData(); }

}

// ............................................................................
// Додавання всіх знайдених текстових блоків до таблиці

int id = 0;
String tmp;
ArrayList<String> row = new ArrayList<>();

for (TextBlock tBlock : textBlocks)
  { tmp = CodeTable.decodeText(tBlock.getRawData());
    row.clear();
    row.add(String.valueOf(++id));
    row.add(tmp.length() + "/" + tBlock.getRawData().length);
    row.add(tmp);
    tModel.addRow(row.toArray(String[]::new)); }

}

// ============================================================================
/// Запис "сирих" байт *.exe файлу
/// @param outputFile вихідний *.exe файл
/// @param table головна таблиця із даними
/// @throws IOException якщо відбулася помилка обробки файлу

public void write (File outputFile, JTable table) throws IOException {

String tmp; // допоміжна змінна

// Обробка всіх редагованих текстових блоків
for (Integer edited : editedList) {

  TextBlock block = textBlocks.get(edited);
  int blockSize = block.getRawData().length;
  tmp = (String) table.getValueAt(edited, 2);
  tmp = Utils.replaceUnusedChars(tmp);
  byte[] bytes = new byte[blockSize];
  byte[] encoded = CodeTable.encodeText(tmp);
    
  // Якщо довжини старого і нового текстів співпадають - все ок
  if (encoded.length == bytes.length) { bytes = encoded; }

  // Якщо довжини не співпадають - заповнюємо вільне місце пробілами
  else { for (int z = 0; z < bytes.length; z++)
           { bytes[z] = z < encoded.length ? encoded[z] : 0x20; }
         // Оновлення дних текстового блоку
         textBlocks.set(edited, new TextBlock(block.getPosition(), bytes)); }
    
  // Заміна оригінальних байт на оброблені
  System.arraycopy(bytes, 0, allBytes, block.getPosition(), bytes.length);
    
}

// Запис результату в файл
try (FileOutputStream fos = new FileOutputStream(outputFile))
  { fos.write(allBytes); }

}

// ============================================================================
/// Перевірка коректності тексту
/// @param text текст для перевірки
/// @return якщо true - текст коректний

private boolean isValidText (String text) {

// Отримання кількості кириличних символів у тексті
long cyrillic = text.codePoints().filter
               (cp -> Character.UnicodeBlock.of(cp)
                   == Character.UnicodeBlock.CYRILLIC).count();
// Перевірка мінімальної кількості кириличних символів
if (cyrillic < 4) { return false; }
// Перевірка співвідношення кирилиці до загальної довжини тексту
if (cyrillic / (double) text.length() < 0.7) { return false; }

// Отримання кількості голосних літер у тексті
long vowel = text.chars().filter(c -> isVowelLetter((char) c)).count();
// Перевірка мінімальної кількості голосних літер
if (vowel < 1) { return false; }
// Перевірка співвідношення голосних літер до загальної довжини тексту
double vowelPercentage = vowel / (double) text.length();
if (text.length() > 9 && // перевіряються лише довгі рядки
   (vowelPercentage < 0.3 || vowelPercentage > 0.5)) { return false; }

return true;

}

// ============================================================================
/// Перевірка, чи є символ голосною літерою

private boolean isVowelLetter (char c) {

    return "аеєиіїоуюяАЕЄИІЇОУЮЯ".indexOf(c) >= 0 ||         // українська мова
           "аеёиоуыэюяАЕЁИОУЫЭЮЯ".indexOf(c) >= 0 ||          // російська мова
           "gjy".indexOf(c) >= 0;                          // допоміжні символи
}

// ============================================================================
/// Скинання всіх лічильників

private void resetExeData()
  { // Очищення буферу
    baos.reset();
    // Скидання позиції обробки
    pos = -1; }

// Кінець класу ExeProcessor ==================================================

}