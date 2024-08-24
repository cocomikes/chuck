package com.readystatesoftware.chuck.monitor.mock

import com.readystatesoftware.chuck.monitor.MonitorHelper
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.regex.Matcher
import java.util.regex.Pattern

object MockHelper {

    var isOpen = false

    /**
     * mock服务的baseUrl  例如PostMan的mock服务：https://881adba2-445e-4ad2-8633-3206a2b0ef94.mock.pstmn.io
     */
    var mockBaseUrl: String = ""

    /**
     * 需要mock的接口path  例如：/article/list/0/json
     */
    var mockPaths: String = ""

    /**
     * 手动创建的response
     */
    var mockResponse: String = ""

    /**
     * 请求成功的code
     */
    var mockSuccessResponseCode = 200

    fun isMockPath(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return mockPaths.contains(path)
    }

    @JvmStatic
    fun isMockByCustomResponse(path: String?): Boolean {
        return isOpen && isMockPath(path) && mockResponse.isNotBlank()
    }

    @JvmStatic
    fun isMockByNetworkResponse(path: String?): Boolean {
        return isOpen && isMockPath(path) && mockBaseUrl.isNotBlank() && mockResponse.isBlank()
    }

    fun configMock(mockBaseUrl: String, mockPath: String, mockResponse: String) {
        MockHelper.mockBaseUrl = mockBaseUrl
        mockPaths = mockPath
        MockHelper.mockResponse = mockResponse
    }

    @JvmStatic
    fun buildMockServer(chain: Interceptor.Chain, request: Request): Response {
        val oldHttpUrl = request.url
        val newBaseUrl = mockBaseUrl.toHttpUrlOrNull() ?: return chain.proceed(request)
        val newHttpUrl = oldHttpUrl
            .newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            .build()
        return chain.proceed(request.newBuilder().url(newHttpUrl).build())
    }

    @JvmStatic
    fun mockResponseBody(chain: Interceptor.Chain): Response {
        if (mockResponse.isBlank()) return chain.proceed(chain.request())
        val response = chain.proceed(chain.request())
        val contentType = response.body?.contentType()
        val responseBody = mockResponse.toResponseBody(contentType)
        return response.newBuilder()
            .message("mock成功")
            .code(mockSuccessResponseCode)
            .body(responseBody)
            .build()
    }

    @JvmStatic
    fun String?.isWhiteContentType(): Boolean {
        val whiteContentTypes = MonitorHelper.whiteContentTypes
        if(whiteContentTypes.isNullOrEmpty()) return true
        val intercept = !this.isNullOrEmpty() && whiteContentTypes.contains(this.split(";")[0])
//    println("${MonitorHelper.TAG}---->whiteContentTypes = $whiteContentTypes 当前ContentType = $this   是否在白名单:$intercept")
        return intercept
    }

    @JvmStatic
    fun String?.isWhiteHosts(): Boolean {
        val whiteHosts = MonitorHelper.whiteHosts
        if(whiteHosts.isNullOrEmpty()) return true
        val intercept = this.isNullOrEmpty() || whiteHosts.contains(this)
//    println("${MonitorHelper.TAG}---->whiteHosts = $whiteHosts  当前host= $this   是否在白名单:$intercept")
        return intercept
    }

    @JvmStatic
    fun String?.isBlackHosts(): Boolean {
        if (this.isNullOrEmpty()) return false
        val blackHosts = MonitorHelper.blackHosts
        if(blackHosts.isNullOrEmpty()) return false
        val intercept = blackHosts.contains(this)
//    println("${MonitorHelper.TAG}---->blackHosts = $blackHosts  当前host= $this   是否在黑名单:$intercept")
        return intercept
    }

    @JvmStatic
    fun String?.isSkipInterceptByHost(): Boolean = !this.isWhiteHosts() && this.isBlackHosts()

    private val IP_ADDRESS_PATTERN: Pattern = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})")

    @JvmStatic
    fun String?.isIpAddress(): Boolean {
        if (this.isNullOrBlank()) return false
        val matcher: Matcher = IP_ADDRESS_PATTERN.matcher(this)
        if (!matcher.matches()) return false
        for (i in 1..matcher.groupCount()) {
            try {
                val group = matcher.group(i) ?: return false
                if (group.toInt() > 255) {
                    return false
                }
            } catch (e: Exception) {
                return false
            }
        }
        return true
    }
}
