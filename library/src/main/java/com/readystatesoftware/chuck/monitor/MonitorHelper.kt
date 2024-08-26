package com.readystatesoftware.chuck.monitor

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.core.content.ContentResolverCompat
import androidx.loader.content.CursorLoader
import com.android.local.service.core.ALSHelper
import com.android.local.service.core.data.ServiceConfig
import com.readystatesoftware.chuck.internal.data.ChuckContentProvider
import com.readystatesoftware.chuck.internal.data.HttpTransaction
import com.readystatesoftware.chuck.internal.data.LocalCupboard
import com.readystatesoftware.chuck.internal.support.NotificationHelper
import com.readystatesoftware.chuck.monitor.ui.MonitorService
import com.readystatesoftware.chuck.monitor.data.MonitorProperties
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread


@SuppressLint("StaticFieldLeak")
object MonitorHelper {
    const val TAG = "MonitorHelper"
    /**
     * content-type类型 https://www.runoob.com/http/http-content-type.html
     */
    private var defaultContentTypes = "application/json,application/xml,text/html,text/plain,text/xml"
    @JvmField
    var lastUpdateFetchId= 0L

    var context: Context? = null

    @JvmField
    var port = 0
    @JvmField
    var whiteContentTypes: String? = null
    @JvmField
    var whiteHosts: String? = null
    @JvmField
    var blackHosts: String? = null
    @JvmField
    var isFilterIPAddressHost: Boolean = false

    var isOpenMonitor = true

    private var singleThreadExecutor: ExecutorService? = null

    private var sCursorLoader : CursorLoader?=null

    private fun threadExecutor(action: () -> Unit) {
        if (singleThreadExecutor == null || singleThreadExecutor?.isShutdown == true) {
            singleThreadExecutor = Executors.newSingleThreadExecutor()
        }
        singleThreadExecutor?.execute(action)
    }

    @JvmStatic
    fun init(context: Context?) {
        if(context != null){
            MonitorHelper.context = context
            thread {
                MonitorProperties().paramsProperties()?.let { propertiesData ->
                    val contentTypes = propertiesData.whiteContentTypes
                    whiteContentTypes = if (contentTypes.isNullOrBlank()) defaultContentTypes else contentTypes
                    whiteHosts = propertiesData.whiteHosts
                    blackHosts = propertiesData.blackHosts
                    port = propertiesData.port.toInt()
                    isFilterIPAddressHost = propertiesData.isFilterIPAddressHost
                }
                initPCService(context, port)
            }
        } else{
            Log.e("MonitorProvider", "MonitorProvider初始化context失败")
        }
    }

