package com.sungbin.kakaotalk.trashfile.deleter

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.android.synthetic.main.activity_main.*
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.AsyncTask
import android.os.Environment
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.*
import android.widget.*
import cn.pedant.SweetAlert.SweetAlertDialog
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import kotlin.math.round
import android.widget.RelativeLayout
import com.fsn.cauly.*
import com.fsn.cauly.CaulyInterstitialAd


class MainActivity : AppCompatActivity(), CaulyAdViewListener, CaulyInterstitialAdListener {

    private var alert: SweetAlertDialog? = null
	private var totalSize = 0.0f
    private var face: ImageView? = null
    private var size: TextView? = null
    private var state: TextView? = null
    private var adView: CaulyAdView? = null
    private var interstial: CaulyInterstitialAd? = null
    private var showInterstitial = false
    private var rootView: RelativeLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar!!.subtitle = "ⓒ 2019 SungBin. all rights reserved."

        requestPermission()
        rootView = findViewById(R.id.adview)
        face = findViewById(R.id.face)
        state = findViewById(R.id.state)
        size = findViewById(R.id.size)
        if(readData("init", "false")!!.toBoolean()) LoadSizeTask().execute()
        clear.setOnClickListener {
            if(readData("noti", "false")!!.toBoolean()) startDeleteTask()
            else showDeleteNotice()
        }
        requestLoadAd()
    }

    override fun onFailedToReceiveAd(p0: CaulyAdView?, p1: Int, p2: String?) {
        toast("배너 광고 로드중에 오류가 발생했습니다.\n$p2", FancyToast.ERROR)
    }

    override fun onCloseLandingScreen(p0: CaulyAdView?) {
    }

    override fun onShowLandingScreen(p0: CaulyAdView?) {
        toast("광고를 클릭해 주셔서 감사합니다.")
    }

    override fun onReceiveAd(p0: CaulyAdView?, p1: Boolean) {
    }

    override fun onLeaveInterstitialAd(p0: CaulyInterstitialAd?) {
    }

    override fun onReceiveInterstitialAd(ad: CaulyInterstitialAd?, p1: Boolean) {
        if(showInterstitial){
            ad!!.show()
            showInterstitial = false
        }
        else ad!!.cancel()
    }

    override fun onClosedInterstitialAd(p0: CaulyInterstitialAd?) {
    }

    override fun onFailedToReceiveInterstitialAd(p0: CaulyInterstitialAd?, p1: Int, p2: String?) {
        toast("전면 광고 로드중에 오류가 발생했습니다.\n$p2", FancyToast.ERROR)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> showSettingDialog()
            R.id.action_tips -> showTipsDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showTipsDialog(){
        val dialog = AlertDialog.Builder(MainActivity@this)
        dialog.setTitle("어플 사용 팁")
        dialog.setMessage("1. 어플 위젯이 존재합니다.\n2. 어플 위젯에서 얼굴모양 아이콘을 누르면 위젯을 리로드 할 수 있습니다.")
        dialog.setPositiveButton("닫기", null)
        dialog.show()
    }

    private fun showDeleteNotice(){
        val dialog = AlertDialog.Builder(MainActivity@this)
        dialog.setTitle("임시폴더 삭제 주의사항")
        dialog.setMessage("카카오톡의 임시폴더를 삭제하게 되면 카카오톡에서 사진을 불러오는데 문제가 생길 수 있습니다. (지금까지 받은 사진 한정)\n\n이 메세지는 한 번만 표시됩니다.")
        dialog.setPositiveButton("확인 (삭제)") { _: DialogInterface, _: Int ->
            startDeleteTask()
            saveData("noti", "true")
        }
        dialog.setNegativeButton("취소", null)
        dialog.show()
    }

    private fun requestLoadAd(){
        val adInfo =
            CaulyAdInfoBuilder("69uUWcir").effect("TopSlide").bannerHeight("Fixed_50").build()

        adView = CaulyAdView(this)
        adView!!.setAdInfo(adInfo)
        adView!!.setAdViewListener(this)

        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        rootView!!.addView(adView, params)
    }

    private fun startDeleteTask(){
        val path = readData("path", "null")!!
        if(path != "null")
            for (i in path.indices) FileDeleteTask().execute(path.split("\n")[i])
        FileDeleteTask().execute(Environment.getExternalStorageDirectory().absolutePath +
                "/Android/data/com.kakao.talk")
    }

    private fun showLoadingView(isShow: Boolean, desc: String){
        try {
            if (!isShow) alert!!.cancel()
            else {
                alert = SweetAlertDialog(MainActivity@ this, SweetAlertDialog.PROGRESS_TYPE)
                alert!!.progressHelper.barColor = Color.parseColor("#f48fb1")
                alert!!.titleText = desc
                alert!!.setCancelable(false)
                alert!!.show()
            }
        }
        catch (e: java.lang.Exception){
            toast("로딩 다이얼로그 표시 중 오류 발셍!\n\n${e.message}")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showFinishDialog(){
        SweetAlertDialog(MainActivity@this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("임시폴더 삭제 완료")
            .setContentText("총 ${String.format("%.2f", totalSize)}MB 용량 확보!")
            .setConfirmButton("닫기", null)
            .show()

        size!!.text = "임시폴더 용량 : 0.00MB"
        state!!.text = "상태 : 좋음"
        state!!.setTextColor(Color.parseColor("#31B404"))
        face!!.setImageDrawable(getDrawable(R.drawable.ic_good_face_green_24dp))

        val adInfo = CaulyAdInfoBuilder("69uUWcir").build()
        showInterstitial = true

        interstial = CaulyInterstitialAd()
        interstial!!.setAdInfo(adInfo)
        interstial!!.setInterstialAdListener(this@MainActivity)
        interstial!!.requestInterstitialAd(this@MainActivity)
    }

    private fun showOpenSourceDialog(){
        val dialog = AlertDialog.Builder(this@MainActivity)
        dialog.setTitle("오픈소스 라이선스")

        val string = """
            [Sweet Alert Dialog]
            The MIT License (MIT)

        Copyright (c) 2014 Pedant(http://pedant.cn)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

            -------------------

            [FancyToast-Android]
        Copyright 2019 Shashank Singhal

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

            --------------
            
            [TedPermission]
        Copyright 2017 Ted Park

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
        """

        dialog.setMessage(string)
        dialog.show()
    }

    private fun showSettingDialog() {
        val dialog = AlertDialog.Builder(this@MainActivity)
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = LinearLayout(this@MainActivity)
        inflater.inflate(R.layout.layout_setting, layout, true)
        layout.findViewById<EditText>(R.id.path).text = SpannableStringBuilder(readData("path", ""))
        layout.findViewById<Button>(R.id.opensource).setOnClickListener {
            showOpenSourceDialog()
        }
        dialog.setView(layout)
        dialog.setPositiveButton("확인") { _, _ ->
            saveData("path", layout.findViewById<EditText>(R.id.path).text.toString())
            toast("저장되었습니다.")
        }
        dialog.show()
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
    private inner class FileDeleteTask : AsyncTask<String?, Void?, Void?>() {
        override fun onPreExecute() {
            showLoadingView(true, "임시폴더 삭제중...")
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
            showLoadingView(false, "")
            showFinishDialog()
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class LoadSizeTask : AsyncTask<Void?, Void?, Void?>() {
        override fun onPreExecute() {
            showLoadingView(true, "임시폴더 크기 계산중...")
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
            showLoadingView(false, "")
            size!!.text = "임시폴더 용량 : ${String.format("%.2f", totalSize)}MB"
            when {
                totalSize.toInt()<=100 -> {
                    state!!.text = "상태 : 좋음"
                    state!!.setTextColor(Color.parseColor("#31B404"))
                    face!!.setImageDrawable(getDrawable(R.drawable.ic_good_face_green_24dp))
                }
                totalSize.toInt() in 101..500 -> {
                    state!!.text = "상태 : 보통"
                    state!!.setTextColor(Color.parseColor("#9E9E9E"))
                    face!!.setImageDrawable(getDrawable(R.drawable.ic_normal_face_gray_24dp))
                }
                totalSize.toInt()>500 -> {
                    state!!.text = "상태 : 심각"
                    state!!.setTextColor(Color.parseColor("#FA5858"))
                    face!!.setImageDrawable(getDrawable(R.drawable.ic_bad_face_red_24dp))
                }
            }
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
        val pref = getSharedPreferences("pref", Context.MODE_PRIVATE)
        return pref.getString(name, _null)
    }

    private fun saveData(name: String, value: String) {
        val pref = getSharedPreferences("pref", Context.MODE_PRIVATE)
        val editor = pref.edit()

        editor.putString(name, value)
        editor.apply()
    }

    private fun toast(string: String, type: Int = FancyToast.SUCCESS){
        runOnUiThread {
            FancyToast.makeText(MainActivity@this, string,
                FancyToast.LENGTH_SHORT, type, false).show()
        }
    }

    private fun requestPermission(){
        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                if(!readData("init", "false")!!.toBoolean()) {
                    toast("권한 사용에 동의하셨습니다 :)")
                    saveData("init", "true")
                    LoadSizeTask().execute()
                }
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {
               toast("권한 사용이 동의하셔야 카카오톡의 임시폴더 관리가 가능합니다 :(",
                   FancyToast.WARNING)
                finish()
            }
        }

        TedPermission.with(this)
            .setPermissionListener(permissionListener)
            .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .setDeniedTitle("권한 사용 동의 필요")
            .setDeniedMessage("권한 사용에 동의해 주셔야 카카오톡의 임시폴더 관리가 가능합니다!\n안드로이드 설정에서 해당 어플 설정에 들어가 권한 사용에 동의해 주세요.")
            .setRationaleTitle("권한 사용 동의 필요")
            .setRationaleMessage("카카오톡의 임시폴더를 관리하기 위해서 해당 권한 사용에 대한 동의가 필요합니다!")
            .check()
    }
}
