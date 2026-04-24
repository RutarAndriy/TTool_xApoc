package com.rutar.ttool_xapoc;

import java.io.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.font.*;
import javax.imageio.*;
import java.util.jar.*;
import java.awt.event.*;
import java.awt.image.*;
import java.nio.charset.*;
import javax.swing.event.*;
import javax.swing.table.*;
import com.formdev.flatlaf.*;
import com.rutar.ua_translator.*;
import com.formdev.flatlaf.themes.*;

import static javax.swing.JOptionPane.*;
import static javax.swing.JFileChooser.*;

// ............................................................................
/// Головний клас програми
/// @author Rutar_Andriy
/// 19.02.2026

public class TToolxApoc extends JFrame {

private File inputFile;                                         // вхідний файл
private File outputFile;                                       // вихідний файл

private final JFileChooser fileOpen;           // відкривання/збереження файлів
private final JFileChooser fntCompile;                  // компілювання шрифтів
private final JFileChooser fntDecompile;              // декомпілювання шрифтів

private File tmpFile;                                       // допоміжна змінна
private String appDescription;                                 // опис програми
private DefaultTableModel tableModel;              // стандартна модель таблиці

private boolean dataWasChanged = false;        // якщо true - дані були змінені
private boolean reactOnChange = true;                       // допоміжна змінна

private final Font strikeFont;                             // закреслений шрифт

// ............................................................................

public static final ArrayList<Integer> editedList
              = new ArrayList<>();            // масив індексів змінених рядків
public static final ArrayList<TextBlock> textBlocks
              = new ArrayList<>();                    // масив текстових блоків
public static final ArrayList<UFOPediaBlock> ufopediaBlocks
              = new ArrayList<>();                     // масив блоків НЛОпедії

public static String fileExt;                    // розширення відкритого файлу
public static boolean debug = false; // якщо true - увімк. режим налагоджування

// ============================================================================
/// Конструктор за замовчуванням

public TToolxApoc() {

initComponents();
initAppIcons();

fileOpen     = Utils.getFileChooser(FILES_ONLY, Map.of
                                   ("mt",  "xApoc НЛОпедія",
                                    "exe", "xApoc виконувані файли"));
fntCompile   = Utils.getFileChooser(DIRECTORIES_ONLY,
                                    "dat", "xApoc файли шрифтів");
fntDecompile = Utils.getFileChooser(FILES_ONLY,
                                    "dat", "xApoc файли шрифтів");

// Ініціалізація закресленого шрифта
Map<TextAttribute, Object> attr = new HashMap<>(mni_about.getFont()
                                                         .getAttributes());
attr.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
strikeFont = mni_about.getFont().deriveFont(attr);

}

// ============================================================================
/// Головний метод програми
/// @param args масив переданих параметрів

public static void main (String args[]) {
    
    if (args.length > 0 &&
        args[0].equals("--debug")) { debug = true; }
    
    // ........................................................................
    
    UATranslator.init();
    UIManager.put("FileChooser.readOnly", true);

    JFrame .setDefaultLookAndFeelDecorated(true);
    JDialog.setDefaultLookAndFeelDecorated(true);
    
    FlatLaf.registerCustomDefaultsSource("com.rutar.ttool_xapoc.themes");

    try { FlatMacDarkLaf.setup(); }
    catch (Exception e) {}
    
    // ........................................................................
    
    SwingUtilities.invokeLater(() ->
      { var window = new TToolxApoc();
        window.setVisible(true);
        SwingUtilities.invokeLater(() ->
          { window.setMinimumSize(window.getSize()); }); });
}

// ============================================================================
/// Відкривання файлів

private void showOpenDialog() {

// Дані змінилися - запитуємо чи відкривати новий файл
if (dataWasChanged) { 

String saveDataQuestion = """
  У відкритому файлі присутні зміни. При відкриванні
  нового файлу вони будуть втрачені. Бажаєте продовжити?
  """;

int answer = showConfirmDialog(this, saveDataQuestion,
                              "Повідомлення", YES_NO_OPTION);

if (answer != YES_OPTION) { return; }

}

// ............................................................................

int result = fileOpen.showOpenDialog(this);
if (result != JFileChooser.APPROVE_OPTION) { return; }

String[] split = fileOpen.getSelectedFile().getName().split("\\.");
fileExt = split[split.length - 1];

switch (fileExt.toLowerCase()) { case "mt"  -> openMtFile();
                                 case "exe" -> openExeFile(); }

updateAppTitle();

}

// ============================================================================
/// Відкривання *.mt файлів

private void openMtFile() {

prepareNewTable(true);
dataWasChanged = false;

inputFile = fileOpen.getSelectedFile();

// Читання файлів НЛОпедії
try { new UFOPediaProcessor().read(inputFile, tbl_main);
      finalizeNewTable(true); }

// ............................................................................

catch (IOException e)
  { showMessageDialog(this, "При обробленні файлу відбулася критична " +
                            "помилка", "Помилка", ERROR_MESSAGE); }

}

// ============================================================================
/// Відкривання *.exe файлів

private void openExeFile() {

prepareNewTable(false);
dataWasChanged = false;

inputFile = fileOpen.getSelectedFile();

// Вимкнення фільтрування рядків
boolean filterRows  = !mni_filterRows .getFont().equals(strikeFont);
boolean strictRules = !mni_strictRules.getFont().equals(strikeFont);

// Читання "сирих" байт *.exe файлу
try { new ExeProcessor().read(inputFile, filterRows, strictRules, tbl_main);
      finalizeNewTable(false); }

// ............................................................................

catch (IOException _)
  { showMessageDialog(this, "При відкриванні файлу відбулася критична "
                          + "помилка", "Помилка", ERROR_MESSAGE); }

}

// ============================================================================
/// Збереження файлів

private void showSaveDialog() {

// Перевірка, чи всі введені строки є допустимими (лише для *.exe файлів)
if (fileExt.toLowerCase().equals("exe")) {

  for (int z = 0; z < editedList.size(); z++) {

    int id = editedList.get(z);

    String editedText = (String) tbl_main.getValueAt(id, 2);

    int oldLength = textBlocks.get(id).getRawData().length;
    int newLength = CodeTable.encodeText(editedText).length;

    if (newLength > oldLength) {
      Utils.selectCell(tbl_main, 2, id);
      int delta = newLength - oldLength;
      String msg = "Довжина рядка №%d перевищує довжину%n"
                 + "оригінального рядка на %d символ/ів";
      showMessageDialog(this, msg.formatted(id+1, delta), "Помилка", 0);
      return;
    } } }

// ............................................................................

fileOpen.setSelectedFile(inputFile);
int result = fileOpen.showSaveDialog(this);
if (result != JFileChooser.APPROVE_OPTION) { return; }

switch (fileExt.toLowerCase()) { case "mt"  -> saveMtFile();
                                 case "exe" -> saveExeFile(); }

updateAppTitle();

}

// ============================================================================
/// Збереження *.mt файлів

private void saveMtFile() {

outputFile = fileOpen.getSelectedFile();

try {

new UFOPediaProcessor().write(outputFile, tbl_main);
dataWasChanged = false;
updateAppTitle();

showMessageDialog(this, "Файл " + outputFile.getName() + " успішно збережено",
                        "Повідомлення", INFORMATION_MESSAGE); }

// ............................................................................

catch (HeadlessException | IOException _)
  { showMessageDialog(this, "При збереженні файлу відбулася критична "
                          + "помилка", "Помилка", ERROR_MESSAGE); }

}

// ============================================================================
/// Збереження *.exe файлів

private void saveExeFile() {

outputFile = fileOpen.getSelectedFile();

try {

new ExeProcessor().write(outputFile, tbl_main);
dataWasChanged = false;
updateAppTitle();

showMessageDialog(this, "Файл " + outputFile.getName() + " успішно збережено",
                        "Повідомлення", INFORMATION_MESSAGE); }

// ............................................................................

catch (HeadlessException | IOException _)
  { showMessageDialog(this, "При збереженні файлу відбулася критична "
                          + "помилка", "Помилка", ERROR_MESSAGE); }

}

// ============================================================================
/// Відображення інформації про програму

private void showInfoDialog() {

// Отримуємо текст опису програми
if (appDescription == null) {

URL descriptionUrl = getClass().getResource("others/appDescription.txt");
URL channelUrl     = getClass().getResource("others/channelURL.txt");
URL manifestUrl    = getClass().getClassLoader()
                    .getResource("META-INF/MANIFEST.MF");

try (InputStream desc = descriptionUrl.openStream();
     InputStream link = channelUrl    .openStream();
     InputStream data = manifestUrl   .openStream()) {

Attributes attributes = new Manifest(data).getMainAttributes();
    
String channelURL = new String(link.readAllBytes(), StandardCharsets.UTF_8);
String appVersion = attributes.getValue("Version");
String buildDate  = attributes.getValue("Build-Date");

appVersion = (appVersion == null) ? "0.0.1" : appVersion;
buildDate  = (buildDate  == null) ? "25.04.1995" : buildDate.split(" ")[0];

appDescription = new String(desc.readAllBytes(), StandardCharsets.UTF_8)
                    .formatted(channelURL, appVersion, buildDate); }

catch (IOException _) {} }

// ............................................................................

JEditorPane pane = new JEditorPane("text/html", appDescription);
pane.setEditable(false);
pane.setFocusable(false);

pane.addHyperlinkListener((HyperlinkEvent e) -> {
  if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
    try { Desktop.getDesktop().browse(e.getURL().toURI()); }
    catch (IOException | URISyntaxException _) { }
  }
});

showMessageDialog(this, pane, "Про програму", INFORMATION_MESSAGE);

}

