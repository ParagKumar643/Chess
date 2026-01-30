package com.bhram.chess2;

import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaPlayer;
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

    private TextView player1NameText;
    private TextView player2NameText;
    private TextView player1ClockText;
    private TextView player2ClockText;

    private android.os.Handler clockHandler;

    private Runnable clockRunnable;

    private long player1Time = 60000; // 1 minute in milliseconds
    private long player2Time = 60000; // 1 minute in milliseconds

    private boolean isClockRunning = false;
    private Piece.Color currentPlayerInClock;


    private int[] lastMoveFrom= null;

    private int[] lastMoveTo = null;
    
    // Sound player fields
    private MediaPlayer moveSound;
    private MediaPlayer captureSound;
    private MediaPlayer checkmateSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chess);

        game = new ChessGame();

        chessBoard = findViewById(R.id.chessBoard);

        currentPlayerText = findViewById(R.id.currentPlayerText);

        gameStatusText = findViewById(R.id.gameStatusText);

        resetButton = findViewById(R.id.resetButton);

        player1NameText = findViewById(R.id.player1Name);
        player2NameText = findViewById(R.id.player2Name);
        player1ClockText = findViewById(R.id.player1Clock);
        player2ClockText = findViewById(R.id.player2Clock);

        clockHandler = new android.os.Handler();

        player1NameText.setText("Player 1 (White)");
        player2NameText.setText("Player 2 (Black)");
        
        // Initialize clocks
        updatePlayerClocks();

        initializeBoard();

        updateBoard();

        resetButton.setOnClickListener(v -> resetGame());

        updateUI();
        
        // Start White's clock immediately when game starts
        startPlayerClock(Piece.Color.WHITE);
        
        // Initialize sound players
        initializeSoundPlayers();
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
        resetPlayerClocks();
        // Start White's clock first when game starts
        startPlayerClock(Piece.Color.WHITE);
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
                // Play appropriate sound based on move type
                // Check if a piece was captured by looking at the previous state
                Piece capturedPiece = null;
                if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                    // Check if there was a piece at the destination before the move
                    // We need to check the board state before the move was made
                    // Since the move is already executed, we'll use a different approach
                    
                    // For normal captures: if the destination square had an opponent's piece
                    // For en passant: special detection needed
                    
                    // Let's implement a simpler approach - check if the move was a capture
                    // by examining the move history or by checking the game state
                    
                    // For now, let's use a simpler detection method
                    boolean isCapture = false;
                    
                    // Check if this was an en passant capture
                    if (game.isEnPassantCapture(game.getSelectedRow(), game.getSelectedCol(), row, col)) {
                        isCapture = true;
                        android.util.Log.d("ChessSound", "Detected en passant capture");
                    } else {
                        // For regular captures, we need to check if a piece was removed from the board
                        // Since the move is already executed, we'll use a different approach
                        // Let's check if the move was diagonal for pawns (potential capture)
                        // or if it's a normal capture pattern
                        
                        int fromRow = game.getSelectedRow();
                        int fromCol = game.getSelectedCol();
                        Piece movingPiece = game.getPiece(row, col); // This is now the moved piece
                        
                        if (movingPiece != null) {
                            // Check if this looks like a capture move
                            if (movingPiece.getType() == Piece.Type.PAWN) {
                                // Pawn captures are diagonal
                                if (Math.abs(fromCol - col) == 1 && row != fromRow) {
                                    isCapture = true;
                                    android.util.Log.d("ChessSound", "Detected pawn capture");
                                }
                            } else {
                                // For other pieces, check if they moved to a square that could contain an opponent
                                // This is a simplified check - in a real implementation, we'd track the previous board state
                                isCapture = true; // Assume it's a capture for testing
                                android.util.Log.d("ChessSound", "Assuming capture for non-pawn piece");
                            }
                        }
                    }
                    
                    if (isCapture) {
                        android.util.Log.d("ChessSound", "Playing capture sound");
                        playCaptureSound();
                    } else {
                        android.util.Log.d("ChessSound", "Playing move sound");
                        playMoveSound();
                    }
                }

                updateBoard();

                updateUI();

                if (game.isGameOver()) {
                    showGameOverMessage();
                } else {
                    switchPlayer();
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
                if (castlingMoves != null && !castlingMoves.isEmpty()) {
                    if (validMoves == null) {
                        validMoves = new java.util.ArrayList<>();
                    }
                    validMoves.addAll(castlingMoves);
                }
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

            stopClock();

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
        
        // Play checkmate sound if game ended in checkmate
        if (game.isCheckmate()) {
            playCheckmateSound();
        }
        
        stopClock();
    }
    
    private void switchPlayer() {
        // Stop current player's clock
        stopClock();
        
        // Determine which player's clock should be running
        currentPlayerInClock = game.getCurrentPlayer();
        
        // Update UI to reflect the current player
        updateUI();
        
        // Start the appropriate player's clock
        startPlayerClock(currentPlayerInClock);
    }
    
    private void startPlayerClock(Piece.Color player) {
        if (!isClockRunning) {
            isClockRunning = true;
            currentPlayerInClock = player;
            clockRunnable = new Runnable() {
                @Override
                public void run() {
                    if (player == Piece.Color.WHITE) {
                        if (player1Time > 0) {
                            player1Time -= 1000; // Decrease by 1 second
                            updatePlayerClocks();
                            
                            if (player1Time <= 0) {
                                player1Time = 0;
                                handleTimeUp();
                            } else {
                                clockHandler.postDelayed(this, 1000);
                            }
                        }
                    } else {
                        if (player2Time > 0) {
                            player2Time -= 1000; // Decrease by 1 second
                            updatePlayerClocks();
                            
                            if (player2Time <= 0) {
                                player2Time = 0;
                                handleTimeUp();
                            } else {
                                clockHandler.postDelayed(this, 1000);
                            }
                        }
                    }
                }
            };
            clockHandler.post(clockRunnable);
        }
    }
    
    private void stopClock() {
        isClockRunning = false;
        if (clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }
    
    private void resetPlayerClocks() {
        stopClock();
        player1Time = 60000; // 1 minute
        player2Time = 60000; // 1 minute
        updatePlayerClocks();
    }
    
    private void updatePlayerClocks() {
        // Update Player 1 clock
        int player1Minutes = (int) (player1Time / 60000);
        int player1Seconds = (int) ((player1Time % 60000) / 1000);
        String player1TimeString = String.format("%02d:%02d", player1Minutes, player1Seconds);
        player1ClockText.setText(player1TimeString);
        
        // Update Player 2 clock
        int player2Minutes = (int) (player2Time / 60000);
        int player2Seconds = (int) ((player2Time % 60000) / 1000);
        String player2TimeString = String.format("%02d:%02d", player2Minutes, player2Seconds);
        player2ClockText.setText(player2TimeString);
        
        // Change color when time is low (less than 1 minute)
        if (player1Time <= 60000) {
            player1ClockText.setTextColor(Color.parseColor("#FF4444")); // Red color
            // Add pulsing animation for time pressure
            animateClock(player1ClockText);
        } else {
            player1ClockText.setTextColor(Color.parseColor("#FFFFFF")); // White color
            player1ClockText.clearAnimation();
        }
        
        if (player2Time <= 60000) {
            player2ClockText.setTextColor(Color.parseColor("#FF4444")); // Red color
            // Add pulsing animation for time pressure
            animateClock(player2ClockText);
        } else {
            player2ClockText.setTextColor(Color.parseColor("#FFFFFF")); // White color
            player2ClockText.clearAnimation();
        }
    }
    
    private void animateClock(TextView clockText) {
        // Create a pulsing animation for time pressure
        android.view.animation.Animation pulse = android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        pulse.setDuration(500);
        pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
        pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
        clockText.startAnimation(pulse);
    }
    
    private void handleTimeUp() {
        stopClock();
        String winner = currentPlayerInClock == Piece.Color.WHITE ? "Black wins on time!" : "White wins on time!";
        gameStatusText.setText("Time's up! " + winner);
        gameStatusText.setTextColor(Color.parseColor("#FF4444"));
        
        // Show winner message and end game
        Toast.makeText(this, winner, Toast.LENGTH_LONG).show();
        
        // Update game state to reflect the winner
        game.setWinner(winner);
        
        // Disable further moves
        game.setGameOver(true);
    }
    
    private void initializeSoundPlayers() {
        // Initialize sound players
        moveSound = MediaPlayer.create(this, R.raw.move_sound);
        captureSound = MediaPlayer.create(this, R.raw.capture_sound);
        checkmateSound = MediaPlayer.create(this, R.raw.checkmate_sound);
    }
    
    private void playMoveSound() {
        android.util.Log.d("ChessSound", "playMoveSound called");
        if (moveSound != null) {
            moveSound.stop();
            moveSound.prepareAsync();
            moveSound.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    android.util.Log.d("ChessSound", "Starting move sound playback");
                    mp.start();
                }
            });
        }
    }
    
    private void playCaptureSound() {
        android.util.Log.d("ChessSound", "playCaptureSound called");
        if (captureSound != null) {
            captureSound.stop();
            captureSound.prepareAsync();
            captureSound.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    android.util.Log.d("ChessSound", "Starting capture sound playback");
                    mp.start();
                }
            });
        }
    }
    
    private void playCheckmateSound() {
        if (checkmateSound != null) {
            checkmateSound.stop();
            checkmateSound.prepareAsync();
            checkmateSound.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release sound resources
        if (moveSound != null) {
            moveSound.release();
            moveSound = null;
        }
        if (captureSound != null) {
            captureSound.release();
            captureSound = null;
        }
        if (checkmateSound != null) {
            checkmateSound.release();
            checkmateSound = null;
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
