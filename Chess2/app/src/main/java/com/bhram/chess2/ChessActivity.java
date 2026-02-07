package com.bhram.chess2;

import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChessActivity extends AppCompatActivity {
    
    private ImageView[][] pieceViews = new ImageView[8][8];
    private GridLayout chessBoard;
    private String[] columns = {"a", "b", "c", "d", "e", "f", "g", "h"};
    private String[] rows = {"8", "7", "6", "5", "4", "3", "2", "1"};
    
    private ChessGame game;
    private Button backButton;
    private Button forwardButton;

    private ImageView floatingPiece;
    private TextView player1NameText;
    private TextView player2NameText;
    private TextView player1ClockText;
    private TextView player2ClockText;
    private android.os.Handler clockHandler;
    private Runnable clockRunnable;
    private long player1Time = 600000; // 10 minutes in milliseconds
    private long player2Time = 600000; // 10 minutes in milliseconds
    private boolean isClockRunning = false;
    private Piece.Color currentPlayerInClock;
    private int[] lastMoveFrom= null;
    private int[] lastMoveTo = null;
    private Piece[][] previousBoardState = new Piece[8][8];
    private boolean isCaptureMove = false;
    private MediaPlayer moveSound;
    private MediaPlayer captureSound;
    private MediaPlayer checkmateSound;
    private LinearLayout moveHistoryContainer;
    private int moveCounter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chess);

        game = new ChessGame();

        chessBoard = findViewById(R.id.chessBoard);

        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);

        player1NameText = findViewById(R.id.player1Name);
        player2NameText = findViewById(R.id.player2Name);
        player1ClockText = findViewById(R.id.player1Clock);
        player2ClockText = findViewById(R.id.player2Clock);
        moveHistoryContainer = findViewById(R.id.moveHistoryContainer);

        clockHandler = new android.os.Handler();

        player1NameText.setText("Player 1 (White)");
        player2NameText.setText("Player 2 (Black)");
        
        // Initialize clocks
        updatePlayerClocks();

        initializeBoard();

        updateBoard();

        backButton.setOnClickListener(v -> goBackMove());
        forwardButton.setOnClickListener(v -> goForwardMove());

        // Start White's clock immediately when game starts
        startPlayerClock(Piece.Color.WHITE);
        
        // Initialize sound players
        initializeSoundPlayers();
    }

    private void initializeBoard() {
        createBoardSquares();
    }
    
    private void createBoardSquares() {
        chessBoard.removeAllViews();
        
        int squareSize = 300; // 500dp board width divided by 8 squares = 62.5dp per square
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                // Create a RelativeLayout for each square
                RelativeLayout square = new RelativeLayout(this);
                
                // Set square size to exactly match board dimensions
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.rowSpec = GridLayout.spec(row,1f);
                params.columnSpec = GridLayout.spec(col,1f);
                square.setLayoutParams(params);
                
                // Set background color using blue and white theme
                boolean isLightSquare = (row + col) % 2 == 0;
                int bgColor = isLightSquare ? Color.parseColor("#FFFFFF") : Color.parseColor("#1976D2"); // White and dark blue
                square.setBackgroundColor(bgColor);
                
                // Create coordinate label (only for edge squares)
                if (row == 7 || col == 0) { // Bottom row or left column
                    TextView coordLabel = new TextView(this);
                    coordLabel.setTextSize(8f); // Very small font
                    coordLabel.setTypeface(null, Typeface.BOLD);
                    
                    // Set text color for better contrast
                    int textColor = isLightSquare ? Color.parseColor("#1976D2") : Color.parseColor("#FFFFFF");
                    coordLabel.setTextColor(textColor);
                    
                    RelativeLayout.LayoutParams labelParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    );
                    
                    if (row == 7 && col == 0) {
                        // Bottom-left corner: show "a1"
                        coordLabel.setText("a1");
                        labelParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                        labelParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        labelParams.leftMargin = 2;
                        labelParams.bottomMargin = 2;
                    } else if (row == 7) {
                        // Bottom row: show column letters
                        coordLabel.setText(columns[col]);
                        labelParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                        labelParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        labelParams.leftMargin = 2;
                        labelParams.bottomMargin = 2;
                    } else if (col == 0) {
                        // Left column: show row numbers
                        coordLabel.setText(rows[row]);
                        labelParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        labelParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        labelParams.leftMargin = 2;
                        labelParams.topMargin = 2;
                    }
                    
                    coordLabel.setLayoutParams(labelParams);
                    square.addView(coordLabel);
                }
                
                // Create ImageView for chess piece
                ImageView pieceView = new ImageView(this);
                RelativeLayout.LayoutParams pieceParams = new RelativeLayout.LayoutParams(
                    squareSize,squareSize
                );
                pieceParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                pieceView.setLayoutParams(pieceParams);
                pieceView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                
                square.addView(pieceView);
                pieceViews[col][row] = pieceView;
                
                // Set click listener
                final int finalRow = row;
                final int finalCol = col;
                square.setOnClickListener(v -> onSquareClick(finalRow, finalCol));
                
                chessBoard.addView(square);
            }
        }
    }
    
    private void setupInitialPieces() {
        // White pieces
        placePiece(0, 7, R.drawable.rook_white); // a1 rook
        placePiece(1, 7, R.drawable.knight_white); // b1 knight
        placePiece(2, 7, R.drawable.bishop_white); // c1 bishop
        placePiece(3, 7, R.drawable.queen_white); // d1 queen
        placePiece(4, 7, R.drawable.king_white); // e1 king
        placePiece(5, 7, R.drawable.bishop_white); // f1 bishop
        placePiece(6, 7, R.drawable.knight_white); // g1 knight
        placePiece(7, 7, R.drawable.rook_white); // h1 rook
        
        // White pawns
        for (int col = 0; col < 8; col++) {
            placePiece(col, 6, R.drawable.pawn_white);
        }
        
        // Black pieces
        placePiece(0, 0, R.drawable.rook_black); // a8 rook
        placePiece(1, 0, R.drawable.knight_black); // b8 knight
        placePiece(2, 0, R.drawable.bishop_black); // c8 bishop
        placePiece(3, 0, R.drawable.queen_black); // d8 queen
        placePiece(4, 0, R.drawable.king_black); // e8 king
        placePiece(5, 0, R.drawable.bishop_black); // f8 bishop
        placePiece(6, 0, R.drawable.knight_black); // g8 knight
        placePiece(7, 0, R.drawable.rook_black); // h8 rook
        
        // Black pawns
        for (int col = 0; col < 8; col++) {
            placePiece(col, 1, R.drawable.pawn_black);
        }
    }
    
    private void placePiece(int col, int row, int pieceResId) {
        if (pieceViews[col][row] != null) {
            pieceViews[col][row].setImageResource(pieceResId);
            pieceViews[col][row].setVisibility(View.VISIBLE);
        }
    }


    private void resetGame() {
        game = new ChessGame();
        initializeBoard();
        updateBoard();
        resetPlayerClocks();
        // Start White's clock first when game starts
        startPlayerClock(Piece.Color.WHITE);
        // Clear move history for new game
        moveHistoryContainer.removeAllViews();
        moveCounter = 1;
        Toast.makeText(this, "New game", Toast.LENGTH_SHORT).show();
    }
    
    private void goBackMove() {
        if (game.canGoBack()) {
            // Debug: Log current state before going back
            System.out.println("DEBUG: Before goBack - currentMoveIndex: " + (game.isNavigating() ? "navigating" : "not navigating"));
            System.out.println("DEBUG: Before goBack - moveHistory size: " + game.getMoveHistory().size());
            
            game.goBack();
            
            // Debug: Log state after going back
            System.out.println("DEBUG: After goBack - currentMoveIndex: " + (game.isNavigating() ? "navigating" : "not navigating"));
            System.out.println("DEBUG: After goBack - moveHistory size: " + game.getMoveHistory().size());
            
            updateBoard();
            // Show the correct move number - if navigating, show the move index + 1
            int moveNumber = game.isNavigating() ? (game.getCurrentMoveNumber() - 1) : (game.getCurrentMoveNumber() - 1);
            Toast.makeText(this, "Moved back to move " + moveNumber, Toast.LENGTH_SHORT).show();
            // Debug toast to show move history
            Toast.makeText(this, game.getMoveHistoryDebug(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Cannot go back further - " + game.getMoveHistoryDebug(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void goForwardMove() {
        if (game.canGoForward()) {
            game.goForward();
            updateBoard();
            Toast.makeText(this, "Moved forward to move " + game.getCurrentMoveNumber(), Toast.LENGTH_SHORT).show();
            // Debug toast to show move history
            Toast.makeText(this, game.getMoveHistoryDebug(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Cannot go forward further - " + game.getMoveHistoryDebug(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void exitNavigation() {
        game.exitNavigation();
        updateBoard();
    }
    
    // Save current board state for capture detection
    private void saveBoardState() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                previousBoardState[row][col] = game.getPiece(row, col);
            }
        }
    }
    
    // Detect if a move was a capture by comparing board states
    private boolean detectCaptureMove(int fromRow, int fromCol, int toRow, int toCol) {
        // Check for en passant capture first
        if (game.isEnPassantCapture(fromRow, fromCol, toRow, toCol)) {
            android.util.Log.d("ChessSound", "Detected en passant capture");
            return true;
        }
        
        // Check if the destination square had an opponent's piece before the move
        Piece previousPieceAtDestination = previousBoardState[toRow][toCol];
        Piece movingPiece = game.getPiece(toRow, toCol);
        
        if (movingPiece != null && previousPieceAtDestination != null) {
            // If there was a piece at destination and it's different from what's there now,
            // or if the moving piece is different color from the previous piece
            if (previousPieceAtDestination.getColor() != movingPiece.getColor()) {
                android.util.Log.d("ChessSound", "Detected capture: opponent piece was at destination");
                return true;
            }
        }
        
        // For pawn captures (diagonal moves), check if it's a capture pattern
        if (movingPiece != null && movingPiece.getType() == Piece.Type.PAWN) {
            if (Math.abs(fromCol - toCol) == 1 && fromRow != toRow) {
                // Pawn moved diagonally - this is a capture move
                android.util.Log.d("ChessSound", "Detected pawn capture (diagonal move)");
                return true;
            }
        }
        
        // If no capture detected, it's a normal move
        android.util.Log.d("ChessSound", "No capture detected - normal move");
        return false;
    }

    private void onSquareClick(int row, int col) {
        // Add bounds checking
        if (row < 0 || row >= 8 || col < 0 || col >= 8) {
            return;
        }

        // If in navigation mode, clicking anywhere exits navigation
        if (game.isNavigating()) {
            exitNavigation();
            return;
        }

        if (game.isGameOver()) {
            return;
        }

        // Check if we're in the middle of a promotion
        if (game.isPromoting()) {
            // If promotion is in progress, ignore clicks until promotion is complete
            return;
        }

        if (game.isPieceSelected()) {
            // Track the move for animation
            lastMoveFrom = new int[]{game.getSelectedRow(), game.getSelectedCol()};
            lastMoveTo = new int[]{row, col};
            
            // Save current board state before making the move
            saveBoardState();
            
            boolean moveSuccessful = game.movePiece(row, col);

            if (moveSuccessful) {
                // Check if this move triggered a promotion
                if (game.isPromoting()) {
                    // Show promotion dialog
                    showPromotionDialog();
                    return; // Don't update board yet, wait for promotion selection
                }
                
                // Determine if this was a capture move by comparing board states
                isCaptureMove = detectCaptureMove(game.getSelectedRow(), game.getSelectedCol(), row, col);
                
                // Generate move notation and add to history
                String moveNotation = generateMoveNotation(game.getSelectedRow(), game.getSelectedCol(), row, col, isCaptureMove);
                addMoveToHistory(moveNotation);
                
                // Play appropriate sound based on move type
                if (isCaptureMove) {
                    android.util.Log.d("ChessSound", "Playing capture sound");
                    playCaptureSound();
                } else {
                    android.util.Log.d("ChessSound", "Playing move sound");
                    playMoveSound();
                }

                updateBoard();

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
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ImageView pieceView = pieceViews[col][row];
                if (pieceView != null) {
                    // Reset background color based on square position (using classic theme)
                    boolean isLightSquare = (row + col) % 2 == 0;
                    int bgColor = isLightSquare ? Color.parseColor("#F0D9B5") : Color.parseColor("#B58863");
                    // Note: We can't easily change the background of the RelativeLayout parent here
                    // The background is set in createBoardSquares and should remain consistent
                    
                    boolean isValidMove = false;
                    if (validMoves != null) {
                        for (int[] move : validMoves) {
                            if (move[0] == row && move[1] == col) {
                                isValidMove = true;
                                break;
                            }
                        }
                    }

                    Piece piece = game.getPiece(row, col);
                    boolean isNewPosition = lastMoveTo != null && lastMoveTo[0] == row && lastMoveTo[1] == col;

                    if (piece != null) {
                        // Display appropriate piece image based on type and color
                        switch (piece.getType()) {
                            case PAWN:
                                if (piece.getColor() == Piece.Color.BLACK) {
                                    pieceView.setImageResource(R.drawable.pawn_black);
                                } else {
                                    pieceView.setImageResource(R.drawable.pawn_white);
                                }
                                break;
                            case ROOK:
                                if (piece.getColor() == Piece.Color.BLACK) {
                                    pieceView.setImageResource(R.drawable.rook_black);
                                } else {
                                    pieceView.setImageResource(R.drawable.rook_white);
                                }
                                break;
                            case KNIGHT:
                                if (piece.getColor() == Piece.Color.BLACK) {
                                    pieceView.setImageResource(R.drawable.knight_black);
                                } else {
                                    pieceView.setImageResource(R.drawable.knight_white);
                                }
                                break;
                            case BISHOP:
                                if (piece.getColor() == Piece.Color.BLACK) {
                                    pieceView.setImageResource(R.drawable.bishop_black);
                                } else {
                                    pieceView.setImageResource(R.drawable.bishop_white);
                                }
                                break;
                            case QUEEN:
                                if (piece.getColor() == Piece.Color.BLACK) {
                                    pieceView.setImageResource(R.drawable.queen_black);
                                } else {
                                    pieceView.setImageResource(R.drawable.queen_white);
                                }
                                break;
                            case KING:
                                if (piece.getColor() == Piece.Color.BLACK) {
                                    pieceView.setImageResource(R.drawable.king_black);
                                } else {
                                    pieceView.setImageResource(R.drawable.king_white);
                                }
                                break;
                        }

                        // Show shadow on opponent pieces that can be captured as overlay
                        if (isValidMove && piece.getColor() != game.getCurrentPlayer()) {
                            // Create a combined drawable with piece image and circle overlay
                            pieceView.setImageDrawable(createPieceWithShadowDrawable(piece));
                        } else {
                            // Ensure normal piece display for non-capture moves
                            switch (piece.getType()) {
                                case PAWN:
                                    if (piece.getColor() == Piece.Color.BLACK) {
                                        pieceView.setImageResource(R.drawable.pawn_black);
                                    } else {
                                        pieceView.setImageResource(R.drawable.pawn_white);
                                    }
                                    break;
                                case ROOK:
                                    if (piece.getColor() == Piece.Color.BLACK) {
                                        pieceView.setImageResource(R.drawable.rook_black);
                                    } else {
                                        pieceView.setImageResource(R.drawable.rook_white);
                                    }
                                    break;
                                case KNIGHT:
                                    if (piece.getColor() == Piece.Color.BLACK) {
                                        pieceView.setImageResource(R.drawable.knight_black);
                                    } else {
                                        pieceView.setImageResource(R.drawable.knight_white);
                                    }
                                    break;
                                case BISHOP:
                                    if (piece.getColor() == Piece.Color.BLACK) {
                                        pieceView.setImageResource(R.drawable.bishop_black);
                                    } else {
                                        pieceView.setImageResource(R.drawable.bishop_white);
                                    }
                                    break;
                                case QUEEN:
                                    if (piece.getColor() == Piece.Color.BLACK) {
                                        pieceView.setImageResource(R.drawable.queen_black);
                                    } else {
                                        pieceView.setImageResource(R.drawable.queen_white);
                                    }
                                    break;
                                case KING:
                                    if (piece.getColor() == Piece.Color.BLACK) {
                                        pieceView.setImageResource(R.drawable.king_black);
                                    } else {
                                        pieceView.setImageResource(R.drawable.king_white);
                                    }
                                    break;
                            }
                        }

                        if (isNewPosition) {
                            pieceView.setAlpha(0f);
                            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(pieceView, "alpha", 0f, 1f);
                            fadeIn.setDuration(200);
                            fadeIn.start();
                        }
                    } else if (isValidMove) {
                        // Show black circle for valid moves on empty squares
                        pieceView.setImageDrawable(createBlackCircleDrawable());
                    } else {
                        pieceView.setImageResource(0);
                    }
                }
            }
        }
    }

    private void showGameOverMessage() {
        String winner = game.getWinner();
        String gameResult = "";
        
        if (winner.contains("Draw")) {
            gameResult = "Draw";
            Toast.makeText(this, "Game Over! " + winner, Toast.LENGTH_LONG).show();
        } else if (winner.contains("time")) {
            gameResult = "Time Up";
            Toast.makeText(this, "Game Over! " + winner, Toast.LENGTH_LONG).show();
        } else {
            gameResult = "Checkmate";
            Toast.makeText(this, "Game Over! " + winner + " wins!", Toast.LENGTH_LONG).show();
        }
        
        // Play checkmate sound if game ended in checkmate
        if (game.isCheckmate()) {
            playCheckmateSound();
        }
        
        stopClock();
        
        // Show Game Over Fragment
        showGameOverFragment(winner, gameResult);
    }
    
    private void switchPlayer() {
        // Stop current player's clock
        stopClock();
        
        // Determine which player's clock should be running
        currentPlayerInClock = game.getCurrentPlayer();

        // Start the appropriate player's clock
        startPlayerClock(currentPlayerInClock);
    }
    
    private void addMoveToHistory(String moveNotation) {
        // Create a TextView for the move
        TextView moveText = new TextView(this);
        moveText.setText(moveNotation);
        moveText.setTextSize(14f);
        moveText.setTextColor(Color.WHITE);
        moveText.setPadding(12, 8, 12, 8);
        moveText.setBackgroundColor(Color.parseColor("#4A3728"));
        moveText.setGravity(Gravity.CENTER);
        moveText.setMinWidth(80);
        moveText.setSingleLine(true);
        moveText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        moveText.setPaddingRelative(12, 8, 12, 8);
        
        // Add margin between moves for better spacing
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(4, 0, 4, 0);
        moveText.setLayoutParams(params);
        
        // Add to the move history container
        moveHistoryContainer.addView(moveText);
    }
    
    private String generateMoveNotation(int fromRow, int fromCol, int toRow, int toCol, boolean isCapture) {
        // Get the piece that was moved
        Piece movingPiece = game.getPiece(toRow, toCol);
        if (movingPiece == null) {
            return ""; // Should not happen for valid moves
        }
        
        // Build the move notation
        StringBuilder notation = new StringBuilder();
        
        // Add move number
        notation.append("  ").append(moveCounter++).append(".  ");

        // Add piece symbol (except for pawns)
        if (movingPiece.getType() != Piece.Type.PAWN) {
            switch (movingPiece.getType()) {
                case KNIGHT:
                    // Use Unicode knight symbol (♘ for white, ♞ for black)
                    notation.append(movingPiece.getColor() == Piece.Color.WHITE ? "♘" : "♞");
                    break;
                case BISHOP:
                    // Use text representation for bishop
                    notation.append(movingPiece.getColor() == Piece.Color.WHITE ? "B" : "b");
                    break;
                case ROOK:
                    // Use Unicode rook symbol (♖ for white, ♜ for black)
                    notation.append(movingPiece.getColor() == Piece.Color.WHITE ? "♖" : "♜");
                    break;
                case QUEEN:
                    // Use Unicode queen symbol (♕ for white, ♛ for black)
                    notation.append(movingPiece.getColor() == Piece.Color.WHITE ? "♕" : "♛");
                    break;
                case KING:
                    // Use Unicode king symbol (♔ for white, ♚ for black)
                    notation.append(movingPiece.getColor() == Piece.Color.WHITE ? "♔" : "♚");
                    break;
            }
        }
        
        // Add destination square
        notation.append(columns[toCol]).append(rows[toRow]);
        
        // Check if the move puts the opponent in check
        if (game.isInCheck()) {
            notation.append("+");
        }
        
        // Check if the move results in checkmate
        if (game.isCheckmate()) {
            notation.append("#");
        }
        
        return notation.toString();
    }
    
    private void startPlayerClock(Piece.Color player) {
        // Always stop current clock first to prevent conflicts
        stopClock();
        isClockRunning = true;
        currentPlayerInClock = player;
        
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                // Double-check if still running to prevent race conditions
                if (!isClockRunning) {
                    return;
                }
                
                try {
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
                } catch (Exception e) {
                    android.util.Log.e("ChessGame", "Timer error: " + e.getMessage());
                    stopClock();
                }
            }
        };
        clockHandler.post(clockRunnable);
    }
    
    private void stopClock() {
        isClockRunning = false;
        if (clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }
    
    private void resetPlayerClocks() {
        stopClock();
        player1Time = 600000; // 10 minutes
        player2Time = 600000; // 10 minutes
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
            try {
                if (moveSound.isPlaying()) {
                    moveSound.stop();
                }
                moveSound.seekTo(0);
                moveSound.start();
            } catch (Exception e) {
                android.util.Log.e("ChessSound", "Error playing move sound: " + e.getMessage());
            }
        }
    }
    
    private void playCaptureSound() {
        android.util.Log.d("ChessSound", "playCaptureSound called");
        if (captureSound != null) {
            try {
                if (captureSound.isPlaying()) {
                    captureSound.stop();
                }
                captureSound.seekTo(0);
                captureSound.start();
            } catch (Exception e) {
                android.util.Log.e("ChessSound", "Error playing capture sound: " + e.getMessage());
            }
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
    
    private void showGameOverFragment(String winner, String gameResult) {
        // Make the fragment container visible
        findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
        
        // Show Game Over Fragment
        GameOverFragment gameOverFragment = GameOverFragment.newInstance(winner, gameResult, moveCounter - 1, formatTime(player1Time + player2Time));
        gameOverFragment.setOnGameOverActionListener(new GameOverFragment.OnGameOverActionListener() {
            @Override
            public void onNewGame() {
                // Hide the fragment
                androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (fragment != null) {
                    getSupportFragmentManager().beginTransaction()
                        .remove(fragment)
                        .commit();
                }
                // Reset the game
                resetGame();
            }

            @Override
            public void onBackToHome() {
                // Hide the fragment
                androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (fragment != null) {
                    getSupportFragmentManager().beginTransaction()
                        .remove(fragment)
                        .commit();
                }
                // Navigate back to home
                finish();
            }
        });

        getSupportFragmentManager().beginTransaction()
            .add(R.id.fragment_container, gameOverFragment)
            .commit();
    }
    
    private void showPromotionDialog() {
        // Show the promotion dialog
        PawnPromotionDialog.showPromotionDialog(this, game.getPromotingPlayer(), new PawnPromotionDialog.OnPromotionSelectedListener() {
            @Override
            public void onPromotionSelected(Piece.Type promotionType) {
                // Complete the promotion
                game.setPromotionComplete(promotionType);
                
                // Update the board to show the promoted piece
                updateBoard();
                
                // Switch player after promotion is complete
                switchPlayer();
            }
        });
    }
    
    private String formatTime(long timeInMillis) {
        int totalSeconds = (int) (timeInMillis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}