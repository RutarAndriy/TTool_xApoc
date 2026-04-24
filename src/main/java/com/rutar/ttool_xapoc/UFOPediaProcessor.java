package com.rutar.ttool_xapoc;

import java.io.*;
import java.nio.*;
import java.util.*;
import javax.swing.*;
import java.nio.file.*;
import javax.swing.table.*;

import static com.rutar.ttool_xapoc.TToolxApoc.*;

// ............................................................................
/// Обробка файлів НЛОпедії
/// @author Rutar_Andriy
/// 05.03.2026

public class UFOPediaProcessor {

private File mainFile;                                          // вхідний файл
private File descFile;                                       // файл-дескриптор
private ByteBuffer buffer;                                             // буфер

private final ArrayList<Integer> indexes = new ArrayList<>();        // індекси

// ============================================================================
/// Читання файлів НЛОпедії (*.MT та *.MTI)
/// @param inputFile вхідний файл
/// @param table головна таблиця із даними
/// @throws IOException якщо відбулася помилка читання файлів

public void read (File inputFile, JTable table) throws IOException {

// Ініціалізація вхідних файлів
mainFile = inputFile;
descFile = new File(mainFile.getAbsolutePath() + "I");

// Доступ до моделі даних головної таблиці
DefaultTableModel tModel = (DefaultTableModel) table.getModel();

// Очищення попередніх даних
ufopediaBlocks.clear();
indexes.clear();

// Зчитування файлів НЛОпедії
byte[] mainBytes = Files.readAllBytes(mainFile.toPath());
byte[] descBytes = Files.readAllBytes(descFile.toPath());

buffer = ByteBuffer.wrap(descBytes);
buffer.order(ByteOrder.LITTLE_ENDIAN);

// Отримання індексів значущих блоків
while (buffer.remaining() >= 4) { indexes.add(buffer.getInt()); }

// Обробка блоків НЛОпедії в циклі
for (int z = 0; z < indexes.size()-1; z++)
  { int from = indexes.get(z);
    int to   = indexes.get(z + 1);
    var block = new UFOPediaBlock(Arrays.copyOfRange(mainBytes, from, to));
    ufopediaBlocks.add(block); }

ArrayList<String> row = new ArrayList<>();

// Обробка оброблених блоків НЛОпедії в циклі
for (int z = 0; z < ufopediaBlocks.size(); z++)
  { // Отримання блоку даних
    var block = ufopediaBlocks.get(z);
    // Парсинг блоку даних
    row.clear();
    row.add(String.valueOf(z + 1));
    row.add(block.getTitle());
    row.add(block.getDescription());
    // Додавання даних у таблицю
    tModel.addRow(row.toArray(String[]::new)); }

}

// ============================================================================
/// Запис файлів НЛОпедії (*.MT та *.MTI)
/// @param outputFile вихідний файл
/// @param table головна таблиця із даними
/// @throws IOException якщо відбулася помилка запису файлів

public void write (File outputFile, JTable table) throws IOException {

// Ініціалізація вихідних файлів
mainFile = outputFile;
descFile = new File(mainFile.getAbsolutePath() + "I");

// Ініціалізація буферу для запису даних
buffer = ByteBuffer.allocate((ufopediaBlocks.size() + 1) * 4);
buffer.order(ByteOrder.LITTLE_ENDIAN);
buffer.putInt(0); // запис нульової позиції

// Запис НЛОпедії та файлу-дескриптора
try (FileOutputStream pediaF = new FileOutputStream(mainFile, false);
     FileOutputStream pediaD = new FileOutputStream(descFile, false)) {

String tmp;            // допоміжна змінна
byte[] data;           // масив даних
int pos = 0;           // позиція обробки тексту
UFOPediaBlock block;   // блок НЛОпедії

// Обробка текстових блоків у циклі
for (int z = 0; z < ufopediaBlocks.size(); z++) {
    
  block = ufopediaBlocks.get(z);
  tmp = (String) table.getValueAt(z, 1);
  tmp = Utils.replaceUnusedChars(tmp);
  block.setTitle(tmp);
  tmp = (String) table.getValueAt(z, 2);
  tmp = Utils.replaceUnusedChars(tmp);
  block.setDescription(tmp);
    
  data = block.getRawData();
  pos += data.length;

  pediaF.write(data);
  buffer.putInt(pos);
    
}

// Запис даних у файл-дескриптор
pediaD.write(buffer.array());

}
}

// Кінець класу UFOPediaProcessor =============================================

}