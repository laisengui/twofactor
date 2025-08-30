package cn.lsg.twofactor;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.codec.binary.Base32;

import org.apache.commons.codec.binary.Hex;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Google Authenticator 双因素认证 工具类
 *
 * @author Norton Lai
 * @created 2024 -08-08 16:28:20
 */
public class GoogleAuthenticatorUtils {

    /**
     * 时间前后偏移量
     * 用于防止客户端时间不精确导致生成的TOTP与服务器端的TOTP一直不一致
     * 如果为0,当前时间为 10:10:15
     * 则表明在 10:10:00-10:10:30 之间生成的TOTP 能校验通过
     * 如果为1,则表明在
     * 10:09:30-10:10:00
     * 10:10:00-10:10:30
     * 10:10:30-10:11:00 之间生成的TOTP 能校验通过
     * 以此类推
     */
    private static final int TIME_OFFSET = 1;

    /**
     * 服务端 创建一个密钥
     *
     * @return the string
     * @author Norton Lai
     * @created 2024 -08-08 16:17:32
     */
    public static String createSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return new Base32().encodeToString(bytes).toLowerCase();
    }

    /**
     * 客户端 根据密钥获取验证码
     *  返回字符串是因为数值有可能以0开头
     *
     * @param secretKey 密钥
     * @param time      第几个30秒 System.currentTimeMillis() / 1000 / 30
     * @return the string
     * @author Norton Lai
     * @created 2024 -08-08 16:17:45
     */
    public static String generateTOTP(String secretKey, long time) {
        byte[] bytes = new Base32().decode(secretKey.toUpperCase());
        String hexKey = Hex.encodeHexString(bytes);
        String hexTime = Long.toHexString(time);
        return Totp.generateTOTP(hexKey, hexTime, "6");
    }

    /**
     * 服务端 生成 Google Authenticator Key Uri
     * Google Authenticator 规定的 Key Uri 格式: otpauth://totp/{issuer}:{account}?secret={secret}&issuer={issuer}
     * https://github.com/google/google-authenticator/wiki/Key-Uri-Format
     * 参数需要进行 url 编码 +号需要替换成%20
     *
     * @param secret  密钥 使用 createSecretKey 方法生成
     * @param account 用户账户 如: example@domain.com
     * @param issuer  服务名称 如: Google,GitHub
     */
    public static String createKeyUri(String secret, String account, String issuer) {

        try {
            String accountStr = URLEncoder.encode(account, "UTF-8").replace("+", "%20");
            String secretStr = URLEncoder.encode(secret, "UTF-8").replace("+", "%20");
            String issuerStr = URLEncoder.encode(issuer, "UTF-8").replace("+", "%20");
            String qrCodeStr = "otpauth://totp/" + issuerStr + ":" + accountStr + "?secret=" + secretStr + "&issuer=" + issuerStr;
            return qrCodeStr;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 解析TOTP二维码内容
     * @param qrContent 二维码扫描到的内容
     * @return 包含解析结果的Map，包含issuer, account, secret等字段
     * @throws IllegalArgumentException 当内容格式不正确时抛出异常
     */
    public static Map<String, String> parseTOTPUri(String qrContent) {
        // 验证输入不为空
        if (qrContent == null || qrContent.trim().isEmpty()) {
            throw new IllegalArgumentException("QR内容不能为空");
        }

        // 验证是否以otpauth://totp/开头
        if (!qrContent.startsWith("otpauth://totp/")) {
            throw new IllegalArgumentException("不是有效的TOTP URI格式");
        }

        Map<String, String> result = new HashMap<>();

        try {
            URI uri = new URI(qrContent);

            // 解析路径部分获取issuer和account
            String path = uri.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1); // 移除开头的斜杠
            }

            // 路径格式可能是 "issuer:account" 或直接是 "account"
            int colonIndex = path.indexOf(':');
            if (colonIndex != -1) {
                // 有issuer的情况
                String issuerFromPath = path.substring(0, colonIndex);
                String account = path.substring(colonIndex + 1);

                // 优先使用路径中的issuer，但可能被查询参数中的issuer覆盖
                result.put("account", account);
                result.put("issuer", issuerFromPath);
            } else {
                // 没有issuer，只有account
                result.put("account", path);
            }

            // 解析查询参数
            String query = uri.getQuery();
            if (query != null && !query.isEmpty()) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = keyValue[1];

                        // 特殊处理issuer参数，如果存在则覆盖路径中的issuer
                        if ("issuer".equals(key)) {
                            result.put("issuer", value);
                        } else {
                            result.put(key, value);
                        }
                    }
                }
            }

            // 验证必须的secret字段是否存在
            if (!result.containsKey("secret")) {
                throw new IllegalArgumentException("TOTP URI缺少必需的secret参数");
            }

            return result;

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("无效的URI格式: " + e.getMessage(), e);
        }
    }

    /**
     * 服务端 校验方法
     *
     * @param secretKey 密钥
     * @param totpCode  TOTP 一次性密码
     * @return 验证结果 boolean
     * @author Norton Lai
     * @created 2024 -08-08 16:18:14
     */
    public static boolean verification(String secretKey, String totpCode) {
        long time = System.currentTimeMillis() / 1000 / 30;
        // 优先计算当前时间,然后再计算偏移量,因为大部分情况下客户端与服务的时间一致
        if ( generateTOTP(secretKey, time).equals(totpCode)) {
            return true;
        }
        for (int i = -TIME_OFFSET; i <= TIME_OFFSET; i++) {
            // i == 0 的情况已经算过
            if (i != 0) {
                if ( generateTOTP(secretKey, time + i).equals(totpCode)) {
                    return true;
                }
            }
        }
        return false;
    }

}
