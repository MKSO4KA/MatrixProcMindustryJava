package com.mkso4ka.mindustry.matrixproc;

import java.awt.image.BufferedImage;

/**
 * Вспомогательный класс для представления изображения в виде 2D-массива пикселей.
 */
class Pixmap {
    private final int width;
    private final int height;
    private final int[][] pixels;

    public Pixmap(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width][height];
    }

    public int get(int x, int y) {
        return pixels[x][y];
    }

    public void set(int x, int y, int color) {
        pixels[x][y] = color;
    }

    public void each(PixelFunction func) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                func.apply(x, y, pixels[x][y]);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static Pixmap fromBufferedImage(BufferedImage image) {
        Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight());
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                pixmap.set(x, y, image.getRGB(x, y));
            }
        }
        return pixmap;
    }
}