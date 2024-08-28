freeStyleJob("Test1"){
  steps {
    shell('echo "Hello Rinku in Test 1"')
  }
}

freeStyleJob("Test2"){
  steps {
    shell('echo "Hello Rinku in Test 2"')
  }
}
freeStyleJob("Test3"){
  steps {
    shell('echo "Hello Rinku in Test 3"')
  }
}
listView("FromFile_DSL"){
  jobs {
     regex("Test.*")
  }
  columns{
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}
