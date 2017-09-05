package cn.ennwifi.testplugin;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * test maven plugin.
 *
 */
@Mojo(name = "buildinfo")
public class BuildInfoMojo extends AbstractMojo {

  /**
   * project.
   */
  @Parameter(property = "project")
  private MavenProject project;

  /**
   * profix.
   */
  @Parameter(property = "prefix", defaultValue = "+++")
  private String prefix;

  public void execute() throws MojoExecutionException {
    Build build = project.getBuild();
    String outputDirectory = build.getOutputDirectory();
    String sourceDirectory = build.getSourceDirectory();
    String testOutputDirectory = build.getTestOutputDirectory();
    String testSourceDirectory = build.getTestSourceDirectory();
    getLog().info("\n=======+===================\nProject build info:");
    String[] info = {outputDirectory, sourceDirectory, testOutputDirectory, testSourceDirectory};
    for (String item : info) {
      getLog().info("\t" + prefix + "   " + item);
    }
    getLog().info("=======================");
  }

}
