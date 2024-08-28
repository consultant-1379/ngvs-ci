package com.ericsson.bss.decorators

import com.ericsson.bss.decorators.scriptbuilders.ProjectVersionsOnOffJobScriptBuilder
import com.ericsson.bss.decorators.scriptbuilders.ProjectVersionsScriptBuilder
import com.ericsson.bss.job.washingmachine.utils.EnabledHostsScriptBuilder
import com.ericsson.bss.job.washingmachine.utils.JobEnabledScriptBuilder
import com.ericsson.bss.job.washingmachine.utils.SelectedStringScriptBuilder
import com.ericsson.bss.util.JobContext
import javaposse.jobdsl.dsl.Job

class JobParamsDecorator {

    private static final String STRING_PARAM = 'string'
    private static final String BOOLEAN_PARAM = 'boolean'
    private static final String CHOICE_PARAM = 'choice'
    private static final String ACTIVE_CHOICE_PARAM = 'active_choice'
    private static final String ACTIVE_CHOICE_REACTIVE_REFERENCE_PARAM = 'active_choice_reactive_reference_param'

    private Job job
    private ConfigObject config

    public JobParamsDecorator(ConfigObject config, Job job) {
        this.config = config
        this.job = job
    }

    public void addParameters() {
        for (i in 0..config.params.size() - 1) {
            switch (config.params[i].type) {
                case STRING_PARAM:
                    addStringParam(config.params[i].name, config.params[i].defaultValue, config.params[i].description)
                    break
                case BOOLEAN_PARAM:
                    addBooleanParam(config.params[i].name, config.params[i].defaultValue, config.params[i].description)
                    break
                case CHOICE_PARAM:
                    addChoiceParam(config.params[i].name, config.params[i].values, config.params[i].description)
                    break
                case ACTIVE_CHOICE_PARAM:
                    if (config.params[i].scriptBuilderName) {
                        addActiveChoiceParam(
                                config.params[i].name,
                                config.params[i].description,
                                config.params[i].choiceType,
                                useScriptBuilder(config.params[i].scriptBuilderName, config.params[i].scriptBuilderParams),
                                config.params[i].fallbackScript,
                                config.params[i].isFilterable
                        )
                    } else if (config.params[i].scriptUrl) {
                        addActiveChoiceParam(
                                config.params[i].name,
                                config.params[i].description,
                                config.params[i].choiceType,
                                loadScriptFromFile(config.params[i].scriptUrl),
                                config.params[i].fallbackScript,
                                config.params[i].isFilterable
                        )
                    } else {
                        addActiveChoiceParam(
                                config.params[i].name,
                                config.params[i].description,
                                config.params[i].choiceType,
                                config.params[i].script,
                                config.params[i].fallbackScript,
                                config.params[i].isFilterable
                        )
                    }
                    break
                case ACTIVE_CHOICE_REACTIVE_REFERENCE_PARAM:
                    if (config.params[i].scriptBuilderName) {
                        addActiveChoiceReactiveReferenceParam(
                                config.params[i].name,
                                config.params[i].description,
                                config.params[i].choiceType,
                                useScriptBuilder(config.params[i].scriptBuilderName, config.params[i].scriptBuilderParams),
                                config.params[i].fallbackScript,
                                config.params[i].referencedParameters
                        )
                    }
                    else {
                        addActiveChoiceReactiveReferenceParam(
                                config.params[i].name,
                                config.params[i].description,
                                config.params[i].choiceType,
                                loadScriptFromFile(config.params[i].scriptUrl),
                                config.params[i].fallbackScript,
                                config.params[i].referencedParameters
                        )
                    }
                    break
            }
        }
    }

    private void addStringParam(String name, String defaultValue, String description) {
        job.with {
            parameters {
                stringParam(name, defaultValue, description)
            }
        }
    }

    private void addBooleanParam(String name, Boolean defaultValue, String description) {
        job.with {
            parameters {
                booleanParam(name, defaultValue, description)
            }
        }
    }

    private void addChoiceParam(String name, List<String> options, String description) {
        job.with {
            parameters {
                choiceParam(name, options, description)
            }
        }
    }

    /**
     *
     * @param name Defines a parameter that dynamically generates a list of value options for a build parameter using a Groovy script or a script from the Scriptler catalog.
     * @param desc Sets a description for the parameter.
     * @param isFilterable If set, provides a text box filter in the UI control where a text filter can be typed.
     * @param _choiceType Selects one of four different rendering options for the option values. Must be one of 'SINGLE_SELECT' (default), 'MULTI_SELECT', 'CHECKBOX' or 'RADIO'.
     * @param _script Sets the script that will dynamically generate the parameter value options.
     * @param _fallbackScript Provides alternate parameter value options in case the main script fails.
     */
    private void addActiveChoiceParam(String _name, String desc, String _choiceType, String _script, String _fallbackScript, Boolean isFilterable) {
        job.with {
            parameters {
                activeChoiceParam(_name) {
                    description(desc)
                    filterable(isFilterable)
                    choiceType(_choiceType)
                    groovyScript {
                        script(_script)
                        fallbackScript(_fallbackScript)
                    }
                }
            }
        }

    }

    /**
     * Defines a parameter that dynamically generates a list of value options for a build parameter using a Groovy script or a script from the Scriptler catalog and
     * that dynamically updates when the value of other job UI controls change.
     * @param name
     * @param desc
     * @param _choiceType Selects one of four different rendering options for the option values. Must be one of 'TEXT_BOX' (default),
     *                                                  'FORMATTED_HTML', 'FORMATTED_HIDDEN_HTML', 'ORDERED_LIST' or 'UNORDERED_LIST'.
     * @param _script
     * @param _fallbackScript
     * @param params Specifies a list of job parameters that trigger an auto-refresh.
     * @param _omitValueField Omits the hidden value field.
     */
    private void addActiveChoiceReactiveReferenceParam(String name, String desc, String _choiceType, String _script, String _fallbackScript, List params,
                                                       boolean _omitValueField = true) {
        job.with {
            parameters {
                parameters {
                    activeChoiceReactiveReferenceParam(name) {
                        description(desc)
                        omitValueField(_omitValueField)
                        choiceType(_choiceType)
                        groovyScript {
                            script(_script)
                            fallbackScript(_fallbackScript)
                        }

                        for (String param : params) {
                            referencedParameter(param)
                        }
                    }
                }
            }
        }
    }

    private String useScriptBuilder(String scriptBuilderClassName, Map params) {
        switch (scriptBuilderClassName) {
            case ProjectVersionsScriptBuilder.getSimpleName():
                return ProjectVersionsScriptBuilder.newBuilder(params).build()
            case ProjectVersionsOnOffJobScriptBuilder.getSimpleName():
                return ProjectVersionsOnOffJobScriptBuilder.newBuilder(params).build()
            case EnabledHostsScriptBuilder.getSimpleName():
                return EnabledHostsScriptBuilder.newBuilder(params).build()
            case JobEnabledScriptBuilder.getSimpleName():
                return JobEnabledScriptBuilder.newBuilder(params).build()
            case SelectedStringScriptBuilder.getSimpleName():
                return SelectedStringScriptBuilder.newBuilder(params).build()
        }
    }

    private String loadScriptFromFile(String file) {
        return JobContext.getDSLFactory().readFileFromWorkspace(file)
    }
}
