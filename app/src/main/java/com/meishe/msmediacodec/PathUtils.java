package com.meishe.msmediacodec;

import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * * All rights reserved,Designed by www.meishesdk.com
 *
 * @Author : lpf
 * @CreateDate : 2022/6/13 上午10:41
 * @Description :
 * @Copyright :www.meishesdk.com Inc.All rights reserved.
 */
public class PathUtils {

    private static final String TAG = PathUtils.class.getName();

    private static String SDK_FILE_ROOT_DIRECTORY = "MSMediaCodec" + File.separator;

    private static String RECORD_DIRECTORY = SDK_FILE_ROOT_DIRECTORY + "record";

    /**
     * 删除文件
     * @param filePath
     */
    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    deleteDirectoryFile(f);
                }
            }
        } else if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 删除文件和文件夹
     * @param filePath
     */
    public static void deleteFileAndDir(String filePath) {
        File file = new File(filePath);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    deleteDirectoryFile(f);
                }
            }
            file.delete();
        } else if (file.exists()) {
            file.delete();
        }
    }


    public static void deleteDirectoryFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    deleteDirectoryFile(f);
                }
            }
            /*
             * 如要保留文件夹，只删除文件，请注释这行
             * To keep the folder and delete only the files, comment this line
             * */
            //file.delete();
        } else if (file.exists()) {
            file.delete();
        }
    }


    public static String getRecordDir() {
        return getFolderDirPath(RECORD_DIRECTORY);
    }


    public static String getFolderDirPath(String dstDirPathToCreate) {
        File dstFileDir = new File(Environment.getExternalStorageDirectory(), dstDirPathToCreate);
        if (AndroidOS.USE_SCOPED_STORAGE) {
            dstFileDir = new File(App.getContext().getExternalFilesDir(""), dstDirPathToCreate);
        }
        if (!dstFileDir.exists() && !dstFileDir.mkdirs()) {
            Log.e(TAG, "Failed to create file dir path--->" + dstDirPathToCreate);
            return null;
        }
        return dstFileDir.getAbsolutePath();
    }



}
