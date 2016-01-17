package com.victorlecointre.mps;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

//Test
public class Sms extends ActionBarActivity implements OnCallAlgorithm{

    private SharedPreferences SMS_Value ;
    private SharedPreferences.Editor editorSMS ;
    EditText mEdit;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        SMS_Value = getSharedPreferences(PREFS_SMS_MESSAGE, 0);
        editorSMS = SMS_Value.edit();

        mEdit = (EditText) findViewById(R.id.SMS_Example);

        // Restore preferences
        String message;
        if ((message = SMS_Value.getString("SMS_Today", "")) != "") {
            mEdit.setText(message);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sms, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Buttons responses
    public void SaveSMSChanges(View v){
        editorSMS.putString("SMS_Today", mEdit.getText().toString());
        editorSMS.commit();
        Toast.makeText(getApplicationContext(), "Changes Saved", Toast.LENGTH_SHORT).show();
    }

    public void action(Context context,TimeTable t, String phoneNumber){}
}
