package com.rutar.ttool_xapoc;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

import static com.rutar.ttool_xapoc.TToolxApoc.*;

// ............................................................................
/// Реалізація зміненої промальовки клітинок таблиці
/// @author Rutar_Andriy
/// 19.02.2026

public class CellRender extends DefaultTableCellRenderer {

private Color searchColor;
private Color defaultColor;

private final Color okColor;
private final Color infoColor;
private final Color errorColor;
private final Color warningColor;

private static int searchedCol = -1;
private static int searchedRow = -1;

// ============================================================================

public CellRender() {

    okColor      = new Color(77,  255, 77 );
    infoColor    = new Color(150, 150, 255);
    errorColor   = new Color(255, 120, 120);
    warningColor = new Color(255, 255, 120);

}

// ============================================================================

@Override
public Component getTableCellRendererComponent(JTable table,
                                               Object value,
                                               boolean isSelected,
                                               boolean hasFocus,
                                               int row, int col) {

Component component = super.getTableCellRendererComponent(table, value,
                                                          isSelected,
                                                          hasFocus, row, col);

if (defaultColor == null)
    { defaultColor = component.getForeground();
      searchColor = UIManager.getColor("Component.accentColor"); }

// ............................................................................

if (searchedCol != -1 && searchedCol == col &&
    searchedRow != -1 && searchedRow == row)
     { component.setForeground(searchColor); }
else { setNormalColor(table, component, col, row); }

return component;

}

// ============================================================================

private void setNormalColor (JTable table, Component component,
                             int col, int row) {

// Подання даних при відкриванні *.exe файлу
if (fileExt.toLowerCase().equals("exe")) {

switch (col) {

    case 0 -> // колір тексту першого стовбця
        { component.setForeground(editedList.contains(row) ? infoColor :
                                                             Color.GRAY); }
        
    case 1 -> // колір тексту другого стовбця
        { if (!editedList.contains(row))
              { component.setForeground(defaultColor); }
          else
              { String newValue = (String) table.getValueAt(row, 2);
                int oldValue = textBlocks.get(row).getRawData().length;

                int delta = newValue.length() - oldValue;

                if (delta == 0)     
                    { component.setForeground(okColor); }
                else if (delta > 0)
                    { component.setForeground(errorColor);}
                else if (delta < 0)
                    { component.setForeground(warningColor); } } }

    default -> { component.setForeground(defaultColor); } } }

// Подання даних при відкриванні *.mt файлу
else {
    
    if (col < 1)
         { component.setForeground(Color.GRAY);   }
    else { component.setForeground(defaultColor); }
    
}
}

// ============================================================================

public static void setSearchedCell (int searchedCol, int searchedRow)
    { CellRender.searchedCol = searchedCol;
      CellRender.searchedRow = searchedRow; }

// Кінець класу CellRender ====================================================

}
