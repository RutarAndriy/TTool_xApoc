package com.rutar.ttool_xapoc;

import java.io.*;
import java.awt.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import javax.swing.*;
import java.awt.font.*;
import java.nio.file.*;
import javax.imageio.*;
import java.util.jar.*;
import java.awt.event.*;
import java.awt.image.*;
import java.nio.charset.*;
import javax.swing.event.*;
import javax.swing.table.*;
import com.formdev.flatlaf.*;
import javax.swing.filechooser.*;
import com.rutar.ua_translator.*;
import com.formdev.flatlaf.themes.*;

import static java.io.File.*;
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

private String appDescription;                                 // опис програми
private DefaultTableModel tableModel;              // стандартна модель таблиці

private boolean dataWasChanged;                // якщо true - дані були змінені

private File tmpFile;                                       // допоміжна змінна
private byte[] allBytes;                                   // всі зчитані байти
private byte[] allAdditional;
private final int minStrLen = 2;
private ByteBuffer buffer;                        // буфер для зчитування даних
private SearchDialog searchDialog;         // діалогове вікно пошуку інформації

private final Font strikeFont;

private final ArrayList<Integer> ufopediaIndexes      = new ArrayList<>();
private final ArrayList<UFOPediaBlock> ufopediaBlocks = new ArrayList<>();

public static final ArrayList<TextBlock> textBlocks = new ArrayList<>();
public static final ArrayList<Integer> editedList = new ArrayList<>();

private boolean reactOnChange = true;

// ............................................................................

public static String fileExt;                    // розширення відкритого файлу

// Домашня директорія користувача
public static final File HOME_DIR = FileSystemView.getFileSystemView()
                                                  .getHomeDirectory();

public static boolean debug = true;  // якщо true - увімк. режим налагоджування

// ============================================================================
/// Конструктор за замовчуванням

