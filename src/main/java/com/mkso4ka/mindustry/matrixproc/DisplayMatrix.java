package com.mkso4ka.mindustry.matrixproc;

class DisplayMatrix {
    public DisplayMatrix() {}

    public MatrixBlueprint placeDisplaysXxY(int x, int y, int displaySize, double processorReach) {
        int border = (int) Math.ceil(processorReach) + 1;
        int spacing = 0;

        int n = border * 2 + y * displaySize + Math.max(0, y - 1) * spacing;
        int m = border * 2 + x * displaySize + Math.max(0, x - 1) * spacing;

        int[][] centers = new int[y * x][2];
        int startOffset = border;
        int count = 0;

        for (int i = 0; i < y; i++) {
            for (int j = 0; j < x; j++) {
                int topLeftX = startOffset + i * displaySize;
                int topLeftY = startOffset + j * displaySize;
                centers[count][0] = topLeftX + displaySize / 2;
                centers[count][1] = topLeftY + displaySize / 2;
                count++;
            }
        }
        return new MatrixBlueprint(n, m, centers);
    }
}