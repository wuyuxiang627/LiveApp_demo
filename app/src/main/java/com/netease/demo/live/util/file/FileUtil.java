package com.netease.demo.live.util.file;

import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.netease.nim.uikit.NimUIKit;
import com.netease.nim.uikit.common.util.storage.StorageType;
import com.netease.nim.uikit.common.util.storage.StorageUtil;
import com.netease.nim.uikit.common.util.string.StringUtil;

import java.io.File;
import java.util.Locale;

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static boolean hasExtentsion(String filename) {
        int dot = filename.lastIndexOf('.');
        if ((dot > -1) && (dot < (filename.length() - 1))) {
            return true;
        } else {
            return false;
        }
    }

    // 获取文件扩展名
    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return "";
    }

    // 获取文件名
    public static String getFileNameFromPath(String filepath) {
        if ((filepath != null) && (filepath.length() > 0)) {
            int sep = filepath.lastIndexOf('/');
            if ((sep > -1) && (sep < filepath.length() - 1)) {
                return filepath.substring(sep + 1);
            }
        }
        return filepath;
    }

    // 获取不带扩展名的文件名
    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    public static String getMimeType(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return "";
        }
        String type = null;
        String extension = getExtensionName(filePath.toLowerCase());
        if (!TextUtils.isEmpty(extension)) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        Log.i(TAG, "url:" + filePath + " " + "type:" + type);

        // FIXME
        if (StringUtil.isEmpty(type) && filePath.endsWith("aac")) {
            type = "audio/aac";
        }

        return type;
    }

    public enum SizeUnit {
        Byte,
        KB,
        MB,
        GB,
        TB,
        Auto,
    }

    public static String formatFileSize(long size) {
        return formatFileSize(size, SizeUnit.Auto);
    }

    public static String formatFileSize(long size, SizeUnit unit) {
        if (size < 0) {
            return NimUIKit.getContext().getString(com.netease.nim.uikit.R.string.unknow_size);
        }

        final double KB = 1024;
        final double MB = KB * 1024;
        final double GB = MB * 1024;
        final double TB = GB * 1024;
        if (unit == SizeUnit.Auto) {
            if (size < KB) {
                unit = SizeUnit.Byte;
            } else if (size < MB) {
                unit = SizeUnit.KB;
            } else if (size < GB) {
                unit = SizeUnit.MB;
            } else if (size < TB) {
                unit = SizeUnit.GB;
            } else {
                unit = SizeUnit.TB;
            }
        }

        switch (unit) {
            case Byte:
                return size + "B";
            case KB:
                return String.format(Locale.US, "%.2fKB", size / KB);
            case MB:
                return String.format(Locale.US, "%.2fMB", size / MB);
            case GB:
                return String.format(Locale.US, "%.2fGB", size / GB);
            case TB:
                return String.format(Locale.US, "%.2fPB", size / TB);
            default:
                return size + "B";
        }
    }

    /**
     * 删除单个文件
     *
     * @param filePath 被删除文件的文件名
     * @return 文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * 递归删除文件及文件夹
     * @param file
     */
    public static void delete(File file) {
        if (file.isFile()) {
            file.delete();
            return;
        }

        if(file.isDirectory()){
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                file.delete();
                return;
            }

            for (int i = 0; i < childFiles.length; i++) {
                delete(childFiles[i]);
            }
            file.delete();
        }
    }

    /**
     * 获取或创建缩略图文件地址
     * @param name 显示名称
     * @param ext   扩展名
     * @param storageType   存储类型
     * @return 缩略图文件地址
     */
    public static String getThumbPath(String name, String ext, StorageType storageType) {
        String path = StorageUtil.getWritePath(name + ext, storageType);
        File file = new File(path);
        if (!file.exists()) {
            try {
                //在指定的文件夹中创建文件
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return path;
    }
}
