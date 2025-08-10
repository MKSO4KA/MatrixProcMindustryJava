package com.mkso4ka.mindustry.matrixproc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public class Main {
    // КОНСТАНТА: Лимит команд на процессор (999 - 10 зарезервированных)
    private static final int COMMANDS_PER_PROCESSOR = 989;
    // КОНСТАНТА: Размер контура/отступа для каждого фрагмента
    private static final int CONTOUR_PADDING = 8;

    public static void main(String[] args) {
        try {
            // --- Входные параметры ---
            int displaysX = 4;
            int displaysY = 4;
            int displaySize = 3; // Абстрактный размер блока (3 для Logic Display)
            File sourceImageFile = new File("/storage/emulated/0/1АA/python.png");

            File outputDir = new File("./output_images");
            if (!outputDir.exists()) outputDir.mkdirs();

            // =================================================================
            // НОВЫЙ БЛОК: Определяем реальный размер дисплея в пикселях
            // =================================================================
            int displayPixelSize;
            switch (displaySize) {
                case 3: // Logic Display
                    displayPixelSize = 80;
                    break;
                case 6: // Large Logic Display
                    displayPixelSize = 176;
                    break;
                default:
                    System.err.println("Ошибка: Неизвестный размер дисплея " + displaySize + ". Используется размер по умолчанию 80.");
                    displayPixelSize = 80;
                    break;
            }
            System.out.println("0. Определение параметров: Размер дисплея " + displaySize + " соответствует " + displayPixelSize + "x" + displayPixelSize + " пикселей.");

            // 1. Загружаем мастер-изображение
            System.out.println("1. Загрузка изображения: " + sourceImageFile.getName());
            BufferedImage masterImage = ImageIO.read(sourceImageFile);

            // 2. Создаем чертеж расположения дисплеев
            DisplayMatrix displayMatrix = new DisplayMatrix();
            MatrixBlueprint blueprint = displayMatrix.placeDisplaysXxY(
                displaysX, displaysY, displaySize, DisplayProcessorMatrixFinal.PROCESSOR_REACH
            );
            System.out.println("2. Создан чертеж для " + blueprint.displayCoordinates.length + " дисплеев.");

            // 3. Масштабируем мастер-изображение под ОБЩИЙ РАЗМЕР СЕТКИ В ПИКСЕЛЯХ
            int totalDisplayWidth = displaysX * displayPixelSize;
            int totalDisplayHeight = displaysY * displayPixelSize;
            System.out.println("3. Масштабирование изображения до " + totalDisplayWidth + "x" + totalDisplayHeight + " пикселей.");
            BufferedImage scaledMasterImage = ImageUtils.resize(masterImage, totalDisplayWidth, totalDisplayHeight);

            // 4. Рассчитываем потребность в процессорах для КАЖДОГО дисплея
            System.out.println("4. Анализ фрагментов и расчет потребностей...");
            int[] processorsPerDisplay = new int[blueprint.displayCoordinates.length];
            
            for (int i = 0; i < blueprint.displayCoordinates.length; i++) {
                // 4.1. Нарезаем фрагмент, используя РЕАЛЬНЫЙ РАЗМЕР В ПИКСЕЛЯХ
                int gridX = i / displaysY;
                int gridY = i % displaysY;
                BufferedImage cropped = scaledMasterImage.getSubimage(
                    gridX * displayPixelSize, 
                    gridY * displayPixelSize, 
                    displayPixelSize, 
                    displayPixelSize
                );

                // 4.2. Добавляем контур к фрагменту
                BufferedImage contoured = ImageUtils.addContour(cropped, CONTOUR_PADDING);
                File debugFile = new File(outputDir.getPath() + "/debug_tile_" + i + ".png");
                ImageIO.write(contoured, "png", debugFile);

                // 4.3. Анализируем фрагмент
                Pixmap pixmap = Pixmap.fromBufferedImage(contoured);
                ImageProcessor processor = new ImageProcessor(pixmap);
                Map<Integer, List<Rect>> rects = processor.groupOptimal();

                // 4.4. Считаем команды
                int commandCount = countCommands(rects);
                
                // 4.5. Рассчитываем процессоры
                processorsPerDisplay[i] = (int) Math.ceil((double) commandCount / COMMANDS_PER_PROCESSOR);
                System.out.println("   Дисплей " + i + ": " + commandCount + " команд -> " + processorsPerDisplay[i] + " процессоров.");
            }

            System.out.println("--- ИТОГ АНАЛИЗА ---");
            System.out.println("Рассчитанные потребности: " + Arrays.toString(processorsPerDisplay));
            System.out.println("----------------------");

            // 5. Запускаем физическое размещение. Сюда передаем абстрактный displaySize.
            DisplayProcessorMatrixFinal displayProcessorMatrix = new DisplayProcessorMatrixFinal(
                blueprint.n, blueprint.m, processorsPerDisplay, blueprint.displayCoordinates, displaySize
            );
            displayProcessorMatrix.placeProcessors();

            // 6. Сохраняем результат
            displayProcessorMatrix.createImage(outputDir.getPath() + "/final_schematic.png");

        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла изображения!");
            e.printStackTrace();
        }
    }

    private static int countCommands(Map<Integer, List<Rect>> rects) {
        int count = 0;
        for (List<Rect> rectList : rects.values()) {
            if (!rectList.isEmpty()) {
                count++;
                count += rectList.size();
            }
        }
        return count;
    }
}