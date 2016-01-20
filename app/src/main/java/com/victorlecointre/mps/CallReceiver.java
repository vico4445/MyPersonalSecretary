package com.victorlecointre.mps;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract.*;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.TelephonyManager;
import android.telephony.gsm.SmsManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.Map;

/**
 * Created by victorlecointre on 04/01/15.
 */

public class CallReceiver extends BroadcastReceiver implements OnCallAlgorithm{
    //public static final String PREFS_NAME = "AccountCheckedPref";
    //public static final String PREFS_NAME_SMS = "PeopleSMSSent";

    private SharedPreferences settings ;
    private SharedPreferences settingsSMSsent ;
    private SharedPreferences SMS_Value ;
    private SharedPreferences.Editor editorSMSsent ;

    private Context ctx;
    private long Current_time_long;
    private long Max_time_long;

    private Boolean Occupied = Boolean.FALSE;
    private String message;

 //   private int call_state = 0; // Is it ringinf ?
    @Override
    public void onReceive(Context context, Intent intent) {
        this.ctx = context;

        System.out.println("DEBUG : onReceive");
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String state = extras.getString(TelephonyManager.EXTRA_STATE);
            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                //call_state = 1;
                String phoneNumber = extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

                update_current_and_max_time(); // Update time to look into the calendar [current time ;  end of day]
                settings = ctx.getSharedPreferences(PREFS_NAME, 0);
                List<TimeTable> l = null;
                List<Cursor> cur = null;
                TimeTable t = null;

                cur = ListEventsInCalendars(); // Retrieve the Events in the calendars; Need to ask content provider to the app
                l = TimeToCallBack(cur); // Return a list of potential TimeTable to callback
                t = SelectTimeTable(l,0); // Return the TimeTable when to callback [t_min ; t_max]
                action(context,t,phoneNumber); // Send an Sms if the user is busy
            }
//            else{
//                call_state = 0;
//            }
        }
    }

    public void update_current_and_max_time() {
        Date date = new Date();
        Current_time_long = date.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String input = (String) DateFormat.format("yyyy-MM-dd 23:59:59.000", date.getTime());

        Date datemax = sdf.parse(input, new ParsePosition(0));
        Max_time_long = datemax.getTime();

        //SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
        ContentResolver cr = ctx.getContentResolver();
        Uri uri = Events.CONTENT_URI;
        String selection = "((" + Events.CALENDAR_ID + " = ?) AND ("
                + Events.DTEND + " >= ?) AND ("
                + Events.DTSTART + " <= ?) AND ("
                + Events.AVAILABILITY + "= 0))";
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

    /*TimeTable updateTimeTable(Cursor cur, TimeTable TimeFree) {
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
*/
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
                    Log.d("Algorithm Calendar", "Event Time Table configuration not supported");
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
        Collections.sort(PotentialTimes, new MyTimeTableComp());
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
        sendSMS(context, t, phoneNumber);
    }
    void sendSMS(Context context,TimeTable t, String phoneNumber){
        if (Occupied) {
            try {
                SmsManager smsManager = SmsManager.getDefault();

                if (t != null) {
                    Date t_min = new Date(t.getTime_min());
                    Date t_max = new Date(t.getTime_max());
                    SimpleDateFormat df = new SimpleDateFormat("HH:mm");

                    SMS_Value = context.getSharedPreferences(PREFS_SMS_MESSAGE, 0);
                    message = SMS_Value.getString("SMS_Today", context.getString(R.string.SMS_Example));

                    smsManager.sendTextMessage(phoneNumber, null, String.format(message, df.format(t_min), df.format(t_max)), null, null);
                    Toast.makeText(context, "SMS sent to :" + phoneNumber + "\nCall back : [" + df.format(t_min) + "-" + df.format(t_max) + "]", Toast.LENGTH_LONG).show();
                    UpdateSharedPref(phoneNumber, df.format(t_min), df.format(t_max)); // Keep in memory fact of sending the sms

                } else {
                    smsManager.sendTextMessage(phoneNumber, null, message_tomorrow, null, null);
                }
            } catch (Exception e) {
                Toast.makeText(context, "SMS failed", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

    void UpdateSharedPref(String phonenumber, String t_min, String t_max){
        settingsSMSsent = ctx.getSharedPreferences(PREFS_NAME_SMS, 0);
        editorSMSsent = settingsSMSsent.edit();

        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        String time_call = df.format(Current_time_long);

        String contact;
        if ((contact = GetContactFromNumber(phonenumber)) == null){
            contact = phonenumber;
        }

        int size = settingsSMSsent.getInt("Size",0);

        editorSMSsent.putString(Integer.toString(size+1),contact);
        editorSMSsent.putString(Integer.toString(size+2),"Called at "+time_call);
        editorSMSsent.putString(Integer.toString(size+3),"["+t_min+"-"+t_max+"]");
        editorSMSsent.putInt("Size",size+3);
        editorSMSsent.commit();
    }

    String GetContactFromNumber(String phonenumber) {
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phonenumber));

        String name = null;
        Cursor cursor = ctx.getContentResolver().query(uri,
                new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            cursor.close();
        }
        return name;
    }
}