    private fun createLoader(context : Context?, limit: Int, offset: Int, filter : String, fetchId : Long?=null) : Cursor?{
        if(context == null) return null
        var uri : Uri?=null
        if (ChuckContentProvider.TRANSACTION_URI != null) {
            uri = ChuckContentProvider.TRANSACTION_URI
            if(limit > 0){
                uri = uri!!.buildUpon().appendQueryParameter("limit", "$limit").build()
            }
            if(offset > 0){
                uri = uri!!.buildUpon().appendQueryParameter("offset", "$offset").build()
            }
        }
        if(uri == null) return null
        return kotlin.runCatching {
            val selection = StringBuilder()
            val selectionArgs = mutableListOf<String>()
            if (!TextUtils.isEmpty(filter)) {
                if (TextUtils.isDigitsOnly(filter)) {
                    selection.append("responseCode LIKE ?")
                    selectionArgs.add("$filter%")
                } else {
                    selection.append("path LIKE ?")
                    selectionArgs.add("%$filter%")
                }
            }
            if(fetchId != null && fetchId > 0){
                if(selection.isNotEmpty()){
                    selection.append("AND")
                        .append("_id > ?")
                } else{
                    selection.append("_id > ?")
                }
                selectionArgs.add("$fetchId")
            }
            ContentResolverCompat.query(context.contentResolver,
                uri,
                HttpTransaction.PARTIAL_PROJECTION_MONITOR,
                if(selection.isNotEmpty()) selection.toString() else null,
                if(selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null,
                "requestDate DESC",
                null)
        }.onFailure { ex ->
            Log.e("MonitorProvider", "CREATE LOADER ERROR.", ex)
        }.getOrNull()
    }

    private fun initPCService(context: Context, port: Int = 0) {
        ALSHelper.init(context)
        ALSHelper.startService(if (port > 0) ServiceConfig(MonitorService::class.java, port) else ServiceConfig(MonitorService::class.java))
        MonitorHelper.port = ALSHelper.serviceList.firstOrNull()?.port ?: 0
    }

    fun deleteAll() {
        lastUpdateFetchId = 0L
        if (ChuckContentProvider.TRANSACTION_URI != null) {
            context?.contentResolver?.delete(ChuckContentProvider.TRANSACTION_URI, null, null)
        }
        NotificationHelper.clearBuffer()
    }

    fun getMonitorDataList(limit: Int = 50, offset: Int = 0, filter : String): MutableList<HttpTransaction> {
        val cursor = createLoader(context, limit = limit, offset = offset, filter = filter) ?: return mutableListOf()
        return LocalCupboard.getInstance().withCursor(cursor).list(HttpTransaction::class.java)
    }

    fun getMonitorDataById(limit: Int = 50, filter : String, fetchId : Long): MutableList<HttpTransaction> {
        val cursor = createLoader(context, limit = limit, offset = 0, filter = filter, fetchId) ?: return mutableListOf()
        return LocalCupboard.getInstance().withCursor(cursor).list(HttpTransaction::class.java)
    }

//    fun getSharedPrefsFilesData(): HashMap<String, HashMap<String, SpValueInfo?>> {
//        val ctx = context ?: return hashMapOf()
//        val map = hashMapOf<String, HashMap<String, SpValueInfo?>>()
//        val targetFile = File("${ctx.cacheDir.parentFile?.absolutePath}/shared_prefs")
//        if (!targetFile.exists()) return hashMapOf()
//        if (targetFile.isDirectory) {
//            targetFile.listFiles()?.forEach { spFile ->
//                Log.d(TAG, "getSharedPrefsFiles: " + spFile.name)
//                val fileName = spFile.name
//                if (!fileName.isNullOrBlank()) {
//                    val name = if (fileName.endsWith(".xml")) fileName.split(".xml")[0] else fileName
//                    val value = getSpFile(name)
//                    map[name] = value
//                }
//            }
//        }
//        return map
//    }
//
//    fun getSpFile(name: String?): HashMap<String, SpValueInfo?> {
//        if (name.isNullOrBlank()) return hashMapOf()
//        val map = hashMapOf<String, SpValueInfo?>()
//        context?.getSharedPreferences(name, Context.MODE_PRIVATE)?.all?.entries?.forEach {
//            val valueType = when (it.value) {
//                is Int -> SPValueType.Int
//                is Double -> SPValueType.Double
//                is Float -> SPValueType.Float
//                is Long -> SPValueType.Long
//                is Boolean -> SPValueType.Boolean
//                is String -> SPValueType.String
//                else -> SPValueType.String
//            }
//            val key = it.key
//            if (!key.isNullOrBlank()) {
//                map[key] = SpValueInfo(it.value, valueType)
//            }
//        }
//        return map
//    }
//
//    fun updateSpValue(fileName: String, key: String, value: Any?) {
//        SPUtils.saveValue(context ?: return, fileName, key, value)
//    }

    @JvmStatic
    fun getPhoneWifiIpAddress(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val inetAddresses = networkInterface.inetAddresses
                while (inetAddresses.hasMoreElements()) {
                    val inetAddress = inetAddresses.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (e: SocketException) {
            Log.e("MonitorPCService", "get ip", e)
            return null
        }
        return null
    }
}