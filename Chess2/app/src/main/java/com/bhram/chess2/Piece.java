package com.bhram.chess2;

public class Piece {
    public enum Color {
        WHITE, BLACK
    }


    public enum Type {
        PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING
    }

    private Color color;

    private Type type;

    private boolean hasMoved;


    public Piece(Color color, Type type) {
        this.color = color;
        this.type = type;
        this.hasMoved = false;
    }

    public Color getColor() { return color; }

    public Type getType() { return type; }

    public boolean hasMoved() { return hasMoved; }


    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    @Override
    public String toString() {
        return color + "_" + type;
    }
}
