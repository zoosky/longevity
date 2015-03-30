package longevity.test

import emblem.traversors.Generator.CustomGeneratorPool
import longevity.context.ShorthandPool
import longevity.persistence.InMem
import longevity.persistence.RepoPool
import longevity.persistence.buildRepoPool
import longevity.subdomain.Subdomain

/** test utilities for your bounded context provided by longevity */
class TestContext private[longevity](
  subdomain: Subdomain,
  shorthandPool: ShorthandPool,
  customGenerators: CustomGeneratorPool,
  repoPool: RepoPool) {

  /** An in-memory set of repositories for this longevity context, for use in testing. at the moment, no
   * specializations are provided. */
  lazy val inMemRepoPool = buildRepoPool(subdomain, shorthandPool, InMem)

  /** a simple [[http://www.scalatest.org/ ScalaTest]] fixture to test your [[repoPool repo pool]].
   * all you have to do is extend this class some place where ScalaTest is going to find it.
   */
  class RepoPoolSpec extends longevity.test.RepoPoolSpec(
    subdomain,
    shorthandPool,
    customGenerators,
    repoPool,
    Some("(Mongo)"))

  /** a simple [[http://www.scalatest.org/ ScalaTest]] fixture to test your [[inMemRepoPool in-memory repo
   * pool]]. all you have to do is extend this class some place where ScalaTest is going to find it.
   */
  class InMemRepoPoolSpec extends longevity.test.RepoPoolSpec(
    subdomain,
    shorthandPool,
    customGenerators,
    inMemRepoPool,
    Some("(InMem)"))

}
