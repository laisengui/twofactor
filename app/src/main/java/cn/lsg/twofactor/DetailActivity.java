package cn.lsg.twofactor;

import static android.content.ContentValues.TAG;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class DetailActivity extends AppCompatActivity {

    private static final int REFRESH_INTERVAL = 5000; // 刷新间隔（毫秒）- 5秒
    String secretKey;

    private TextView countdownTextView;
    private ProgressBar countdownProgressBar;
    private CountDownTimer countDownTimer;

    private TextView[] digitViews;

    private int id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // 获取从主页面传递过来的数据
        String name = getIntent().getStringExtra("name");
        String user = getIntent().getStringExtra("user");
        id = getIntent().getIntExtra("id",0);
        secretKey = getIntent().getStringExtra("secretKey");
        int position = getIntent().getIntExtra("position", -1);

        // 显示数据
        TextView nameTextView = findViewById(R.id.tv_detail_name);
        TextView userTextView = findViewById(R.id.tv_detail_user);

        nameTextView.setText(name);
        userTextView.setText("用户: " + user);


        ImageButton backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> finish()); // 关闭当前页面返回上一页

        ImageButton delButton=findViewById(R.id.btn_del);
        delButton.setOnClickListener(v->{
            showDeleteConfirmationDialog();
        });

        digitViews = new TextView[]{
                findViewById(R.id.digit1),
                findViewById(R.id.digit2),
                findViewById(R.id.digit3),
                findViewById(R.id.digit4),
                findViewById(R.id.digit5),
                findViewById(R.id.digit6)
        };

        countdownTextView = findViewById(R.id.tv_countdown);
        countdownProgressBar = findViewById(R.id.progress_countdown);

        // 设置进度条最大值
        countdownProgressBar.setMax(30);

         refreshDetailInfo();

    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("删除确认");
        builder.setMessage("确定要删除此双因素秘钥吗？此操作不可撤销。");

        builder.setPositiveButton("确认删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 用户确认后执行删除
                MainActivity.mainActivity.delItem(id);
                finish();
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // 只是关闭对话框，什么都不做
            }
        });

        // 设置危险操作样式（可选）
        AlertDialog dialog = builder.create();
        dialog.show();

        // 将确认按钮设置为红色强调危险操作
//        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
//        if (positiveButton != null) {
//            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.red_600));
//        }
    }

    /**
     * 计算到下一个30秒整点的剩余时间
     */
    private long calculateRemainingTime() {
        long currentTime = System.currentTimeMillis() / 1000; // 当前时间（秒）
        long updateTime =currentTime/30*30;
        //距离刷新的时间
        long c=currentTime%30;

        return c ;
    }

    /**
     * 开始倒计时
     */
    private void startCountdown() {
        long remainingTime = 30-calculateRemainingTime();
        Log.d(TAG, "距离下次刷新还有: " + remainingTime + "s");

        // 初始化倒计时器
        countDownTimer = new CountDownTimer(remainingTime*1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "定时时间: " + millisUntilFinished + "ms");
                // 每秒更新一次进度
                long secondsLeft = calculateRemainingTime();
                int progress =  30-(int)(secondsLeft);
                countdownTextView.setText("下次刷新: " + progress + "秒");

                // 更新进度条（30秒为最大值）

                countdownProgressBar.setProgress(progress);
            }

            @Override
            public void onFinish() {
                // 倒计时结束，刷新内容
                refreshDetailInfo();
            }
        }.start();
    }

    // 刷新详情信息
    private void refreshDetailInfo() {
       String code= GoogleAuthenticatorUtils.generateTOTP(secretKey,System.currentTimeMillis() / 1000 / 30);
        displayVerificationCode(code);
        // 立即开始下一个倒计时周期
        startCountdown();
    }

    private void displayVerificationCode(String code) {
        for (int i = 0; i < 6; i++) {
            digitViews[i].setText(String.valueOf(code.charAt(i)));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 页面销毁时取消倒计时，防止内存泄漏
        if (countDownTimer != null) {
            countDownTimer.cancel();
            Log.d(TAG, "倒计时已取消");
        }
    }

}
