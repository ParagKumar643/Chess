package com.bhram.chess2;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startGameButton = findViewById(R.id.startGameButton);

        startGameButton.setOnClickListener(v -> {


            Intent intent =  new Intent(MainActivity.this, com.bhram.chess2.ChessActivity.class);
            startActivity(intent);
        });
    }

}

