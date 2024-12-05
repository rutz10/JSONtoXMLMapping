package org.rutz;

public class FieldMapping {
    private String jsonField;
    private String dataType;
    private String group;
    private String xmlPath;

    public FieldMapping(String jsonField, String dataType, String group, String xmlPath) {
        this.jsonField = jsonField;
        this.dataType = dataType;
        this.group = group;
        this.xmlPath = xmlPath;
    }

    public String getJsonField() {
        return jsonField;
    }

    public String getDataType() {
        return dataType;
    }

    public String getGroup() {
        return group;
    }

    public String getXmlPath() {
        return xmlPath;
    }

    @Override
    public String toString() {
        return "FieldMapping{" +
                "jsonField='" + jsonField + '\'' +
                ", dataType='" + dataType + '\'' +
                ", group='" + group + '\'' +
                ", xmlPath='" + xmlPath + '\'' +
                '}';
    }
}
