package com.ericsson.bss.util

import javaposse.jobdsl.dsl.DslFactory

import javax.management.BadAttributeValueExpException

final class JobContext {

    private static DslFactory dslFactory

    private static void setDSLFactory(DslFactory dslFactory) {
        if (!dslFactory) {
            throw new BadAttributeValueExpException("dslFactory can not be null.")
        }

        this.dslFactory = dslFactory
    }

    public static DslFactory getDSLFactory()  {
        if (!dslFactory) {
            throw new BadAttributeValueExpException("dslFactory can not be null.")
        }

        return dslFactory
    }
}
