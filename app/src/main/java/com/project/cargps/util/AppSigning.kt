package com.project.cargps.util


import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale

object AppSigning {
    const val MD5 = "MD5"
    const val SHA1 = "SHA1"
    const val SHA256 = "SHA256"

    /**
     * 返回一个签名的对应类型的字符串
     *
     * @param context
     * @param packageName
     * @param type
     * @return
     */
    fun getSingInfo(context: Context, packageName: String?, type: String): String {
        var tmp = "error!"
        try {
            val signs = getSignatures(context, packageName)
            val sig = signs!![0]
            if (MD5 == type) {
                tmp = getSignatureString(sig, MD5)
            } else if (SHA1 == type) {
                tmp = getSignatureString(sig, SHA1)
            } else if (SHA256 == type) {
                tmp = getSignatureString(sig, SHA256)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return tmp
    }

    /**
     * 返回对应包的签名信息
     *
     * @param context
     * @param packageName
     * @return
     */
    fun getSignatures(context: Context, packageName: String?): Array<Signature>? {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = context.packageManager.getPackageInfo(packageName!!, PackageManager.GET_SIGNATURES)
            return packageInfo.signatures
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 获取相应的类型的字符串（把签名的byte[]信息转换成16进制）
     *
     * @param sig
     * @param type
     * @return
     */
    fun getSignatureString(sig: Signature, type: String?): String {
        val hexBytes = sig.toByteArray()
        var fingerprint = "error!"
        try {
            val buffer = StringBuffer()
            val digest = MessageDigest.getInstance(type)
            if (digest != null) {
                digest.reset()
                digest.update(hexBytes)
                val byteArray = digest.digest()
                for (i in byteArray.indices) {
                    if (Integer.toHexString(0xFF and byteArray[i].toInt()).length == 1) {
                        buffer.append("0").append(Integer.toHexString(0xFF and byteArray[i].toInt())) //补0，转换成16进制
                    } else {
                        buffer.append(Integer.toHexString(0xFF and byteArray[i].toInt())) //转换成16进制
                    }
                }
                fingerprint = buffer.toString().uppercase(Locale.getDefault()) //转换成大写
                //fingerprint = buffer.toString().toLowerCase();//转换成小写
                val sb = StringBuilder()
                for (i in fingerprint.indices) {
                    sb.append(fingerprint[i])
                    if (i < fingerprint.length - 1 && (i + 1) % 2 == 0) {
                        sb.append(":")
                    }
                }
                fingerprint = sb.toString()
            }
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return fingerprint
    }
}