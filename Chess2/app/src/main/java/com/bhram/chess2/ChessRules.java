package com.bhram.chess2;

import java.util.ArrayList;
import java.util.List;

/**
 * ChessRules class handles all chess game logic and rules.
 * This includes move validation, check detection, checkmate detection,
 * and special moves like castling and en passant.
 */
public class ChessRules {
    
    /**
     * Validates if a move is legal according to chess rules
     */
    public static boolean isValidMove(Piece[][] board, int startRow, int startCol, int endRow, int endCol, boolean isWhiteTurn) {
        // Basic bounds checking
        if (!isValidPosition(endRow, endCol)) {
            return false;
        }
        
        // Cannot move to same position
        if (startRow == endRow && startCol == endCol) {
            return false;
        }
        
        Piece startPiece = board[startRow][startCol];
        
        // Must have a piece to move
        if (startPiece == null) {
            return false;
        }
        
        // Must move own piece
        if (startPiece.getColor() == Piece.Color.WHITE != isWhiteTurn) {
            return false;
        }
        
        // Cannot capture own piece
        Piece endPiece = board[endRow][endCol];
        if (endPiece != null && endPiece.getColor() == startPiece.getColor()) {
            return false;
        }
        
        // Validate piece-specific movement
        if (!isValidPieceMove(board, startPiece, startRow, startCol, endRow, endCol)) {
            return false;
        }
        
        // Check if move puts own king in check
        if (wouldPutKingInCheck(board, startRow, startCol, endRow, endCol, isWhiteTurn)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates piece-specific movement rules
     */
    public static boolean isValidPieceMove(Piece[][] board, Piece piece, int startRow, int startCol, int endRow, int endCol) {
        switch (piece.getType()) {
            case PAWN:
                return isValidPawnMove(board, piece, startRow, startCol, endRow, endCol);
            case ROOK:
                return isValidRookMove(board, startRow, startCol, endRow, endCol);
            case KNIGHT:
                return isValidKnightMove(startRow, startCol, endRow, endCol);
            case BISHOP:
                return isValidBishopMove(board, startRow, startCol, endRow, endCol);
            case QUEEN:
                return isValidQueenMove(board, startRow, startCol, endRow, endCol);
            case KING:
                return isValidKingMove(board, startRow, startCol, endRow, endCol);
            default:
                return false;
        }
    }
    
    /**
     * Validates pawn movement rules
     */
    private static boolean isValidPawnMove(Piece[][] board, Piece pawn, int startRow, int startCol, int endRow, int endCol) {
        int startRank = pawn.getColor() == Piece.Color.WHITE ? 6 : 1;
        int direction = pawn.getColor() == Piece.Color.WHITE ? -1 : 1;
        
        // Forward move
        if (startCol == endCol) {
            // Single square forward
            if (endRow == startRow + direction && board[endRow][endCol] == null) {
                return true;
            }
            // Double square forward from starting position
            if (startRow == startRank && endRow == startRow + 2 * direction && 
                board[startRow + direction][startCol] == null && board[endRow][endCol] == null) {
                return true;
            }
        }
        // Diagonal capture
        else if (Math.abs(startCol - endCol) == 1 && endRow == startRow + direction) {
            Piece targetPiece = board[endRow][endCol];
            return targetPiece != null && targetPiece.getColor() != pawn.getColor();
        }
        
        return false;
    }
    
    /**
     * Validates rook movement rules (horizontal and vertical)
     */
    private static boolean isValidRookMove(Piece[][] board, int startRow, int startCol, int endRow, int endCol) {
        // Must move in straight line
        if (startRow != endRow && startCol != endCol) {
            return false;
        }
        
        // Check if path is clear
        return isPathClear(board, startRow, startCol, endRow, endCol);
    }
    
    /**
     * Validates knight movement rules (L-shape)
     */
    private static boolean isValidKnightMove(int startRow, int startCol, int endRow, int endCol) {
        int rowDiff = Math.abs(startRow - endRow);
        int colDiff = Math.abs(startCol - endCol);
        
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }
    
    /**
     * Validates bishop movement rules (diagonal)
     */
    private static boolean isValidBishopMove(Piece[][] board, int startRow, int startCol, int endRow, int endCol) {
        // Must move diagonally
        if (Math.abs(startRow - endRow) != Math.abs(startCol - endCol)) {
            return false;
        }
        
        // Check if path is clear
        return isPathClear(board, startRow, startCol, endRow, endCol);
    }
    
    /**
     * Validates queen movement rules (combination of rook and bishop)
     */
    private static boolean isValidQueenMove(Piece[][] board, int startRow, int startCol, int endRow, int endCol) {
        // Horizontal, vertical, or diagonal movement
        boolean isStraight = (startRow == endRow || startCol == endCol);
        boolean isDiagonal = (Math.abs(startRow - endRow) == Math.abs(startCol - endCol));
        
        if (!isStraight && !isDiagonal) {
            return false;
        }
        
        // Check if path is clear
        return isPathClear(board, startRow, startCol, endRow, endCol);
    }
    
    /**
     * Validates king movement rules (one square in any direction)
     */
    private static boolean isValidKingMove(Piece[][] board, int startRow, int startCol, int endRow, int endCol) {
        int rowDiff = Math.abs(startRow - endRow);
        int colDiff = Math.abs(startCol - endCol);
        
        // King can move one square in any direction
        return rowDiff <= 1 && colDiff <= 1 && (rowDiff != 0 || colDiff != 0);
    }
    
    /**
     * Checks if the path between two positions is clear (for rook, bishop, queen)
     */
    private static boolean isPathClear(Piece[][] board, int startRow, int startCol, int endRow, int endCol) {
        int rowStep = Integer.compare(endRow, startRow);
        int colStep = Integer.compare(endCol, startCol);
        
        int currentRow = startRow + rowStep;
        int currentCol = startCol + colStep;
        
        while (currentRow != endRow || currentCol != endCol) {
            if (board[currentRow][currentCol] != null) {
                return false;
            }
            currentRow += rowStep;
            currentCol += colStep;
        }
        
        return true;
    }
    
    /**
     * Checks if a move would put the player's own king in check
     */
    public static boolean wouldPutKingInCheck(Piece[][] board, int startRow, int startCol, int endRow, int endCol, boolean isWhiteTurn) {
        // Create a temporary board to simulate the move
        Piece[][] tempBoard = copyBoard(board);
        
        // Make the move on the temporary board
        tempBoard[endRow][endCol] = tempBoard[startRow][startCol];
        tempBoard[startRow][startCol] = null;
        
        // Check if the king is in check after the move
        return isKingInCheck(tempBoard, isWhiteTurn);
    }
    
    /**
     * Checks if the player's king is in check
     */
    public static boolean isKingInCheck(Piece[][] board, boolean isWhiteTurn) {
        int[] kingPosition = findKing(board, isWhiteTurn);
        if (kingPosition == null) {
            return false; // King not found (shouldn't happen in valid game)
        }
        
        int kingRow = kingPosition[0];
        int kingCol = kingPosition[1];
        
        // Check if any opponent piece can attack the king
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getColor() != (isWhiteTurn ? Piece.Color.WHITE : Piece.Color.BLACK)) {
                    if (canAttackKing(board, piece, row, col, kingRow, kingCol)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a piece can attack the king at the given position
     */
    private static boolean canAttackKing(Piece[][] board, Piece piece, int pieceRow, int pieceCol, int kingRow, int kingCol) {
        switch (piece.getType()) {
            case PAWN:
                return canPawnAttackKing(piece, pieceRow, pieceCol, kingRow, kingCol);
            case ROOK:
                return canRookAttackKing(board, pieceRow, pieceCol, kingRow, kingCol);
            case KNIGHT:
                return canKnightAttackKing(pieceRow, pieceCol, kingRow, kingCol);
            case BISHOP:
                return canBishopAttackKing(board, pieceRow, pieceCol, kingRow, kingCol);
            case QUEEN:
                return canQueenAttackKing(board, pieceRow, pieceCol, kingRow, kingCol);
            case KING:
                return canKingAttackKing(pieceRow, pieceCol, kingRow, kingCol);
            default:
                return false;
        }
    }
    
    /**
     * Checks if a pawn can attack the king
     */
    private static boolean canPawnAttackKing(Piece pawn, int pawnRow, int pawnCol, int kingRow, int kingCol) {
        int direction = pawn.getColor() == Piece.Color.WHITE ? -1 : 1;
        return kingRow == pawnRow + direction && Math.abs(kingCol - pawnCol) == 1;
    }
    
    /**
     * Checks if a rook can attack the king
     */
    private static boolean canRookAttackKing(Piece[][] board, int rookRow, int rookCol, int kingRow, int kingCol) {
        if (rookRow != kingRow && rookCol != kingCol) {
            return false;
        }
        return isPathClear(board, rookRow, rookCol, kingRow, kingCol);
    }
    
    /**
     * Checks if a knight can attack the king
     */
    private static boolean canKnightAttackKing(int knightRow, int knightCol, int kingRow, int kingCol) {
        int rowDiff = Math.abs(knightRow - kingRow);
        int colDiff = Math.abs(knightCol - kingCol);
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }
    
    /**
     * Checks if a bishop can attack the king
     */
    private static boolean canBishopAttackKing(Piece[][] board, int bishopRow, int bishopCol, int kingRow, int kingCol) {
        if (Math.abs(bishopRow - kingRow) != Math.abs(bishopCol - kingCol)) {
            return false;
        }
        return isPathClear(board, bishopRow, bishopCol, kingRow, kingCol);
    }
    
    /**
     * Checks if a queen can attack the king
     */
    private static boolean canQueenAttackKing(Piece[][] board, int queenRow, int queenCol, int kingRow, int kingCol) {
        // Queen can attack like rook or bishop
        return canRookAttackKing(board, queenRow, queenCol, kingRow, kingCol) ||
               canBishopAttackKing(board, queenRow, queenCol, kingRow, kingCol);
    }
    
    /**
     * Checks if a king can attack another king (for check detection)
     */
    private static boolean canKingAttackKing(int king1Row, int king1Col, int king2Row, int king2Col) {
        int rowDiff = Math.abs(king1Row - king2Row);
        int colDiff = Math.abs(king1Col - king2Col);
        return rowDiff <= 1 && colDiff <= 1 && (rowDiff != 0 || colDiff != 0);
    }
    
    /**
     * Finds the position of the king for the given player
     */
    private static int[] findKing(Piece[][] board, boolean isWhite) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getType() == Piece.Type.KING && piece.getColor() == (isWhite ? Piece.Color.WHITE : Piece.Color.BLACK)) {
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }
    
    /**
     * Checks if the game is in checkmate
     */
    public static boolean isCheckmate(Piece[][] board, boolean isWhiteTurn) {
        if (!isKingInCheck(board, isWhiteTurn)) {
            return false;
        }
        
        // Check if any legal move can get out of check
        for (int startRow = 0; startRow < 8; startRow++) {
            for (int startCol = 0; startCol < 8; startCol++) {
                Piece piece = board[startRow][startCol];
                if (piece != null && piece.getColor() == (isWhiteTurn ? Piece.Color.WHITE : Piece.Color.BLACK)) {
                    for (int endRow = 0; endRow < 8; endRow++) {
                        for (int endCol = 0; endCol < 8; endCol++) {
                            if (isValidMove(board, startRow, startCol, endRow, endCol, isWhiteTurn)) {
                                return false; // Found a legal move
                            }
                        }
                    }
                }
            }
        }
        
        return true; // No legal moves available
    }
    
    /**
     * Checks if the game is in stalemate
     */
    public static boolean isStalemate(Piece[][] board, boolean isWhiteTurn) {
        // First check if king is in check - if so, it's checkmate, not stalemate
        if (isKingInCheck(board, isWhiteTurn)) {
            return false; // Not stalemate if in check
        }
        
        // Check if any legal move is available for the current player
        for (int startRow = 0; startRow < 8; startRow++) {
            for (int startCol = 0; startCol < 8; startCol++) {
                Piece piece = board[startRow][startCol];
                if (piece != null && piece.getColor() == (isWhiteTurn ? Piece.Color.WHITE : Piece.Color.BLACK)) {
                    for (int endRow = 0; endRow < 8; endRow++) {
                        for (int endCol = 0; endCol < 8; endCol++) {
                            if (isValidMove(board, startRow, startCol, endRow, endCol, isWhiteTurn)) {
                                return false; // Found a legal move
                            }
                        }
                    }
                }
            }
        }
        
        return true; // No legal moves available but not in check
    }
    
    /**
     * Gets all valid moves for a piece at the given position
     */
    public static List<int[]> getValidMoves(Piece[][] board, int row, int col, boolean isWhiteTurn) {
        List<int[]> validMoves = new ArrayList<>();
        
        if (row < 0 || row >= 8 || col < 0 || col >= 8) {
            return validMoves;
        }
        
        Piece piece = board[row][col];
        if (piece == null || piece.getColor() != (isWhiteTurn ? Piece.Color.WHITE : Piece.Color.BLACK)) {
            return validMoves;
        }
        
        // Try all possible target positions
        for (int endRow = 0; endRow < 8; endRow++) {
            for (int endCol = 0; endCol < 8; endCol++) {
                if (isValidMove(board, row, col, endRow, endCol, isWhiteTurn)) {
                    validMoves.add(new int[]{endRow, endCol});
                }
            }
        }
        
        return validMoves;
    }
    
    /**
     * Checks if a position is within the board bounds
     */
    private static boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
    
    /**
     * Creates a deep copy of the board
     */
    private static Piece[][] copyBoard(Piece[][] original) {
        Piece[][] copy = new Piece[8][8];
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (original[row][col] != null) {
                    copy[row][col] = new Piece(original[row][col].getColor(), original[row][col].getType());
                }
            }
        }
        return copy;
    }
    
    /**
     * Checks if castling is possible
     */
    public static boolean canCastle(Piece[][] board, Piece.Color color, boolean kingside) {
        int[] kingPos = findKing(board, color == Piece.Color.WHITE);
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
            if (isSquareAttacked(board, row, col, opponentColor)) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * Checks if en passant is possible
     */
    public static boolean canEnPassant(Piece[][] board, int fromRow, int fromCol, int toRow, int toCol, int lastDoublePawnMoveRow, int lastDoublePawnMoveCol) {
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
    
    /**
     * Checks if a square is attacked by opponent pieces
     */
    public static boolean isSquareAttacked(Piece[][] board, int row, int col, Piece.Color byColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board[r][c];
                if (piece != null && piece.getColor() == byColor) {
                    if (canPieceAttack(board, r, c, row, col)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Checks if a piece can attack a square (without considering check)
     */
    private static boolean canPieceAttack(Piece[][] board, int fromRow, int fromCol, int toRow, int toCol) {
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
                return isValidRookMove(board, fromRow, fromCol, toRow, toCol);
            case KNIGHT:
                return isValidKnightMove(fromRow, fromCol, toRow, toCol);
            case BISHOP:
                return isValidBishopMove(board, fromRow, fromCol, toRow, toCol);
            case QUEEN:
                return isValidQueenMove(board, fromRow, fromCol, toRow, toCol);
            case KING:
                int rowDiff = Math.abs(fromRow - toRow);
                int colDiff = Math.abs(fromCol - toCol);
                return rowDiff <= 1 && colDiff <= 1;
            default:
                return false;
        }
    }
    
    /**
     * Checks if a move would leave the king in check
     */
    public static boolean wouldBeInCheck(Piece[][] board, int fromRow, int fromCol, int toRow, int toCol, Piece.Color color) {
        // Create a temporary board to simulate the move
        Piece[][] tempBoard = copyBoard(board);
        
        // Make the move on the temporary board
        tempBoard[toRow][toCol] = tempBoard[fromRow][fromCol];
        tempBoard[fromRow][fromCol] = null;
        
        // Check if king is in check
        int[] kingPos = findKing(tempBoard, color == Piece.Color.WHITE);
        boolean inCheck = false;
        if (kingPos != null) {
            Piece.Color opponentColor = color == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE;
            inCheck = isSquareAttacked(tempBoard, kingPos[0], kingPos[1], opponentColor);
        }
        
        return inCheck;
    }
    
    /**
     * Checks if current player is in check
     */
    public static boolean isInCheck(Piece[][] board, Piece.Color currentPlayer) {
        int[] kingPos = findKing(board, currentPlayer == Piece.Color.WHITE);
        if (kingPos == null) {
            return false;
        }
        Piece.Color opponentColor = currentPlayer == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE;
        return isSquareAttacked(board, kingPos[0], kingPos[1], opponentColor);
    }
    
    /**
     * Checks if current player is in checkmate
     */
    public static boolean isInCheckmate(Piece[][] board, Piece.Color currentPlayer) {
        if (!isInCheck(board, currentPlayer)) {
            return false;
        }

        // Check if any piece can make a valid move
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getColor() == currentPlayer) {
                    List<int[]> validMoves = getValidMoves(board, row, col, currentPlayer == Piece.Color.WHITE);
                    if (!validMoves.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Checks for stalemate
     */
    public static boolean isInStalemate(Piece[][] board, Piece.Color currentPlayer) {
        if (isInCheck(board, currentPlayer)) {
            return false;
        }

        // Check if any piece can make a valid move
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getColor() == currentPlayer) {
                    List<int[]> validMoves = getValidMoves(board, row, col, currentPlayer == Piece.Color.WHITE);
                    if (!validMoves.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Checks if a move is a capture
     */
    public static boolean isCaptureMove(Piece[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = board[fromRow][fromCol];
        Piece target = board[toRow][toCol];
        return piece != null && target != null && target.getColor() != piece.getColor();
    }
    
    /**
     * Checks if a move is en passant capture
     */
    public static boolean isEnPassantCapture(Piece[][] board, int fromRow, int fromCol, int toRow, int toCol, int lastDoublePawnMoveRow, int lastDoublePawnMoveCol) {
        Piece piece = board[fromRow][fromCol];
        if (piece == null || piece.getType() != Piece.Type.PAWN) {
            return false;
        }
        return canEnPassant(board, fromRow, fromCol, toRow, toCol, lastDoublePawnMoveRow, lastDoublePawnMoveCol);
    }
}
