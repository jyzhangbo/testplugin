package cn.ennwifi.testplugin.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhangbo
 *
 */
public class ColumnDescriptor {

  private static Pattern ENUM_PATTERN = Pattern.compile("enum\\((.+)\\)");
  private static Pattern COLUMN_TYPE_PATTERN = Pattern.compile("^(\\w+)(?:\\((\\d+)\\))?");

  private List<String> enumValues = new ArrayList<String>();
  private static Map<String, Class<?>> typeMapping = new HashMap<String, Class<?>>();

  static {
    typeMapping.put("varchar", String.class);
    typeMapping.put("enum", String.class);
    typeMapping.put("bigint", Long.class);
    typeMapping.put("long", Long.class);
    typeMapping.put("integer", Integer.class);
    typeMapping.put("float", Float.class);
    typeMapping.put("double", Double.class);
    typeMapping.put("int", Integer.class);
    typeMapping.put("timestamp", Timestamp.class);
    typeMapping.put("datetime", Date.class);
    typeMapping.put("boolean", boolean.class);
    typeMapping.put("tinyint", boolean.class);
    typeMapping.put("bool", boolean.class);
    typeMapping.put("decimal", BigDecimal.class);
  }

  public String columnName;

  public Object defaultValue;

  public String dataType;

  public boolean nullable;

  public boolean primary;

  public String columnType;

  private String comment;

  public int size;


  /**
   * @return the columnName
   */
  public String getColumnName() {
    return columnName;
  }

  public String getGetterMethodName() {
    if (isBoolean()) {
      return "is" + columnName;
    }
    return "get" + columnName;
  }

  public String getSetterMethodName() {
    return "set" + columnName;
  }

  public boolean isTimestamp() {
    return "timestamp".equalsIgnoreCase(dataType);
  }

  public boolean isBoolean() {
    return boolean.class.getName().equals(getJavaType());
  }

  public String getDefaultValueCode() {
    if (isEnum()) {
      return getSimpleJavaTypeName() + "." + defaultValue;
    }
    if (isBoolean()) {
      if ("1".equals(defaultValue.toString())) {
        return "true";
      } else {
        return "false";
      }
    }
    if (isTimestamp()) {
      if (("0000-00-00 00:00:00".equals(defaultValue) || "CURRENT_TIMESTAMP".equals(defaultValue))) {
        return "new Timestamp(System.currentTimeMillis())";
      }
    }
    if (defaultValue != null && Long.class.getName().equals(getJavaType())) {
      return defaultValue + "L";
    }
    if (defaultValue != null && BigDecimal.class.getName().equals(getJavaType())) {
      return "new BigDecimal(\"" + defaultValue.toString() + "\")";
    }
    if (defaultValue != null && Integer.class.getName().equals(getJavaType())) {
      return "new Integer(\"" + defaultValue.toString() + "\")";
    }
    return "\"" + getDefaultValue().toString() + "\"";
  }

  public void setDefaultValue(Object v) {
    this.defaultValue = v;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public String getColumnAnnotation() {
    if (primary) {
      return "@Id";
    }
    return "@Column";
  }

  public List<String> getEnumValues() {
    return enumValues;
  }

  public boolean isEnum() {
    return "enum".equalsIgnoreCase(dataType);
  }

  public String getSimpleJavaTypeName() {
    return getJavaType().replaceFirst("^.*\\.", "");
  }

  public String getJavaType() {
    if ("tinyint".equalsIgnoreCase(dataType) && size == 1) {
      return boolean.class.getName();
    }
    if ("enum".equalsIgnoreCase(dataType)) {
      return columnName;
    }
    Class<?> type = typeMapping.get(dataType);
    if (type != null) {
      return type.getName();
    }

    return String.class.getName();
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
    if (comment == null) {
      return;
    }

  }

  public void setColumnType(String columnType) {
    Matcher m = ENUM_PATTERN.matcher(columnType);
    if (m.find()) {
      this.columnType = "enum";

      String s = m.group(1);
      for (String v : s.split(",")) {
        v = v.trim().replaceAll("'", "");
        enumValues.add(v);
      }
      return;
    }

    m = COLUMN_TYPE_PATTERN.matcher(columnType);
    if (m.find()) {
      if (m.group(2) != null) {
        this.size = Integer.parseInt(m.group(2));
      }
      this.columnType = m.group(1);
    } else {
      throw new IllegalArgumentException();
    }
  }

}
