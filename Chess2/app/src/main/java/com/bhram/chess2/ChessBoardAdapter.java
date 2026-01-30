package com.bhram.chess2;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ChessBoardAdapter extends BaseAdapter {

    private Context context;
    private ChessGame game;
    private OnSquareClickListener listener;

    public interface OnSquareClickListener {
        void onSquareClick(int row, int col);
    }

    public ChessBoardAdapter(Context context, ChessGame game) {
        this.context = context;
        this.game = game;
    }

    public void setOnSquareClickListener(OnSquareClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return 64;
    }

    @Override
    public Object getItem(int position) {
        int row = position / 8;
        int col = position % 8;
        return game.getPiece(row, col);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RelativeLayout squareLayout;
        TextView pieceTextView;

        if (convertView == null) {
            squareLayout = new RelativeLayout(context);

            // Set fixed size in pixels
            int squareSize = 100;

            GridView.LayoutParams gridParams = new GridView.LayoutParams(squareSize, squareSize);
            squareLayout.setLayoutParams(gridParams);
            squareLayout.setMinimumWidth(squareSize);
            squareLayout.setMinimumHeight(squareSize);

            pieceTextView = new TextView(context);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, 
                RelativeLayout.LayoutParams.MATCH_PARENT
            );
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            pieceTextView.setLayoutParams(params);
            pieceTextView.setTextSize(24);
            pieceTextView.setTypeface(Typeface.DEFAULT_BOLD);
            pieceTextView.setGravity(Gravity.CENTER);

            squareLayout.addView(pieceTextView);
            squareLayout.setTag(pieceTextView);
        } else {
            squareLayout = (RelativeLayout) convertView;
            pieceTextView = (TextView) squareLayout.getTag();
        }

        int row = position / 8;
        int col = position % 8;

        // Set square color - use same color for all squares to verify visibility
        squareLayout.setBackgroundColor(Color.parseColor("#F0D9B5"));

        // Set piece text
        Piece piece = game.getPiece(row, col);
        if (piece != null) {
            String pieceInitial = getPieceInitial(piece);
            pieceTextView.setText(pieceInitial);

            // Set text color based on piece color
            if (piece.getColor() == Piece.Color.WHITE) {
                pieceTextView.setTextColor(Color.WHITE);
                pieceTextView.setShadowLayer(4, 2, 2, Color.BLACK);
            } else {
                pieceTextView.setTextColor(Color.BLACK);
                pieceTextView.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
            }
            pieceTextView.setVisibility(View.VISIBLE);
        } else {
            pieceTextView.setVisibility(View.GONE);
        }

        // Set click listener
        final int finalRow = row;
        final int finalCol = col;
        squareLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSquareClick(finalRow, finalCol);
            }
        });

        return squareLayout;
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
}