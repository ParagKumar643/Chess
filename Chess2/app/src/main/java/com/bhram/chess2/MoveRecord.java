package com.bhram.chess2;

public class MoveRecord {
    private int fromRow;
    private int fromCol;
    private int toRow;
    private int toCol;
    private Piece movingPiece;
    private Piece capturedPiece;
    private boolean isCastling;
    private boolean isEnPassant;
    private boolean isPromotion;
    private Piece.Color playerColor;
    
    public MoveRecord(int fromRow, int fromCol, int toRow, int toCol, 
                     Piece movingPiece, Piece capturedPiece,
                     boolean isCastling, boolean isEnPassant, boolean isPromotion,
                     Piece.Color playerColor) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.movingPiece = movingPiece;
        this.capturedPiece = capturedPiece;
        this.isCastling = isCastling;
        this.isEnPassant = isEnPassant;
        this.isPromotion = isPromotion;
        this.playerColor = playerColor;
    }
    
    // Getters
    public int getFromRow() { return fromRow; }
    public int getFromCol() { return fromCol; }
    public int getToRow() { return toRow; }
    public int getToCol() { return toCol; }
    public Piece getMovingPiece() { return movingPiece; }
    public Piece getCapturedPiece() { return capturedPiece; }
    public boolean isCastling() { return isCastling; }
    public boolean isEnPassant() { return isEnPassant; }
    public boolean isPromotion() { return isPromotion; }
    public Piece.Color getPlayerColor() { return playerColor; }
}