public TToolxApoc() {

initComponents();
initAppIcons();

fileOpen     = Utils.getFileChooser(FILES_ONLY, Map.of
                                   ("mt",  "xApoc нлопедія",
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
    
    EventQueue.invokeLater(() -> {
        new TToolxApoc().setVisible(true);
    });
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

inputFile = fileOpen.getSelectedFile();
ArrayList<String> row = new ArrayList<>();
File mtDesc = new File(inputFile.getAbsolutePath() + "I");

// Очищення попередніх даних
ufopediaBlocks.clear();
ufopediaIndexes.clear();
dataWasChanged = false;

try { 

// Зчитування файлів UFO-педії
allBytes = Files.readAllBytes(inputFile.toPath());
allAdditional = Files.readAllBytes(mtDesc.toPath());

buffer = ByteBuffer.wrap(allAdditional);
buffer.order(ByteOrder.LITTLE_ENDIAN);

// Отримання індексів значущих блоків
while (buffer.remaining() >= 4) { ufopediaIndexes.add(buffer.getInt()); }

// Обробка блоків FO-педії в циклі
for (int z = 0; z < ufopediaIndexes.size() - 1; z++) {
    
    // Отримання блоку даних
    int from = ufopediaIndexes.get(z);
    int to   = ufopediaIndexes.get(z + 1);
    var block = new UFOPediaBlock(Arrays.copyOfRange(allBytes, from, to));
    
    // Парсинг блоку даних
    row.clear();
    row.add(String.valueOf(z + 1));
    row.add(block.getTitle());
    row.add(block.getDescription());
    
    // Додавання даних у таблицю
    tableModel.addRow(row.toArray(String[]::new));
    ufopediaBlocks.add(block);
}

finalizeNewTable(true);

}

// ............................................................................

catch (IOException e)
    { showMessageDialog(this, "При відкриванні файлу відбулася критична " +
                              "помилка", "Помилка", ERROR_MESSAGE); }
}

// ============================================================================
/// Відкривання *.exe файлів

private void openExeFile() {

int pos = -1;                   // позиція читання даних, якщо (-1) - не задана
var baos = new ByteArrayOutputStream();         // контейнер для зчитаних даних
ArrayList<String> row = new ArrayList<>();    // представляє один рядок таблиці

// Очищення попередніх даних
textBlocks.clear();
editedList.clear();
dataWasChanged = false;

prepareNewTable(false);
inputFile = fileOpen.getSelectedFile();

// ............................................................................
/// Зчитування *.exe файлів, як набору байтових даних

try {

// Зчитування всіх байт
allBytes = Files.readAllBytes(inputFile.toPath());

// Ручне задавання діапазонів, у межах яких є дані для обробки
Range range;
switch (inputFile.getName())
    { case "UFO2P.EXE"  -> { range = new Range(ufo2p);  }
      case "UFO2P4.EXE" -> { range = new Range(ufo2p4); }
      case "TACP.EXE"   -> { range = new Range(tacp);   }
      case "TACP4.EXE"  -> { range = new Range(tacp4);  }
      default           -> { range = new Range("-");    } }

// Вимкнення фільтрування рядків
if (mni_filterRows.getFont().equals(strikeFont)) { range = new Range("-"); }

// Пошук послідовностей, які потенційно можуть бути текстом для перекладу
for (int z = 0; z < allBytes.length; z++) {

    // Пропуск даних поза робочим діапазоном
    if (!range.contains(z)) { pos = -1;
                              baos.reset();
                              continue; }
    
    // Виявлено потенційний кінець текстового рядка
    if (allBytes[z] == 0) {
        // Отримання байтового масиву
        byte[] bytes = baos.toByteArray();
        // Перевірка мінімальної довжини текстового блоку
        if (bytes.length < minStrLen) { pos = -1; baos.reset(); continue; }
        // Додавання нового текстового блоку до загального масиву
        textBlocks.add(new TextBlock(pos, bytes));
        // Очищення буферу
        baos.reset();
        // Скидання позиції обробки
        pos = -1;
    }
    
    // Виявлено допустимий символ
    else if (CodeTable.isValidByte(allBytes[z])) {
        // Якщо позиція обробки не задана - задаємо її
        if (pos == -1) { pos = z; }
        // Запис допустимого символу в буфер
        baos.write(allBytes[z]); }
    
    // Виявлено недопустимий символ
    else { // Очищення буферу
           baos.reset();
           // Скидання позиції обробки
           pos = -1; }

}

// ............................................................................
// Додавання всіх знайдених текстових блоків до таблиці

int id = 0;
String tmp;

for (TextBlock tBlock : textBlocks)
    { tmp = CodeTable.decodeText(tBlock.getRawData());
      row.clear();
      row.add(String.valueOf(++id));
      row.add(tmp.length() + "/" + tBlock.getRawData().length);
      row.add(tmp);
      tableModel.addRow(row.toArray(String[]::new)); }

finalizeNewTable(false);

}

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

// Utils.replaceUnusedChars("...");

try {

outputFile = fileOpen.getSelectedFile();
File ufopediaDesc = new File(inputFile.getAbsolutePath() + "I");
UFOPediaBlock block;
String tmp;
byte[] data;
int pos = 0;

// Ініціалізація буферу для запису даних
buffer = ByteBuffer.allocate((ufopediaBlocks.size() + 1) * 4);
buffer.order(ByteOrder.LITTLE_ENDIAN);
buffer.putInt(pos);

// Запис нлопедії та файлу-дескриптора
try (FileOutputStream pediaF = new FileOutputStream(outputFile, false);
     FileOutputStream pediaD = new FileOutputStream(ufopediaDesc, false)) {

// Обробка текстових блоків у циклі
for (int z = 0; z < ufopediaBlocks.size(); z++) {
    
    block = ufopediaBlocks.get(z);
    tmp = (String) tbl_main.getValueAt(z, 1);
    block.setTitle(tmp);
    tmp = (String) tbl_main.getValueAt(z, 2);
    block.setDescription(tmp);
    
    data = block.getRawData();
    pos += data.length;
    
    pediaF.write(data);
    buffer.putInt(pos);
    
}

// Запис даних у файл-дескриптор
pediaD.write(buffer.array());

}

// ............................................................................

dataWasChanged = false;
ufopediaIndexes.clear();
ufopediaBlocks.clear();
updateAppTitle();

showMessageDialog(this, "Файл " + outputFile.getName() + " успішно збережено",
                        "Повідомлення", INFORMATION_MESSAGE);

}

// ............................................................................

catch (HeadlessException | IOException _)
    { showMessageDialog(this, "При збереженні файлу відбулася критична "
                            + "помилка", "Помилка", ERROR_MESSAGE); }
}

// ============================================================================
/// Збереження *.exe файлів

private void saveExeFile() {

// Utils.replaceUnusedChars("...");

// Обробка всіх текстових блоків
for (Integer edited : editedList) {

    TextBlock block = textBlocks.get(edited);
    int blockSize = block.getRawData().length;
    String newText = (String) tbl_main.getValueAt(edited, 2);
    byte[] bytes = new byte[blockSize];
    byte[] encoded = CodeTable.encodeText(newText);
    
    // Якщо довжини старого і нового текстів співпадають - все ок
    if (encoded.length == bytes.length) { bytes = encoded; }
    
    // Якщо довжини не співпадають - заповнюємо вільне місце пробілами
    else {
        
        for (int z = 0; z < bytes.length; z++) {
            
            if (z < encoded.length) { bytes[z] = encoded[z]; }
            else                    { bytes[z] = 0x20; }
            
        }
        
        // Оновлення даних у таблиці
        reactOnChange = false;
        tbl_main.setValueAt(bytes.length + "/" + bytes.length, edited, 1);
        tbl_main.setValueAt(CodeTable.decodeText(bytes), edited, 2);
        reactOnChange = true;
        
        // Оновлення дних текстового блоку
        textBlocks.set(edited, new TextBlock(block.getPosition(), bytes));
        
    }
    
    // Заміна оригінальних байт на оброблені
    System.arraycopy(bytes, 0, allBytes, block.getPosition(), bytes.length);
    
}

// Отримання назви вихідного файлу
outputFile = fileOpen.getSelectedFile();

// Запис результату в файл
try (FileOutputStream fos = new FileOutputStream(outputFile)) {
    
fos.write(allBytes);

editedList.clear();
tbl_main.repaint();
dataWasChanged = false;
updateAppTitle();

showMessageDialog(this, "Файл " + outputFile.getName() + " успішно збережено",
                        "Повідомлення", INFORMATION_MESSAGE);
}

// ............................................................................

catch (Exception e)
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
    { searchDialog = new SearchDialog(this);   
      searchDialog.setVisible(true); }

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

String dirPath;                         // шлях до папки із результатом обробки
int w, h, d, color;                                         // допоміжні змінні
BufferedImage image;                                   // зображення для запису

// Ініціалізація допоміжних змінних, які залежать від назви файлу шрифту
switch (inputFile.getName().split("\\.")[0]) {
    case "BIGFONT"  -> { w = 14; h = 24; d = 24; }
    case "SMALFONT" -> { w = 14; h = 15; d = 30; }
    case "SMALLSET" -> { w = 8;  h = 12; d = -1; }
    default         -> { showMessageDialog(this, "Неможливо розпакувати даний "
                                               + "шрифт!"); return; } }

// ............................................................................

try {

// Зчитування файлів шрифта та дескиптора, якщо він існує 
allBytes = Files.readAllBytes(inputFile.toPath());
if (d != -1) { File desc = new File(inputFile.getAbsolutePath()
                                             .replace(".DAT", ".SPC"));
               allAdditional = Files.readAllBytes(desc.toPath()); }

// Створення папки для запису результатів розпакування
dirPath  = inputFile.getParent() + separator;
dirPath += inputFile.getName().replace(".DAT", separator);
new File(dirPath).mkdir();

// Ініціалізація допоміжної змінної та кількості символів у шрифті
String proc = d == -1 ? "" : "_%d";
int charsCount = allBytes.length / w / h;

// ............................................................................
// Обробка усіх символів у циклі

for (int z = 0; z < charsCount; z++) {
    
    int len = -1;
    if (d != -1) { len = allAdditional[z * d + d + d/2]; }
    image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
    
    for (int r = 0; r < h; r++) {
    for (int c = 0; c < w; c++) {

        int index = (z * w * h) + r * w + c;
        color = allBytes[index] == 0 ? 0x0 : 0xFFFFFF;
        image.setRGB(c, r, color);
        
    }
    }
    
    // Запис результату в файл
    String imageName = String.format("%03d_%02X" + proc, z + 1, z + 33, len);
    File output = new File(dirPath + imageName + ".bmp");
    ImageIO.write(image, "bmp", output);
    
}

showMessageDialog(this, "Шрифт успішно розпаковано!");

}

// ............................................................................

catch (HeadlessException | IOException _)
    { showMessageDialog(this, "При розпакуванні шрифта відбулася критична "
                            + "помилка", "Помилка", ERROR_MESSAGE); }
}

// ============================================================================
/// Вибір розпакованого шрифту для пакування

private void showCompileFontDialog() {

tmpFile = Utils.getLastDir(fntDecompile);
if (tmpFile != null) { fntCompile.setCurrentDirectory(tmpFile); }

int result = fntCompile.showOpenDialog(this);
if (result != JFileChooser.APPROVE_OPTION) { return; }

inputFile = fntCompile.getSelectedFile();

int w, h, d, color;                                         // допоміжні змінні
BufferedImage image;                                   // зображення для запису
String dirPath = inputFile.getAbsolutePath();        // шлях до вихідних файлів

outputFile = new File(dirPath + ".DAT");

// Ініціалізація допоміжної змінної, яка залежать від назви файлу шрифту
switch (inputFile.getName()) {
   case "BIGFONT"  -> { d = 24; }
   case "SMALFONT" -> { d = 30; }
   case "SMALLSET" -> { d = -1; }
   default         -> { showMessageDialog(this, "Неможливо запакувати даний "
                                              + "шрифт!"); return; } }

// Файл опису шрифта
File fontDesc = new File(outputFile.getAbsolutePath().replace(".DAT", ".SPC"));

// Якщо файл опису відсутній - відображення повідомлення про помилку
if (d != -1 && !fontDesc.exists())
    { showMessageDialog(this, "Не знайдено файл опису \n" +
                              "шрифта: " + fontDesc.getName(), "Помилка", 0);
      return; }

// ............................................................................
// Збирання окремих символів у єдиний файл шрифту

try (FileOutputStream fos = new FileOutputStream(outputFile);
     BufferedOutputStream bos = new BufferedOutputStream(fos)) {

// Масив зображень окремих символів
File[] allFiles = inputFile.listFiles();

// Зчитування оригінальних описів шрифтів
if (d != -1) { allAdditional = Files.readAllBytes(fontDesc.toPath()); }

// Обробка символів у циклі
for (int z = 0; z < allFiles.length; z++) {

    // Отримання назви файлу для обробки
    String imageName = String.format("%03d_%02X", z + 1, z + 33);
    for (File file : allFiles) {
        if (file.getName().startsWith(imageName)) { imageName = file.getName();
                                                    break; } }
    
    // Отримання ширини символу
    byte len = -1;
    if (d != -1) { int from = imageName.lastIndexOf("_") + 1;
                   int to = imageName.lastIndexOf(".");
                   len = Byte.parseByte(imageName.substring(from, to)); }
    
    // Зчитування даних зображення
    image = ImageIO.read(new File(dirPath + separator + imageName));
    byte[] imageData = ((DataBufferByte)(image.getRaster().getDataBuffer()))
                                                          .getData();
    byte[] writable = new byte[imageData.length / 3];
    
    // Обробка та запис даних
    for (int pixel = 0; pixel < writable.length; pixel++)
        { writable[pixel] = (byte) (imageData[pixel * 3] == 0 ? 0x0 : 0x1); }
    bos.write(writable);
    
    // Обробка даних опису шрифта
    if (d != -1) { for (int q = 0; q < d/2; q++)
                       { allAdditional[z * d + d + d/2 + q] = len; } }
}

// Запис даних опису шрифта у файл
if (d != -1) { try (var fosDesc = new FileOutputStream(fontDesc))
                   { fosDesc.write(allAdditional); } }

showMessageDialog(this, "Шрифт успішно запаковано!");

}

// ............................................................................

catch (Exception _)
    { showMessageDialog(this, "При пакуванні шрифта відбулася критична "
                            + "помилка", "Помилка", ERROR_MESSAGE); }
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
else { tableModel.addColumn("№");
       tableModel.addColumn("Розмір");
       tableModel.addColumn("Текст для перекладу"); }

}

// ============================================================================
/// Завершальна ініціалізація нової таблиці

private void finalizeNewTable (boolean isUFOPedia) {

TableColumn tColumn;
int prefW, totalW = 0;
CellRender cellRender = new CellRender();
cellRender.setHorizontalAlignment(SwingConstants.CENTER);

// ............................................................................
// Відкрито *.mt файл

if (isUFOPedia) {

tColumn = tbl_main.getColumnModel().getColumn(0);
tColumn.setCellRenderer(cellRender);
tColumn.setPreferredWidth(prefW = 45);
tColumn.setResizable(false);
totalW += prefW;

for (int z = 1; z < tbl_main.getColumnCount(); z++) {
    tColumn = tbl_main.getColumnModel().getColumn(z);
    tColumn.setCellRenderer(new CellRender());
    if (z < tbl_main.getColumnCount() - 1)
         { tColumn.setPreferredWidth(prefW = 175);  
           totalW += prefW; }
    else { prefW  = sp_table.getViewport().getWidth() - totalW;
           prefW -= sp_table.getVerticalScrollBar().getPreferredSize().width;
           tColumn.setPreferredWidth(prefW >= 175 ? prefW : 175); } }
}

// ............................................................................
// Відкрито *.exe файл

else {

    tColumn = tbl_main.getColumnModel().getColumn(0);
    tColumn.setCellRenderer(cellRender);
    tColumn.setPreferredWidth(prefW = 45);
    tColumn.setResizable(false);
    totalW += prefW;

    tColumn = tbl_main.getColumnModel().getColumn(1);
    tColumn.setCellRenderer(cellRender);
    tColumn.setPreferredWidth(prefW = 55);
    tColumn.setResizable(false);
    totalW += prefW;

    tColumn = tbl_main.getColumnModel().getColumn(2);
    tColumn.setCellRenderer(new CellRender());
    prefW  = sp_table.getViewport().getWidth() - totalW;
    prefW -= sp_table.getVerticalScrollBar().getPreferredSize().width;
    tColumn.setPreferredWidth(prefW >= 175 ? prefW : 175);

}

// ............................................................................

updateTableInfo();

mni_find.setEnabled(true);
tableModel.addTableModelListener((TableModelEvent evt) -> {
    updateTableData(evt);
    updateAppTitle();
});

}

// ============================================================================
/// Оновлення даних в таблиці

private void updateTableData (TableModelEvent e) {

    if (!reactOnChange) { return; }

    int row = e.getFirstRow();
    mni_save.setEnabled(true);
    dataWasChanged = true;

    if (!fileExt.toLowerCase().equals("exe")) { return; }
    if (!editedList.contains(row)) { editedList.add(row); }

    int newLength = ((String) tbl_main.getValueAt(row, 2)).length();
    int oldLength = textBlocks.get(row).getRawData().length;

    String stat = newLength + "/" + oldLength;

    reactOnChange = false;
    tbl_main.setValueAt(stat, row, 1);
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

        mni_filterRows.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        mni_filterRows.setText("Фільтрувати рядки");
        mni_filterRows.setActionCommand("filterRows");
        mni_filterRows.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_edit.add(mni_filterRows);

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
        case "filterRows"    -> mni_filterRows.setFont(mni_filterRows.getFont()
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
    
    if (evt.getButton() == MouseEvent.BUTTON2) {
       
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
    private JPanel pnl_footer;
    private JPopupMenu.Separator sep_one;
    private JPopupMenu.Separator sep_three;
    private JPopupMenu.Separator sep_two;
    private JScrollPane sp_table;
    public JTable tbl_main;
    // End of variables declaration//GEN-END:variables

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

// Кінець класу TToolxApoc ====================================================

}
