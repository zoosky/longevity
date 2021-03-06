package longevity.integration

import longevity.context._
import longevity.shorthands._
import longevity.subdomain._

/** covers a root entity with attributes of every supported basic type */
package object allAttributes {

  val entityTypes = EntityTypePool() + AllAttributes

  val subdomain = Subdomain("All Attributes", entityTypes)

  val longevityContext = LongevityContext(subdomain, ShorthandPool.empty, Mongo)

}
