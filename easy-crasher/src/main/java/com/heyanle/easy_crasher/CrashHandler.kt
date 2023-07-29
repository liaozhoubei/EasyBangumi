package com.heyanle.easy_crasher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.os.Environment
import android.os.Process
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Thread.UncaughtExceptionHandler
import java.text.SimpleDateFormat
import java.util.Date


/**
 * Created by HeYanLe on 2022/9/4 15:10.
 * https://github.com/heyanLE
 */
class CrashHandler(
    private val context: Context
) : Thread.UncaughtExceptionHandler {

    private val TAG: String? = "CrashHandler"
    private val DEBUG = true

    private val PATH = Environment.getExternalStorageDirectory().path + "/CrashTest/log/"
    private val FILE_NAME = "crash"
    private val FILE_NAME_SUFFIX = ".trace"

    //    private val sInstance: CrashHandler = CrashHandler()
    private var mDefaultCrashHandler: UncaughtExceptionHandler? = null
    private var mContext: Context? = null

    init {
        init(context)
    }

    fun init(context: Context) {
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        mContext = context.applicationContext
    }

    /**
     * 这个是最关键的函数，当程序中有未被捕获的异常，系统将会自动调用#uncaughtException方法
     * thread为出现未捕获异常的线程，ex为未捕获的异常，有了这个ex，我们就可以得到异常信息。
     */
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        try {
            //导出异常信息到SD卡中
            dumpExceptionToSDCard(ex)
            uploadExceptionToServer()
            //这里可以通过网络上传异常信息到服务器，便于开发人员分析日志从而解决bug
        } catch (e: IOException) {
            e.printStackTrace()
        }
        ex.printStackTrace()

        //如果系统提供了默认的异常处理器，则交给系统去结束我们的程序，否则就由我们自己结束自己
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler!!.uncaughtException(thread, ex)
        } else {
            Process.killProcess(Process.myPid())
        }
    }

    @Throws(IOException::class)
    private fun dumpExceptionToSDCard(ex: Throwable) {
        //如果SD卡不存在或无法使用，则无法把异常信息写入SD卡
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            if (DEBUG) {
                Log.w(TAG, "sdcard unmounted,skip dump exception")
                return
            }
        }
        val dir = File(PATH)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val current = System.currentTimeMillis()
        val time: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(current))
        try {
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            printWriter.println(time)
            dumpPhoneInfo(printWriter)
            printWriter.println()

            ex.printStackTrace(printWriter)
            var th: Throwable? = ex.cause
            while (th != null) {
                th.printStackTrace(printWriter)
                th = th.cause
            }
            ex.printStackTrace()

            printWriter.close()

            val intent = Intent(context, CrashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra(CrashActivity.KEY_ERROR_MSG, stringWriter.toString())
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "dump crash info failed")
        }
    }

    @Throws(NameNotFoundException::class)
    private fun dumpPhoneInfo(pw: PrintWriter) {
        val pm = mContext!!.packageManager
        val pi = pm.getPackageInfo(mContext!!.packageName, PackageManager.GET_ACTIVITIES)
        pw.print("App Version: ")
        pw.print(pi.versionName)
        pw.print('_')
        pw.println(pi.versionCode)

        //android版本号
        pw.print("OS Version: ")
        pw.print(Build.VERSION.RELEASE)
        pw.print("_")
        pw.println(Build.VERSION.SDK_INT)

        //手机制造商
        pw.print("Vendor: ")
        pw.println(Build.MANUFACTURER)

        //手机型号
        pw.print("Model: ")
        pw.println(Build.MODEL)

        //cpu架构
        pw.print("CPU ABI: ")
        pw.println(Build.CPU_ABI)
    }

    private fun uploadExceptionToServer() {
        //TODO Upload Exception Message To Your Web Server
    }
}