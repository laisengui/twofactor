package cn.lsg.twofactor;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
public class ItemDao {
    private DBHelper dbHelper;

    public ItemDao(Context context) {
        dbHelper = new DBHelper(context);
    }

    /**
     * 保存单个Item到数据库
     * @param item 要保存的Item对象
     * @return 新插入记录的ID，-1表示失败
     */
    public long saveItem(Item item) {
        // 获取可写数据库
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // 准备要插入的数据
        ContentValues values = new ContentValues();
        values.put("name", item.getName());
        values.put("user", item.getUser());
        values.put("secret", item.getSecretKey());

        // 插入数据并获取ID
        long id = db.insert(DBHelper.TABLE_NAME, null, values);

        // 关闭数据库连接
        db.close();

        return id;
    }

    /**
     * 从数据库加载所有Item
     * @return 所有Item的列表
     */
    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 查询所有记录，按ID降序排列（最新的在前面）
        Cursor cursor = db.query(
                DBHelper.TABLE_NAME,
                null, // 查询所有字段
                null, // 无查询条件
                null,
                null,
                null,
                DBHelper.COLUMN_ID + " DESC" // 排序
        );

        // 遍历查询结果
        if (cursor.moveToFirst()) {
            do {
                // 从游标中获取数据
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String user = cursor.getString(cursor.getColumnIndexOrThrow("user"));
                String secret = cursor.getString(cursor.getColumnIndexOrThrow("secret"));

                // 创建Item对象并添加到列表
                Item item = new Item(id, name, user, secret);
                items.add(item);
            } while (cursor.moveToNext());
        }

        // 关闭游标和数据库
        cursor.close();
        db.close();

        return items;
    }

    /**
     * 根据ID删除Item
     * @param id 要删除的Item的ID
     * @return 删除的行数
     */
    public int deleteItem(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int rowsDeleted = db.delete(
                DBHelper.TABLE_NAME,
                DBHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)}
        );

        db.close();
        return rowsDeleted;
    }

}
