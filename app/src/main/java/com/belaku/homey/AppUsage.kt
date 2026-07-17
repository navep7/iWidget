package com.belaku.homey

class AppUsage {

    var appName: String
    var usageTime: String

    constructor(appName: String, usageTime: String) {
        this.appName = appName
        this.usageTime = usageTime
    }

    override fun toString(): String {
        return "User(appN=$appName, appUsage='$usageTime')"
    }
}
