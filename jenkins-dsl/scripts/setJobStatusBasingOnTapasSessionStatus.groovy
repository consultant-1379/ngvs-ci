if (manager.logContains(".*Reporting end of session with status 2.*")) {
    manager.buildUnstable()
} else if (manager.logContains(".*Reporting end of session with status 3.*")) {
    manager.buildFailure()
}