// ============================================================================
/// Відображення вікна пошуку інформації

private void showSearchDialog()
  { new SearchDialog(this).setVisible(true); }

// ============================================================================
/// Відображення вікна підтвердження виходу

private void showExitDialog() {

// Якщо дані не змінювалися - просто виходимо
if (!dataWasChanged) { System.exit(0); }

String saveDataQuestion = """
  Ви бажаєте вийти з програми?
  Усі незбережені дані буде втрачено
  """;

int answer = showConfirmDialog(this, saveDataQuestion,
                              "Підтвердження виходу", YES_NO_OPTION);

if (answer == YES_OPTION) { System.exit(0); }

}

// ============================================================================
/// Вибір шрифту для розпакування

private void showDecompileFontDialog() {

    int result = fntDecompile.showOpenDialog(this);
    if (result != JFileChooser.APPROVE_OPTION) { return; }

    inputFile = fntDecompile.getSelectedFile();
    new FontProcessor(this, inputFile).decompileFont();
}

// ============================================================================
/// Вибір розпакованого шрифту для пакування

private void showCompileFontDialog() {

    tmpFile = Utils.getLastDir(fntDecompile);
    if (tmpFile != null) { fntCompile.setCurrentDirectory(tmpFile); }

    int result = fntCompile.showOpenDialog(this);
    if (result != JFileChooser.APPROVE_OPTION) { return; }

    inputFile = fntCompile.getSelectedFile();
    new FontProcessor(this, inputFile).compileFont();
}

