package com.mkso4ka.mindustry.matrixproc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public class Main {
    private static final int COMMANDS_PER_PROCESSOR = 989;
    private static final int BORDER_SIZE = 8;

    public static void main(String[] args) {
        try {
            // --- Входные параметры ---
            int displaysX = 2;
            int displaysY = 2;
            int displaySize = 3;
            File sourceImageFile = new File("/storage/emulated/0/1АA/python.png");

            // --- 1. Очистка и создание папок ---
            File outputDir = new File("./output_images");
            File processorCodeDir = new File("./outputProcessors");
            cleanAndCreateDirectory(outputDir);
            cleanAndCreateDirectory(processorCodeDir);
            System.out.println("1. Папки для вывода очищены.");

            // 2. Определяем параметры
            int displayPixelSize = getDisplayPixelSize(displaySize);
            System.out.println("2. Параметры: Видимая область=" + displayPixelSize + "px, Внутренняя рамка=" + BORDER_SIZE + "px.");

            // 3. Рассчитываем итоговый размер мастер-изображения, компенсируя ТОЛЬКО внутренние рамки
            int totalWidth = (displaysX * displayPixelSize) + (Math.max(0, displaysX - 1) * BORDER_SIZE * 2);
            int totalHeight = (displaysY * displayPixelSize) + (Math.max(0, displaysY - 1) * BORDER_SIZE * 2);
            System.out.println("3. Масштабирование исходного изображения до " + totalWidth + "x" + totalHeight + ".");
            BufferedImage masterImage = ImageIO.read(sourceImageFile);
            BufferedImage scaledMasterImage = ImageUtils.resize(masterImage, totalWidth, totalHeight);

            // 4. Создаем чертеж
            DisplayMatrix displayMatrix = new DisplayMatrix();
            MatrixBlueprint blueprint = displayMatrix.placeDisplaysXxY(
                displaysX, displaysY, displaySize, DisplayProcessorMatrixFinal.PROCESSOR_REACH
            );
            System.out.println("4. Создан чертеж для " + blueprint.displayCoordinates.length + " дисплеев.");

            // 5. Анализируем фрагменты
            System.out.println("5. Анализ фрагментов...");
            int[] processorsPerDisplay = new int[blueprint.displayCoordinates.length];
            
            int currentY = 0;
            for (int i = 0; i < displaysY; i++) { // Внешний цикл по рядам (Y)
                int currentX = 0;
                int rowHeight = 0; // Высота текущего ряда фрагментов
                for (int j = 0; j < displaysX; j++) { // Внутренний цикл по колонкам (X)
                    int displayIndex = j * displaysY + i;

                    // 5.1. Определяем, является ли дисплей крайним
                    boolean isFirstCol = (j == 0);
                    boolean isLastCol = (j == displaysX - 1);
                    boolean isFirstRow = (i == 0);
                    boolean isLastRow = (i == displaysY - 1);

                    // 5.2. Рассчитываем размер фрагмента для вырезания
                    int sliceWidth = displayPixelSize + (isFirstCol ? 0 : BORDER_SIZE) + (isLastCol ? 0 : BORDER_SIZE);
                    int sliceHeight = displayPixelSize + (isFirstRow ? 0 : BORDER_SIZE) + (isLastRow ? 0 : BORDER_SIZE);
                    rowHeight = sliceHeight; // Все фрагменты в одном ряду имеют одинаковую высоту

                    // 5.3. Нарезаем финальный фрагмент, используя накопители currentX и currentY
                    BufferedImage finalSlice = scaledMasterImage.getSubimage(currentX, currentY, sliceWidth, sliceHeight);
                    currentX += sliceWidth; // Сдвигаем X для следующего фрагмента в ряду

                    // ОТЛАДКА
                    File debugFile = new File(outputDir.getPath() + "/debug_tile_final_" + displayIndex + ".png");
                    ImageIO.write(finalSlice, "png", debugFile);

                    // 5.4. Анализируем
                    Pixmap pixmap = Pixmap.fromBufferedImage(finalSlice);
                    ImageProcessor processor = new ImageProcessor(pixmap);
                    Map<Integer, List<Rect>> rects = processor.groupOptimal();

                    // 5.5. Генерируем команды и считаем процессоры
                    List<String> allCommands = generateCommandList(rects, finalSlice.getHeight());
                    int commandCount = allCommands.size();
                    processorsPerDisplay[displayIndex] = (int) Math.ceil((double) commandCount / COMMANDS_PER_PROCESSOR);
                    System.out.println("   Дисплей " + displayIndex + " (X:" + j + ",Y:" + i + "): " + commandCount + " команд -> " + processorsPerDisplay[displayIndex] + " процессоров.");

                    // 5.6. Сохраняем код
                    for (int p = 0; p < processorsPerDisplay[displayIndex]; p++) {
                        int start = p * COMMANDS_PER_PROCESSOR;
                        int end = Math.min(start + COMMANDS_PER_PROCESSOR, commandCount);
                        List<String> chunk = allCommands.subList(start, end);
                        StringBuilder codeBuilder = new StringBuilder();
                        chunk.forEach(command -> codeBuilder.append(command).append("\n"));
                        codeBuilder.append("drawflush display1");
                        String fileName = "display_" + displayIndex + "_proc_" + p + ".txt";
                        Files.write(Paths.get(processorCodeDir.getPath(), fileName), codeBuilder.toString().getBytes());
                    }
                }
                currentY += rowHeight; // Сдвигаем Y для следующего ряда
            }

            System.out.println("--- ИТОГ АНАЛИЗА ---");
            System.out.println("Код для процессоров сохранен в папку: " + processorCodeDir.getName());
            System.out.println("Рассчитанные потребности: " + Arrays.toString(processorsPerDisplay));
            System.out.println("----------------------");

            // 6. Запускаем физическое размещение
            DisplayProcessorMatrixFinal displayProcessorMatrix = new DisplayProcessorMatrixFinal(
                blueprint.n, blueprint.m, processorsPerDisplay, blueprint.displayCoordinates, displaySize
            );
            displayProcessorMatrix.placeProcessors();

            // 7. Сохраняем результат
            displayProcessorMatrix.createImage(outputDir.getPath() + "/final_schematic.png");

        } catch (Exception e) {
            System.err.println("Произошла критическая ошибка!");
            e.printStackTrace();
        }
    }

    private static void cleanAndCreateDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        } else {
            directory.mkdirs();
        }
    }

    private static int getDisplayPixelSize(int displayBlockSize) {
        switch (displayBlockSize) {
            case 3: return 80;
            case 6: return 176;
            default:
                System.err.println("Внимание: Неизвестный размер дисплея " + displayBlockSize + ". Используется размер по умолчанию 80.");
                return 80;
        }
    }

    private static List<String> generateCommandList(Map<Integer, List<Rect>> rects, int canvasHeight) {
        List<String> commands = new ArrayList<>();
        for (Map.Entry<Integer, List<Rect>> entry : rects.entrySet()) {
            List<Rect> rectList = entry.getValue();
            if (!rectList.isEmpty()) {
                commands.add(formatColorCommand(entry.getKey()));
                for (Rect rect : rectList) {
                    int mindustryY = canvasHeight - rect.y - rect.h;
                    commands.add(formatRectCommand(rect, mindustryY));
                }
            }
        }
        return commands;
    }

    private static String formatColorCommand(int pixel) {
        int a = (pixel >> 24) & 0xff;
        int r = (pixel >> 16) & 0xff;
        int g = (pixel >> 8) & 0xff;
        int b = pixel & 0xff;
        return String.format("draw color %d %d %d %d 0 0", r, g, b, a);
    }

    private static String formatRectCommand(Rect rect, int mindustryY) {
        return String.format("draw rect %d %d %d %d 0 0", rect.x, mindustryY, rect.w, rect.h);
    }
}