package com.rutar.ttool_xapoc;

import java.util.*;

// ............................................................................
/// Реалізація комбінованого числового ряду
/// @author Rutar_Andriy
/// 19.02.2026

public class Range {

private boolean includeAll = false;
private final ArrayList<SimpleRange> ranges = new ArrayList<>();

// ============================================================================
/// Конструктор за замовчуванням
/// Min - значення включається в регіон. Ставити на потрібний елемент
/// Max - значення виключається із регіону. Ставити на непотрібний елемент
/// @param allRanges текстове представлення рядів

public Range (String allRanges) {

if (allRanges.equals("-")) { includeAll = true;
                             return; }

for (String range : allRanges.split(",")) {
    
    if (range.contains(".."))
        { String[] split = range.split("\\.\\.");
          int min = Integer.parseInt(split[0]);
          int max = Integer.parseInt(split[1]);
          ranges.add(new SimpleRange(min, max)); }
    
    else
        { int value = Integer.parseInt(range);
          ranges.add(new SimpleRange(value, value)); }
    
}
}

// ============================================================================
/// Перевіряє, чи входить задане значення в діапазон
/// @param value значення для перевірки
/// @return якщо true - задане значення входить в діапазон

public boolean contains (int value) {
    
    if (includeAll) { return true; }
    
    for (SimpleRange range : ranges) {
        if (range.checkValue(value)) { return true; }
    }
    
    return false;
}

// ============================================================================
/// Повертає текстове представлення числового діапазону
/// @return текстове представлення числового діапазону

@Override
public String toString() {
    
    StringBuilder builder = new StringBuilder();
    for (SimpleRange range : ranges) {
        builder.append(range.toString());
        builder.append(",");
    }
    
    builder.deleteCharAt(builder.lastIndexOf(","));
    
    return builder.toString();
}

// ============================================================================
/// Реалізація найпростішого числового ряду
/// @author Rutar_Andriy
/// 19.02.2026

private class SimpleRange {

private final int min, max;

// ============================================================================
/// Конструктор за замовчуванням
/// @param min мінімальне значення
/// @param max максимальне значення

public SimpleRange (int min, int max) { this.min = min;
                                        this.max = max; }
 
// ============================================================================
/// Перевіряє, чи входить задане значення в діапазон
/// @param value значення для перевірки
/// @return якщо true - задане значення входить в діапазон

private boolean checkValue (int value) {
    return value >= min && value <= max;
}

// ============================================================================
/// Повертає текстове представлення числового діапазону
/// @return текстове представлення числового діапазону

@Override
public String toString() {

    if (min == max) { return String.valueOf(min); }
    else            { return "" + min + ".." + max; }
}

// Кінець класу SimpleRange ===================================================

};

// Кнець класу Range ==========================================================

}