package net.micode.notes.application;

import android.app.Application;

import net.micode.notes.tool.AlarmManagerUtil;
import net.micode.notes.tool.BackupUtils;

import me.dawson.applock.core.LockManager;

/**
 * Created by zhangmeng on 15/11/14.
 */
public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LockManager.getInstance().enableAppLock(this);
        if(isBackup){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    isBackup=false;
                    AlarmManagerUtil.sendUpdateBroadcastRepeat(BaseApplication.this);
                    BackupUtils.getInstance(BaseApplication.this).exportToXMl(false);
                    BackupUtils.getInstance(BaseApplication.this).exportToText(false);
                }
            }).start();

        }
    }
    private static boolean isBackup=true;

//    private static final ArrayList<String> activityList = new ArrayList<>();
//    private boolean lcokStatus = true;
//    private boolean isUnlock = false;
//    private Activity currentActivity;
//
//    public void onCreate() {
//        super.onCreate();
//        activityList.add("net.micode.notes.ui.NotesListActivity");
//        activityList.add("net.micode.notes.ui.PassportActivity");
//        activityList.add("net.micode.notes.ui.NoteEditActivity");
//        activityList.add("net.micode.notes.ui.AlarmAlertActivity");
//        activityList.add("net.micode.notes.ui.NotesPreferenceActivity");
//        this.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
//
//            @Override
//            public void onActivityStopped(Activity activity) {
//
//            }
//
//            @Override
//            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
//
//            }
//
//            @Override
//            public void onActivityStarted(Activity activity) {
//
//            }
//
//            @Override
//            public void onActivityResumed(Activity activity) {
//                if ("net.micode.notes.ui.PassportActivity".equals(activity.getComponentName().getClassName()))
//                    return;
////                lcokStatus = !isUnlock || isBackground(activity);
//                if (isNeedLock(activity)) {
//                    lock(activity);
//                }
//            }
//
//            @Override
//            public void onActivityPaused(Activity activity) {
//                if ("net.micode.notes.ui.PassportActivity".equals(activity.getComponentName().getClassName()))
//                    return;
//                lcokStatus = !isUnlock || isBackground(activity);
//            }
//
//            @Override
//            public void onActivityDestroyed(Activity activity) {
//                if (!isUnlock && activity.getClass().equals(PassportActivity.class))
//                    System.exit(0);
//            }
//
//            @Override
//            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
//
//            }
//        });
//    }
//
//
//    public boolean isNeedLock(Activity activity) {
//        PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
//        boolean isScreenOn = pm.isScreenOn();
//
//        return activity.getSharedPreferences(Const.PASSPROT_PERFERENCE_NAME, MODE_PRIVATE).getBoolean(Const.PASSPROT_STATUS, false)
//                && (!isScreenOn || lcokStatus);
//    }
//
//    private void lock(Activity activity) {
//        Intent lcokIntent = new Intent(activity, PassportActivity.class);
//        lcokIntent.putExtra(Const.PASSPROT_INFO, Const.INNPUT_PASSPROT);
//        lcokIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        isUnlock = false;
//        startActivity(lcokIntent);
//    }
//
//    public void setIsUnlock(boolean isUnlock) {
//        this.isUnlock = isUnlock;
//    }
//
//    public static boolean isBackground(Context context) {
//
//
//        ActivityManager activityManager = (ActivityManager) context
//                .getSystemService(Context.ACTIVITY_SERVICE);
//        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
//                .getRunningAppProcesses();
//        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
//            if (appProcess.processName.equals(context.getPackageName())) {
//                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
////                    Log.i(String.format("Background App:", appProcess.processName));
//                    return true;
//                } else {
////                    Log.i(String.format("Foreground App:", appProcess.processName));
//                    return false;
//                }
//            }
//        }
//        return false;
//    }
}
