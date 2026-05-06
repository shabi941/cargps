package com.project.cargps.util;

import android.content.Context;

import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;

public class PermissionUtil {
    /**
     * 是否电话权限
     */
    public static boolean hasPhonePermission(Context context) {
        return XXPermissions.isGrantedPermission(context,
                PermissionLists.getReadPhoneStatePermission());
    }

    /**
     * 是否定位通知权限
     */
    public static boolean hasLocationPermission(Context context) {
        boolean hasPermissions1 = XXPermissions.isGrantedPermission(context,
                PermissionLists.getAccessFineLocationPermission());
        boolean hasPermissions2 = XXPermissions.isGrantedPermission(context,
                PermissionLists.getAccessCoarseLocationPermission());
        boolean hasPermissions3 = XXPermissions.isGrantedPermission(context,
                PermissionLists.getPostNotificationsPermission());

        return hasPermissions1 && hasPermissions2 && hasPermissions3;
    }

    /**
     * 是否有存储权限
     */
    public static boolean hasStoragePermission(Context context) {
        boolean hasPermissions1 = XXPermissions.isGrantedPermission(context,
                PermissionLists.getReadMediaImagesPermission());
        boolean hasPermissions2 = XXPermissions.isGrantedPermission(context,
                PermissionLists.getReadMediaVideoPermission());
        boolean hasPermissions3 = XXPermissions.isGrantedPermission(context,
                PermissionLists.getReadMediaAudioPermission());

        return hasPermissions1 && hasPermissions2 && hasPermissions3;
    }

    /**
     * 是否有安装权限
     */
    public static boolean hasInstallPermission(Context contex) {
        return XXPermissions.isGrantedPermission(contex,
                PermissionLists.getRequestInstallPackagesPermission());
    }
}
