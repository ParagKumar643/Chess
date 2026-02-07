package com.bhram.chess2;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

public class PawnPromotionDialog {
    
    public interface OnPromotionSelectedListener {
        void onPromotionSelected(Piece.Type promotionType);
    }
    
    public static void showPromotionDialog(Context context, Piece.Color playerColor, 
                                         OnPromotionSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_pawn_promotion, null);
        
        builder.setView(dialogView);
        
        final AlertDialog dialog = builder.create();
        
        // Set player color indicator
        TextView playerColorText = dialogView.findViewById(R.id.textViewPlayerColor);
        if (playerColor == Piece.Color.WHITE) {
            playerColorText.setText("White's turn");
            playerColorText.setTextColor(context.getResources().getColor(R.color.blue));
        } else {
            playerColorText.setText("Black's turn");
            playerColorText.setTextColor(context.getResources().getColor(R.color.black));
        }
        
        // Set piece images based on player color
        ImageView queenImage = dialogView.findViewById(R.id.imageQueen);
        ImageView rookImage = dialogView.findViewById(R.id.imageRook);
        ImageView bishopImage = dialogView.findViewById(R.id.imageBishop);
        ImageView knightImage = dialogView.findViewById(R.id.imageKnight);
        
        if (playerColor == Piece.Color.WHITE) {
            queenImage.setImageResource(R.drawable.queen_white);
            rookImage.setImageResource(R.drawable.rook_white);
            bishopImage.setImageResource(R.drawable.bishop_white);
            knightImage.setImageResource(R.drawable.knight_white);
        } else {
            queenImage.setImageResource(R.drawable.queen_black);
            rookImage.setImageResource(R.drawable.rook_black);
            bishopImage.setImageResource(R.drawable.bishop_black);
            knightImage.setImageResource(R.drawable.knight_black);
        }
        
        // Set click listeners for each promotion option
        CardView cardQueen = dialogView.findViewById(R.id.cardQueen);
        CardView cardRook = dialogView.findViewById(R.id.cardRook);
        CardView cardBishop = dialogView.findViewById(R.id.cardBishop);
        CardView cardKnight = dialogView.findViewById(R.id.cardKnight);
        
        cardQueen.setOnClickListener(v -> {
            listener.onPromotionSelected(Piece.Type.QUEEN);
            dialog.dismiss();
        });
        
        cardRook.setOnClickListener(v -> {
            listener.onPromotionSelected(Piece.Type.ROOK);
            dialog.dismiss();
        });
        
        cardBishop.setOnClickListener(v -> {
            listener.onPromotionSelected(Piece.Type.BISHOP);
            dialog.dismiss();
        });
        
        cardKnight.setOnClickListener(v -> {
            listener.onPromotionSelected(Piece.Type.KNIGHT);
            dialog.dismiss();
        });
        
        // Make dialog non-cancelable
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        
        dialog.show();
    }
}