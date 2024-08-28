import hudson.model.*

try{
    def ALLOCATED_PORT_OFFSET = 10000;
    def ALLOCATED_PORT_MAX_BUILDS = 100;
    def display = "${DISPLAY}"
    assert display != "" : 'ERROR: display is empty'
    assert display != "null" : 'ERROR: display is null'
    def allocated_port = ((display.substring(1).toInteger() *
        ALLOCATED_PORT_MAX_BUILDS) +
        ALLOCATED_PORT_OFFSET).toString()
    def zookeeper_port = ((display.substring(1).toInteger() *
        ALLOCATED_PORT_MAX_BUILDS) +
        ALLOCATED_PORT_OFFSET+1).toString()

    def pa = new ParametersAction([
      new StringParameterValue("ALLOCATED_PORT", allocated_port),
      new StringParameterValue("JETTY_PORT", zookeeper_port),
      new StringParameterValue("CONFIG_BACKEND_SERVER", "memory"),
      new StringParameterValue("MESOS_EXECUTOR_NUMBER", display.substring(1)),
      new StringParameterValue("EXECUTOR_NUMBER", display.substring(1))
    ])

    Thread.currentThread().executable.addAction(pa)
} catch (Throwable t) {
    println(t)
    throw t
}
