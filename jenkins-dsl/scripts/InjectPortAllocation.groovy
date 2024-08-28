try {
    def ALLOCATED_PORT_OFFSET = 10000;
    def ALLOCATED_PORT_MAX_BUILDS = 100;
    def display = "${DISPLAY}"
    assert display != "" : 'ERROR: display is empty'
    assert display != "null" : 'ERROR: display is null'
    def allocated_port = ((display.substring(1).toInteger() *
        ALLOCATED_PORT_MAX_BUILDS) +
        ALLOCATED_PORT_OFFSET).toString()

    def mapVar = [:]
    mapVar['ALLOCATED_PORT'] = allocated_port
    mapVar['MESOS_EXECUTOR_NUMBER'] = display.substring(1)
    mapVar['EXECUTOR_NUMBER'] = display.substring(1)
    return mapVar
} catch (Throwable t) {
    println(t)
    throw t
}
