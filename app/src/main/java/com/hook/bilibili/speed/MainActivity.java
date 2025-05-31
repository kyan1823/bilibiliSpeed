package com.hook.bilibili.speed;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import android.widget.Button;
import android.widget.EditText;


public class MainActivity extends Activity {
    private static SharedPreferences prefs;
    public static float defValue = 1.0f;

    private EditText speedInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 获取SharedPreferences实例
        prefs = this.getSharedPreferences("speed", MODE_PRIVATE);
        if (prefs == null) {
            Toast.makeText(this, "不支持XSharedPreferences", Toast.LENGTH_SHORT).show();
        } else {
            setContentView(R.layout.activity_main);
            // 初始化UI组件
            speedInput = findViewById(R.id.speed_input);
            Button saveButton = findViewById(R.id.save_button);
            // 加载保存的配置
            loadPreferences();
            // 保存按钮点击事件
            saveButton.setOnClickListener(v -> savePreferences());
        }
    }

    /**
     * 从SharedPreferences加载配置
     */
    private void loadPreferences() {
        // 加载速度值，默认1
        float savedSpeed = prefs.getFloat("speed", defValue);
        speedInput.setText(String.valueOf(savedSpeed));
    }

    /**
     * 保存配置到SharedPreferences
     */
    private void savePreferences() {
        try {
            // 获取输入的速度值
            float speed = Float.parseFloat(speedInput.getText().toString());
            // 验证速度范围 [0.2-8.0]
            if (speed < 0.2f || speed > 8.0f) {
                Toast.makeText(this, "速度值必须在[0.2-8.0]", Toast.LENGTH_SHORT).show();
                return;
            }
            // 保存配置
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("speed");
            editor.apply();
            editor.putFloat("speed", speed);
            editor.commit();
            Toast.makeText(this, "配置已保存: " + speed + ",下个视频生效", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的速度值", Toast.LENGTH_SHORT).show();
        }
    }
}
