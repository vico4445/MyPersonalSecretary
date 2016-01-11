package com.victorlecointre.mps;

import android.content.Context;
import android.provider.CalendarContract;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by victorlecointre on 07/03/15.
 */
public interface OnCallAlgorithm {
    public static final String PREFS_NAME = "AccountCheckedPref";
    public static final String PREFS_NAME_SMS = "PeopleSMSSent";
    public static final String PREFS_ACTIV = "Active";
    public static final String PREFS_SMS_MESSAGE = "SMSmessage";

    public String message = "I am currently busy, please call me back between %s and %s this day " +
            "\nThank you" +
            "\n"+
            "\n#This message had been automatically sent by the app CallMeMaybe";

    public String message_tomorrow = "I am busy today, please call me back another day" +
            "\nThank you" +
            "\n"+
            "\n#This message had been automatically sent by the app CallMeMaybe";

    public static final String[] EVENT_INFOS = new String[] {
            CalendarContract.Events.DTSTART,                 // 3
            CalendarContract.Events.DTEND,                   // 4
    };

    public class TimeTable {
        long time_min ;
        long time_max;
        boolean Occupied;

        TimeTable(long time_min,long time_max){
            this.time_min = time_min;
            this.time_max = time_max;
            this.Occupied = Boolean.FALSE;
        }

        TimeTable(long time_min,long time_max,Boolean Occupied){
            this.time_min = time_min;
            this.time_max = time_max;
            this.Occupied = Occupied;
        }

        long getTime_max() {
            return time_max;
        }

        long getTime_min() {
            return time_min;
        }

        boolean getOccupied() {
            return Occupied;
        }

        public String toString(){
            Date t_min = new Date(time_min);
            Date t_max = new Date(time_max);
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

            return "["+df.format(t_min)+":"+df.format(t_max)+"]";
        }
    }

    public class MyTimeTableComp implements Comparator<TimeTable>{

        @Override
        public int compare(TimeTable e1, TimeTable e2) {
            if(e2.getTime_min() < e1.getTime_min()){
                return 1;
            } else {
                return -1;
            }
        }
    }

    public void action(Context context, TimeTable t, String phoneNumber);
}
