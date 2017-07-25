package cn.ennwifi.testplugin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.nutz.lang.Strings;

/**
 * @author zhangbo
 *
 */
public class TableDescriptor {

  public String tableName;// 表名称

  private String comment;// 注释

  private String entityName;// 实体类名称

  private final List<ColumnDescriptor> columns = new ArrayList<ColumnDescriptor>();

  public TableDescriptor(String tableName) {
    this.tableName = tableName;
  }

  public List<ColumnDescriptor> getColumns() {
    return columns;
  }

  /**
   * 获取表明.
   * 
   * @return
   */
  public String getTableName() {
    return tableName.toUpperCase();
  }

  /**
   * 获取实体类名称.
   * 
   * @return
   */
  public String getEntityClassName() {
    if (Strings.isBlank(entityName)) {
      return tableName.toUpperCase() + "Obj";
    }
    return entityName;
  }

  /**
   * 获取导入的类.
   * 
   * @return
   */
  public List<String> getImports() {
    Set<String> klasses = new LinkedHashSet<String>();

    for (ColumnDescriptor column : columns) {
      String klass = column.getJavaType();
      if (klass.startsWith("java.lang") || klass.indexOf('.') == -1) {
        continue;
      }
      klasses.add(column.getJavaType());
    }

    List<String> imports = new ArrayList<String>();
    imports.addAll(klasses);
    imports.add(null);

    imports.add(Serializable.class.getName());
    imports.add(null);

    klasses.clear();
    if (klasses.size() > 0) {
      imports.addAll(klasses);
      imports.add(null);
    }

    return imports;
  }

  public void addColumn(ColumnDescriptor column) {
    columns.add(column);
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

}
