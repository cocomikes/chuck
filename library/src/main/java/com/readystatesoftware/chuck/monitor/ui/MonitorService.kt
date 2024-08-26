package com.readystatesoftware.chuck.monitor.ui

import com.android.local.service.annotation.Get
import com.android.local.service.annotation.Page
import com.android.local.service.annotation.Service
import com.readystatesoftware.chuck.internal.data.HttpTransaction
import com.readystatesoftware.chuck.monitor.MonitorHelper
import com.readystatesoftware.chuck.monitor.mock.MockHelper
import com.readystatesoftware.chuck.monitor.weaknetwork.WeakNetworkHelper

@Service(port = 9527)
abstract class MonitorService {

    @Page("index")
    fun showMonitorPage() = "monitor_index.html"

//    @Page("sp_index")
//    fun showSpPage() = "sp_index.html"

//    @Page("mqtt_index")
//    fun showMqttPage() = "mqtt_index.html"

    @Get("query")
    fun queryMonitorData(limit: Int, offset: Int, filter: String, fetchId : Long): MutableList<HttpTransaction> {
        //第一次加载时限制最大条数: 200
        return if(fetchId == 0L){
            MonitorHelper.getMonitorDataList(limit = 200, offset, filter)
        } else if(filter.isNotEmpty()){
            MonitorHelper.getMonitorDataList(limit = 200, offset, filter)
        } else{
            MonitorHelper.getMonitorDataList(limit, offset, filter)
        }
    }

    @Get("clean")
    fun cleanMonitorData() {
        MonitorHelper.deleteAll()
    }

    @Get("autoFetch")
    fun autoFetchData(fetchId: Long, limit: Int, filter: String): MutableList<HttpTransaction> {
        while (true) {
            Thread.sleep(1000)
            return if (fetchId != MonitorHelper.lastUpdateFetchId || fetchId == 0L) {
                MonitorHelper.getMonitorDataById(limit, filter, fetchId)
            } else {
                mutableListOf()
            }
        }
    }

//    @Get("sharedPrefs")
//    fun getSharedPrefsFilesData() = MonitorHelper.getSharedPrefsFilesData()
//
//    @Get("getSharedPrefsByFileName")
//    fun getSharedPrefsByFileName(fileName: String) = MonitorHelper.getSpFile(fileName)
//
//    @Get("updateSpValue")
//    fun updateSpValue(fileName: String, key: String, value: String, valueType: String) {
//        val realValue = when (valueType) {
//            SPValueType.Int.value -> value.toIntOrNull()
//            SPValueType.Double.value -> value.toDoubleOrNull()
//            SPValueType.Long.value -> value.toLongOrNull()
//            SPValueType.Float.value -> value.toFloatOrNull()
//            SPValueType.Boolean.value -> value.toBoolean()
//            else -> value
//        }
//        MonitorHelper.updateSpValue(fileName, key, realValue)
//    }

    @Get("setWeakNetConfig")
    fun configWeak(weakType: String) {
        WeakNetworkHelper.configWeak(weakType)
    }

    @Get("setMockConfig")
    fun setMockConfig(mockBaseUrl: String, mockPath: String, mockResponse: String) {
        MockHelper.configMock(mockBaseUrl, mockPath, mockResponse)
    }

    @Get("openMockService")
    fun openMockService(isOpen: Boolean) {
        MockHelper.isOpen = isOpen
    }
}