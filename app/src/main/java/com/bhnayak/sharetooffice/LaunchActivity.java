package com.bhnayak.sharetooffice;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class LaunchActivity extends AppCompatActivity {

    static final int NEW_NOTE_REQUEST = 1;  // The request code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type) || "text/html".equals(type) || type.startsWith("image/") ) {
                Intent sendIntent = new Intent(LaunchActivity.this, NewNoteActivity.class);
                sendIntent.setAction(action);
                sendIntent.setType(type);
                sendIntent.putExtras(intent.getExtras());
                LaunchActivity.this.startActivityForResult(sendIntent, NEW_NOTE_REQUEST);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        finish();
    }
}
