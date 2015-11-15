package net.micode.notes.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.application.BaseApplication;
import net.micode.notes.common.Const;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by zhangmeng on 15/11/14.
 */
public class PassportActivity extends Activity {
    private byte mode = Const.SET_PASSPROT;
    private EditText passprot;
    private EditText passprotRepeat;
    private Button confirm;
    private SharedPreferences sharedPreferences ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password);
        sharedPreferences = getSharedPreferences(Const.PASSPROT_PERFERENCE_NAME,MODE_PRIVATE);

        mode = getIntent().getByteExtra(Const.PASSPROT_INFO, Const.SET_PASSPROT);
        initView();
    }

    private void initView() {
        passprot = (EditText) findViewById(R.id.password_text);
        passprotRepeat = (EditText) findViewById(R.id.password_repeat);
        confirm = (Button) findViewById(R.id.passport_confirm);
        confirm.setOnClickListener(listener);
        if (mode == Const.SET_PASSPROT) {
            setRepeatView(View.VISIBLE);
        }
    }


    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            switch (mode) {
                case Const.INNPUT_PASSPROT:
                    if(getMD5Code(passprot.getText().toString().trim()).equals(sharedPreferences.getString(Const.PASSPROT,""))){
                        ((BaseApplication)getApplication()).setLcokStatus(false);
                        finish();
                    }else
                        Toast.makeText(PassportActivity.this,R.string.passport_wrong,Toast.LENGTH_LONG).show();
                    break;
                case Const.SET_PASSPROT:
                    if (passprot.getText().toString().trim().equals(passprotRepeat.getText().toString().trim())) {
                        sharedPreferences.edit().putString(Const.PASSPROT, getMD5Code(passprot.getText().toString().trim())).apply();
                        sharedPreferences.edit().putBoolean(Const.PASSPROT_STATUS, true).apply();
                        ((BaseApplication)getApplication()).setLcokStatus(false);
                        finish();
                    }
                    else
                        Toast.makeText(PassportActivity.this,R.string.passport_different,Toast.LENGTH_LONG).show();
                        break;
                case Const.PASSPROT_RESET:
                    setRepeatView(View.GONE);
                    if(getMD5Code(passprot.getText().toString().trim()).equals(sharedPreferences.getString(Const.PASSPROT,""))){
                        ((BaseApplication)getApplication()).setLcokStatus(false);

                        Intent intent=new Intent(PassportActivity.this,PassportActivity.class);
                        intent.putExtra(Const.PASSPROT_INFO, Const.PASSPROT_RESET);
                        startActivity(intent);
                    }else{
                        Toast.makeText(PassportActivity.this,R.string.passport_different_old,Toast.LENGTH_LONG).show();
                    }

                    break;
                default:
                    break;
            }
        }
    };

    private String getMD5Code(String passort) {
        String resultString = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            // md.digest() 该函数返回值为存放哈希值结果的byte数组
            byte[] md5 = md.digest(passort.getBytes());
            resultString = new String(md5);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return resultString;
    }

    public void setRepeatView(int repeatView) {
        findViewById(R.id.password_repeat_nick).setVisibility(repeatView);
        passprotRepeat.setVisibility(repeatView);
    }
}
