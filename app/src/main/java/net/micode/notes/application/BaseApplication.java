package net.micode.notes.application;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;

import net.micode.notes.common.Const;
import net.micode.notes.ui.PassportActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangmeng on 15/11/14.
 */
public class BaseApplication extends Application {
    private static final ArrayList<String> activityList = new ArrayList<>();
    private boolean lcokStatus = true;

    public void onCreate() {
        super.onCreate();
        activityList.add("net.micode.notes.ui.NotesListActivity");
        activityList.add("net.micode.notes.ui.PassportActivity");
        activityList.add("net.micode.notes.ui.NoteEditActivity");
        activityList.add("net.micode.notes.ui.AlarmAlertActivity");
        activityList.add("net.micode.notes.ui.NotesPreferenceActivity");
        this.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityStopped(Activity activity) {
                PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                boolean isScreenOn = pm.isScreenOn();
                setLcokStatus(!isScreenOn);
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                if (isNeedLock(activity)) {
                    Intent lcokIntent = new Intent(activity, PassportActivity.class);
                    lcokIntent.putExtra(Const.PASSPROT_INFO, Const.INNPUT_PASSPROT);
                    lcokIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(lcokIntent);
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks(1);
                ActivityManager.RunningTaskInfo rti = runningTasks.get(0);
                ComponentName component = rti.topActivity;
                if (activityList.contains(component.getClassName()))
                    setLcokStatus(false);
                else
                    setLcokStatus(true);
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (lcokStatus && activity.getClass().equals(PassportActivity.class))
                    System.exit(0);
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }
        });
    }


    public boolean isNeedLock(Activity activity) {
        return activity.getSharedPreferences(Const.PASSPROT_PERFERENCE_NAME, MODE_PRIVATE).getBoolean(Const.PASSPROT_STATUS, false) && lcokStatus;
    }

    public void setLcokStatus(boolean lcokStatus) {
        this.lcokStatus = lcokStatus;
    }
}
