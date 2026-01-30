package com.bhram.chess2;

import java.util.ArrayList;
import java.util.List;

public class ChessGame {
    private Piece[][] board;
    private Piece.Color currentPlayer;
    private boolean pieceSelected;
    private int selectedRow;
    private int selectedCol;
    private boolean gameOver;
    private String winner;

    public ChessGame() {
        board = new Piece[8][8];
        currentPlayer = Piece.Color.WHITE;
        pieceSelected = false;
        gameOver = false;
        winner = null;
        initializeBoard();
    }

    private void initializeBoard() {
        // Black pieces
        board[0][0] = new Piece(Piece.Color.BLACK, Piece.Type.ROOK);
        board[0][1] = new Piece(Piece.Color.BLACK, Piece.Type.KNIGHT);
        board[0][2] = new Piece(Piece.Color.BLACK, Piece.Type.BISHOP);
        board[0][3] = new Piece(Piece.Color.BLACK, Piece.Type.QUEEN);
        board[0][4] = new Piece(Piece.Color.BLACK, Piece.Type.KING);
        board[0][5] = new Piece(Piece.Color.BLACK, Piece.Type.BISHOP);
        board[0][6] = new Piece(Piece.Color.BLACK, Piece.Type.KNIGHT);
        board[0][7] = new Piece(Piece.Color.BLACK, Piece.Type.ROOK);
        
        for (int i = 0; i < 8; i++) {
            board[1][i] = new Piece(Piece.Color.BLACK, Piece.Type.PAWN);
        }

        // White pieces
        for (int i = 0; i < 8; i++) {
            board[6][i] = new Piece(Piece.Color.WHITE, Piece.Type.PAWN);
        }
        board[7][0] = new Piece(Piece.Color.WHITE, Piece.Type.ROOK);
        board[7][1] = new Piece(Piece.Color.WHITE, Piece.Type.KNIGHT);
        board[7][2] = new Piece(Piece.Color.WHITE, Piece.Type.BISHOP);
        board[7][3] = new Piece(Piece.Color.WHITE, Piece.Type.QUEEN);
        board[7][4] = new Piece(Piece.Color.WHITE, Piece.Type.KING);
        board[7][5] = new Piece(Piece.Color.WHITE, Piece.Type.BISHOP);
        board[7][6] = new Piece(Piece.Color.WHITE, Piece.Type.KNIGHT);
        board[7][7] = new Piece(Piece.Color.WHITE, Piece.Type.ROOK);
    }

    public Piece getPiece(int row, int col) {
        if (row < 0 || row >= 8 || col < 0 || col >= 8) {
            return null;
        }
        return board[row][col];
    }

