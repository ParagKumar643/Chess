package com.bhram.chess2;

import android.animation.ObjectAnimator;

import android.graphics.Color;

import android.os.Bundle;

import android.view.Gravity;

import android.view.View;

import android.widget.Button;

import android.widget.GridLayout;

import android.widget.ImageView;

import android.widget.TextView;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChessActivity extends AppCompatActivity {


    private GridLayout chessBoard;

    private ChessGame game;

    private TextView currentPlayerText;

    private TextView gameStatusText;

    private Button resetButton;

    private ImageView floatingPiece;


    private int[] lastMoveFrom= null;

    private int[] lastMoveTo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chess);

        game = new ChessGame();

        chessBoard = findViewById(R.id.chessBoard);

        currentPlayerText = findViewById(R.id.currentPlayerText);

        gameStatusText = findViewById(R.id.gameStatusText);

        resetButton = findViewById(R.id.resetButton);

        initializeBoard();

        updateBoard();

        resetButton.setOnClickListener(v -> resetGame());

        updateUI();
    }

    private void initializeBoard() {

        chessBoard.removeAllViews();

        chessBoard.setColumnCount(8);

        chessBoard.setRowCount(8);

        int boardSize = (int) (40 * getResources().getDisplayMetrics().density);

        for (int row = 0; row < 8; row++) {

            for (int col = 0; col < 8; col++) {

                final int finalRow = row;

                final int finalCol = col;

                TextView square = new TextView(this);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();

                params.width = boardSize;

                params.height = boardSize;

                params.setGravity(Gravity.CENTER);

                params.columnSpec = GridLayout.spec(col);

                params.rowSpec = GridLayout.spec(row);

                square.setLayoutParams(params);

                boolean isWhiteSquare = (row + col) % 2 == 0;

                int lightSquare = Color.parseColor("#F0D9B5");

                int darkSquare = Color.parseColor("#858863");

                square.setBackgroundColor(isWhiteSquare ? lightSquare : darkSquare);

                square.setGravity(Gravity.CENTER);

                square.setTextSize(24);

                square.setTypeface(null, android.graphics.Typeface.BOLD);

                square.setOnClickListener(v -> onSquareClick(finalRow, finalCol));

                chessBoard.addView(square);

            }

        }
    }


    private void resetGame() {
        game = new ChessGame();
        initializeBoard();
        updateBoard();
        updateUI();
        Toast.makeText(this, "New game", Toast.LENGTH_SHORT).show();
    }

    private void onSquareClick(int row, int col) {

        if (game.isGameOver()) {

            return;

        }

        if (game.isPieceSelected()) {

            // Track the move for animation
            lastMoveFrom = new int[]{game.getSelectedRow(), game.getSelectedCol()};

            lastMoveTo = new int[]{row, col};
            boolean moveSuccessful = game.movePiece(row, col);

            if (moveSuccessful) {

                updateBoard();

                updateUI();

                if (game.isGameOver()) {
                    showGameOverMessage();
                }

            } else {

                // If move failed, try to select a different piece
                Piece clickedPiece = game.getPiece(row, col);

                if (clickedPiece != null && clickedPiece.getColor() == game.getCurrentPlayer()) {

                    game.selectPiece(row, col);

                    updateBoard();

                } else {
                    game.deselectPiece();
                    updateBoard();
                }
                // Deselect if clicking on invalid square
                lastMoveFrom = null;
                lastMoveTo = null;

            }
        } else {
            boolean selected = game.selectPiece(row, col);
            if (selected) {
                updateBoard();

            } else {
                Toast.makeText(this, "Select your piece", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateBoard() {

        java.util.List<int[]> validMoves = null;

        if (game.isPieceSelected()) {

            validMoves = game.getValidMoves(game.getSelectedRow(), game.getSelectedCol());

            Piece selectedPiece = game.getPiece(game.getSelectedRow(), game.getSelectedCol());
            if (selectedPiece != null && selectedPiece.getType() == Piece.Type.KING) {
                java.util.List<int[]> castlingMoves = game.getCastlingMoves(game.getSelectedRow(), game.getSelectedCol());
                validMoves.addAll(castlingMoves);

            }
        }
        
        for (int i = 0; i < chessBoard.getChildCount(); i++) {

            View child = chessBoard.getChildAt(i);

            if (child instanceof TextView) {

                TextView square = (TextView) child;

                int row = i / 8;

                int col = i % 8;

                boolean isWhiteSquare = (row + col) % 2 == 0;

                int lightSquare = Color.parseColor("#F0D9B5");

                int darkSquare = Color.parseColor("#858863");

                int baseColor = isWhiteSquare ? lightSquare : darkSquare;


                boolean isValidMove = false;

                if (validMoves != null) {

                    for (int[] move : validMoves) {

                        if (move[0] == row && move[1] == col) {

                            isValidMove = true;
                            break;

                        }

                    }

                }

                if (game.isPieceSelected() &&

                        game.getSelectedRow() == row &&

                        game.getSelectedCol() == col) {

                    square.setBackgroundColor(Color.parseColor("#FFD700"));

                } else if (isValidMove) {

                    square.setBackgroundColor(Color.parseColor("#90EE98"));

                } else {

                    square.setBackgroundColor(baseColor);

                }

                Piece piece = game.getPiece(row, col);

                if (piece != null) {

                    String pieceInitial = getPieceInitial(piece);

                    boolean isNewPosition = lastMoveTo != null && lastMoveTo[0] == row && lastMoveTo[1] == col;

                    square.setText(pieceInitial);


                    if (piece.getColor() == Piece.Color.WHITE) {
                        square.setTextColor(Color.WHITE);
                        square.setShadowLayer(4, 2, 2, Color.BLACK);
                    } else {
                        square.setTextColor(Color.BLACK);
                        square.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
                    }

                    if (isNewPosition) {
                        square.setAlpha(0f);
                        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(square, "alpha", 0f, 1f);
                        fadeIn.setDuration(200);
                        fadeIn.start();
                    }

                } else {
                    square.setText("");
                }

            }

        }
    }


    private String getPieceInitial(Piece piece) {
        switch (piece.getType()) {

            case PAWN:
                return "P";
            case ROOK:
                return "R";
            case KNIGHT:
                return "N";
            case BISHOP:
                return "B";
            case QUEEN:
                return "Q";
            case KING:
                return "K";
            default:
                return "P";
        }
    }

    private void updateUI() {

        String player = game.getCurrentPlayer() == Piece.Color.WHITE ? "White" : "Black";

        currentPlayerText.setText("Current Player: " + player);

        if (game.isGameOver()) {

            gameStatusText.setText("Game Over! " + game.getWinner());

            gameStatusText.setTextColor(Color.parseColor("#FF4444"));

            resetButton.setEnabled(true);

        } else if (game.isInCheck()) {

            gameStatusText.setText(player + " is in CHECK!");

            gameStatusText.setTextColor(Color.parseColor("#FF6600"));

            resetButton.setEnabled(true);

        } else {

            gameStatusText.setText("");

            resetButton.setEnabled(true);

        }
    }

    private void showGameOverMessage() {

        String winner = game.getWinner();

        if (winner.contains("Draw")) {

            Toast.makeText(this, "Game Over! " + winner, Toast.LENGTH_LONG).show();

        } else {

            Toast.makeText(this, "Game Over! " + winner + " wins!", Toast.LENGTH_LONG).show();
        }
    }
}
