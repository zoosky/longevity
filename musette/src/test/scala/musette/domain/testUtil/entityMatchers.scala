package musette.domain.testUtil

import org.scalatest._
import longevity.domain.Assoc
import longevity.repo.Id
import musette.domain._

// TODO: find some way to do this kind of stuff generically
object entityMatchers extends Matchers {

  def persistedBlogShouldMatchUnpersisted(persisted: Blog, unpersisted: Blog): Unit = {
    persisted.uri should equal (unpersisted.uri)
    persisted.site.isPersisted should be (true)
    persistedSiteShouldMatchUnpersisted(persisted.site.persisted, unpersisted.site.unpersisted)
    userSetsShouldMatch(persisted.authors, unpersisted.authors)
    persisted.slug should equal (unpersisted.slug)
  }

  def persistedBlogPostShouldMatchUnpersisted(persisted: BlogPost, unpersisted: BlogPost): Unit = {
    persisted.uri should equal (unpersisted.uri)
    persisted.blog.isPersisted should be (true)
    persistedBlogShouldMatchUnpersisted(persisted.blog.persisted, unpersisted.blog.unpersisted)
    userSetsShouldMatch(persisted.authors, unpersisted.authors)
    persisted.content should equal (unpersisted.content)
    persisted.slug should equal (unpersisted.slug)
  }

  def persistedCommentShouldMatchUnpersisted(persisted: Comment, unpersisted: Comment): Unit = {
    persisted.uri should equal (unpersisted.uri)
    persisted.subject.isPersisted should be (true)
    persistedBlogPostShouldMatchUnpersisted(persisted.subject.persisted, unpersisted.subject.unpersisted)
    persisted.author.isPersisted should be (true)
    persistedUserShouldMatchUnpersisted(persisted.author.persisted, unpersisted.author.unpersisted)
    persisted.content should equal (unpersisted.content)
  }

  def persistedSiteShouldMatchUnpersisted(persisted: Site, unpersisted: Site): Unit = {
    persisted.uri should equal (unpersisted.uri)
  }

  def persistedUserShouldMatchUnpersisted(persisted: User, unpersisted: User): Unit = {
    persisted.uri should equal (unpersisted.uri)
    persisted.site.isPersisted should be (true)
    persistedSiteShouldMatchUnpersisted(persisted.site.persisted, unpersisted.site.unpersisted)
    persisted.email should equal (unpersisted.email)
    persisted.handle should equal (unpersisted.handle)
    persisted.slug should equal (unpersisted.slug)
  }

  private def userSetsShouldMatch(persisted: Set[Assoc[User]], unpersisted: Set[Assoc[User]]): Unit = {
    val uriToUnpersistedUserMap = unpersisted.map(_.unpersisted).map(user => (user.uri -> user)).toMap
    persisted.foreach { userAssoc =>
      userAssoc.isPersisted should be (true)
      val user = userAssoc.persisted
      uriToUnpersistedUserMap should contain key (user.uri)
      persistedUserShouldMatchUnpersisted(user, uriToUnpersistedUserMap(user.uri))
    }
  }

  def persistedWikiShouldMatchUnpersisted(persisted: Wiki, unpersisted: Wiki): Unit = {
    persisted.uri should equal (unpersisted.uri)
    persisted.site.isPersisted should be (true)
    persistedSiteShouldMatchUnpersisted(persisted.site.persisted, unpersisted.site.unpersisted)
    userSetsShouldMatch(persisted.authors, unpersisted.authors)
    persisted.slug should equal (unpersisted.slug)
  }

  def persistedWikiPageShouldMatchUnpersisted(persisted: WikiPage, unpersisted: WikiPage): Unit = {
    persisted.uri should equal (unpersisted.uri)
    persisted.wiki.isPersisted should be (true)
    persistedWikiShouldMatchUnpersisted(persisted.wiki.persisted, unpersisted.wiki.unpersisted)
    userSetsShouldMatch(persisted.authors, unpersisted.authors)
    persisted.content should equal (unpersisted.content)
    persisted.slug should equal (unpersisted.slug)
  }

}