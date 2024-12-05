package org.rutz;

public class FieldMapping {
    private String jsonField;
    private String dataType;
    private String group;
    private String xmlPath;

    // Constructors
    public FieldMapping() {}

    public FieldMapping(String jsonField, String dataType, String group, String xmlPath) {
        this.jsonField = jsonField;
        this.dataType = dataType;
        this.group = group;
        this.xmlPath = xmlPath;
    }

    // Getters and Setters
    public String getJsonField() {
        return jsonField;
    }

    public void setJsonField(String jsonField) {
        this.jsonField = jsonField;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getXmlPath() {
        return xmlPath;
    }

    public void setXmlPath(String xmlPath) {
        this.xmlPath = xmlPath;
    }
}