// ============================================================================
/// Попередня ініціалізація нової таблиці

private void prepareNewTable (boolean isUFOPedia) {

dataWasChanged = false;
inputFile = fileOpen.getSelectedFile();
sp_table.getVerticalScrollBar().setValue(0);

tableModel = new DefaultTableModel() {
    @Override
    public boolean isCellEditable (int row, int column)
      { if (fileExt.toLowerCase().equals("exe")) { return column >= 2; }
        else                                     { return column >= 1; } }
};

tbl_main.setModel(tableModel);

if (isUFOPedia)
  { tableModel.addColumn("№");
    tableModel.addColumn("Заголовок");
    tableModel.addColumn("Опис"); }
else
  { tableModel.addColumn("№");
    tableModel.addColumn("Розмір");
    tableModel.addColumn("Текст для перекладу"); }

}

// ============================================================================
/// Завершальна ініціалізація нової таблиці

private void finalizeNewTable (boolean isUFOPedia) {

CellRender centerRender = new CellRender();
centerRender.setHorizontalAlignment(SwingConstants.CENTER);

if (isUFOPedia) { setColumnParams(centerRender, 45, 175, 175); }
else            { setColumnParams(centerRender, 45, 55,  175); }

updateTableInfo();

// ............................................................................

mni_find.setEnabled(true);
tableModel.addTableModelListener((TableModelEvent evt) ->
  { updateTableData(evt);
    updateAppTitle(); });

}