    public Piece.Color getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isPieceSelected() {
        return pieceSelected;
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    public int getSelectedCol() {
        return selectedCol;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public String getWinner() {
        return winner;
    }

    public boolean selectPiece(int row, int col) {
        if (gameOver) {
            return false;
        }

        Piece piece = board[row][col];
        if (piece != null && piece.getColor() == currentPlayer) {
            selectedRow = row;
            selectedCol = col;
            pieceSelected = true;
            return true;
        }
        return false;
    }

    public boolean movePiece(int toRow, int toCol) {
        if (!pieceSelected || gameOver) {
            return false;
        }

        Piece piece = board[selectedRow][selectedCol];

        // Check for castling
        boolean isCastling = false;
        if (piece.getType() == Piece.Type.KING && Math.abs(toCol - selectedCol) == 2) {
            isCastling = true;
            boolean kingside = toCol > selectedCol;
            int row = selectedRow;
            int rookCol = kingside ? 7 : 0;
            int newRookCol = kingside ? 5 : 3;

            // Move rook
            Piece rook = board[row][rookCol];
            board[row][newRookCol] = rook;
            board[row][rookCol] = null;
            rook.setHasMoved(true);
        }

        if (isValidMove(selectedRow, selectedCol, toRow, toCol)) {
            Piece capturedPiece = board[toRow][toCol];
            if (capturedPiece != null && capturedPiece.getType() == Piece.Type.KING) {
                gameOver = true;
                winner = currentPlayer == Piece.Color.WHITE ? "White" : "Black";
            }

            // Move the piece
            board[toRow][toCol] = piece;
            board[selectedRow][selectedCol] = null;
            piece.setHasMoved(true);

            // Pawn promotion
            if (piece.getType() == Piece.Type.PAWN) {
                if ((piece.getColor() == Piece.Color.WHITE && toRow == 0) || 
                    (piece.getColor() == Piece.Color.BLACK && toRow == 7)) {
                    board[toRow][toCol] = new Piece(piece.getColor(), Piece.Type.QUEEN);
                }
            }

            // Switch player
            currentPlayer = currentPlayer == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE;
            pieceSelected = false;

            // Check for checkmate or stalemate
            if (isInCheckmate()) {
                gameOver = true;
                winner = currentPlayer == Piece.Color.WHITE ? "Black" : "White";
            } else if (isInStalemate()) {
                gameOver = true;
                winner = "Draw (Stalemate)";
            }

            return true;
        }
        return false;
    }

    public void deselectPiece() {
        pieceSelected = false;
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = board[fromRow][fromCol];
        Piece target = board[toRow][toCol];

        // Can't capture own piece
        if (target != null && target.getColor() == piece.getColor()) {
            return false;
        }

        // Can't move to same position
        if (fromRow == toRow && fromCol == toCol) {
            return false;
        }

        switch (piece.getType()) {
            case PAWN:
                return isValidPawnMove(fromRow, fromCol, toRow, toCol, piece);
            case ROOK:
                return isValidRookMove(fromRow, fromCol, toRow, toCol);
            case KNIGHT:
                return isValidKnightMove(fromRow, fromCol, toRow, toCol);
            case BISHOP:
                return isValidBishopMove(fromRow, fromCol, toRow, toCol);
            case QUEEN:
                return isValidQueenMove(fromRow, fromCol, toRow, toCol);
            case KING:
                // Check for castling
                if (Math.abs(toCol - fromCol) == 2 && fromRow == toRow) {
                    boolean kingside = toCol > fromCol;
                    return canCastle(piece.getColor(), kingside);
                }
                return isValidKingMove(fromRow, fromCol, toRow, toCol);
            default:
                return false;
        }
    }

    private boolean isValidPawnMove(int fromRow, int fromCol, int toRow, int toCol, Piece piece) {
        int direction = piece.getColor() == Piece.Color.WHITE ? -1 : 1;
        int startRow = piece.getColor() == Piece.Color.WHITE ? 6 : 1;

        // Move forward one square
        if (fromCol == toCol && toRow == fromRow + direction && board[toRow][toCol] == null) {
            return true;
        }

        // Move forward two squares from starting position
        if (fromCol == toCol && fromRow == startRow && toRow == fromRow + 2 * direction && 
            board[toRow][toCol] == null && board[fromRow + direction][fromCol] == null) {
            return true;
        }

        // Capture diagonally
        if (Math.abs(fromCol - toCol) == 1 && toRow == fromRow + direction) {
            Piece target = board[toRow][toCol];
            if (target != null && target.getColor() != piece.getColor()) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidRookMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow != toRow && fromCol != toCol) {
            return false;
        }
        return isPathClear(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidKnightMove(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }

    private boolean isValidBishopMove(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);
        if (rowDiff != colDiff) {
            return false;
        }
        return isPathClear(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidQueenMove(int fromRow, int fromCol, int toRow, int toCol) {
        return isValidRookMove(fromRow, fromCol, toRow, toCol) || isValidBishopMove(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidKingMove(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);
        return rowDiff <= 1 && colDiff <= 1;
    }

    private boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDirection = Integer.compare(toRow, fromRow);
        int colDirection = Integer.compare(toCol, fromCol);
        int currentRow = fromRow + rowDirection;
        int currentCol = fromCol + colDirection;

        while (currentRow != toRow || currentCol != toCol) {
            if (board[currentRow][currentCol] != null) {
                return false;
            }
            currentRow += rowDirection;
            currentCol += colDirection;
        }
        return true;
    }

    public List<int[]> getValidMoves(int row, int col) {
        List<int[]> validMoves = new ArrayList<>();
        Piece piece = board[row][col];

        if (piece == null) {
            return validMoves;
        }

        for (int toRow = 0; toRow < 8; toRow++) {
            for (int toCol = 0; toCol < 8; toCol++) {
                if (isValidMove(row, col, toRow, toCol)) {
                    // Check if move would leave king in check
                    if (!wouldBeInCheck(row, col, toRow, toCol, piece.getColor())) {
                        validMoves.add(new int[]{toRow, toCol});
                    }
                }
            }
        }
        return validMoves;
    }

    // Find king position for a given color
    private int[] findKing(Piece.Color color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getType() == Piece.Type.KING && piece.getColor() == color) {
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }

    // Check if a square is attacked by opponent pieces
    private boolean isSquareAttacked(int row, int col, Piece.Color byColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board[r][c];
                if (piece != null && piece.getColor() == byColor) {
                    if (canPieceAttack(r, c, row, col)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Check if a piece can attack a square (without considering check)
    private boolean canPieceAttack(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = board[fromRow][fromCol];
        Piece target = board[toRow][toCol];

        // Can't attack own piece
        if (target != null && target.getColor() == piece.getColor()) {
            return false;
        }

        switch (piece.getType()) {
            case PAWN:
                int direction = piece.getColor() == Piece.Color.WHITE ? -1 : 1;
                // Pawns attack diagonally
                return Math.abs(fromCol - toCol) == 1 && toRow == fromRow + direction;
            case ROOK:
                return isValidRookMove(fromRow, fromCol, toRow, toCol);
            case KNIGHT:
                return isValidKnightMove(fromRow, fromCol, toRow, toCol);
            case BISHOP:
                return isValidBishopMove(fromRow, fromCol, toRow, toCol);
            case QUEEN:
                return isValidQueenMove(fromRow, fromCol, toRow, toCol);
            case KING:
                int rowDiff = Math.abs(fromRow - toRow);
                int colDiff = Math.abs(fromCol - toCol);
                return rowDiff <= 1 && colDiff <= 1;
            default:
                return false;
        }
    }

    // Check if a move would leave the king in check
    private boolean wouldBeInCheck(int fromRow, int fromCol, int toRow, int toCol, Piece.Color color) {
        // Make temporary move
        Piece movingPiece = board[fromRow][fromCol];
        Piece capturedPiece = board[toRow][toCol];
        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;

        // Check if king is in check
        int[] kingPos = findKing(color);
        boolean inCheck = false;
        if (kingPos != null) {
            Piece.Color opponentColor = color == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE;
            inCheck = isSquareAttacked(kingPos[0], kingPos[1], opponentColor);
        }

        // Undo temporary move
        board[fromRow][fromCol] = movingPiece;
        board[toRow][toCol] = capturedPiece;

        return inCheck;
    }

    // Check if current player is in check
    public boolean isInCheck() {
        int[] kingPos = findKing(currentPlayer);
        if (kingPos == null) {
            return false;
        }
        Piece.Color opponentColor = currentPlayer == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE;
        return isSquareAttacked(kingPos[0], kingPos[1], opponentColor);
    }

    // Check if current player is in checkmate
    public boolean isInCheckmate() {
        if (!isInCheck()) {
            return false;
        }

        // Check if any piece can make a valid move
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getColor() == currentPlayer) {
                    List<int[]> validMoves = getValidMoves(row, col);
                    if (!validMoves.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // Check for stalemate
    public boolean isInStalemate() {
        if (isInCheck()) {
            return false;
        }

        // Check if any piece can make a valid move
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getColor() == currentPlayer) {
                    List<int[]> validMoves = getValidMoves(row, col);
                    if (!validMoves.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // Check if castling is possible
    private boolean canCastle(Piece.Color color, boolean kingside) {
        int[] kingPos = findKing(color);
        if (kingPos == null) {
            return false;
        }

        Piece king = board[kingPos[0]][kingPos[1]];
        if (king == null || king.hasMoved()) {
            return false;
        }

        int row = color == Piece.Color.WHITE ? 7 : 0;
        int rookCol = kingside ? 7 : 0;
        Piece rook = board[row][rookCol];

        if (rook == null || rook.getType() != Piece.Type.ROOK || rook.hasMoved()) {
            return false;
        }

        // Check if path is clear
        int startCol = kingside ? 5 : 1;
        int endCol = kingside ? 6 : 3;
        for (int col = startCol; col <= endCol; col++) {
            if (board[row][col] != null) {
                return false;
            }
        }

        // Check if king passes through or ends in check
        Piece.Color opponentColor = color == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE;
        for (int col = 4; col <= (kingside ? 6 : 2); col++) {
            if (isSquareAttacked(row, col, opponentColor)) {
                return false;
            }
        }

        return true;
    }

    // Set castling moves
    public List<int[]> getCastlingMoves(int row, int col) {
        List<int[]> castlingMoves = new ArrayList<>();
        Piece piece = board[row][col];

        if (piece == null || piece.getType() != Piece.Type.KING) {
            return castlingMoves;
        }

        if (canCastle(piece.getColor(), true)) {
            int toRow = row;
            int toCol = 6;
            castlingMoves.add(new int[]{toRow, toCol});
        }

        if (canCastle(piece.getColor(), false)) {
            int toRow = row;
            int toCol = 2;
            castlingMoves.add(new int[]{toRow, toCol});
        }

        return castlingMoves;
    }
}