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


    private int[] lastMoveFrom = null;

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

                // Use ImageView for all squares to support both images and text
                ImageView square = new ImageView(this);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();

                params.width = boardSize;

                params.height = boardSize;

                params.setGravity(Gravity.CENTER);

                params.columnSpec = GridLayout.spec(col);

                params.rowSpec = GridLayout.spec(row);

                square.setLayoutParams(params);
                square.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                boolean isWhiteSquare = (row + col) % 2 == 0;

                int lightSquare = Color.parseColor("#F0D9B5");

                int darkSquare = Color.parseColor("#858863");

                square.setBackgroundColor(isWhiteSquare ? lightSquare : darkSquare);

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

            if (child instanceof ImageView) {
                ImageView square = (ImageView) child;

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
                    // Create a black circle for valid move indicators
                    square.setImageDrawable(createBlackCircleDrawable());
                    // Keep the original board color, don't change background
                } else {
                    square.setBackgroundColor(baseColor);
                }

                Piece piece = game.getPiece(row, col);
                boolean isNewPosition = lastMoveTo != null && lastMoveTo[0] == row && lastMoveTo[1] == col;

                if (piece != null) {
                    // Display appropriate piece image based on type and color
                    switch (piece.getType()) {
                        case PAWN:
                            if (piece.getColor() == Piece.Color.BLACK) {
                                square.setImageResource(R.drawable.pawn_black);
                            } else {
                                square.setImageResource(R.drawable.pawn_white);
                            }
                            break;
                        case ROOK:
                            if (piece.getColor() == Piece.Color.BLACK) {
                                square.setImageResource(R.drawable.rook_black);
                            } else {
                                square.setImageResource(R.drawable.rook_white);
                            }
                            break;
                        case KNIGHT:
                            if (piece.getColor() == Piece.Color.BLACK) {
                                square.setImageResource(R.drawable.knight_black);
                            } else {
                                square.setImageResource(R.drawable.knight_white);
                            }
                            break;
                        case BISHOP:
                            if (piece.getColor() == Piece.Color.BLACK) {
                                square.setImageResource(R.drawable.bishop_black);
                            } else {
                                square.setImageResource(R.drawable.bishop_white);
                            }
                            break;
                        case QUEEN:
                            if (piece.getColor() == Piece.Color.BLACK) {
                                square.setImageResource(R.drawable.queen_black);
                            } else {
                                square.setImageResource(R.drawable.queen_white);
                            }
                            break;
                        case KING:
                            if (piece.getColor() == Piece.Color.BLACK) {
                                square.setImageResource(R.drawable.king_black);
                            } else {
                                square.setImageResource(R.drawable.king_white);
                            }
                            break;
                    }

                    // Show shadow on opponent pieces that can be captured as overlay
                    if (isValidMove && piece.getColor() != game.getCurrentPlayer()) {
                        // Create a combined drawable with piece image and circle overlay
                        square.setImageDrawable(createPieceWithShadowDrawable(piece));
                    }

                    if (isNewPosition) {
                        square.setAlpha(0f);
                        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(square, "alpha", 0f, 1f);
                        fadeIn.setDuration(200);
                        fadeIn.start();
                    }
                } else if (isValidMove) {
                    // Show black circle for valid moves on empty squares
                    square.setImageDrawable(createBlackCircleDrawable());
                } else {
                    square.setImageResource(0);
                }
            }
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

    private android.graphics.drawable.Drawable createBlackCircleDrawable() {
        // Create a simple dark grey filled circle drawable for empty squares
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(60, 60, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(Color.parseColor("#333333")); // Dark grey color
        paint.setAntiAlias(true);
        paint.setStyle(android.graphics.Paint.Style.FILL);
        
        int radius = 20; // Circle radius
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;
        
        canvas.drawCircle(centerX, centerY, radius, paint);
        
        return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    }

    private android.graphics.drawable.Drawable createPieceWithShadowDrawable(Piece piece) {
        // Create a combined drawable with piece image and circle overlay
        int boardSize = (int) (40 * getResources().getDisplayMetrics().density);
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(boardSize, boardSize, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        
        // Draw the piece image at full size
        android.graphics.drawable.Drawable pieceDrawable = null;
        switch (piece.getType()) {
            case PAWN:
                if (piece.getColor() == Piece.Color.BLACK) {
                    pieceDrawable = getResources().getDrawable(R.drawable.pawn_black);
                } else {
                    pieceDrawable = getResources().getDrawable(R.drawable.pawn_white);
                }
                break;
            case ROOK:
                if (piece.getColor() == Piece.Color.BLACK) {
                    pieceDrawable = getResources().getDrawable(R.drawable.rook_black);
                } else {
                    pieceDrawable = getResources().getDrawable(R.drawable.rook_white);
                }
                break;
            case KNIGHT:
                if (piece.getColor() == Piece.Color.BLACK) {
                    pieceDrawable = getResources().getDrawable(R.drawable.knight_black);
                } else {
                    pieceDrawable = getResources().getDrawable(R.drawable.knight_white);
                }
                break;
            case BISHOP:
                if (piece.getColor() == Piece.Color.BLACK) {
                    pieceDrawable = getResources().getDrawable(R.drawable.bishop_black);
                } else {
                    pieceDrawable = getResources().getDrawable(R.drawable.bishop_white);
                }
                break;
            case QUEEN:
                if (piece.getColor() == Piece.Color.BLACK) {
                    pieceDrawable = getResources().getDrawable(R.drawable.queen_black);
                } else {
                    pieceDrawable = getResources().getDrawable(R.drawable.queen_white);
                }
                break;
            case KING:
                if (piece.getColor() == Piece.Color.BLACK) {
                    pieceDrawable = getResources().getDrawable(R.drawable.king_black);
                } else {
                    pieceDrawable = getResources().getDrawable(R.drawable.king_white);
                }
                break;
        }
        
        if (pieceDrawable != null) {
            // Draw the piece image at full canvas size to prevent shrinking
            pieceDrawable.setBounds(0, 0, boardSize, boardSize);
            pieceDrawable.draw(canvas);
        }
        
        // Draw the dark grey circular outline overlay in the center
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(Color.parseColor("#333333")); // Dark grey color
        paint.setAntiAlias(true);
        paint.setAlpha(180); // Semi-transparent
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        paint.setStrokeWidth(8); // Increased line thickness
        
        int radius = boardSize / 2 - 10; // Larger radius to cover the image completely
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;
        
        canvas.drawCircle(centerX, centerY, radius, paint);
        
        return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    }
}
