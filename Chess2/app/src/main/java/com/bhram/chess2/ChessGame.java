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

        // Add bounds checking
        if (row < 0 || row >= 8 || col < 0 || col >= 8) {
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
        
        // Safety check: ensure the selected piece still exists
        if (piece == null) {
            pieceSelected = false;
            return false;
        }

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

    // Pawn promotion - check if pawn reached the end
            if (piece.getType() == Piece.Type.PAWN) {
                boolean isPromotion = (piece.getColor() == Piece.Color.WHITE && toRow == 0) || 
                                     (piece.getColor() == Piece.Color.BLACK && toRow == 7);
                
                if (isPromotion) {
                    // Set promotion state
                    isPromoting = true;
                    promotionRow = toRow;
                    promotionCol = toCol;
                    promotingPlayer = piece.getColor();
                    
                    // Don't switch player yet - wait for promotion to be completed
                    // The activity will handle the promotion and then call setPromotionComplete
                    return true;
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
            if (ChessRules.isInCheckmate(board, currentPlayer)) {
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

        // Use ChessRules class for piece movement validation
        if (!ChessRules.isValidPieceMove(board, piece, fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        // Check if move puts own king in check
        if (ChessRules.wouldPutKingInCheck(board, fromRow, fromCol, toRow, toCol, piece.getColor() == Piece.Color.WHITE)) {
            return false;
        }

        return true;
    }


    public List<int[]> getValidMoves(int row, int col) {
        return ChessRules.getValidMoves(board, row, col, currentPlayer == Piece.Color.WHITE);
    }


    // Check if castling is possible
    private boolean canCastle(Piece.Color color, boolean kingside) {
        return ChessRules.canCastle(board, color, kingside);
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
        return ChessRules.canEnPassant(board, fromRow, fromCol, toRow, toCol, lastDoublePawnMoveRow, lastDoublePawnMoveCol);
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
        // Need at least 8 moves (4 full moves) to have a chance of repetition
        if (moveHistory.size() < 8) {
            return false;
        }

        // Count occurrences of current board position
        String currentPosition = getBoardPosition();
        int count = 1; // Count current position as 1
        
        // Count how many times the current position appears in the move history
        // We need to reconstruct the board state for each move to check for repetition
        for (int i = 0; i < moveHistory.size() - 1; i++) { // Exclude current move
            // Create a temporary board to check the position at move i
            Piece[][] tempBoard = new Piece[8][8];
            // Start with initial board state
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    tempBoard[row][col] = null;
                }
            }
            
            // Reconstruct board state at move i by replaying moves from the beginning
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

        // Debug: Print repetition count
        System.out.println("DEBUG: Threefold repetition check - count: " + count + ", threshold: 3");
        
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
        // Debug: Print current game state
        System.out.println("DEBUG: isDraw() called - currentPlayer: " + currentPlayer + ", moveHistory.size(): " + moveHistory.size());
        
        // Check for threefold repetition
        boolean isThreefold = isThreefoldRepetition();
        if (isThreefold) {
            System.out.println("DEBUG: Threefold repetition detected");
        }
        
        // Check for fifty-move rule
        boolean isFiftyMove = isFiftyMoveRule();
        if (isFiftyMove) {
            System.out.println("DEBUG: Fifty-move rule detected - halfMoveClock: " + halfMoveClock);
        }
        
        // Check for insufficient material
        boolean isInsufficient = isInsufficientMaterial();
        if (isInsufficient) {
            System.out.println("DEBUG: Insufficient material detected");
        }
        
        // Check for stalemate of the opponent (the player whose turn it is now)
        // After a move, currentPlayer has been switched to the opponent
        // But we need to check if the opponent (currentPlayer) has any legal moves
        // This should only trigger if the opponent is in stalemate (no legal moves and not in check)
        boolean isInStalemate = ChessRules.isInStalemate(board, currentPlayer);
        
        // Additional safety check: only trigger stalemate if the player has actually moved
        // and there are no legal moves available
        boolean hasLegalMoves = false;
        if (isInStalemate) {
            System.out.println("DEBUG: Initial stalemate check returned true for player: " + currentPlayer);
            
            // Double-check by scanning all pieces for the current player
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    Piece piece = board[row][col];
                    if (piece != null && piece.getColor() == currentPlayer) {
                        List<int[]> validMoves = ChessRules.getValidMoves(board, row, col, currentPlayer == Piece.Color.WHITE);
                        if (validMoves != null && !validMoves.isEmpty()) {
                            hasLegalMoves = true;
                            System.out.println("DEBUG: Found legal moves for piece at (" + row + "," + col + ") - type: " + piece.getType() + ", color: " + piece.getColor());
                            break;
                        }
                    }
                }
                if (hasLegalMoves) break;
            }
            
            // If we found legal moves, it's not actually stalemate
            if (hasLegalMoves) {
                isInStalemate = false;
                System.out.println("DEBUG: False stalemate detected - legal moves found for player: " + currentPlayer);
            } else {
                System.out.println("DEBUG: Confirmed stalemate - no legal moves found for player: " + currentPlayer);
            }
        }
        
        // Debug logging to understand when draw is triggered
        if (isInStalemate) {
            System.out.println("DEBUG: Stalemate detected for player: " + currentPlayer);
            System.out.println("DEBUG: Move history size: " + moveHistory.size());
            System.out.println("DEBUG: Half move clock: " + halfMoveClock);
        }
        
        // TEMPORARILY DISABLE STALEMATE TO TEST - uncomment the line below to disable stalemate
        // isInStalemate = false;
        
        boolean isDraw = isThreefold || isFiftyMove || isInsufficient || isInStalemate;
        
        if (isDraw) {
            System.out.println("DEBUG: Draw detected - threefold: " + isThreefold + ", fiftyMove: " + isFiftyMove + ", insufficient: " + isInsufficient + ", stalemate: " + isInStalemate);
        }
        
        return isDraw;
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
    
    // Promotion tracking
    private boolean isPromoting = false;
    private int promotionRow = -1;
    private int promotionCol = -1;
    private Piece.Color promotingPlayer;
    
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
            
            // Add bounds checking to prevent invalid index access
            if (currentMoveIndex < -1 || currentMoveIndex >= moveHistory.size()) {
                currentMoveIndex = -1; // Reset to safe state
            }
            
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
        // After moveIndex moves, if moveIndex is even, it's White's turn (0, 2, 4...)
        // If moveIndex is odd, it's Black's turn (1, 3, 5...)
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
            // After all moves have been replayed, the current player should be the opposite of the last move's player
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
        int fromRow = record.getFromRow();
        int fromCol = record.getFromCol();
        int toRow = record.getToRow();
        int toCol = record.getToCol();
        Piece movingPiece = record.getMovingPiece();
        Piece capturedPiece = record.getCapturedPiece();
        
        // Handle castling
        if (record.isCastling()) {
            boolean kingside = toCol > fromCol;
            int row = fromRow;
            int rookCol = kingside ? 7 : 0;
            int newRookCol = kingside ? 5 : 3;

            // Move rook
            Piece rook = board[row][rookCol];
            if (rook != null) {
                board[row][newRookCol] = rook;
                board[row][rookCol] = null;
                rook.setHasMoved(true);
            }
        }
    }

    // Get current move number
    public int getCurrentMoveNumber() {
        return fullMoveNumber;
    }

    // Check if a square is under attack
    public boolean isSquareUnderAttack(int row, int col, Piece.Color attackerColor) {
        return ChessRules.isSquareAttacked(board, row, col, attackerColor);
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
        return !ChessRules.wouldBeInCheck(board, fromRow, fromCol, toRow, toCol, piece.getColor());
    }
    
    // Check if a move is a capture
    public boolean isCaptureMove(int fromRow, int fromCol, int toRow, int toCol) {
        return ChessRules.isCaptureMove(board, fromRow, fromCol, toRow, toCol);
    }
    
    // Check if a move is en passant capture
    public boolean isEnPassantCapture(int fromRow, int fromCol, int toRow, int toCol) {
        return ChessRules.isEnPassantCapture(board, fromRow, fromCol, toRow, toCol, lastDoublePawnMoveRow, lastDoublePawnMoveCol);
    }
    
    // Check if the game ended in checkmate
    public boolean isCheckmate() {
        return gameOver && winner != null && !winner.equals("Draw") && !winner.contains("time");
    }
    
    // Check if current player is in check
    public boolean isInCheck() {
        return ChessRules.isInCheck(board, currentPlayer);
    }
    
    // Promotion methods
    public boolean isPromoting() {
        return isPromoting;
    }
    
    public int getPromotionRow() {
        return promotionRow;
    }
    
    public int getPromotionCol() {
        return promotionCol;
    }
    
    public Piece.Color getPromotingPlayer() {
        return promotingPlayer;
    }
    
    public void setPromotionComplete(Piece.Type promotionType) {
        if (isPromoting && promotionRow >= 0 && promotionCol >= 0) {
            // Replace the pawn with the promoted piece
            Piece pawn = board[promotionRow][promotionCol];
            if (pawn != null && pawn.getType() == Piece.Type.PAWN) {
                board[promotionRow][promotionCol] = new Piece(pawn.getColor(), promotionType);
            }
            // Reset promotion state
            isPromoting = false;
            promotionRow = -1;
            promotionCol = -1;
            promotingPlayer = null;
            
            // Switch player after promotion is complete
            currentPlayer = currentPlayer == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE;
        }
    }
}
