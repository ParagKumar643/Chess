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
    private List<MoveRecord> moveHistory;
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
        currentMoveIndex = -1; // Initialize to -1 (at current game state)
        navigationPlayer = Piece.Color.WHITE;
        isNavigating = false;
        initializeBoard();
    }

    private void initializeBoard() {
        // Clear the entire board first
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board[row][col] = null;
            }
        }
        
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
            MoveRecord moveRecord = new MoveRecord(selectedRow, selectedCol, toRow, toCol, 
                                                 piece, capturedPiece, isCastling, isEnPassant, 
                                                 false, currentPlayer);
            moveHistory.add(moveRecord);

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

            // Update navigation state - we're now at the current game state
            currentMoveIndex = -1;
            isNavigating = false;

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
        
        // Count how many times the current position appears in the move history
        // We need to reconstruct the board state for each move to check for repetition
        for (int i = 0; i < moveHistory.size(); i++) {
            // Create a temporary board to check the position at move i
            Piece[][] tempBoard = new Piece[8][8];
            // Copy current board state
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    tempBoard[row][col] = board[row][col];
                }
            }
            
            // Reconstruct board state at move i
            for (int j = 0; j <= i; j++) {
                MoveRecord record = moveHistory.get(j);
                executeMoveFromRecordForRepetitionCheck(tempBoard, record);
            }
            
            // Check if this position matches current position
            String positionAtMove = getBoardPositionForBoard(tempBoard);
            if (positionAtMove.equals(currentPosition)) {
                count++;
            }
        }

        return count >= 3;
    }
    
    // Helper method to get board position for a specific board state
    private String getBoardPositionForBoard(Piece[][] boardState) {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = boardState[row][col];
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
    
    // Helper method to execute move for repetition checking (doesn't modify actual game state)
    private void executeMoveFromRecordForRepetitionCheck(Piece[][] boardState, MoveRecord record) {
        int fromRow = record.getFromRow();
        int fromCol = record.getFromCol();
        int toRow = record.getToRow();
        int toCol = record.getToCol();
        Piece movingPiece = record.getMovingPiece();
        
        // Handle normal moves
        boardState[toRow][toCol] = movingPiece;
        boardState[fromRow][fromCol] = null;
        
        // Handle castling
        if (record.isCastling()) {
            int row = record.getPlayerColor() == Piece.Color.WHITE ? 7 : 0;
            boolean kingside = toCol > fromCol;
            
            if (kingside) {
                // Move rook from h-file to f-file
                Piece rook = boardState[row][7];
                boardState[row][5] = rook;
                boardState[row][7] = null;
            } else {
                // Move rook from a-file to d-file
                Piece rook = boardState[row][0];
                boardState[row][3] = rook;
                boardState[row][0] = null;
            }
        }
        
        // Handle en passant
        if (record.isEnPassant()) {
            int direction = (toRow - fromRow) > 0 ? 1 : -1;
            int capturedRow = fromRow + direction;
            int capturedCol = toCol;
            boardState[capturedRow][capturedCol] = null;
        }
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
        List<String> moveStrings = new ArrayList<>();
        for (MoveRecord record : moveHistory) {
            moveStrings.add(getMoveNotation(record.getFromRow(), record.getFromCol(), 
                                          record.getToRow(), record.getToCol(),
                                          record.getMovingPiece(), record.getCapturedPiece()));
        }
        return moveStrings;
    }
    
    // Navigation methods for move history
    private int currentMoveIndex = -1;
    private Piece.Color navigationPlayer;
    private boolean isNavigating = false;
    
    public boolean canGoBack() {
        // Can go back if:
        // 1. We're at current game state and there are moves (can go to position before last move)
        // 2. We're at a specific move and can go to previous move
        // 3. We're at the first move and can go to starting position
        // 4. We're NOT already at the starting position with no moves
        boolean canGoBack = (currentMoveIndex == -1 && moveHistory.size() > 0) || 
                           (currentMoveIndex > 0) || 
                           (currentMoveIndex == 0);
        
        // Additional check: if we're already at starting position with no moves, can't go back
        if (currentMoveIndex == -1 && moveHistory.size() == 0) {
            canGoBack = false;
        }
        
        System.out.println("DEBUG: canGoBack() - currentMoveIndex: " + currentMoveIndex + ", moveHistory.size(): " + moveHistory.size() + ", result: " + canGoBack);
        return canGoBack;
    }
    
    public boolean canGoForward() {
        // Can go forward if:
        // 1. We're at starting position and there are moves available
        // 2. We're at a specific move and can go to next move
        boolean canGoForward = (currentMoveIndex == -1 && moveHistory.size() > 0) || 
                              (currentMoveIndex >= 0 && currentMoveIndex < moveHistory.size() - 1);
        System.out.println("DEBUG: canGoForward() - currentMoveIndex: " + currentMoveIndex + ", moveHistory.size(): " + moveHistory.size() + ", result: " + canGoForward);
        return canGoForward;
    }
    
    public void goBack() {
        if (canGoBack()) {
            System.out.println("DEBUG: goBack() called - currentMoveIndex: " + currentMoveIndex + ", moveHistory.size(): " + moveHistory.size());
            
            // If we're at current game state and there are moves, go to the position before the last move
            if (currentMoveIndex == -1 && moveHistory.size() > 0) {
                currentMoveIndex = moveHistory.size() - 2; // Go to position before the last move
            } else if (currentMoveIndex > 0) {
                currentMoveIndex--;
            } else if (currentMoveIndex == 0) {
                // At the first move, go to the starting position (before any moves)
                currentMoveIndex = -1; // This represents the starting position
            } else {
                // Already at the starting position, can't go back further
                return;
            }
            
            // Restore board to the specified move
            if (currentMoveIndex >= 0) {
                restoreBoardToMove(currentMoveIndex);
                // Update navigation player based on move count
                navigationPlayer = (currentMoveIndex % 2 == 0) ? Piece.Color.WHITE : Piece.Color.BLACK;
            } else {
                // At starting position, it's White's turn
                initializeBoard();
                navigationPlayer = Piece.Color.WHITE;
            }
            
            isNavigating = true;
            
            System.out.println("DEBUG: goBack() finished - currentMoveIndex: " + currentMoveIndex + ", isNavigating: " + isNavigating);
        }
    }
    
    public void goForward() {
        if (canGoForward()) {
            System.out.println("DEBUG: goForward() called - currentMoveIndex: " + currentMoveIndex + ", moveHistory.size(): " + moveHistory.size());
            
            currentMoveIndex++;
            
            // Restore board to the specified move
            if (currentMoveIndex >= 0) {
                restoreBoardToMove(currentMoveIndex);
                // Update navigation player based on move count
                navigationPlayer = (currentMoveIndex % 2 == 0) ? Piece.Color.WHITE : Piece.Color.BLACK;
            } else {
                // At starting position, it's White's turn
                initializeBoard();
                navigationPlayer = Piece.Color.WHITE;
            }
            
            isNavigating = true;
            
            System.out.println("DEBUG: goForward() finished - currentMoveIndex: " + currentMoveIndex + ", isNavigating: " + isNavigating);
        }
    }
    
    // Debug method to check move history
    public String getMoveHistoryDebug() {
        StringBuilder sb = new StringBuilder();
        sb.append("Move history: ");
        
        // Show only moves up to current navigation point if navigating
        int maxMovesToShow = isNavigating ? (currentMoveIndex + 1) : moveHistory.size();
        
        for (int i = 0; i < maxMovesToShow; i++) {
            MoveRecord record = moveHistory.get(i);
            String move = getMoveNotation(record.getFromRow(), record.getFromCol(), 
                                        record.getToRow(), record.getToCol(),
                                        record.getMovingPiece(), record.getCapturedPiece());
            sb.append(i).append(":").append(move).append(" ");
        }
        
        // Add navigation info
        sb.append(" | Current index: ").append(currentMoveIndex);
        sb.append(" | Navigating: ").append(isNavigating);
        sb.append(" | Player: ").append(navigationPlayer);
        
        return sb.toString();
    }
    
    private void restoreBoardToMove(int moveIndex) {
        // Reset to initial position
        initializeBoard();
        
        // Debug: Print initial board state
        System.out.println("Initial board state:");
        printBoardState();
        
        // Replay moves up to the desired index
        for (int i = 0; i <= moveIndex; i++) {
            MoveRecord record = moveHistory.get(i);
            System.out.println("Replaying move " + i + ": " + getMoveNotation(record.getFromRow(), record.getFromCol(), 
                                          record.getToRow(), record.getToCol(),
                                          record.getMovingPiece(), record.getCapturedPiece()));
            executeMoveFromRecord(record);
        }
        
        // Update current player based on move count
        navigationPlayer = (moveIndex % 2 == 0) ? Piece.Color.WHITE : Piece.Color.BLACK;
        
        // Debug: Print the board state after restoration
        System.out.println("Restored board to move " + moveIndex);
        printBoardState();
    }
    
    private void printBoardState() {
        System.out.println("Current board state:");
        for (int row = 0; row < 8; row++) {
            StringBuilder rowStr = new StringBuilder();
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece == null) {
                    rowStr.append("- ");
                } else {
                    char type = ' ';
                    switch (piece.getType()) {
                        case PAWN: type = 'P'; break;
                        case ROOK: type = 'R'; break;
                        case KNIGHT: type = 'N'; break;
                        case BISHOP: type = 'B'; break;
                        case QUEEN: type = 'Q'; break;
                        case KING: type = 'K'; break;
                    }
                    char color = piece.getColor() == Piece.Color.WHITE ? 'W' : 'B';
                    rowStr.append(color).append(type).append(" ");
                }
            }
            System.out.println(rowStr.toString());
        }
    }
    
    // Get current player during navigation
    public Piece.Color getNavigationPlayer() {
        return navigationPlayer;
    }
    
    // Get current player (for normal gameplay)
    public Piece.Color getCurrentPlayerForUI() {
        return isNavigating ? navigationPlayer : currentPlayer;
    }
    
    public void exitNavigation() {
        if (isNavigating) {
            // Restore to current game state
            initializeBoard();
            currentMoveIndex = -1;
            
            // Replay all moves
            for (int i = 0; i < moveHistory.size(); i++) {
                MoveRecord record = moveHistory.get(i);
                executeMoveFromRecord(record);
            }
            
            // Restore game state
            currentPlayer = (moveHistory.size() % 2 == 0) ? Piece.Color.WHITE : Piece.Color.BLACK;
            gameOver = false;
            winner = null;
            isNavigating = false;
        }
    }
    
    public boolean isNavigating() {
        return isNavigating;
    }
    
    private void executeMoveFromRecord(MoveRecord record) {
        // Handle castling
        if (record.isCastling()) {
            handleCastlingMove(record);
        }
        // Handle en passant
        else if (record.isEnPassant()) {
            handleEnPassantMove(record);
        }
        // Handle normal moves
        else {
            handleNormalMove(record);
        }
    }
    
    // Navigate back to previous move (for viewing, not undoing)
    private void navigateToPreviousMove() {
        if (moveHistory.isEmpty()) {
            return;
        }
        
        // If we're at current game state, go to the last move
        if (currentMoveIndex == -1) {
            currentMoveIndex = moveHistory.size() - 1;
        } else if (currentMoveIndex > 0) {
            currentMoveIndex--;
        } else {
            // Already at the first move, can't go back further
            return;
        }
        
        // Restore board to the specified move
        restoreBoardToMove(currentMoveIndex);
        
        // Update navigation player based on move count
        navigationPlayer = (currentMoveIndex % 2 == 0) ? Piece.Color.WHITE : Piece.Color.BLACK;
        
        // Debug: Print the board state after navigation
        System.out.println("Board state after navigating to move " + currentMoveIndex + ":");
        printBoardState();
        System.out.println("Updated currentMoveIndex to: " + currentMoveIndex + " (moveHistory.size(): " + moveHistory.size() + ")");
    }
    
    private void undoNormalMove(MoveRecord record) {
        int fromRow = record.getFromRow();
        int fromCol = record.getFromCol();
        int toRow = record.getToRow();
        int toCol = record.getToCol();
        Piece movingPiece = record.getMovingPiece();
        Piece capturedPiece = record.getCapturedPiece();
        
        // Move the piece back to its original position
        board[fromRow][fromCol] = movingPiece;
        board[toRow][toCol] = null;
        movingPiece.setHasMoved(false);
        
        // Restore captured piece if there was one
        if (capturedPiece != null) {
            board[toRow][toCol] = capturedPiece;
        }
        
        // Debug: Print the board state after undo
        System.out.println("Board state after undoing normal move:");
        printBoardState();
    }
    
    private void undoCastlingMove(MoveRecord record) {
        int row = record.getPlayerColor() == Piece.Color.WHITE ? 7 : 0;
        int fromRow = record.getFromRow();
        int fromCol = record.getFromCol();
        int toRow = record.getToRow();
        int toCol = record.getToCol();
        
        // Determine if kingside or queenside
        boolean kingside = toCol > fromCol;
        
        if (kingside) {
            // Undo kingside castling
            // Move king back from g-file to e-file
            Piece king = board[row][6];
            board[row][4] = king;
            board[row][6] = null;
            king.setHasMoved(false);
            
            // Move rook back from f-file to h-file
            Piece rook = board[row][5];
            board[row][7] = rook;
            board[row][5] = null;
            rook.setHasMoved(false);
        } else {
            // Undo queenside castling
            // Move king back from c-file to e-file
            Piece king = board[row][2];
            board[row][4] = king;
            board[row][2] = null;
            king.setHasMoved(false);
            
            // Move rook back from d-file to a-file
            Piece rook = board[row][3];
            board[row][0] = rook;
            board[row][3] = null;
            rook.setHasMoved(false);
        }
        
        // Debug: Print the board state after undo
        System.out.println("Board state after undoing castling move:");
        printBoardState();
    }
    
    private void undoEnPassantMove(MoveRecord record) {
        int fromRow = record.getFromRow();
        int fromCol = record.getFromCol();
        int toRow = record.getToRow();
        int toCol = record.getToCol();
        Piece movingPiece = record.getMovingPiece();
        
        // Move the pawn back to its original position
        board[fromRow][fromCol] = movingPiece;
        board[toRow][toCol] = null;
        movingPiece.setHasMoved(false);
        
        // Restore the captured pawn (en passant)
        int direction = (toRow - fromRow) > 0 ? 1 : -1;
        int capturedRow = fromRow + direction;
        int capturedCol = toCol;
        Piece capturedPawn = new Piece(record.getPlayerColor() == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE, Piece.Type.PAWN);
        board[capturedRow][capturedCol] = capturedPawn;
        
        // Debug: Print the board state after undo
        System.out.println("Board state after undoing en passant move:");
        printBoardState();
    }
    
    private void handleNormalMove(MoveRecord record) {
        int fromRow = record.getFromRow();
        int fromCol = record.getFromCol();
        int toRow = record.getToRow();
        int toCol = record.getToCol();
        Piece movingPiece = record.getMovingPiece();
        Piece capturedPiece = record.getCapturedPiece();
        
        // Clear the destination square first to prevent duplicates
        board[toRow][toCol] = null;
        
        // Move the piece
        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;
        movingPiece.setHasMoved(true);
        
        // Debug: Print the board state after the move
        System.out.println("Board state after normal move:");
        printBoardState();
    }
    
    private void handleCastlingMove(MoveRecord record) {
        int row = record.getPlayerColor() == Piece.Color.WHITE ? 7 : 0;
        int fromRow = record.getFromRow();
        int fromCol = record.getFromCol();
        int toRow = record.getToRow();
        int toCol = record.getToCol();
        
        // Determine if kingside or queenside
        boolean kingside = toCol > fromCol;
        
        if (kingside) {
            // Kingside castling
            // Move king from e-file to g-file
            Piece king = board[row][4];
            board[row][6] = king;
            board[row][4] = null;
            king.setHasMoved(true);
            
            // Move rook from h-file to f-file
            Piece rook = board[row][7];
            board[row][5] = rook;
            board[row][7] = null;
            rook.setHasMoved(true);
        } else {
            // Queenside castling
            // Move king from e-file to c-file
            Piece king = board[row][4];
            board[row][2] = king;
            board[row][4] = null;
            king.setHasMoved(true);
            
            // Move rook from a-file to d-file
            Piece rook = board[row][0];
            board[row][3] = rook;
            board[row][0] = null;
            rook.setHasMoved(true);
        }
        
        // Debug: Print the board state after the move
        System.out.println("Board state after castling move:");
        printBoardState();
    }
    
    private void handleEnPassantMove(MoveRecord record) {
        int fromRow = record.getFromRow();
        int fromCol = record.getFromCol();
        int toRow = record.getToRow();
        int toCol = record.getToCol();
        Piece movingPiece = record.getMovingPiece();
        
        // Move the pawn
        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;
        movingPiece.setHasMoved(true);
        
        // Remove the captured pawn (en passant)
        int direction = (toRow - fromRow) > 0 ? 1 : -1;
        int capturedRow = fromRow + direction;
        int capturedCol = toCol;
        board[capturedRow][capturedCol] = null;
        
        // Debug: Print the board state after the move
        System.out.println("Board state after en passant move:");
        printBoardState();
    }
    
    private void executeMoveFromNotation(String move) {
        // Simple move notation parser (e.g., "e2e4", "Nf3", "O-O")
        if (move.equals("O-O") || move.equals("O-O-O")) {
            // Handle castling
            handleCastlingNotation(move);
        } else if (move.length() == 4 || move.length() == 5) {
            // Handle standard moves (e.g., "e2e4", "Nf3x")
            parseStandardMove(move);
        }
    }
    
    private void handleCastlingNotation(String move) {
        int row = currentPlayer == Piece.Color.WHITE ? 7 : 0;
        
        if (move.equals("O-O")) {
            // Kingside castling
            // Move king from e-file to g-file
            Piece king = board[row][4];
            board[row][6] = king;
            board[row][4] = null;
            king.setHasMoved(true);
            
            // Move rook from h-file to f-file
            Piece rook = board[row][7];
            board[row][5] = rook;
            board[row][7] = null;
            rook.setHasMoved(true);
        } else {
            // Queenside castling
            // Move king from e-file to c-file
            Piece king = board[row][4];
            board[row][2] = king;
            board[row][4] = null;
            king.setHasMoved(true);
            
            // Move rook from a-file to d-file
            Piece rook = board[row][0];
            board[row][3] = rook;
            board[row][0] = null;
            rook.setHasMoved(true);
        }
    }
    
    private void parseStandardMove(String move) {
        // Extract source and destination squares
        char fromFile = move.charAt(0);
        char fromRank = move.charAt(1);
        char toFile = move.charAt(2);
        char toRank = move.charAt(3);
        
        int fromCol = fromFile - 'a';
        int fromRow = 8 - Character.getNumericValue(fromRank);
        int toCol = toFile - 'a';
        int toRow = 8 - Character.getNumericValue(toRank);
        
        // Debug: Print the move being parsed
        System.out.println("Parsing move: " + move + " from (" + fromRow + "," + fromCol + ") to (" + toRow + "," + toCol + ")");
        
        // Move the piece
        Piece piece = board[fromRow][fromCol];
        if (piece != null) {
            // Debug: Print what piece is being moved
            System.out.println("Moving piece: " + piece.getColor() + " " + piece.getType());
            
            // Clear the destination square first to prevent duplicates
            board[toRow][toCol] = null;
            
            // Move the piece
            board[toRow][toCol] = piece;
            board[fromRow][fromCol] = null;
            piece.setHasMoved(true);
            
            // Debug: Print the board state after the move
            System.out.println("Board state after move:");
            printBoardState();
        } else {
            System.out.println("ERROR: No piece found at source position (" + fromRow + "," + fromCol + ")");
        }
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