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
    private List<String> moveHistory;
    private int halfMoveClock;
    private int fullMoveNumber;
    private int lastDoublePawnMoveRow;
    private int lastDoublePawnMoveCol;

    public ChessGame() {
        board = new Piece[8][8];
        currentPlayer = Piece.Color.WHITE;
        pieceSelected = false;
        gameOver = false;
        winner = null;
        moveHistory = new ArrayList<>();
        halfMoveClock = 0;
        fullMoveNumber = 1;
        lastDoublePawnMoveRow = -1;
        lastDoublePawnMoveCol = -1;
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
    
    public void setWinner(String winner) {
        this.winner = winner;
    }
    
    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
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
            boolean kingside = toCol > selectedCol;
            isCastling = canCastle(piece.getColor(), kingside);
        }

        // Check for en passant
        boolean isEnPassant = false;
        if (piece.getType() == Piece.Type.PAWN && Math.abs(selectedCol - toCol) == 1 && board[toRow][toCol] == null) {
            isEnPassant = canEnPassant(selectedRow, selectedCol, toRow, toCol);
        }

        // Get valid moves for this piece (this includes check validation)
        List<int[]> validMoves = getValidMoves(selectedRow, selectedCol);
        
        // Check if the requested move is in the list of valid moves
        boolean isValidMove = false;
        for (int[] move : validMoves) {
            if (move[0] == toRow && move[1] == toCol) {
                isValidMove = true;
                break;
            }
        }

        if (isValidMove) {
            // Execute castling if valid
            if (isCastling) {
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
            Piece capturedPiece = board[toRow][toCol];
            
            // Handle en passant capture
            if (isEnPassant) {
                executeEnPassant(selectedRow, selectedCol, toRow, toCol);
                // The captured piece is the pawn that was captured en passant
                capturedPiece = new Piece(currentPlayer == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE, Piece.Type.PAWN);
            }

            // Handle castling - move the king to the correct position
            if (isCastling) {
                boolean kingside = toCol > selectedCol;
                int kingToCol = kingside ? 6 : 2;
                board[selectedRow][kingToCol] = piece;
                board[selectedRow][selectedCol] = null;
                piece.setHasMoved(true);
            } else {
                // Normal move
                if (capturedPiece != null && capturedPiece.getType() == Piece.Type.KING) {
                    gameOver = true;
                    winner = currentPlayer == Piece.Color.WHITE ? "White" : "Black";
                }

                // Move the piece
                board[toRow][toCol] = piece;
                board[selectedRow][selectedCol] = null;
                piece.setHasMoved(true);
            }

            // Track double pawn moves for en passant
            if (piece.getType() == Piece.Type.PAWN && Math.abs(toRow - selectedRow) == 2) {
                // Track the position of the pawn that moved 2 squares (the starting position)
                lastDoublePawnMoveRow = selectedRow;
                lastDoublePawnMoveCol = selectedCol;
            } else {
                lastDoublePawnMoveRow = -1;
                lastDoublePawnMoveCol = -1;
            }

            // Update half-move clock
            if (piece.getType() == Piece.Type.PAWN || capturedPiece != null) {
                halfMoveClock = 0;
            } else {
                halfMoveClock++;
            }

            // Update move history
            String move = getMoveNotation(selectedRow, selectedCol, toRow, toCol, piece, capturedPiece);
            moveHistory.add(move);

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

            // Increment full move number after black's move
            if (currentPlayer == Piece.Color.WHITE) {
                fullMoveNumber++;
            }

            // Check for game end conditions
            if (isInCheckmate()) {
                gameOver = true;
                winner = currentPlayer == Piece.Color.WHITE ? "Black" : "White";
            } else if (isDraw()) {
                gameOver = true;
                winner = "Draw";
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

        // En passant capture
        if (Math.abs(fromCol - toCol) == 1 && toRow == fromRow + direction) {
            Piece target = board[toRow][toCol];
            if (target == null) {
                // Check if en passant is possible
                return canEnPassant(fromRow, fromCol, toRow, toCol);
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

        // If king is in check, only allow moves that get out of check
        boolean inCheck = isInCheck();
        
        for (int toRow = 0; toRow < 8; toRow++) {
            for (int toCol = 0; toCol < 8; toCol++) {
                if (isValidMove(row, col, toRow, toCol)) {
                    // Check if move would leave king in check
                    if (!wouldBeInCheck(row, col, toRow, toCol, piece.getColor())) {
                        // If king is in check, only allow moves that actually get out of check
                        if (inCheck) {
                            // Make the temporary move to test if it gets out of check
                            Piece movingPiece = board[row][col];
                            Piece capturedPiece = board[toRow][toCol];
                            board[toRow][toCol] = movingPiece;
                            board[row][col] = null;
                            
                            // Check if king is still in check after the move
                            boolean stillInCheck = isInCheck();
                            
                            // Undo the temporary move
                            board[row][col] = movingPiece;
                            board[toRow][toCol] = capturedPiece;
                            
                            // Only add the move if it gets the king out of check
                            if (!stillInCheck) {
                                validMoves.add(new int[]{toRow, toCol});
                            }
                        } else {
                            // If not in check, normal validation applies
                            validMoves.add(new int[]{toRow, toCol});
                        }
                    }
                }
            }
        }
        
        // Add en passant moves for pawns
        if (piece.getType() == Piece.Type.PAWN) {
            int direction = piece.getColor() == Piece.Color.WHITE ? -1 : 1;
            
            // Check left en passant
            if (col > 0) {
                int toRow = row + direction;
                int toCol = col - 1;
                if (canEnPassant(row, col, toRow, toCol)) {
                    // Check if en passant move would leave king in check
                    if (!wouldBeInCheck(row, col, toRow, toCol, piece.getColor())) {
                        // If king is in check, only allow moves that actually get out of check
                        if (inCheck) {
                            // Make the temporary move to test if it gets out of check
                            Piece movingPiece = board[row][col];
                            Piece capturedPiece = board[toRow][toCol];
                            board[toRow][toCol] = movingPiece;
                            board[row][col] = null;
                            
                            // For en passant, we need to temporarily remove the captured pawn
                            int capturedRow = row + direction;
                            Piece enPassantCaptured = board[capturedRow][toCol];
                            board[capturedRow][toCol] = null;
                            
                            // Check if king is still in check after the move
                            boolean stillInCheck = isInCheck();
                            
                            // Undo the temporary move
                            board[row][col] = movingPiece;
                            board[toRow][toCol] = capturedPiece;
                            board[capturedRow][toCol] = enPassantCaptured;
                            
                            // Only add the move if it gets the king out of check
                            if (!stillInCheck) {
                                validMoves.add(new int[]{toRow, toCol});
                            }
                        } else {
                            // If not in check, normal validation applies
                            validMoves.add(new int[]{toRow, toCol});
                        }
                    }
                }
            }
            
            // Check right en passant
            if (col < 7) {
                int toRow = row + direction;
                int toCol = col + 1;
                if (canEnPassant(row, col, toRow, toCol)) {
                    // Check if en passant move would leave king in check
                    if (!wouldBeInCheck(row, col, toRow, toCol, piece.getColor())) {
                        // If king is in check, only allow moves that actually get out of check
                        if (inCheck) {
                            // Make the temporary move to test if it gets out of check
                            Piece movingPiece = board[row][col];
                            Piece capturedPiece = board[toRow][toCol];
                            board[toRow][toCol] = movingPiece;
                            board[row][col] = null;
                            
                            // For en passant, we need to temporarily remove the captured pawn
                            int capturedRow = row + direction;
                            Piece enPassantCaptured = board[capturedRow][toCol];
                            board[capturedRow][toCol] = null;
                            
                            // Check if king is still in check after the move
                            boolean stillInCheck = isInCheck();
                            
                            // Undo the temporary move
                            board[row][col] = movingPiece;
                            board[toRow][toCol] = capturedPiece;
                            board[capturedRow][toCol] = enPassantCaptured;
                            
                            // Only add the move if it gets the king out of check
                            if (!stillInCheck) {
                                validMoves.add(new int[]{toRow, toCol});
                            }
                        } else {
                            // If not in check, normal validation applies
                            validMoves.add(new int[]{toRow, toCol});
                        }
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

    // Check if en passant is possible
    private boolean canEnPassant(int fromRow, int fromCol, int toRow, int toCol) {
        // Check if this is a pawn capture move
        if (Math.abs(fromCol - toCol) != 1 || Math.abs(fromRow - toRow) != 1) {
            return false;
        }

        // Check if there's a pawn that just moved two squares
        int direction = (toRow - fromRow) > 0 ? 1 : -1;
        int adjacentRow = fromRow + direction;
        int adjacentCol = toCol;
        
        if (adjacentRow < 0 || adjacentRow >= 8 || adjacentCol < 0 || adjacentCol >= 8) {
            return false;
        }

        Piece adjacentPiece = board[adjacentRow][adjacentCol];
        if (adjacentPiece == null || adjacentPiece.getType() != Piece.Type.PAWN) {
            return false;
        }

        // Check if this pawn just moved two squares in the last move
        // The lastDoublePawnMoveRow/Col tracks the starting position of the double move
        // We need to check if the adjacent pawn is the one that moved 2 squares
        // The adjacent pawn should be at the destination of the double move
        int expectedStartRow = adjacentRow - (2 * direction);
        if (expectedStartRow >= 0 && expectedStartRow < 8) {
            if (lastDoublePawnMoveRow == expectedStartRow && lastDoublePawnMoveCol == adjacentCol) {
                return true;
            }
        }

        return false;
    }

    // Execute en passant capture
    private void executeEnPassant(int fromRow, int fromCol, int toRow, int toCol) {
        int direction = (toRow - fromRow) > 0 ? 1 : -1;
        int capturedRow = fromRow + direction;
        int capturedCol = toCol;

        // Remove the captured pawn
        board[capturedRow][capturedCol] = null;
    }

    // Check for threefold repetition
    public boolean isThreefoldRepetition() {
        if (moveHistory.size() < 8) {
            return false;
        }

        // Count occurrences of current board position
        String currentPosition = getBoardPosition();
        int count = 0;
        
        for (String position : moveHistory) {
            if (position.equals(currentPosition)) {
                count++;
            }
        }

        return count >= 3;
    }

    // Check for fifty-move rule
    public boolean isFiftyMoveRule() {
        return halfMoveClock >= 100; // 50 moves by each player = 100 half-moves
    }

    // Check for insufficient material
    public boolean isInsufficientMaterial() {
        int whitePieces = 0;
        int blackPieces = 0;
        boolean whiteHasBishop = false;
        boolean blackHasBishop = false;
        boolean whiteHasKnight = false;
        boolean blackHasKnight = false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null) {
                    switch (piece.getType()) {
                        case PAWN:
                        case ROOK:
                        case QUEEN:
                            return false; // Game not drawn
                        case BISHOP:
                            if (piece.getColor() == Piece.Color.WHITE) {
                                whiteHasBishop = true;
                            } else {
                                blackHasBishop = true;
                            }
                            break;
                        case KNIGHT:
                            if (piece.getColor() == Piece.Color.WHITE) {
                                whiteHasKnight = true;
                            } else {
                                blackHasKnight = true;
                            }
                            break;
                    }
                }
            }
        }

        // Check for insufficient material combinations
        // King vs King
        if (!whiteHasBishop && !whiteHasKnight && !blackHasBishop && !blackHasKnight) {
            return true;
        }
        
        // King + Bishop vs King
        if ((whiteHasBishop && !whiteHasKnight && !blackHasBishop && !blackHasKnight) ||
            (!whiteHasBishop && !whiteHasKnight && blackHasBishop && !blackHasKnight)) {
            return true;
        }
        
        // King + Knight vs King
        if ((whiteHasKnight && !whiteHasBishop && !blackHasBishop && !blackHasKnight) ||
            (!whiteHasBishop && !whiteHasKnight && blackHasKnight && !blackHasBishop)) {
            return true;
        }

        return false;
    }

    // Get board position for repetition checking
    private String getBoardPosition() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece == null) {
                    sb.append("-");
                } else {
                    sb.append(piece.getColor() == Piece.Color.WHITE ? "W" : "B");
                    switch (piece.getType()) {
                        case PAWN: sb.append("P"); break;
                        case ROOK: sb.append("R"); break;
                        case KNIGHT: sb.append("N"); break;
                        case BISHOP: sb.append("B"); break;
                        case QUEEN: sb.append("Q"); break;
                        case KING: sb.append("K"); break;
                    }
                }
            }
        }
        return sb.toString();
    }

    // Check if game is drawn
    public boolean isDraw() {
        return isThreefoldRepetition() || isFiftyMoveRule() || isInsufficientMaterial() || isInStalemate();
    }

    // Get move notation for move history
    private String getMoveNotation(int fromRow, int fromCol, int toRow, int toCol, Piece piece, Piece capturedPiece) {
        StringBuilder notation = new StringBuilder();
        
        // Add piece type (except for pawns)
        if (piece.getType() != Piece.Type.PAWN) {
            switch (piece.getType()) {
                case ROOK: notation.append("R"); break;
                case KNIGHT: notation.append("N"); break;
                case BISHOP: notation.append("B"); break;
                case QUEEN: notation.append("Q"); break;
                case KING: notation.append("K"); break;
            }
        }
        
        // Add source column (for disambiguation if needed)
        char fromFile = (char) ('a' + fromCol);
        notation.append(fromFile);
        
        // Add capture marker if capturing
        if (capturedPiece != null) {
            notation.append("x");
        }
        
        // Add destination square
        char toFile = (char) ('a' + toCol);
        int toRank = 8 - toRow;
        notation.append(toFile).append(toRank);
        
        return notation.toString();
    }

    // Get game status
    public String getGameStatus() {
        if (gameOver) {
            if (winner.equals("Draw")) {
                return "Game ended in a draw";
            } else {
                return "Game over - " + winner + " wins";
            }
        } else if (isInCheck()) {
            return currentPlayer + " is in check";
        } else {
            return currentPlayer + "'s turn";
        }
    }

    // Reset game
    public void resetGame() {
        board = new Piece[8][8];
        currentPlayer = Piece.Color.WHITE;
        pieceSelected = false;
        gameOver = false;
        winner = null;
        moveHistory.clear();
        halfMoveClock = 0;
        fullMoveNumber = 1;
        lastDoublePawnMoveRow = -1;
        lastDoublePawnMoveCol = -1;
        initializeBoard();
    }

    // Get move history
    public List<String> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    // Get current move number
    public int getCurrentMoveNumber() {
        return fullMoveNumber;
    }

    // Check if a square is under attack
    public boolean isSquareUnderAttack(int row, int col, Piece.Color attackerColor) {
        return isSquareAttacked(row, col, attackerColor);
    }

    // Get number of half-moves since last capture or pawn move
    public int getHalfMoveClock() {
        return halfMoveClock;
    }

    // Check if a piece can move to a specific square (considering check)
    public boolean canPieceMoveTo(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = board[fromRow][fromCol];
        if (piece == null) {
            return false;
        }

        if (!isValidMove(fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        // Check if move would leave king in check
        return !wouldBeInCheck(fromRow, fromCol, toRow, toCol, piece.getColor());
    }
    
    // Check if a move is a capture
    public boolean isCaptureMove(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = board[fromRow][fromCol];
        Piece target = board[toRow][toCol];
        return piece != null && target != null && target.getColor() != piece.getColor();
    }
    
    // Check if a move is en passant capture
    public boolean isEnPassantCapture(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = board[fromRow][fromCol];
        if (piece == null || piece.getType() != Piece.Type.PAWN) {
            return false;
        }
        return canEnPassant(fromRow, fromCol, toRow, toCol);
    }
    
    // Check if the game ended in checkmate
    public boolean isCheckmate() {
        return gameOver && winner != null && !winner.equals("Draw") && !winner.contains("time");
    }
}