// ============================================================================

private void setColumnParams (CellRender render, int ... columnSizes) {

TableColumn tColumn;
boolean newRender, isResizable;
boolean isExe = fileExt.toLowerCase().equals("exe");

for (int z = 0; z < columnSizes.length; z++) {

  newRender    = isExe ? z > 1 : z > 0;
  isResizable  = isExe ? z > 1 : z > 0;

  tColumn = tbl_main.getColumnModel().getColumn(z);
  tColumn.setCellRenderer(!newRender ? render : new CellRender());
  tColumn.setPreferredWidth(columnSizes[z]);
  tColumn.setResizable(isResizable);

}

// ............................................................................

SwingUtilities.invokeLater(() -> {

  int totalW = 0;
  var cModel = tbl_main.getColumnModel();
  int viewportW = sp_table.getViewport().getWidth();

  for (int q = 0; q < tbl_main.getColumnCount(); q++)
    { totalW += cModel.getColumn(q).getPreferredWidth(); }

  if (totalW < viewportW)
    { var lastColumn = cModel.getColumn(tbl_main.getColumnCount() - 1);
      int prefW = viewportW - totalW + lastColumn.getPreferredWidth();
      lastColumn.setPreferredWidth(prefW); } });

}

// ============================================================================
/// Оновлення даних в таблиці

private void updateTableData (TableModelEvent e) {

    if (!reactOnChange) { return; }

    int rowId = e.getFirstRow();
    mni_save.setEnabled(true);
    dataWasChanged = true;

    if (!fileExt.toLowerCase().equals("exe")) { return; }
    if (!editedList.contains(rowId)) { editedList.add(rowId); }

    int newLength = ((String) tbl_main.getValueAt(rowId, 2)).length();
    int oldLength = textBlocks.get(rowId).getRawData().length;

    String stat = newLength + "/" + oldLength;

    reactOnChange = false;
    tbl_main.setValueAt(stat, rowId, 1);
    reactOnChange = true;
}

// ============================================================================
/// Оновлення інформації про таблицю

private void updateTableInfo() {

    String tmp;
        
    tmp = lbl_rowCount.getText();
    tmp = tmp.substring(0, tmp.indexOf(":") + 1) + " "
                      + tableModel.getRowCount();
    lbl_rowCount.setText(tmp);

    tmp = lbl_colCount.getText();
    tmp = tmp.substring(0, tmp.indexOf(":") + 1) + " "
                      + tableModel.getColumnCount();
    lbl_colCount.setText(tmp);
}

// ============================================================================
/// Оновлення заголовку головного вікна

private void updateAppTitle() {
    
    String newTitle = !dataWasChanged ? inputFile.getName() :
                                 "* " + inputFile.getName() + " *";
    
    if (!getTitle().equals(newTitle)) { setTitle(newTitle); }
}

// ============================================================================
/// Встановлення іконок для головного вікна

private void initAppIcons() {

    BufferedImage icon;
    ArrayList<Image> appIcons = new ArrayList<>();

    try {
        
    for (String resource : new String[] { "icon_16.png",
                                          "icon_32.png" }) {
        resource = "icons/" + resource;
        icon = ImageIO.read(getClass().getResourceAsStream(resource));
        appIcons.add(icon); }
    
    setIconImages(appIcons); }
    
    catch (IOException _) { }
}

