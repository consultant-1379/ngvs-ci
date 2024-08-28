package com.ericsson.bss.decorators

class CommonParamsDecorator {

    private static final String BOOLEAN_PARAM = 'booleanParam'
    private static final String PREDEFINED_PROP = 'predefinedProp'

    public static Closure addParameters(List parameters) {
        return {
            for (j in 0..parameters.size() - 1) {
                switch (parameters[j].type) {
                    case BOOLEAN_PARAM:
                        booleanParam(parameters[j].name, parameters[j].value)
                        break
                    case PREDEFINED_PROP:
                        predefinedProp(parameters[j].name, parameters[j].value)
                        break
                }
            }
        }
    }

    public static Closure addParameters(String condition, List parameters) {
        return {
            for (j in 0..parameters.size() - 1) {
                if (!parameters[j].exclusive || parameters[j].exclusive == condition) {
                    switch (parameters[j].type) {
                        case BOOLEAN_PARAM:
                            booleanParam(parameters[j].name, parameters[j].value)
                            break
                        case PREDEFINED_PROP:
                            predefinedProp(parameters[j].name, parameters[j].value)
                            break
                    }
                }
            }
        }
    }
}
