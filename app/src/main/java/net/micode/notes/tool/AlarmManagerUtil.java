package net.micode.notes.tool;

/**
 * Created by zhangmeng on 16/12/4.
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import static android.content.Context.ALARM_SERVICE;

public class AlarmManagerUtil {
    public static final int REQUEST_CODE = 1;
    public static final int TRIGGER_TIME = 1000 * 60 * 60 * 24 * 7;
//    public static final int TRIGGER_TIME = 1000 * 5;

    public static AlarmManager getAlarmManager(Context ctx) {
        return (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
    }

    /**
     * 指定时间后进行更新赛事信息(有如闹钟的设置)
     * 注意: Receiver记得在manifest.xml中注册
     *
     * @param ctx
     */
    public static void sendUpdateBroadcast(Context ctx) {
        AlarmManager am = getAlarmManager(ctx);
        // 秒后将产生广播,触发UpdateReceiver的执行,这个方法才是真正的更新数据的操作主要代码
        Intent i = new Intent(ctx, BackUpReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, REQUEST_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.RTC, System.currentTimeMillis() + TRIGGER_TIME, pendingIntent);
    }

    /**
     * 取消定时执行(有如闹钟的取消)
     *
     * @param ctx
     */
    public static void cancelUpdateBroadcast(Context ctx) {
        AlarmManager am = getAlarmManager(ctx);
        Intent i = new Intent(ctx, BackUpReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, REQUEST_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pendingIntent);
    }

    public static void sendUpdateBroadcastRepeat(Context ctx) {
        Intent intent = new Intent(ctx, BackUpReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //开始时间
        long firstime = SystemClock.elapsedRealtime();
        AlarmManager am = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstime, TRIGGER_TIME, pendingIntent);
    }
}
