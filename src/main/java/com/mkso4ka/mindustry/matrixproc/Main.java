package com.mkso4ka.mindustry.matrixproc;

import java.awt.Color;
import java.awt.Graphics2D;
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
            int displaysX = 1;
            int displaysY = 1;
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

            // 3. Рассчитываем итоговый размер и создаем мастер-изображение
            int totalWidth = (displaysX * displayPixelSize) + (Math.max(0, displaysX - 1) * BORDER_SIZE * 2);
            int totalHeight = (displaysY * displayPixelSize) + (Math.max(0, displaysY - 1) * BORDER_SIZE * 2);
            System.out.println("3. Масштабирование исходного изображения до " + totalWidth + "x" + totalHeight + ".");
            BufferedImage masterImage = ImageIO.read(sourceImageFile);
            BufferedImage scaledMasterImage = ImageUtils.resize(masterImage, totalWidth, totalHeight);
            ImageIO.write(scaledMasterImage, "png", new File(outputDir, "scaled_master_image.png")); // ДЕБАГ 1

            // 4. Создаем чертеж
            DisplayMatrix displayMatrix = new DisplayMatrix();
            MatrixBlueprint blueprint = displayMatrix.placeDisplaysXxY(
                displaysX, displaysY, displaySize, DisplayProcessorMatrixFinal.PROCESSOR_REACH
            );
            System.out.println("4. Создан чертеж для " + blueprint.displayCoordinates.length + " дисплеев.");

            // 5. Анализируем фрагменты
            System.out.println("5. Анализ и нарезка фрагментов...");
            int[] processorsPerDisplay = new int[blueprint.displayCoordinates.length];
            
            for (int i = 0; i < displaysY; i++) {
                for (int j = 0; j < displaysX; j++) {
                    int displayIndex = j * displaysY + i;

                    // 5.1. Рассчитываем размер и смещение для вырезания
                    int sliceWidth = displayPixelSize + (j > 0 ? BORDER_SIZE : 0) + (j < displaysX - 1 ? BORDER_SIZE : 0);
                    int sliceHeight = displayPixelSize + (i > 0 ? BORDER_SIZE : 0) + (i < displaysY - 1 ? BORDER_SIZE : 0);
                    int subX = j * (displayPixelSize + BORDER_SIZE * 2) - (j > 0 ? BORDER_SIZE : 0);
                    int subY = i * (displayPixelSize + BORDER_SIZE * 2) - (i > 0 ? BORDER_SIZE : 0);

                    // 5.2. Вырезаем фрагмент
                    BufferedImage finalSlice = scaledMasterImage.getSubimage(subX, subY, sliceWidth, sliceHeight);
                    ImageIO.write(finalSlice, "png", new File(outputDir, "debug_tile_raw_slice_" + displayIndex + ".png")); // ДЕБАГ 2

                    // 5.3. Анализируем фрагмент
                    Pixmap pixmap = Pixmap.fromBufferedImage(finalSlice);
                    ImageProcessor processor = new ImageProcessor(pixmap);
                    Map<Integer, List<Rect>> rects = processor.groupOptimal();

                    // --- КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: Определяем смещение для коррекции координат ---
                    int offsetX = (j > 0) ? BORDER_SIZE : 0;
                    int offsetY = (i > 0) ? BORDER_SIZE : 0;

                    // 5.4. Генерируем команды и считаем процессоры
                    List<String> allCommands = generateCommandList(rects, displayPixelSize, offsetX, offsetY);
                    int commandCount = allCommands.size();
                    processorsPerDisplay[displayIndex] = (int) Math.ceil((double) commandCount / COMMANDS_PER_PROCESSOR);
                    System.out.println("   Дисплей " + displayIndex + " (X:" + j + ",Y:" + i + "): "
                        + "Срез " + sliceWidth + "x" + sliceHeight + ". "
                        + "Смещение (" + offsetX + "," + offsetY + "). "
                        + commandCount + " команд -> " + processorsPerDisplay[displayIndex] + " проц.");

                    // --- Секция расширенного дебага ---
                    createDebugImages(outputDir, displayIndex, finalSlice, rects, displayPixelSize, offsetX, offsetY);

                    // 5.5. Сохраняем код для процессоров
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

    /**
     * Создает два отладочных изображения для одного фрагмента.
     */
    private static void createDebugImages(File outputDir, int displayIndex, BufferedImage finalSlice, Map<Integer, List<Rect>> rects, int displayPixelSize, int offsetX, int offsetY) throws IOException {
        // ДЕБАГ 3: Рисуем найденные прямоугольники на "сыром" фрагменте
        BufferedImage sliceWithRects = new BufferedImage(finalSlice.getWidth(), finalSlice.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = sliceWithRects.createGraphics();
        g1.drawImage(finalSlice, 0, 0, null);
        for (List<Rect> rectList : rects.values()) {
            for (Rect rect : rectList) {
                g1.setColor(Color.RED);
                g1.drawRect(rect.x, rect.y, rect.w - 1, rect.h - 1);
            }
        }
        g1.dispose();
        ImageIO.write(sliceWithRects, "png", new File(outputDir, "debug_tile_with_rects_" + displayIndex + ".png"));

        // ДЕБАГ 4: Симулируем отрисовку на дисплее с исправленными координатами
        BufferedImage commandPreview = new BufferedImage(displayPixelSize, displayPixelSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = commandPreview.createGraphics();
        g2.setColor(Color.BLACK); // Фон для наглядности
        g2.fillRect(0, 0, displayPixelSize, displayPixelSize);
        for (Map.Entry<Integer, List<Rect>> entry : rects.entrySet()) {
            g2.setColor(new Color(entry.getKey(), true));
            for (Rect rect : entry.getValue()) {
                int correctedX = rect.x - offsetX;
                int correctedY = rect.y - offsetY;
                // Проверка, чтобы не рисовать за пределами видимой области
                if (correctedX >= 0 && correctedY >= 0 && correctedX + rect.w <= displayPixelSize && correctedY + rect.h <= displayPixelSize) {
                    g2.fillRect(correctedX, correctedY, rect.w, rect.h);
                }
            }
        }
        g2.dispose();
        ImageIO.write(commandPreview, "png", new File(outputDir, "debug_final_commands_preview_" + displayIndex + ".png"));
    }

    private static List<String> generateCommandList(Map<Integer, List<Rect>> rects, int displayPixelSize, int offsetX, int offsetY) {
        List<String> commands = new ArrayList<>();
        for (Map.Entry<Integer, List<Rect>> entry : rects.entrySet()) {
            List<Rect> rectList = entry.getValue();
            if (!rectList.isEmpty()) {
                commands.add(formatColorCommand(entry.getKey()));
                for (Rect rect : rectList) {
                    int correctedX = rect.x - offsetX;
                    int correctedY = rect.y - offsetY;
                    // Инвертируем Y для Mindustry, используя correctedY и размер видимой области
                    int mindustryY = displayPixelSize - correctedY - rect.h;
                    commands.add(formatRectCommand(correctedX, mindustryY, rect.w, rect.h));
                }
            }
        }
        return commands;
    }

    private static String formatRectCommand(int x, int y, int w, int h) {
        return String.format("draw rect %d %d %d %d 0 0", x, y, w, h);
    }
    
    // Остальные вспомогательные методы без изменений
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

    private static String formatColorCommand(int pixel) {
        int a = (pixel >> 24) & 0xff;
        int r = (pixel >> 16) & 0xff;
        int g = (pixel >> 8) & 0xff;
        int b = pixel & 0xff;
        return String.format("draw color %d %d %d %d 0 0", r, g, b, a);
    }
}