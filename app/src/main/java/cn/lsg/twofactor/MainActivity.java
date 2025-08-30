package cn.lsg.twofactor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private RecyclerView recyclerView;
    private LinearLayout emptyView;

    ItemAdapter adapter ;

    private ItemDao itemDao; // 数据库操作对象

    static MainActivity mainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        // 初始化数据库操作对象
        itemDao = new ItemDao(this);




        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
         adapter = new ItemAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        emptyView = findViewById(R.id.empty_view);

        // 初始化数据
        refreshData();


        // 设置添加按钮点击事件
        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNewItem();
            }
        });
        mainActivity=this;

    }

    // 初始化列表数据
    private void refreshData() {
        // 添加示例数据
//        itemList.add(new Item(1,"Github", "root@123.com","vlz6iozywltsr73o6b4f6u3ovyavwscp"));
//        itemList.add(new Item(2,"Server", "admin@123.com","vlz6iozywltsr73o6b4f6u3ovyavwscp"));
//        itemList.add(new Item(2,"Home", "admin@123.com","vlz6iozywltsr73o6b4f6u3ovyavwscp"));
        List<Item> itemList = itemDao.getAllItems();
//        itemList.add(new Item(2,"Home", "admin@123.com","vlz6iozywltsr73o6b4f6u3ovyavwscp"));
        if (itemList.isEmpty()) {
            // 显示空状态
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            // 显示数据
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
        adapter.updateData(itemList);
    }

    // 添加新项
    private void addNewItem() {
        checkCameraPermissionAndScan();
    }

    public void delItem(Integer id){
        itemDao.deleteItem(id);
        refreshData();
    }

    // 检查相机权限并开始扫描
    private void checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // 已有权限，开始扫描
            startQrCodeScan();
        } else {
            // 申请权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    // 开始二维码扫描
    private void startQrCodeScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("请对准二维码进行扫描");
        options.setCameraId(0); // 使用后置摄像头
        options.setBeepEnabled(true); // 扫描成功时发出提示音
        options.setBarcodeImageEnabled(true); // 保存扫描的图片
        options.setOrientationLocked(true);//锁定竖屏
        options.setCaptureActivity(CaptureActivity.class);
        barcodeLauncher.launch(options);
    }

    // 替换之前的扫描回调代码
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "扫描已取消", Toast.LENGTH_SHORT).show();
                } else {
                    // 处理扫描结果
                    processScanResult(result.getContents());
                }
            }
    );

    // 处理扫描结果
    private void processScanResult(String content) {
        // 这里可以根据实际需求处理二维码内容
        Map<String, String> params=GoogleAuthenticatorUtils.parseTOTPUri(content);

        Item item=new Item(null,params.get("issuer"),params.get("account"),params.get("secret"));

        itemDao.saveItem(item);
        refreshData();

        Toast.makeText(this, "扫描成功: " + content, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，开始扫描
                startQrCodeScan();
            } else {
                // 权限被拒绝，提示用户
                Toast.makeText(this, "需要相机权限才能扫描二维码", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