// ============================================================================
/// Цей метод викликається з конструктора для ініціалізації форми.
/// УВАГА: НЕ змінюйте цей код. Вміст цього методу завжди 
/// перезапишеться редактором форм

    @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    sp_table = new JScrollPane();
    tbl_main = new JTable();
    pnl_footer = new JPanel();
    lbl_colCount = new JLabel();
    lbl_rowCount = new JLabel();
    mnb_main = new JMenuBar();
    mn_file = new JMenu();
    mni_open = new JMenuItem();
    mni_save = new JMenuItem();
    sep_one = new JPopupMenu.Separator();
    mni_find = new JMenuItem();
    sep_two = new JPopupMenu.Separator();
    mni_exit = new JMenuItem();
    mn_edit = new JMenu();
    mni_fntDecompile = new JMenuItem();
    mni_fntCompile = new JMenuItem();
    sep_three = new JPopupMenu.Separator();
    mni_filterRows = new JMenuItem();
    mni_strictRules = new JMenuItem();
    mn_info = new JMenu();
    mni_about = new JMenuItem();

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    setTitle("TTool_xApoc");
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        onWindowClose(evt);
      }
    });

    tbl_main.setModel(new DefaultTableModel(
      new Object [][] {

      },
      new String [] {

      }
    ));
    tbl_main.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    tbl_main.setAutoscrolls(false);
    tbl_main.setIntercellSpacing(new Dimension(2, 2));
    tbl_main.setRowSelectionAllowed(false);
    tbl_main.setShowGrid(true);
    tbl_main.getTableHeader().setReorderingAllowed(false);
    tbl_main.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent evt) {
        onTableClick(evt);
      }
    });
    sp_table.setViewportView(tbl_main);

    pnl_footer.setLayout(new FlowLayout(FlowLayout.CENTER, 50, 5));

    lbl_colCount.setText("Кількість стовбців: 0");
    pnl_footer.add(lbl_colCount);

    lbl_rowCount.setText("Кількість рядків: 0");
    pnl_footer.add(lbl_rowCount);

    mn_file.setText("Файл");

    mni_open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
    mni_open.setText("Відкрити");
    mni_open.setActionCommand("open");
    mni_open.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        onMenuClick(evt);
      }
    });
    mn_file.add(mni_open);

    mni_save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
    mni_save.setText("Зберегти");
    mni_save.setActionCommand("save");
    mni_save.setEnabled(false);
    mni_save.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        onMenuClick(evt);
      }
    });
    mn_file.add(mni_save);
    mn_file.add(sep_one);

    mni_find.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
    mni_find.setText("Пошук");
    mni_find.setActionCommand("find");
    mni_find.setEnabled(false);
    mni_find.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        onMenuClick(evt);
      }
    });
    mn_file.add(mni_find);
    mn_file.add(sep_two);

    mni_exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
    mni_exit.setText("Вихід");
    mni_exit.setActionCommand("exit");
    mni_exit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        onMenuClick(evt);
      }
    });
    mn_file.add(mni_exit);

    mnb_main.add(mn_file);

    mn_edit.setText("Правка");

    mni_fntDecompile.setText("Розпакувати шрифт");
    mni_fntDecompile.setActionCommand("decompileFont");
    mni_fntDecompile.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        onMenuClick(evt);
      }
    });
    mn_edit.add(mni_fntDecompile);

    mni_fntCompile.setText("Запакувати шрифт");
    mni_fntCompile.setActionCommand("compileFont");
    mni_fntCompile.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        onMenuClick(evt);
      }
    });
    mn_edit.add(mni_fntCompile);
    mn_edit.add(sep_three);

    mni_filterRows.setText("Ручне фільтрування рядків");
    mni_filterRows.setActionCommand("filterRows");
    mni_filterRows.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        onMenuClick(evt);
      }
    });
    mn_edit.add(mni_filterRows);

    mni_strictRules.setText("Строгі правила фільтрування");
    mni_strictRules.setActionCommand("strictRules");
    mni_strictRules.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        onMenuClick(evt);
      }
    });
    mn_edit.add(mni_strictRules);

    mnb_main.add(mn_edit);

    mn_info.setText("Інфо");

    mni_about.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK));
    mni_about.setText("Про програму");
    mni_about.setActionCommand("info");
    mni_about.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        onMenuClick(evt);
      }
    });
    mn_info.add(mni_about);

    mnb_main.add(mn_info);

    setJMenuBar(mnb_main);

    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addComponent(sp_table, GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
          .addComponent(pnl_footer, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addContainerGap())
    );
    layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(sp_table, GroupLayout.DEFAULT_SIZE, 333, Short.MAX_VALUE)
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(pnl_footer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addContainerGap())
    );

    pack();
    setLocationRelativeTo(null);
  }// </editor-fold>//GEN-END:initComponents

