## This is a maven plugin

#### The version introduce

- 0.0.1:generate the Pojo class that Nutz use, the table name is capital
- 0.0.2:generate the Pojo class that Nutz use, the table name is the same as datasource's table
- 0.0.7:generate the Pojo class that Nutz use, but the version only I can use

for more information,you can contact me use email

You can use it like below:

	<plugin>
		<groupId>cn.ennwifi</groupId>
		<artifactId>testplugin</artifactId>
		<version>0.0.7</version>
		<executions>
			<!-- 生成数据库BEAN -->
			<execution>
				<id>genbean</id>
				<phase>generate-sources</phase>
				<goals>
					<goal>generate2</goal>
				</goals>
				<configuration>
					<path>${project.basedir}/src/main/java</path>
					<driver>com.mysql.jdbc.Driver</driver>
					<jdbcurl>jdbc:mysql://yourIp:3306/datesource</jdbcurl>
					<user>username</user>
					<pwd>password</pwd>
					<packageName>cn.ennwifi.smartpv.data.repository</packageName>
					<interfaceName>TableInfo</interfaceName>
				</configuration>
			</execution>
		</executions>
	</plugin>