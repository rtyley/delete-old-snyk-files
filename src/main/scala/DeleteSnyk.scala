import cats.*
import cats.effect.{ExitCode, IO, Resource, ResourceApp}
import cats.syntax.all.*
import com.madgag.github.apps.GitHubAppJWTs.fromEnvVars
import com.madgag.scalagithub.GitHub
import com.madgag.scalagithub.model.*
import com.madgag.scalagithub.model.Repo.PullRequests.SingleCommitAction.deleteFile

import java.io.File
import java.nio.file.{Files, Path}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.StreamConverters.*

object DeleteSnyk extends ResourceApp {

  private val snykFilePath = ".github/workflows/snyk.yml"

  private val prText = PullRequest.Text(
    title = s"Delete obsolete Snyk workflow `$snykFilePath`",
    body = Files.readString(new File(getClass.getResource("/pr-description.md").toURI).toPath)
  )

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
    repo <- gitHub.getRepo(repoId)
    pr <- repo.pullRequests.create(prText, labels = Set("maintenance"), branch = "delete-old-snyk-file")(deleteFile(snykFilePath))
    _ <- IO.println(pr.html_url)
  } yield ()

}
