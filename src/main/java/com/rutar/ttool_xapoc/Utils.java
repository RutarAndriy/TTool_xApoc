package com.rutar.ttool_xapoc;

import java.io.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.*;

// ............................................................................
/// Корисні допоміжні методи
/// @author Rutar_Andriy
/// 19.02.2026

public class Utils {

// Домашня директорія користувача
public static final File HOME_DIR = FileSystemView.getFileSystemView()
                                                  .getHomeDirectory();

// ============================================================================
/// Виділення клітинок у таблиці
/// @param table таблиця, клітинки якої потрібно виділяти
/// @param col номер стовбця клітинки, яку потрібно виділити
/// @param row номер рядка клітинки, яку потрібно виділити

public static void selectCell (JTable table, int col, int row) {

    table.setRowSelectionInterval   (row, row);
    table.setColumnSelectionInterval(col, col);

    Rectangle rect = table.getCellRect(row, col, true);
    table.scrollRectToVisible(rect);

}

// ============================================================================
/// Отримання налаштованого JFileChooser'а
/// @param selectionMode тип виділення (папки, файли, папки+файли)
/// @param ext розширення файлів
/// @param desc опис розширення файлів
/// @return налаштований екземпляр JFileChooser'а

public static JFileChooser getFileChooser (int selectionMode,
                                           String ext, String desc)
    { return getFileChooser(selectionMode, Map.of(ext, desc)); }

// ============================================================================
/// Отримання налаштованого JFileChooser'а
/// @param selectionMode тип виділення (папки, файли, папки+файли)
/// @param filters масив розширень та описів файлів
/// @return налаштований екземпляр JFileChooser'а

public static JFileChooser getFileChooser (int selectionMode,
                                           Map<String, String> filters) {
    
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(selectionMode);
    chooser.removeChoosableFileFilter(chooser
           .getChoosableFileFilters()[0]);
    chooser.setCurrentDirectory(HOME_DIR);
    
    filters.forEach((ext, desc) ->
        { FileNameExtensionFilter f = new FileNameExtensionFilter(desc, ext);
          chooser.addChoosableFileFilter(f); });
    
    return chooser;

}

// ============================================================================
/// Отримання папки, у якій міститься останній виділений файл/папка
/// @param chooser jFileChooser, який використовувався для вибору файлу
/// @return папка, у якій міститься останній виділений файл/папка

public static File getLastDir (JFileChooser chooser) {
    
    File file = chooser.getSelectedFile();
    
    // Якщо останього файлу немає - повертаємо null
    if (file == null)
        { return null; }
    // Якщо останній файл є папкою - повертаємо батьківську папку
    else if (file.isDirectory())
        { return new File(file.getParent()); }
    // Якщо останній файл є файлом - повертаємо шлях до його папки
    else
        { return new File(file.getPath().replace(file.getName(), "")); }

}

// ============================================================================
/// Заміна невикористовуваних символів у тексті
/// @param value текст із невикористовуваними символами
/// @return текст із заміненими символами

public static String replaceUnusedChars (String value)
    { return value.replace('’', '\''); }

// ============================================================================
/// Виведення байтового масиву в консоль у вигляді hex-значень
/// @param array байтовий масив для виведення в консоль

public static void printAsHex (byte[] array) {

for (int q = 0; q < array.length; q++)
    { IO.print(" " + String.format("%02X", array[q]));
      if ((q+1) % 8  == 0) { IO.print(" ");  }
      if ((q+1) % 16 == 0) { IO.println(""); } } IO.println(); }

// Кінець класу Utils =========================================================

}
