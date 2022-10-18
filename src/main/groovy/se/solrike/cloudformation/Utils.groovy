package se.solrike.cloudformation

import java.time.format.DateTimeFormatter

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.eclipse.jgit.errors.RepositoryNotFoundException


/**
 * @author Lucas Persson
 */
public class Utils {


  public static String getGitVersionOfFile(File projectDir, String fileName) {
    try {
      Grgit repo = Grgit.open(dir: projectDir)
      Commit commit = repo.log(paths: [fileName], maxCommits: 1)[0]
      if (commit != null) {
        String commitDateTime = commit.dateTime.format(DateTimeFormatter.ofPattern('yyyy-MM-dd\'T\'HH:mm:ssZ'))
        String version = "${commit.abbreviatedId} ${commitDateTime} ${commit.author.name}: ${commit.shortMessage}"
        version = version.replace(';', '')
            .replace('\$', '')
            .replace('\n', '')
            .replace('\'', '')
            .replace(',', '')
            .replace('\"', '')
            .replace('!', '')
        return version
      }
      else {
        return 'Not under version control'
      }
    }
    catch (RepositoryNotFoundException e) {
      return 'Not under version control'
    }
  }
}
