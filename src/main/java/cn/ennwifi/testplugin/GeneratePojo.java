package cn.ennwifi.testplugin;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.nutz.dao.Sqls;
import org.nutz.dao.impl.NutDao;
import org.nutz.dao.sql.Sql;
import org.nutz.dao.sql.SqlCallback;
import org.nutz.lang.Files;
import org.nutz.lang.Streams;
import org.nutz.lang.Times;

import com.alibaba.druid.pool.DruidDataSource;

import cn.ennwifi.testplugin.model.ColumnDescriptor;
import cn.ennwifi.testplugin.model.TableDescriptor;

/**
 * generate the database pojo.
 *
 */
@Mojo(name = "generate")
public class GeneratePojo extends AbstractMojo {

  /**
   * path.
   */
  @Parameter(defaultValue = "${project.basedir}/src/main/java", property = "path")
  private String path;

  /**
   * packageName.
   */
  @Parameter(property = "packageName", required = true)
  private String packageName;

  /**
   * driver
   */
  @Parameter(defaultValue = "com.mysql.jdbc.Driver", property = "driver", required = true)
  private String driver;

  /**
   * jdbcurl.
   */
  @Parameter(property = "jdbcurl", required = true)
  private String jdbcurl;

  /**
   * user.
   */
  @Parameter(property = "user", required = true)
  private String user;

  /**
   * pwd.
   */
  @Parameter(property = "pwd", required = true)
  private String pwd;

  public void execute() throws MojoExecutionException, MojoFailureException {
    DruidDataSource ds = new DruidDataSource();
    ds.setDriverClassName(driver);
    ds.setUrl(jdbcurl);
    ds.setPassword(pwd);
    ds.setUsername(user);
    ds.setMaxActive(30);
    ds.setMaxWait(5 * 1000);
    ds.setTestOnBorrow(true);
    ds.setTestWhileIdle(true);
    ds.setTestOnReturn(true);
    NutDao dao = new NutDao(ds);

    Sql sql = Sqls.create("SELECT DATABASE()");
    sql.setCallback(new SqlCallback() {

      public Object invoke(Connection conn, ResultSet rs, Sql sql) throws SQLException {
        if (rs.next()) {
          return rs.getString(1);
        }
        return null;
      }
    });

    dao.execute(sql);
    String database = sql.getString();

    Sql tableSchemaSql =
        Sqls.create("select * from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA = '" + database + "'");
    tableSchemaSql.setCallback(new SqlCallback() {

      public Object invoke(Connection conn, ResultSet rs, Sql sql) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        while (rs.next()) {
          Map<String, Object> record = new HashMap<String, Object>();
          for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            record.put(columnName, rs.getObject(columnName));
          }

          result.add(record);
        }

        return result;
      }
    });

    dao.execute(tableSchemaSql);

    List<Map> columns = tableSchemaSql.getList(Map.class);

    Map<String, TableDescriptor> tables = new HashMap<String, TableDescriptor>();
    for (Map<String, Object> columnInfo : columns) {
      String tableName = (String) columnInfo.get("TABLE_NAME");

      ColumnDescriptor column = new ColumnDescriptor();
      column.columnName = (String) columnInfo.get("COLUMN_NAME");
      column.defaultValue = columnInfo.get("COLUMN_DEFAULT");
      column.dataType = (String) columnInfo.get("DATA_TYPE");
      column.nullable = "YES".equals(columnInfo.get("IS_NULLABLE"));
      column.primary = "PRI".equals(columnInfo.get("COLUMN_KEY"));

      String columnType = (String) columnInfo.get("COLUMN_TYPE");
      column.setColumnType(columnType);

      column.setComment((String) columnInfo.get("COLUMN_COMMENT"));

      TableDescriptor table = tables.get(tableName);
      if (table == null) {
        table = new TableDescriptor(tableName);
        tables.put(tableName, table);
      }
      table.addColumn(column);
    }

    Sql infomationSchemaSql =
        Sqls.create("select * from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA = '" + database + "'");
    infomationSchemaSql.setCallback(new SqlCallback() {

      public Object invoke(Connection conn, ResultSet rs, Sql sql) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        while (rs.next()) {
          Map<String, Object> record = new HashMap<String, Object>();
          for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            record.put(columnName, rs.getObject(columnName));
          }
          result.add(record);
        }
        return result;
      }
    });
    dao.execute(infomationSchemaSql);


    List<Map> tableInfos = infomationSchemaSql.getList(Map.class);

    for (Map<String, Object> tableInfo : tableInfos) {
      String tableName = (String) tableInfo.get("TABLE_NAME");
      String comment = (String) tableInfo.get("TABLE_COMMENT");

      TableDescriptor table = tables.get(tableName);
      if (table != null) {
        table.setComment(comment);
      }
    }

    for (Map.Entry<String, TableDescriptor> entry : tables.entrySet()) {
      TableDescriptor table = entry.getValue();

      String packagePath = packageName.replace('.', '/');

      String className = table.getEntityClassName();

      File file = new File(path, packagePath + "/" + className + ".java");

      if (file.exists()) {
        file.delete();
      }

      file.getParentFile().mkdirs();

      VelocityContext context = new VelocityContext();
      context.put("date", Times.getNowSDT());
      context.put("table", table);
      context.put("packageName", packageName);
      StringWriter writer = new StringWriter();

      String templatePath = "/model.vm";

      String template = "";
      try {
        getLog().info(this.getClass().getResourceAsStream(templatePath).toString());
        template =
            new String(Streams.readBytes(this.getClass().getResourceAsStream(templatePath)), Charset.forName("utf8"));
      } catch (IOException e) {
        e.printStackTrace();
      }
      getLog().info(template);
      VelocityEngine engine = new VelocityEngine();
      engine.setProperty("runtime.references.strict", false);
      engine.init();
      engine.evaluate(context, writer, "generator", template);

      Files.write(file, writer.toString().getBytes(Charset.forName("utf8")));

    }

  }

  public static void main(String[] args) throws MojoExecutionException, MojoFailureException {
    GeneratePojo obj = new GeneratePojo();
    obj.execute();
  }

}
