package com.bhram.chess2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragment displayed when the game ends, showing the winner and providing options
 * to start a new game or return to the home page.
 */
public class GameOverFragment extends Fragment {

    private TextView textViewWinner;
    private TextView textViewGameResult;
    private TextView textViewMoveCount;
    private TextView textViewGameTime;
    private Button buttonNewGame;
    private Button buttonBackToHome;
    
    private String winner;
    private String gameResult;
    private int moveCount;
    private String gameTime;
    
    private OnGameOverActionListener listener;

    public interface OnGameOverActionListener {
        void onNewGame();
        void onBackToHome();
    }

    public static GameOverFragment newInstance(String winner, String gameResult, int moveCount, String gameTime) {
        GameOverFragment fragment = new GameOverFragment();
        Bundle args = new Bundle();
        args.putString("winner", winner);
        args.putString("gameResult", gameResult);
        args.putInt("moveCount", moveCount);
        args.putString("gameTime", gameTime);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            winner = getArguments().getString("winner");
            gameResult = getArguments().getString("gameResult");
            moveCount = getArguments().getInt("moveCount");
            gameTime = getArguments().getString("gameTime");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game_over, container, false);
        
        initViews(view);
        setupClickListeners();
        updateUI();
        
        return view;
    }

    private void initViews(View view) {
        textViewWinner = view.findViewById(R.id.textViewWinner);
        textViewGameResult = view.findViewById(R.id.textViewGameResult);
        textViewMoveCount = view.findViewById(R.id.textViewMoveCount);
        textViewGameTime = view.findViewById(R.id.textViewGameTime);
        buttonNewGame = view.findViewById(R.id.buttonNewGame);
        buttonBackToHome = view.findViewById(R.id.buttonBackToHome);
    }

    private void setupClickListeners() {
        buttonNewGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onNewGame();
                }
            }
        });

        buttonBackToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    showBackToHomeConfirmation();
                }
            }
        });
    }

    private void updateUI() {
        if (winner != null) {
            textViewWinner.setText(winner);
        }
        
        if (gameResult != null) {
            textViewGameResult.setText(gameResult);
        }
        
        textViewMoveCount.setText(String.valueOf(moveCount));
        textViewGameTime.setText(gameTime != null ? gameTime : "00:00");
    }

    public void setOnGameOverActionListener(OnGameOverActionListener listener) {
        this.listener = listener;
    }

    private void showBackToHomeConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Return to Home");
        builder.setMessage("Are you sure you want to return to the home page? Your current game will be lost.");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener != null) {
                    listener.onBackToHome();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }
}