import cats.*
import cats.data.EitherT
import cats.effect.{ExitCode, IO, Resource, ResourceApp}
import cats.syntax.all.*
import com.madgag.github.apps.GitHubAppJWTs.fromEnvVars
import com.madgag.scalagithub.GitHub
import com.madgag.scalagithub.commands.MergePullRequest
import com.madgag.scalagithub.model.*
import sttp.model.Uri

import java.nio.file.{Files, Path}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.jdk.StreamConverters.*

object MergeSnykPR extends ResourceApp {
  
  def run(args: List[String]): Resource[IO, ExitCode] = for {
    gitHubFactory <- GitHub.Factory()
    _ <- Resource.eval(mergeAllPRsListedIn(Path.of(args.head), gitHubFactory))
  } yield ExitCode.Success

  def mergeAllPRsListedIn(prsFile: Path, gitHubFactory: GitHub.Factory): IO[Unit] = for {
    prIds: Seq[PullRequest.Id] <- 
      IO.blocking(Files.lines(prsFile).toScala(Seq).map(url => PullRequest.Id.from(Uri.unsafeParse(url))))
    access <- gitHubFactory.accessSoleAppInstallation(fromEnvVars(prefix = "DELETE_SNYK"))
    _ <- IO.println(access.accountAccess.installation.account.atLogin)
    _ <- IO.println(s"Running as: ${access.accountAccess.principal.html_url}")
    given GitHub = access.gitHub
    _ <- prIds.traverse(prId => mergePR(access.accountAccess.principal, prId))
  } yield ()

  def mergePR(principal: GitHubApp, prId: PullRequest.Id)(using gitHub: GitHub): IO[Unit] = for {
    pr <- gitHub.getPullRequest(prId)
    _ <- IO.println(pr.html_url)
    _ <- EitherT.fromEither[IO](validateEligibleForMerge(principal, pr)).foldF(
      IO.println,
      _ => (pr.merge(MergePullRequest()).flatMap(r => IO.println(s"Merge success: ${r.merged}"))).attempt.flatMap {
        _.fold(_ => IO.println(s"${pr.baseRepo.html_url}/settings/branches"),_ => IO.sleep(1.minutes))
      }
      )
  } yield ()

  def validateEligibleForMerge(principal: GitHubApp, pr: PullRequest): Either[String, Unit] = for {
    _ <- Either.cond(principal.html_url == pr.user.html_url, (), s"Not raised by ${principal.slug}")
    _ <- Either.cond(!pr.merged.contains(true), (), s"Already merged by ${pr.merged_by.map(_.atLogin).mkString}")
    _ <- Either.cond(pr.mergeable.contains(true), (), s"Apparently not mergeable")
    _ <- Either.cond(pr.state == "open", (), s"PR is not open, it is ${pr.state}")
  } yield ()

}
