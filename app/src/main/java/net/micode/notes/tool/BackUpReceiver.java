package net.micode.notes.tool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by zhangmeng on 16/12/4.
 */


// 更新数据库的广播接收器,库的广播接收器,
public class BackUpReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "开始备份日记", Toast.LENGTH_LONG).show();
        // 设置全局定时器(闹钟) 秒后再发广播通知本广播接收器触发执行.
        // 这种方式很像JavaScript中的 setTimeout(xxx,)
        final BackupUtils backup = BackupUtils.getInstance(context);
        backup.exportToText(false);
        backup.exportToXMl(false);
        Toast.makeText(context, "结束备份日记", Toast.LENGTH_LONG).show();
    }
}
