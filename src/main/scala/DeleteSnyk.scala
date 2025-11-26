import cats.*
import cats.effect.{ExitCode, IO, Resource, ResourceApp}
import cats.syntax.all.*
import com.madgag.github.apps.GitHubAppJWTs.fromEnvVars
import com.madgag.scalagithub.GitHub
import com.madgag.scalagithub.model.RepoId

import java.nio.file.{Files, Path}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.jdk.StreamConverters.*

object DeleteSnyk extends ResourceApp {

  private val snykFilePath = ".github/workflows/snyk.yml"

  private val commitMessage =
    """Delete obsolete Snyk workflow `.github/workflows/snyk.yml`
      |
      |This workflow is now obsolete, see https://github.com/guardian/.github/pull/96
      |""".stripMargin

  def run(args: List[String]): Resource[IO, ExitCode] = for {
    gitHubFactory <- GitHub.Factory()
    _ <- Resource.eval(deleteSnykFilesInAllReposListedIn(Path.of(args.head), gitHubFactory))
  } yield ExitCode.Success

  def deleteSnykFilesInAllReposListedIn(repoIdsFile: Path, gitHubFactory: GitHub.Factory): IO[Unit] = for {
    repoIds: Seq[RepoId] <- IO.blocking(Files.lines(repoIdsFile).toScala(Seq).map(RepoId.from))
    access <- gitHubFactory.accessSoleAppInstallation(fromEnvVars(prefix = "DELETE_SNYK"))
    given GitHub = access.gitHub
    _ <- repoIds.traverse(deleteSnykFile)
  } yield ()

  def deleteSnykFile(repoId: RepoId)(using gitHub: GitHub): IO[Unit] = for {
    contents <- gitHub.getRepo(repoId).flatMap(_.contentsFile.get(snykFilePath))
    _ <- IO.println(s"Will next delete: ${contents.html_url}")
    _ <- IO.sleep(5.minutes) // reduces the rate of any resulting RiffRaff deploys
    deletionCommit <- contents.delete(commitMessage)
    _ <- IO.println(deletionCommit.result.commit.html_url)
  } yield ()

}
