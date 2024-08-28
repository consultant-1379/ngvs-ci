package com.ericsson

import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.MemoryJobManagement


class JobSpecMixin {

    JobParent createJobParent() {
        JobParent jp = new JobParent() {
                    @Override
                    Object run() {
                        return null
                    }

                    @Override
                    String readFileFromWorkspace(String filePath) {
                        return ""
                    }
                }
        JobManagement jm = new MemoryJobManagement()
        jp.setJm(jm)
        jp
    }
}
