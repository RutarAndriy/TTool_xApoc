package com.rutar.ttool_xapoc;

import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.nio.file.*;
import javax.imageio.*;
import java.awt.image.*;

import static java.io.File.*;
import static javax.swing.JOptionPane.*;

// ............................................................................
/// Обробка ігрових шрифтів
/// @author Rutar_Andriy
/// 02.03.2026

public class FontProcessor {

private int w;                                             // ширина зображення
private int h;                                             // висота зображення
private int d;                                    // розмір зсуву в *.SPC файлі
private int color;                                 // колір конкретного пікселя
private File outputFile;                       // шлях до вихідного файлу/папки
private BufferedImage image;                           // зображення для запису

private byte[] datData;                                          // дані шрифта
private byte[] spcData;                                     // дані дескриптора

private final JFrame window;                          // головне вікно програми
private final File inputFile;                                   // вхідний файл

// ============================================================================
/// Конструктор за замовчуванням
/// @param window головне вікно програми
/// @param inputFile вхідний файл

public FontProcessor (JFrame window, File inputFile)
  { this.window = window;
    this.inputFile = inputFile; }

// ============================================================================
/// Декомпіляція шрифта

public void decompileFont() {

// *.DAT - графічні дані шрифта
// *.SPC - інформація про відступи
// Загальна кількість символів = (DAT.size / cW / cH) + 1 (пробіл)

// Ініціалізація параметрів шрифта
if (!initFontVariables()) { return; }

try {

// Зчитування файлів шрифта та дескиптора, якщо він існує 
datData = Files.readAllBytes(inputFile.toPath());
if (d != -1) { File desc = new File(inputFile.getAbsolutePath()
                                             .replace(".DAT", ".SPC"));
               spcData = Files.readAllBytes(desc.toPath()); }

// Створення папки для запису результатів розпакування
outputFile = new File(inputFile.getAbsolutePath().replace(".DAT", separator));
outputFile.mkdir();

// Ініціалізація допоміжної змінної та кількості символів у шрифті
String proc = d == -1 ? "" : "_%d";
int charsCount = datData.length / w / h;

// ............................................................................
// Обробка усіх символів у циклі

for (int z = 0; z < charsCount; z++) {
    
  int len = -1;
  // Зсув на 1 символ вправо, бо пробіл не має графічних даних
  if (d != -1) { len = spcData[d * (z+1) + d/2]; }
  // Ініціалізація нового зображення
  image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
    
  // Зчитування даних шрифта
  for (int r = 0; r < h; r++) {
  for (int c = 0; c < w; c++) {
    int index = (z * w * h) + r * w + c;
    color = datData[index] == 0 ? 0x0 : 0xFFFFFF;
    image.setRGB(c, r, color);
  }
  }
    
  // Запис результату в файл
  String imageName = String.format("%03d_%02X" + proc, z + 1, z + 33, len);
  File output = new File(outputFile.getAbsolutePath() + separator +
                                                       imageName + ".bmp");
  ImageIO.write(image, "bmp", output);
  
}

showMessageDialog(window, "Шрифт успішно розпаковано!");

}

// ............................................................................

catch (HeadlessException | IOException e)
  { IO.println(e.getCause());
    showMessageDialog(window, "При розпакуванні шрифта відбулася критична "
                            + "помилка", "Помилка", ERROR_MESSAGE); }
}

// ============================================================================

public void compileFont() {

// Ініціалізація параметрів шрифта
if (!initFontVariables()) { return; }

// Ініціалізація вихідного файлу
outputFile = new File(inputFile.getAbsolutePath() + ".DAT");

// Ініціалізація Файлу-дескриптора
File fontDesc = new File(outputFile.getAbsolutePath().replace(".DAT", ".SPC"));

// Якщо файл опису відсутній - відображення повідомлення про помилку
if (d != -1 && !fontDesc.exists())
  { showMessageDialog(window, "Не знайдено файл опису \n шрифта: " +
                               fontDesc.getName(), "Помилка", 0); return; }

// ............................................................................
// Збирання окремих символів у єдиний файл шрифту

try (FileOutputStream fos = new FileOutputStream(outputFile);
     BufferedOutputStream bos = new BufferedOutputStream(fos)) {

// Масив зображень окремих символів
File[] allFiles = inputFile.listFiles();

// Зчитування оригінальних описів шрифтів
if (d != -1) { spcData = Files.readAllBytes(fontDesc.toPath()); }

// Обробка символів у циклі
for (int z = 0; z < allFiles.length; z++) {

  // Отримання назви файлу для обробки
  String imageName = String.format("%03d_%02X", z + 1, z + 33);
  for (File f : allFiles)
    { if (f.getName().startsWith(imageName)) { imageName = f.getName();
                                               break; } }
    
  // Отримання ширини символу
  byte len = -1;
  if (d != -1) { int from = imageName.lastIndexOf("_") + 1;
                 int to = imageName.lastIndexOf(".");
                 len = Byte.parseByte(imageName.substring(from, to)); }
    
  // Зчитування даних зображення
  image = ImageIO.read(new File(inputFile.getAbsolutePath() + separator
                                                            + imageName));
    
  if (image.getType() != BufferedImage.TYPE_3BYTE_BGR)
    { String msg = "Файл %s має неправильний формат!%n"
                 + "Повинен бути 24-бітний BMP";
      showMessageDialog(window, msg.formatted(imageName), "Помилка", 0);
      return; }
    
  byte[] imageData = ((DataBufferByte)(image.getRaster().getDataBuffer()))
                                                        .getData();
  byte[] writable = new byte[imageData.length / 3];
    
  // Обробка та запис даних
  for (int pixel = 0; pixel < writable.length; pixel++)
    { writable[pixel] = (byte) (imageData[pixel * 3] == 0 ? 0x0 : 0x1); }
  bos.write(writable);
    
  // Обробка даних опису шрифта
  if (d != -1) { for (int q = 0; q < d/2; q++)
                   { spcData[d * (z+1) + d/2 + q] = len; } }
}

// Запис даних опису шрифта у файл
if (d != -1) { try (var fosDesc = new FileOutputStream(fontDesc))
                 { fosDesc.write(spcData); } }

showMessageDialog(window, "Шрифт успішно запаковано!");

}

// ............................................................................

catch (Exception e)
  { IO.println(e.getCause());
    showMessageDialog(window, "При пакуванні шрифта відбулася критична "
                            + "помилка", "Помилка", ERROR_MESSAGE); }
}

// ============================================================================
/// Ініціалізація параметрів шрифта, які залежать від назви файлу

private boolean initFontVariables()
  { switch (inputFile.getName().split("\\.")[0]) {
      case "BIGFONT"  -> { w = 14; h = 24; d = 24; return true; }
      case "SMALFONT" -> { w = 14; h = 15; d = 30; return true; }
      case "SMALLSET" -> { w = 8;  h = 12; d = -1; return true; }
      default -> { showMessageDialog(window, "Неможливо обробити даний "
                                           + "шрифт!"); return false; } } }

// Кінець класу FontProcessor =================================================

}