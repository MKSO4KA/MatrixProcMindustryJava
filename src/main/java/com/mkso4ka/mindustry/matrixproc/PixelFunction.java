package com.mkso4ka.mindustry.matrixproc;

/**
 * Функциональный интерфейс для итерации по пикселям.
 */
interface PixelFunction {
    void apply(int x, int y, int pixel);
}