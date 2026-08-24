package com.bariskeser.byedpi.services

import com.bariskeser.byedpi.data.AppStatus
import com.bariskeser.byedpi.data.Mode

var appStatus = AppStatus.Halted to Mode.VPN
    private set

fun setStatus(status: AppStatus, mode: Mode) {
    appStatus = status to mode
}
