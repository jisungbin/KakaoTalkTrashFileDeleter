package com.sungbin.kakaotalk.trashfile.deleter

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.fsn.cauly.CaulyAdInfoBuilder
import com.fsn.cauly.CaulyInterstitialAd
import com.shashank.sony.fancytoastlib.FancyToast
import java.io.File

class AppWidgetProvider : android.appwidget.AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val views = RemoteViews(context.packageName, R.layout.layout_widget)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisAppWidget = ComponentName(context.packageName, AppWidgetProvider::class.java.name)
        val appWidgets = appWidgetManager.getAppWidgetIds(thisAppWidget)

        val action = intent.action
        if (action == ACTION_RELOAD) {
            onUpdate(context, appWidgetManager, appWidgets)
        } else if (action == ACTION_DELETE) {
            startDeleteTask()
            AppWidgetManager.getInstance(context)
                .updateAppWidget(ComponentName(context, AppWidgetProvider::class.java), views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

    }

    override fun onEnabled(context: Context) {}

    override fun onDisabled(context: Context) {}

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var ctx: Context? = null
        private const val ACTION_RELOAD = "RELOAD"
        private const val ACTION_DELETE = "DELETE"
        private var totalSize = 0.0f
        private var views: RemoteViews? = null
        private var appWidgetManager: AppWidgetManager? = null
        private var appWidgetId = 0

        internal fun updateAppWidget(
            context: Context, appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            this.appWidgetId = appWidgetId
            this.appWidgetManager = AppWidgetManager.getInstance(context)
            ctx = context
            views = RemoteViews(context.packageName, R.layout.layout_widget)

            LoadSizeTask().execute()

            val reloadIntent = Intent(context, AppWidgetProvider::class.java)
            reloadIntent.action = ACTION_RELOAD
            val reloadPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                reloadIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
            views!!.setOnClickPendingIntent(R.id.face, reloadPendingIntent)

            val deleteIntent = Intent(context, AppWidgetProvider::class.java)
            deleteIntent.action = ACTION_DELETE
            val deletePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            views!!.setOnClickPendingIntent(R.id.clear, deletePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            AppWidgetManager.getInstance(context)
                .updateAppWidget(ComponentName(context, AppWidgetProvider::class.java), views)
        }

        private fun toast(string: String, type: Int = FancyToast.SUCCESS){
            Handler(Looper.getMainLooper()).postDelayed({
                FancyToast.makeText(ctx, string,
                    FancyToast.LENGTH_SHORT, type, false).show()
            }, 0)
        }

        @SuppressLint("StaticFieldLeak")
        private class LoadSizeTaskWithoutToast : AsyncTask<Void?, Void?, Void?>() {

            override fun onPreExecute() {
                totalSize = 0.0f
            }

            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    val path = readData("path", "null")!!
                    if(path != "null")
                        for (i in path.indices) totalSize += getFolderSize(path.split("\n")[i])
                    totalSize += getFolderSize(
                        Environment.getExternalStorageDirectory().absolutePath +
                                "/Android/data/com.kakao.talk")
                } catch (e: Exception) {
                    toast("GetSizeTask에서 오류 발생.\n$e", FancyToast.ERROR)
                }

                return null
            }

            @SuppressLint("SetTextI18n")
            override fun onPostExecute(result: Void?) {
                val size = "임시폴더 : ${String.format("%.2f", totalSize)}MB"
                views!!.setTextViewText(R.id.state, size)

                when {
                    totalSize.toInt()<=100 -> {
                        views!!.setTextColor(R.id.state, Color.parseColor("#31B404"))
                        views!!.setImageViewResource(R.id.face, R.drawable.ic_good_face_green_24dp)
                    }
                    totalSize.toInt() in 101..500 -> {
                        views!!.setTextColor(R.id.state, Color.parseColor("#9E9E9E"))
                        views!!.setImageViewResource(R.id.face, R.drawable.ic_normal_face_gray_24dp)
                    }
                    totalSize.toInt()>500 -> {
                        views!!.setTextColor(R.id.state, Color.parseColor("#FA5858"))
                        views!!.setImageViewResource(R.id.face, R.drawable.ic_bad_face_red_24dp)
                    }
                }

                appWidgetManager!!.updateAppWidget(appWidgetId, views!!)
            }
        }

        @SuppressLint("StaticFieldLeak")
        private class LoadSizeTask : AsyncTask<Void?, Void?, Void?>() {

            override fun onPreExecute() {
                toast("위젯 리로드중...")
                totalSize = 0.0f
            }

            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    val path = readData("path", "null")!!
                    if(path != "null")
                        for (i in path.indices) totalSize += getFolderSize(path.split("\n")[i])
                    totalSize += getFolderSize(
                        Environment.getExternalStorageDirectory().absolutePath +
                                "/Android/data/com.kakao.talk")
                } catch (e: Exception) {
                    toast("GetSizeTask에서 오류 발생.\n$e", FancyToast.ERROR)
                }

                return null
            }

            @SuppressLint("SetTextI18n")
            override fun onPostExecute(result: Void?) {
                val size = "임시폴더 : ${String.format("%.2f", totalSize)}MB"
                views!!.setTextViewText(R.id.state, size)

                when {
                    totalSize.toInt()<=100 -> {
                        views!!.setTextColor(R.id.state, Color.parseColor("#31B404"))
                        views!!.setImageViewResource(R.id.face, R.drawable.ic_good_face_green_24dp)
                    }
                    totalSize.toInt() in 101..500 -> {
                        views!!.setTextColor(R.id.state, Color.parseColor("#9E9E9E"))
                        views!!.setImageViewResource(R.id.face, R.drawable.ic_normal_face_gray_24dp)
                    }
                    totalSize.toInt()>500 -> {
                        views!!.setTextColor(R.id.state, Color.parseColor("#FA5858"))
                        views!!.setImageViewResource(R.id.face, R.drawable.ic_bad_face_red_24dp)
                    }
                }

                appWidgetManager!!.updateAppWidget(appWidgetId, views!!)
                toast("위젯 리로드 완료!")
            }
        }

        private fun getFolderSize(path: String): Float {
            return getFolderAllSize(path) / 1000f / 1000f
        }

        private fun getFolderAllSize(path: String): Long {
            try {
                var totalSize: Long = 0
                val file = File(path)
                val childFileList = file.listFiles() ?: return 0

                for (childFile in childFileList) {
                    totalSize += if (childFile.isDirectory) {
                        getFolderAllSize(childFile.absolutePath)
                    } else {
                        childFile.length()
                    }
                }
                return totalSize
            } catch (e: Exception) {
                return 0
            }
        }

        private fun readData(name: String, _null: String): String? {
            val pref = ctx!!.getSharedPreferences("pref", Context.MODE_PRIVATE)
            return pref.getString(name, _null)
        }

        private fun startDeleteTask(){
            val path = readData("path", "null")!!
            if(path != "null")
                for (i in path.indices) FileDeleteTask().execute(path.split("\n")[i])
            FileDeleteTask().execute(Environment.getExternalStorageDirectory().absolutePath +
                    "/Android/data/com.kakao.talk")
        }

        private fun deleteFolder(path: String) {
            try {
                val dir = File(path)
                val childFileList = dir.listFiles()
                if (dir.exists()) {
                    for (childFile in childFileList) {
                        if (childFile.isDirectory) {
                            deleteFolder(childFile.absolutePath)
                        } else {
                            childFile.delete()
                        }
                    }
                    dir.delete()
                }
            } catch (e: Exception) {
                toast("폴더 삭제중 오류 발생.\n$e", FancyToast.ERROR)
            }
        }

        @SuppressLint("StaticFieldLeak")
        private class FileDeleteTask : AsyncTask<String?, Void?, Void?>() {
            override fun onPreExecute() {
                toast("임시폴더 삭제중...")
            }

            override fun doInBackground(vararg params: String?): Void? {
                try {
                    deleteFolder(params[0]!!)
                } catch (e: Exception) {
                    toast("DeleteTask에서 오류 발생.\n$e", FancyToast.ERROR)
                }

                return null
            }

            override fun onPostExecute(result: Void?) {
                toast("임시폴더 삭제 완료!\n총 ${String.format("%.2f", totalSize)}MB 용량 확보")
                LoadSizeTaskWithoutToast().execute()
            }
        }

    }

}
