package com.readystatesoftware.chuck.monitor.data

class PropertiesData(
    var port: String,
    var whiteContentTypes: String?,//ContentType白名单
    var whiteHosts: String?,//host白名单
    var blackHosts: String?,//host黑名单
    var isFilterIPAddressHost: Boolean = false//是否过滤纯IP地址的host
)