package com.victorlecointre.mps;

//import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class CalendarList extends ActionBarActivity implements OnCallAlgorithm {

    //Shared Preferences
    //public static final String PREFS_NAME = "AccountCheckedPref";
    private SharedPreferences settings ;
    private SharedPreferences.Editor editor ;

    // Fake Icoming call
    private SharedPreferences settingsSMSsent ;
    private SharedPreferences.Editor editorSMSsent;

    private long Current_time_long;
    private long Max_time_long;

    private Boolean Occupied = Boolean.FALSE;

    // Constants
    public static final String[] EVENT_PROJECTION = new String[] {
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
            CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
    };

    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_DISPLAY_NAME_INDEX = 2;

    private List<TextView> CalendarNameList;
    private List AccountListId;
    private List<CheckBox> CheckList;

    //Global On click listener for all views
    final View.OnClickListener mGlobal_OnClickListener = new View.OnClickListener() {
        public void onClick(final View v) {
            int index = CheckList.indexOf(((CheckBox) v));
            TextView account = CalendarNameList.get(index);

            if (((CheckBox) v).isChecked()) {
                editor.putString((String) account.getText(), String.valueOf(AccountListId.get(index)));
            }
            else{
                editor.remove((String) account.getText());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Restore preferences
        settings = getSharedPreferences(PREFS_NAME, 0);
        editor = settings.edit();

        editor.clear();

        Cursor cur = ListCalendars();
        CreateTextViews(cur);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_calendar_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.test_incoming:
                FakeCall();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /************** UI ******************/
    // Initialising
    void CreateTextViews(Cursor cur){
        // LinearLayout layout = (LinearLayout)findViewById(R.id.MyLinearLayout);
        LinearLayout layout = (LinearLayout)findViewById(R.id.ScrollViewLinearLayout);
        if (layout == null) {
            Log.i("Layout", "is null");
        }

        CalendarNameList = new ArrayList<TextView>();
        CheckList = new ArrayList<CheckBox>();
        AccountListId = new ArrayList();

        // Use the cursor to step through the returned records
        while (cur.moveToNext() && cur !=null) {
            long calID = 0;
            String displayName = null;
            //String accountName = null;
            //String ownerName = null;

            // Get the field values
            calID = cur.getLong(PROJECTION_ID_INDEX);
            //accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX);
            displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
            //ownerName = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX);

            // Do something with the values...
            LinearLayout newLayout = new LinearLayout(this);
            newLayout.setOrientation(LinearLayout.HORIZONTAL);
            TextView newAccount = new TextView(this);
            newAccount.setText(displayName);
            CheckBox newCheckBox = new CheckBox(this);
            newCheckBox.setChecked(CheckSharedPreferences(newAccount, calID)); // Check Shared pref
            newCheckBox.setEnabled(Boolean.TRUE);

            // Layouts
            newLayout.addView(newCheckBox);
            newLayout.addView(newAccount);
            layout.addView(newLayout);

            // Keep track of the account and checkboxes and id in three lists
            CalendarNameList.add(newAccount);
            AccountListId.add(calID);
            CheckList.add(newCheckBox);

            newCheckBox.setOnClickListener(mGlobal_OnClickListener);
        }

        cur.close();
    }

    // Buttons responses
    public void SaveChanges(View v){
        editor.commit();
        Toast.makeText(getApplicationContext(), "Changes Saved", Toast.LENGTH_SHORT).show();
    }

    /******** Methods *********/

    Cursor ListCalendars(){
        // Run query
        Cursor cur = null;
        ContentResolver cr = getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;

        // Submit the query and get a Cursor object back.
        cur = cr.query(uri, EVENT_PROJECTION, null, null, null);

        return cur;
    }
    Boolean CheckSharedPreferences(TextView account, long ID){
        if(settings.contains((String) account.getText())){
            editor.putString((String) account.getText(), String.valueOf(ID));
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    void FakeCall(){
        update_current_and_max_time(); // Update time to look into the calendar [current time ;  end of day]
        settings = getSharedPreferences(PREFS_NAME, 0);
        List<TimeTable> l = null;
        List<Cursor> cur = null;
        TimeTable t = null;


        cur = ListEventsInCalendars(); // Retrieve the Events in the calendars; Need to ask content provider to the app
        l = TimeToCallBack(cur); // Return a list of potential TimeTable to callback
        t = SelectTimeTable(l,0); // Return the TimeTable when to callback [t_min ; t_max]
        action(this,t,"FakeCall"); // Send an Sms if the user is busy

    }

    public void update_current_and_max_time() {
        Date date = new Date();
        Current_time_long = date.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String input = (String) DateFormat.format("yyyy-MM-dd 23:59:59.000", date.getTime());
        //String input = (String) DateFormat.format("yyyy-MM-dd 21:00:00.000", date.getTime());

        Date datemax = sdf.parse(input, new ParsePosition(0));
        Max_time_long = datemax.getTime();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    List<Cursor> ListEventsInCalendars() {
        List<Cursor> CursorEvents = new ArrayList<Cursor>();
        String Current_time = String.valueOf(Current_time_long);
        String Max_time = String.valueOf(Max_time_long);
        Map<String, ?> accounts = settings.getAll();
        for (Map.Entry<String, ?> entry : accounts.entrySet()) {
            CursorEvents.add(QueryCalendar((String) entry.getValue(),Current_time,Max_time));
        }
        return CursorEvents;
    }

    Cursor QueryCalendar(String ID_CALENDAR,String Current_time, String Max_time){
        // Run query
        Cursor cur = null;
        ContentResolver cr = getContentResolver();
        Uri uri = CalendarContract.Events.CONTENT_URI;
        String selection = "((" + CalendarContract.Events.CALENDAR_ID + " = ?) AND ("
                + CalendarContract.Events.DTEND + " >= ?) AND ("
                + CalendarContract.Events.DTSTART + " <= ?) AND ("
                + CalendarContract.Events.AVAILABILITY + "= 0))";
        String[] selectionArgs = new String[] {ID_CALENDAR,Current_time,Max_time};
        // Submit the query and get a Cursor object back.
        cur = cr.query(uri, EVENT_INFOS, selection, selectionArgs, null);
        return cur;
    }

    List<TimeTable> TimeToCallBack(List<Cursor> cur_list){
        // Return a TimeTable(0,0) if the user is not occupied
        // Return a TimeTable(tmin_tmax) otherwise, with the available timetable to call him back
        long t_min = Current_time_long;
        long t_max = Max_time_long;
        TimeTable TimeFree = new TimeTable(t_min,t_max);

        List<TimeTable> PotentialTimes = new ArrayList<TimeTable>();
        PotentialTimes.add(TimeFree);

        Occupied = Boolean.FALSE;
        for (Cursor cur : cur_list) {
            //TimeFree = updateTimeTable(cur,TimeFree);
            PotentialTimes = updateTimeTable(cur,PotentialTimes);
        }
        return PotentialTimes;
    }

    TimeTable updateTimeTable(Cursor cur, TimeTable TimeFree) {
        long t_min = TimeFree.getTime_min();
        long t_max = TimeFree.getTime_max();
        Boolean Occupied = TimeFree.getOccupied();

        while (cur.moveToNext()) {
            long EventsStartTime = 0;
            long EventsEndTime = 0;

            // Get the field values
            EventsStartTime = cur.getLong(0);
            EventsEndTime = cur.getLong(1);

            //IsOccupied ?
            if (Current_time_long >= EventsStartTime && Current_time_long <= EventsEndTime) {
                Occupied = Boolean.TRUE;
            }
            //Extract available Timetable
            if (t_min >= EventsEndTime) {
                continue;
            }
            if (t_max <= EventsStartTime) {
                continue;
            }
            if (t_min < EventsStartTime) {
                t_max = EventsStartTime;
            } else {
                t_min = EventsEndTime;
            }
        }
        cur.close();
        return new TimeTable(t_min,t_max,Occupied);
    }
    List<TimeTable> updateTimeTable(Cursor cur, List<TimeTable> PotentialTimes) {

        List<TimeTable> Tmp =new ArrayList<TimeTable>();
        while (cur.moveToNext()) {
            Tmp.clear();
            long EventsStartTime = 0;
            long EventsEndTime = 0;

            // Get the field values
            EventsStartTime = cur.getLong(0);
            EventsEndTime = cur.getLong(1);
            for(TimeTable TimeFree : PotentialTimes) {
                Tmp.add(TimeFree);
                long t_min = TimeFree.getTime_min();
                long t_max = TimeFree.getTime_max();

                //IsOccupied ?
                if (Current_time_long >= EventsStartTime && Current_time_long <= EventsEndTime) {
                    Occupied = Boolean.TRUE;
                    //Log.i("OCCUPIED","YES");
                }

                //Extract available Timetable

                //If event is outside TimeTable --> nothing to do
                if (t_min >= EventsEndTime) {
                    //Log.i("eventOutsidelow","YES");
                    continue;
                }
                if (t_max <= EventsStartTime) {
                    //Log.i("eventOutsideHigh","YES");
                    continue;
                }

                // Else something will be done
                Tmp.remove(TimeFree);
                //If event is inside TimeTable --> create two new TimeTable
                if(t_min < EventsStartTime && t_max > EventsEndTime){
                    Tmp.add(new TimeTable(t_min,EventsStartTime));
                    Tmp.add(new TimeTable(EventsEndTime,t_max));
                    //Log.i("eventInside","YES");
                }
                //If event is on the low border of the TimeTable
                else if(t_min>=EventsStartTime && t_max >EventsEndTime){
                    //Log.i("LowBorder","YES");
                    Tmp.add(new TimeTable(EventsEndTime,t_max));
                }
                //If event is on the high border of the TimeTable
                else if(t_min<EventsStartTime && t_max<=EventsEndTime){
                    //Log.i("HighBorder","YES");
                    Tmp.add(new TimeTable(t_min,EventsEndTime));
                }
                //If TimeTable is completely in the event Time
                else if(t_min>=EventsStartTime && t_max<=EventsEndTime){
                    //Log.i("Into the eventTime","YES");
                    continue;
                }
                else{
                    Log.d("Algorithm Calendar","Event Time Table configuration not supported");
                }
            }
            PotentialTimes.clear();
            for(TimeTable t : Tmp) {
                PotentialTimes.add(t);
            }
                /*for (TimeTable t : PotentialTimes){
                    Log.i("PotentialTimes",t.toString());
                }
                Log.i("PotentialTimes","Separator");*/
        }
        cur.close();

        return PotentialTimes;
    }

    public TimeTable SelectTimeTable(List<TimeTable> PotentialTimes,int selectn){
        Collections.sort(PotentialTimes,new MyTimeTableComp());
        for (TimeTable t:PotentialTimes) {
            Log.d("ListSorted", Long.toString(t.getTime_min())+" : "+ Long.toString(t.getTime_max()));
        }

        if (selectn< PotentialTimes.size()) {
            return PotentialTimes.get(selectn);
        }
        else{
            return null;
        }
    }

    public void action(Context context,TimeTable t, String phoneNumber){
        printMessage(t,phoneNumber);
    }

    void printMessage(TimeTable t,String phoneNumber){
        //if (t.getOccupied() == Boolean.TRUE) {
        if (Occupied) {
            if (t != null) {
                try {
                    Date t_min = new Date(t.getTime_min());
                    Date t_max = new Date(t.getTime_max());
                    SimpleDateFormat df = new SimpleDateFormat("HH:mm");

                    Toast.makeText(this, "SMS sent to :" + phoneNumber + "\nCall back between: [" + df.format(t_min) + "-" + df.format(t_max) + "]", Toast.LENGTH_LONG).show();
                    UpdateSharedPref(phoneNumber, df.format(t_min), df.format(t_max)); // Keep in memory fact of sending the sms
                } catch (Exception e) {
                    Toast.makeText(this, "SMS failed", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
            else {
                Toast.makeText(this, "SMS sent to :" + phoneNumber + "\nCall back tomorrow", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(this, " Nothing, you are not busy ", Toast.LENGTH_LONG).show();
        }
    }

    void UpdateSharedPref(String phonenumber, String t_min, String t_max){
        settingsSMSsent = getSharedPreferences(PREFS_NAME_SMS, 0);
        editorSMSsent = settingsSMSsent.edit();

        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        String time_call = df.format(Current_time_long);

        String contact = phonenumber;

        int size = settingsSMSsent.getInt("Size",0);

        editorSMSsent.putString(Integer.toString(size+1),contact);
        editorSMSsent.putString(Integer.toString(size+2),"Called at "+time_call);
        editorSMSsent.putString(Integer.toString(size+3),"["+t_min+"-"+t_max+"]");
        editorSMSsent.putInt("Size",size+3);
        editorSMSsent.commit();
    }
}
