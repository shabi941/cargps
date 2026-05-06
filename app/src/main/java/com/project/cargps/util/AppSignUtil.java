package com.project.cargps.util;



import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class AppSignUtil {
    public static final String MD5 = "MD5";
    public static final String SHA1 = "SHA1";
    public static final String SHA256 = "SHA256";

    /**
     * 返回一个签名的对应类型的字符串
     *
     * @param context
     * @param packageName
     * @param type
     * @return
     */
    public static String getSingInfo(Context context, String packageName, String type) {
        String tmp = "error!";
        try {
            Signature[] signs = getSignatures(context, packageName);
            Signature sig = signs[0];
            if (MD5.equals(type)) {
                tmp = getSignatureString(sig, MD5);
            } else if (SHA1.equals(type)) {
                tmp = getSignatureString(sig, SHA1);
            } else if (SHA256.equals(type)) {
                tmp = getSignatureString(sig, SHA256);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tmp;
    }

    /**
     * 返回对应包的签名信息
     *
     * @param context
     * @param packageName
     * @return
     */
    public static Signature[] getSignatures(Context context, String packageName) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            return packageInfo.signatures;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取相应的类型的字符串（把签名的byte[]信息转换成16进制）
     *
     * @param sig
     * @param type
     * @return
     */
    public static String getSignatureString(Signature sig, String type) {
        byte[] hexBytes = sig.toByteArray();
        String fingerprint = "error!";
        try {
            StringBuffer buffer = new StringBuffer();
            MessageDigest digest = MessageDigest.getInstance(type);
            if (digest != null) {
                digest.reset();
                digest.update(hexBytes);
                byte[] byteArray = digest.digest();
                for (int i = 0; i < byteArray.length; i++) {
                    if (Integer.toHexString(0xFF & byteArray[i]).length() == 1) {
                        buffer.append("0").append(Integer.toHexString(0xFF & byteArray[i])); //补0，转换成16进制
                    } else {
                        buffer.append(Integer.toHexString(0xFF & byteArray[i])); //转换成16进制
                    }
                }
                fingerprint = buffer.toString().toUpperCase(Locale.getDefault()); //转换成大写
                //fingerprint = buffer.toString().toLowerCase();//转换成小写
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < fingerprint.length(); i++) {
                    sb.append(fingerprint.charAt(i));
                    if (i < fingerprint.length() - 1 && (i + 1) % 2 == 0) {
                        sb.append(":");
                    }
                }
                fingerprint = sb.toString();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return fingerprint;
    }
}
