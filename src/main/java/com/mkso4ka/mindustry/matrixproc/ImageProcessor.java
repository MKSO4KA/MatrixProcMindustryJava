package com.mkso4ka.mindustry.matrixproc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Основной класс, реализующий оптимальный алгоритм разбиения изображения на прямоугольники.
 */
public class ImageProcessor {
    private final Pixmap pixmap;
    private final boolean[][] used;
    private final int width;
    private final int height;

    /**
     * Конструктор. Класс теперь работает как объект, хранящий свое состояние.
     * @param pixmap Изображение для обработки.
     */
    public ImageProcessor(Pixmap pixmap) {
        this.pixmap = pixmap;
        this.width = pixmap.getWidth();
        this.height = pixmap.getHeight();
        // Используем boolean массив вместо медленной и опасной статической карты
        this.used = new boolean[width][height];
    }

    /**
     * Основной метод, реализующий оптимальный алгоритм "Поиск и удаление максимального прямоугольника".
     * @return Карта, где ключ - цвет пикселя (Integer), а значение - список найденных прямоугольников.
     */
    public Map<Integer, List<Rect>> groupOptimal() {
        // Используем Integer для цвета вместо строки - это быстрее и правильнее.
        Map<Integer, List<Rect>> out = new HashMap<>();

        while (true) {
            Rect bestRect = null;
            int bestRectArea = -1;
            int bestRectColor = 0;

            // 1. Проходим по КАЖДОМУ пикселю, чтобы найти лучший следующий ход.
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Если пиксель еще не использован
                    if (!used[x][y]) {
                        int currentColor = pixmap.get(x, y);
                        // 2. Находим самый большой прямоугольник, который можно начать в этой точке.
                        Rect candidateRect = findLargestRectangleAt(x, y, currentColor);
                        int candidateArea = candidateRect.w * candidateRect.h;

                        // 3. Если этот кандидат лучше текущего лучшего (по площади), запоминаем его.
                        if (candidateArea > bestRectArea) {
                            bestRectArea = candidateArea;
                            bestRect = candidateRect;
                            bestRectColor = currentColor;
                        }
                    }
                }
            }

            // 4. Если лучший прямоугольник не найден, значит, все пиксели обработаны. Выходим.
            if (bestRect == null) {
                break;
            }

            // 5. Фиксируем лучший найденный прямоугольник: добавляем в результат и помечаем как использованный.
            out.computeIfAbsent(bestRectColor, k -> new ArrayList<>()).add(bestRect);
            markRect(bestRect);
        }

        return out;
    }

    /**
     * Находит самый большой прямоугольник заданного цвета, который можно начать в (startX, startY).
     */
    private Rect findLargestRectangleAt(int startX, int startY, int color) {
        int maxWidth = 0;
        // Находим максимально возможную ширину от стартовой точки
        for (int x = startX; x < width; x++) {
            if (pixmap.get(x, startY) == color && !used[x][startY]) {
                maxWidth++;
            } else {
                break;
            }
        }

        int bestArea = 0;
        Rect bestRect = new Rect(startX, startY, 0, 0);

        // Теперь пытаемся расширить эту полоску вниз, уменьшая ширину при необходимости
        for (int y = startY; y < height; y++) {
            int currentWidth = 0;
            for (int x = startX; x < startX + maxWidth; x++) {
                if (pixmap.get(x, y) == color && !used[x][y]) {
                    currentWidth++;
                } else {
                    break;
                }
            }
            // Если ширина на этой строке стала меньше, обновляем максимальную ширину
            maxWidth = Math.min(maxWidth, currentWidth);
            
            if (maxWidth == 0) {
                break;
            }

            int currentHeight = y - startY + 1;
            int currentArea = maxWidth * currentHeight;

            if (currentArea > bestArea) {
                bestArea = currentArea;
                bestRect = new Rect(startX, startY, maxWidth, currentHeight);
            }
        }
        return bestRect;
    }

    /**
     * Помечает пиксели внутри прямоугольника как использованные.
     */
    private void markRect(Rect rect) {
        for (int y = 0; y < rect.h; y++) {
            for (int x = 0; x < rect.w; x++) {
                used[rect.x + x][rect.y + y] = true;
            }
        }
    }

    // --- Главный метод для запуска и тестирования ---

    /**
     * Вспомогательный метод для форматирования цвета для вывода.
     */
    public static String formatColor(int pixel) {
        int r = (pixel >> 16) & 0xff;
        int g = (pixel >> 8) & 0xff;
        int b = (pixel) & 0xff;
        return String.format("draw color %d %d %d", r, g, b);
    }

    public static void main(String[] args) {
        try {
            // Укажите здесь правильный путь к вашему файлу
            File file = new File("/storage/emulated/0/1АA/python.png");
            if (!file.exists()) {
                System.out.println("Ошибка: Файл не найден по пути: " + file.getAbsolutePath());
                return;
            }
            BufferedImage bufferedImage = ImageIO.read(file);
            Pixmap pixmap = Pixmap.fromBufferedImage(bufferedImage);

            // Создаем экземпляр нашего процессора
            ImageProcessor processor = new ImageProcessor(pixmap);
            // Вызываем новый оптимальный метод
            Map<Integer, List<Rect>> result = processor.groupOptimal();

            System.out.println("Результат оптимального разбиения:");
            // Обработка результата
            for (Map.Entry<Integer, List<Rect>> entry : result.entrySet()) {
                String colorString = formatColor(entry.getKey());
                List<Rect> rects = entry.getValue();
                System.out.print("Цвет: " + colorString + ", Прямоугольники (" + rects.size() + " шт.): ");
                System.out.print("[");
                for (int i = 0; i < rects.size(); i++) {
                    Rect rect = rects.get(i);
                    System.out.print("{x:" + rect.x + ",y:" + rect.y + ",w:" + rect.w + ",h:" + rect.h + "}");
                    if (i < rects.size() - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.println("]");
            }
        } catch (IOException e) {
            System.out.println("Произошла ошибка при чтении файла изображения.");
            e.printStackTrace();
        }
    }
}