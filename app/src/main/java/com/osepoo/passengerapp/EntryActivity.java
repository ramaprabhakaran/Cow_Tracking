package com.osepoo.passengerapp;

import  android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class EntryActivity extends AppCompatActivity {

    private Button enterButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        // Find the button by its ID
        enterButton = findViewById(R.id.enterButton);
    }

    // This method is called when the "Enter" button is clicked
    public void onEnterButtonClick(View view) {
        // Start the MainActivity when the button is clicked
        startActivity(new Intent(this, MainActivity.class));
    }
}
