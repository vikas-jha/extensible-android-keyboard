package com.example.extkeyboard.serde;

import java.util.List;

/**
 * Created by vijha on 10/4/2017.
 */

public class JsonSettings {

    private String id;
    private String label;
    private String settingType;
    private String metadata;

    private transient JsonSettings parent;

    private List<JsonSettings> settings;
    private Boolean on;
    private Integer intValue;
    private Float floatValue;
    private String value;

    /**
     * Should be provided as key.methodName.
     * Will fetch object from callback using key and call method with name a methodName
     */

    private String renderer;

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getSettingType() {
        return settingType;
    }

    public String getMetadata() {
        return metadata;
    }

    public List<JsonSettings> getSettings() {
        return settings;
    }

    public Boolean getOn() {
        return on;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public Float getFloatValue() {
        return floatValue;
    }

    public String getValue() {
        return value;
    }

    public void setOn(Boolean on) {
        this.on = on;
    }

    public void setIntValue(Integer intValue) {
        this.intValue = intValue;
    }

    public void setFloatValue(Float floatValue) {
        this.floatValue = floatValue;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public JsonSettings getParent() {
        return parent;
    }

    public void setParent(JsonSettings parent) {
        this.parent = parent;
    }

    public String getRenderer() {
        return renderer;
    }

    public JsonSettings getSubSetting(String id){
        if(getSettings() != null){
            for(JsonSettings settings : getSettings()){
                if(settings.getId() != null && settings.getId().equals(id)){
                    return settings;
                }
            }
        }
        return null;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
