package com.mkso4ka.mindustry.matrixproc;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ImageUtils {

    /**
     * Изменяет размер изображения до заданных ширины и высоты.
     */
    public static BufferedImage resize(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resizedImage;
    }

    /**
     * Добавляет отступ (контур) вокруг изображения.
     * @param image Исходное изображение.
     * @param padding Размер отступа в пикселях.
     * @return Новое изображение с отступом.
     */
    public static BufferedImage addContour(BufferedImage image, int padding) {
        int newWidth = image.getWidth() + padding * 2;
        int newHeight = image.getHeight() + padding * 2;
        BufferedImage newImage = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g = newImage.createGraphics();
        // Рисуем исходное изображение в центре нового, большего изображения
        g.drawImage(image, padding, padding, null);
        g.dispose();
        return newImage;
    }
}