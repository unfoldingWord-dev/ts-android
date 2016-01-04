package com.door43.translationstudio;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import com.door43.translationstudio.newui.home.HomeActivity;

public class ProfileActivity extends AppCompatActivity {

    private EditText mName;
    private EditText mEmail;
    private EditText mPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mName = (EditText)findViewById(R.id.name_edittext);
        mEmail = (EditText)findViewById(R.id.email_edittext);
        mPhone = (EditText)findViewById(R.id.phone_edittext);

        findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMainActivity();
            }
        });
        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
