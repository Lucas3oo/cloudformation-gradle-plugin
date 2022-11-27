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
        // commit DateTime with time zone offset, e.g. 2022-11-26T13:42:40+01:00
        String commitDateTime = commit.dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        String version = "$commit.abbreviatedId $commitDateTime $commit.author.name $commit.shortMessage"
        def replacements = [
          '!':'',
          ';':'',
          ',':'',
          '\$':'',
          '\n':'',
          '\'':'',
          '\"':'']
        return version.replace(replacements)
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
