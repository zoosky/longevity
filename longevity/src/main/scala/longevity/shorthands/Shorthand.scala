package longevity.shorthands

import emblem.imports._
import longevity.exceptions.ShorthandCreationException

/** describes a relation (one-to-one mapping) between two types, `Actual` and `Abbreviated`. The "actual" type is
 * typically a richer type, such as a case class with a single parameter, and an abbreviated value for the
 * type, such as a string. Provides functions for mapping between the actual and abbreviated types, as well as
 * an `emblem.TypeKey` for both types.
 *
 * @tparam Actual the actual type
 * @tparam Abbreviated the abbreviated type
 */
class Shorthand[Actual, Abbreviated] private[longevity] (
  private[longevity] val extractor: Extractor[Actual, Abbreviated]
) {

  /** a type key for the actual type */
  private[longevity] lazy val actualTypeKey: TypeKey[Actual] = extractor.domainTypeKey

  /** a type key for the abbreviated type */
  private[longevity] lazy val abbreviatedTypeKey: TypeKey[Abbreviated] = extractor.rangeTypeKey

  /** converts from actual to abbreviated */
  def abbreviate(actual: Actual): Abbreviated = extractor.apply(actual)

  /** converts from abbreviate to actual */
  def unabbreviate(abbreviated: Abbreviated): Actual = extractor.inverse(abbreviated)

  override def toString = s"Shorthand[${actualTypeKey.tpe}, ${abbreviatedTypeKey.tpe}]"

}

object Shorthand {

  /** creates and returns a [[Shorthand]] for the specified types `Actual` and `Abbreviated`. `Actual` must be
   * a stable case class with single a parameter list.
   * @throws longevity.exceptions.ShorthandCreationException when `A` is not a stable case class with a single
   * parameter list
   */
  def apply[Actual : TypeKey, Abbreviated : TypeKey]: Shorthand[Actual, Abbreviated] =
    try {
      new Shorthand(emblem.Extractor[Actual, Abbreviated])
    } catch {
      case e: emblem.exceptions.GeneratorException =>
        throw new ShorthandCreationException(e, typeKey[Actual], typeKey[Abbreviated])
    }

}
