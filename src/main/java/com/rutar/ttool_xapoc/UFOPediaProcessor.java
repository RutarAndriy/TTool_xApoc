package com.rutar.ttool_xapoc;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.nio.file.*;

// ............................................................................
/// Обробка файлів НЛОпедії
/// @author Rutar_Andriy
/// 05.03.2026

public class UFOPediaProcessor {

private File descFile;                                       // файл-дескриптор
private ByteBuffer buffer;                                             // буфер

private final File inputFile;                                   // вхідний файл
private final ArrayList<UFOPediaBlock> ufopediaBlocks;        // блоки НЛОпедії
private final ArrayList<Integer> indexes = new ArrayList<>();        // індекси

// ============================================================================
/// Конструктор за замовчуванням
/// @param inputFile вхідний файл
/// @param ufopediaBlocks масив блоків НЛОпедії

public UFOPediaProcessor (File inputFile,
                          ArrayList<UFOPediaBlock> ufopediaBlocks)
    { this.inputFile = inputFile;
      this.ufopediaBlocks = ufopediaBlocks; }

// ============================================================================
/// Обробка файлів НЛОпедії (*.MT та *.MTI)
/// @throws IOException якщо відбулася помилка обробки файлів

public void process() throws IOException {

// Ініціалізація файлу-дескриптору
descFile = new File(inputFile.getAbsolutePath() + "I");

// Очищення попередніх даних
ufopediaBlocks.clear();
indexes.clear();

// Зчитування файлів НЛОпедії
byte[] mainBytes = Files.readAllBytes(inputFile.toPath());
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

}

// Кінець класу UFOPediaProcessor =============================================

}