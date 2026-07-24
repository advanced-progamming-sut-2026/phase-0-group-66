package model;

final class BoardLayoutAnalyzer {
    private BoardLayoutAnalyzer() {
    }

    static boolean isHorizontallySymmetric(Board board) {
        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols() / 2; col++) {
                String left = plantNameAt(board, row, col);
                String right = plantNameAt(board, row, board.getCols() - 1 - col);
                if (!left.equals(right)) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean isStrictlyAsymmetricExceptMiddleRow(Board board) {
        boolean hasPlantOutsideMiddleRow = false;
        for (int row = 0; row < board.getRows() / 2; row++) {
            int mirroredRow = board.getRows() - 1 - row;
            for (int col = 0; col < board.getCols(); col++) {
                String upper = plantStackSignatureAt(board, row, col);
                String lower = plantStackSignatureAt(board, mirroredRow, col);
                boolean upperOccupied = !isEmptySignature(upper);
                boolean lowerOccupied = !isEmptySignature(lower);
                hasPlantOutsideMiddleRow |= upperOccupied || lowerOccupied;
                if (upperOccupied && lowerOccupied && upper.equals(lower)) {
                    return false;
                }
            }
        }
        return hasPlantOutsideMiddleRow;
    }

    static boolean hasEmptyRow(Board board) {
        for (int row = 0; row < board.getRows(); row++) {
            if (isRowEmpty(board, row)) {
                return true;
            }
        }
        return false;
    }

    static boolean isRowEmpty(Board board, int row) {
        for (int col = 0; col < board.getCols(); col++) {
            if (board.getTile(row, col).getPlant() != null) {
                return false;
            }
        }
        return true;
    }

    static boolean hasEmptyColumn(Board board) {
        for (int col = 0; col < board.getCols(); col++) {
            if (isColumnEmpty(board, col)) {
                return true;
            }
        }
        return false;
    }

    static boolean isColumnEmpty(Board board, int col) {
        for (int row = 0; row < board.getRows(); row++) {
            if (board.getTile(row, col).getPlant() != null) {
                return false;
            }
        }
        return true;
    }

    private static String plantNameAt(Board board, int row, int col) {
        Plant plant = board.getTile(row, col).getPlant();
        return plant == null ? "" : plant.getName();
    }

    private static String plantStackSignatureAt(Board board, int row, int col) {
        Tile tile = board.getTile(row, col);
        return plantName(tile.getSupportPlant()) + "|"
            + plantName(tile.getMainPlant()) + "|" + plantName(tile.getCoverPlant());
    }

    private static boolean isEmptySignature(String signature) {
        return "||".equals(signature);
    }

    private static String plantName(Plant plant) {
        return plant == null ? "" : plant.getName();
    }
}