// ============================================================================
/// Прослуховування пунктів меню програми

  private void onMenuClick(ActionEvent evt) {//GEN-FIRST:event_onMenuClick

    switch (evt.getActionCommand()) {

      case "open" -> showOpenDialog();
      case "save" -> showSaveDialog();
      case "find" -> showSearchDialog();
      case "exit" -> showExitDialog();
      case "info" -> showInfoDialog();

      case "decompileFont" -> showDecompileFontDialog();
      case "compileFont"   -> showCompileFontDialog();

      case "filterRows"  -> mni_filterRows.setFont(mni_filterRows.getFont()
                                          .equals(strikeFont) ? null 
                                                : strikeFont);
      case "strictRules" -> mni_strictRules.setFont(mni_strictRules.getFont()
                                           .equals(strikeFont) ? null 
                                                 : strikeFont);
    }
  }//GEN-LAST:event_onMenuClick

// ============================================================================
/// Прослуховування закривання вікна

  private void onWindowClose(WindowEvent evt) {//GEN-FIRST:event_onWindowClose
    showExitDialog();
  }//GEN-LAST:event_onWindowClose

// ============================================================================
/// Прослуховування натискань у таблиці

  private void onTableClick(MouseEvent evt) {//GEN-FIRST:event_onTableClick
    
    int selectedRow = tbl_main.getSelectedRow();
    
    if (selectedRow == -1 ||
        fileExt.toLowerCase().equals("mt")) { return; }  
    
    TextBlock block = textBlocks.get(selectedRow);
    
    // ........................................................................
    
    if (evt.getButton() == MouseEvent.BUTTON2 ||
       (evt.isControlDown() && evt.getButton() == MouseEvent.BUTTON3)) {
       
      String msg = "Оригінальний текст:%n\"%s\"%n%n" +
                   "Розшифрований текст:%n\"%s\"%n%n" +
                   "Новий текст:%n\"%s\"";

      String orgText = new String(block.getRawData());
      String decText = CodeTable.decodeText(block.getRawData());
      String newText = (String) tbl_main.getValueAt(selectedRow, 2);

      showMessageDialog(this, msg.formatted(orgText, decText, newText));
        
    }
    
    // ........................................................................
    
    else if (evt.getButton() == MouseEvent.BUTTON3 &&
             evt.getClickCount() >= 3) {
        
      int length = block.getRawData().length;
      String stat = length + "/" + length;
      String decodedText = CodeTable.decodeText(block.getRawData());    

      reactOnChange = false;
      tbl_main.repaint();
      tbl_main.setValueAt(stat, selectedRow, 1);
      tbl_main.setValueAt(decodedText, selectedRow, 2);
      editedList.remove(Integer.valueOf(selectedRow));
      reactOnChange = true;
    
    }
  }//GEN-LAST:event_onTableClick

// ============================================================================
/// Список усіх об'явлених змінних

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JLabel lbl_colCount;
  private JLabel lbl_rowCount;
  private JMenu mn_edit;
  private JMenu mn_file;
  private JMenu mn_info;
  private JMenuBar mnb_main;
  private JMenuItem mni_about;
  private JMenuItem mni_exit;
  private JMenuItem mni_filterRows;
  private JMenuItem mni_find;
  private JMenuItem mni_fntCompile;
  private JMenuItem mni_fntDecompile;
  private JMenuItem mni_open;
  private JMenuItem mni_save;
  private JMenuItem mni_strictRules;
  private JPanel pnl_footer;
  private JPopupMenu.Separator sep_one;
  private JPopupMenu.Separator sep_three;
  private JPopupMenu.Separator sep_two;
  private JScrollPane sp_table;
  public JTable tbl_main;
  // End of variables declaration//GEN-END:variables

// Кінець класу TToolxApoc ====================================================

}
