package longevity.repo

import scala.reflect.runtime.universe.TypeTag

import longevity.domain._

object RepoPool {

  class MultipleReposForEntityType[E <: Entity](val repo1: Repo[E], val repo2: Repo[E])
  extends Exception(s"multiple repos for entity type ${repo1.entityTypeTag}: $repo1 and $repo2")

  class NoRepoForEntityType[E <: Entity](val entityTypeTag: TypeTag[E])
  extends Exception(s"no repo for entity type $entityTypeTag found in the pool")

}

/** maintains a pool of all the repositories in use. */
class RepoPool {

  private var entityTypeTagToRepo = Map[TypeTag[_], Repo[_]]()

  /** adds a repo to the repo pool for entity type E. */
  @throws[RepoPool.MultipleReposForEntityType[_]]
  private[repo] def addRepo[E <: Entity](repo: Repo[E]): Unit = {
    val entityTypeTag = repo.entityTypeTag
    if (entityTypeTagToRepo.contains(entityTypeTag)) {
      throw new RepoPool.MultipleReposForEntityType(tagToRepo(entityTypeTag), repo)
    }
    entityTypeTagToRepo += (entityTypeTag -> repo)
  }

  @throws[RepoPool.NoRepoForEntityType[_]]
  private[repo] def repoForEntityTypeTag[E <: Entity](entityTypeTag: TypeTag[E]): Repo[E] = {
    if (!entityTypeTagToRepo.contains(entityTypeTag)) {
      throw new RepoPool.NoRepoForEntityType(entityTypeTag)
    }
    tagToRepo(entityTypeTag)
  }

  private def tagToRepo[E <: Entity](entityTypeTag: TypeTag[E]): Repo[E] =
    entityTypeTagToRepo(entityTypeTag).asInstanceOf[Repo[E]]

}
