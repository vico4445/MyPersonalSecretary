package com.victorlecointre.mps;

//import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.victorlecointre.additional_features.*;


public class MainActivity extends ActionBarActivity implements OnCallAlgorithm {

    //Shared Preferences
    //public static final String PREFS_NAME_SMS = "PeopleSMSSent";
    private SharedPreferences settingsSMSsent ;
    private SharedPreferences.Editor editorSMSsent ;

    //public static final String PREFS_ACTIV = "Active";
    private SharedPreferences settingsActiv ;
    private SharedPreferences.Editor editorActiv ;

    int SizePrefShare; // Use to check that the pref share file has not been modified by another instance

    //CallReceiver
    private CallReceiver Receiver;
    private IntentFilter callIntentFilter;

    //UI
    private Switch mySwitch;

    ListView listView ;
    private SimpleAdapter mAdapter;
    private ArrayList<HashMap<String,String>> list;

    private Menu mOptionsMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /******** CallReceiver ************/
        Initialise_callReceiver();

        /******* ListView ***********/
        configureListView();
    }

    @Override
    public void onStart() {
        super.onStart();

        settingsSMSsent = getSharedPreferences(PREFS_NAME_SMS, 0);
        editorSMSsent = settingsSMSsent.edit();
        settingsActiv = getSharedPreferences(PREFS_ACTIV,0);
        editorActiv = settingsActiv.edit();

        InitialiseSwitch();
        InitialiseListView();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // If the user clicks the "Refresh" button.
        if(item.getItemId() == R.id.menu_refresh){
            setRefreshActionButtonState(Boolean.TRUE);
            updateSharedPref();
            InitialiseListView();
            setRefreshActionButtonState(Boolean.FALSE);
            return true;
        }
        else if (item.getItemId() == R.id.menu_sms){
            Intent intent = new Intent(this, Sms.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop(){
        updateSharedPref();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /******** CallReceiver ************/

    public void Initialise_callReceiver(){
        //Creation
        Receiver = new CallReceiver();
        callIntentFilter = new IntentFilter("android.intent.action.PHONE_STATE");

        //Attatch Switch to CallReceiver
        mySwitch = (Switch) findViewById(R.id.switch1);
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if(isChecked){
                    mySwitch.setText("is On");
                    enableBroadcastReceiver();

                }else{
                    mySwitch.setText("is Off");
                    disableBroadcastReceiver();
                }
            }
        });
    }
    public void enableBroadcastReceiver(){
        registerReceiver(Receiver, callIntentFilter);
        Log.i("Broadcast","isOn");
        Toast.makeText(this, "Secretary activated", Toast.LENGTH_SHORT).show();
    }
    public void disableBroadcastReceiver() {
        unregisterReceiver (Receiver);
        Log.i("Broadcast","isOff");

        Toast.makeText(this, "Secretary de-activated",Toast.LENGTH_SHORT).show();
    }

    /************** UI ******************/
    // Initialising
    public void configureListView(){
        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.listSMSSent);

        // Defined Array values to show in ListView
        ArrayList<String> number = new ArrayList<String>();
        ArrayList<String> timetable = new ArrayList<String>();

        list = new ArrayList<HashMap<String,String>>();


        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third parameter - ID of the TextView to which the data is written
        // Forth - the Array of data

        mAdapter = new SimpleAdapter(
                this,
                list,
                R.layout.main_item_two_line_row,
                new String[] { "Contact","Time_called","Callback" },
                new int[] { R.id.text1, R.id.text2, R.id.text3 }  );
        listView.setAdapter(mAdapter);

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                int itemPosition = position;

                // ListView Clicked item value
                HashMap<String,String>  itemValue = (HashMap<String,String>) listView.getItemAtPosition(position);

                String posted_by = null;
                if ((posted_by =GetPhoneNumberfromContact(itemValue.get("Contact")))==null){
                    posted_by = itemValue.get("Contact");
                }

                String uri = "tel:" + posted_by.trim() ;
                Intent i = new Intent(Intent.ACTION_DIAL);
                i.setData(Uri.parse(uri));
                startActivity(i);
            }
        });

        SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener( listView,
                new SwipeDismissListViewTouchListener.DismissCallbacks() {
                    public boolean canDismiss(int position){
                        return true;
                    }
                    public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                        for (int position : reverseSortedPositions) {
                            list.remove(position);
                        }
                        mAdapter.notifyDataSetChanged();
                    }
                });
        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener(touchListener.makeScrollListener());
    }
    public String GetPhoneNumberfromContact(String name){
        String number = null;
        Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,null,
                "DISPLAY_NAME = '" + name + "'",null,null);
        if (cursor != null && cursor.moveToFirst()) {
            String contactId =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
            if (phones != null && phones.moveToFirst()) {
                number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
            phones.close();
        }
        cursor.close();

        return number;
    }

    public void InitialiseSwitch() {
        Boolean ON_OFF = settingsActiv.getBoolean("Active", Boolean.FALSE);
        Switch Act = (Switch) this.findViewById(R.id.switch1);
        Act.setChecked(ON_OFF);
    }
    public void InitialiseListView(){
        SizePrefShare = settingsSMSsent.getInt("Size",0);
        list.clear();
        Map<String, ?> sms = settingsSMSsent.getAll();
        if (!sms.isEmpty()) {
            // The Map is like
            // ( "1" , "Contact/Phone Number")
            // ( "2" , Time Called")
            // ( "3" , CallBack")

            String contact,timecall,callback;
            for (int i = 0; i < (sms.size() / 3); i = i + 1) {
                contact = (String) sms.get(Integer.toString(i*3+1));
                timecall = (String) sms.get(Integer.toString(i*3+2));
                callback = (String) sms.get(Integer.toString(i*3+3));
                AddItem(contact,timecall, callback);
            }
        }
    }

    // Buttons responses
    public void ShowCalendarList(View view) {
        Intent intent = new Intent(this, CalendarList.class);
        startActivity(intent);

    }

    //Additional Features
    public void AddItem(String contact,String Time_Called, String Callback) {
        HashMap<String,String> item = new HashMap<String,String>();
        item.put( "Contact",contact );
        item.put( "Time_called",Time_Called );
        item.put( "Callback",Callback );
        list.add( item );
        mAdapter.notifyDataSetChanged();
    }
    public void setRefreshActionButtonState(boolean refreshing) {
        if (mOptionsMenu == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    /************** Shared Preferences **************/
    public void updateSharedPref(){
        editorSMSsent.clear();
        int diff = Math.abs(settingsSMSsent.getInt("Size",0) - SizePrefShare);
        int count = 0;
        for (int i = 0; i < list.size(); i = i + 1) {
            editorSMSsent.putString(Integer.toString(i * 3 + 1), list.get(i).get("Contact"));
            editorSMSsent.putString(Integer.toString(i * 3 + 2), list.get(i).get("Time_called"));
            editorSMSsent.putString(Integer.toString(i * 3 + 3), list.get(i).get("Callback"));
            count = i * 3 + 3;
        }
        for (int i=1;i<=diff;i++){
            editorSMSsent.putString(Integer.toString(count+1),settingsSMSsent.getString(Integer.toString(SizePrefShare+i),""));
            count = count +1;
        }

        editorSMSsent.putInt("Size", count);
        editorSMSsent.commit();


        // Switch pref
        Switch Act = (Switch) this.findViewById(R.id.switch1);
        editorActiv.putBoolean("Active", Act.isChecked());
        editorActiv.commit();
    }

    public void action(Context context,TimeTable t, String phoneNumber){}
}
