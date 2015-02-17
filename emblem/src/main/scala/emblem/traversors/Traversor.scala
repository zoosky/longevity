package emblem.traversors

import emblem._
import emblem.exceptions.CouldNotTraverseException
import emblem.reflectionUtil.makeTypeTag
import scala.reflect.runtime.universe.typeOf

// TODO scaladoc

/** recursively traverses a data structure by type. the inputs and the outputs of the traversal are abstract
 * here, and specified by the implementing class. this forms a generic pattern for [[Visitor visiting]],
 * [[Generator generating]], and [[Transformer transforming]] data.
 * 
 * you can traverse arbritrary data to your liking by implementing the protected vals and defs in this
 * interface. as of yet, i haven't been able to generate the scaladoc for those protected methods.
 * sorry about that.
 */
trait Traversor {

  // pubic stuff:

  type TraverseInput[A]
  type TraverseResult[A]

  // TODO: these two protected
  type TraverseEmblemPropInput[A <: HasEmblem, B] = (EmblemProp[A, B], TraverseInput[B])
  type TraverseEmblemPropResult[A <: HasEmblem, B] = (EmblemProp[A, B], TraverseResult[B])

  trait CustomTraversor[A] {
    def apply[B <: A : TypeKey](input: TraverseInput[B]): TraverseResult[B]
  }

  /** A [[TypeKeyMap]] for [[CustomTraversors custom traversors]] */
  type CustomTraversors = TypeKeyMap[Any, CustomTraversor]

  /** An empty map of [[CustomTraversor custom traversors]] */
  val emptyCustomTraversor: CustomTraversors = TypeKeyMap[Any, CustomTraversor]()

  /** traverses an object of any supported type.
   * 
   * @throws emblem.exceptions.CouldNotTraverseException when an unsupported type is encountered during the
   * traversal
   */
  def traverse[A : TypeKey](input: TraverseInput[A]): TraverseResult[A] =
    traverseAnyOption[A](input) getOrElse {
      throw new CouldNotTraverseException(typeKey[A])
    }


  // protected stuff:

  protected val shorthandPool: ShorthandPool = ShorthandPool()
  protected val emblemPool: EmblemPool = EmblemPool()
  protected val customTraversors: CustomTraversors = emptyCustomTraversor

  protected def traverseBoolean(input: TraverseInput[Boolean]): TraverseResult[Boolean]

  protected def traverseChar(input: TraverseInput[Char]): TraverseResult[Char]

  protected def traverseDouble(input: TraverseInput[Double]): TraverseResult[Double]

  protected def traverseFloat(input: TraverseInput[Float]): TraverseResult[Float]

  protected def traverseInt(input: TraverseInput[Int]): TraverseResult[Int]

  protected def traverseLong(input: TraverseInput[Long]): TraverseResult[Long]

  protected def traverseString(input: TraverseInput[String]): TraverseResult[String]

  protected def stageEmblemProps[A <: HasEmblem](
    emblem: Emblem[A],
    input: TraverseInput[A])
  : Iterator[TraverseEmblemPropInput[A, _]]

  protected def unstageEmblemProps[A <: HasEmblem](
    emblem: Emblem[A],
    input: TraverseInput[A],
    result: Iterator[TraverseEmblemPropResult[A, _]])
  : TraverseResult[A]

  protected def stageShorthand[Actual, Abbreviated](
    shorthand: Shorthand[Actual, Abbreviated],
    input: TraverseInput[Actual])
  : TraverseInput[Abbreviated]

  protected def unstageShorthand[Actual, Abbreviated](
    shorthand: Shorthand[Actual, Abbreviated],
    abbreviatedResult: TraverseResult[Abbreviated])
  : TraverseResult[Actual]

  protected def stageOptionValue[A : TypeKey](
    input: TraverseInput[Option[A]])
  : Option[TraverseInput[A]] // <<<< this is a different kind of option! it is an iterator of 0 or 1

  protected def unstageOptionValue[A : TypeKey](
    input: TraverseInput[Option[A]],
    result: Option[TraverseResult[A]]) // <<<< this is a different kind of option! it is an iterator of 0 or 1
  : TraverseResult[Option[A]]

  protected def stageSetElements[A : TypeKey](
    input: TraverseInput[Set[A]])
  : Iterator[TraverseInput[A]]

  protected def unstageSetElements[A : TypeKey](
    input: TraverseInput[Set[A]],
    result: Iterator[TraverseResult[A]])
  : TraverseResult[Set[A]]

  protected def stageListElements[A : TypeKey](
    input: TraverseInput[List[A]])
  : Iterator[TraverseInput[A]]

  protected def unstageListElements[A : TypeKey](
    input: TraverseInput[List[A]],
    result: Iterator[TraverseResult[A]])
  : TraverseResult[List[A]]


  // private stuff:

  private type TraversorFunction[A] = (TraverseInput[A]) => TraverseResult[A]

  private val basicTraversors =
    TypeKeyMap[Any, TraversorFunction] +
    traverseBoolean _ +
    traverseChar _ +
    traverseDouble _ +
    traverseFloat _ +
    traverseInt _ +
    traverseLong _ +
    traverseString _

  // custom generators have to come first. after that order is immaterial
  private def traverseAnyOption[A : TypeKey](input: TraverseInput[A]): Option[TraverseResult[A]] =
    traverseCustomOption(input) orElse
    traverseEmblemOptionFromAny(input) orElse
    traverseShorthandOption(input) orElse
    traverseOptionOption(input) orElse
    traverseSetOption(input) orElse
    traverseListOption(input) orElse
    traverseBasicOption(input)

  private def traverseCustomOption[A : TypeKey](input: TraverseInput[A]): Option[TraverseResult[A]] = {
    val keyOpt: Option[TypeKey[_ >: A]] = customTraversors.keys.map(_.castToLowerBound[A]).flatten.headOption
    def getCustomTraversor[B >: A : TypeKey]: CustomTraversor[B] = customTraversors(typeKey[B])
    keyOpt map { key => getCustomTraversor(key).apply[A](input) }
  }

  private def traverseEmblemOptionFromAny[A : TypeKey](input: TraverseInput[A]): Option[TraverseResult[A]] = {
    val keyOption = hasEmblemTypeKeyOption(typeKey[A])
    keyOption flatMap { key => introduceHasEmblemTraverseEmblemOption(input)(key) }
  }

  private def hasEmblemTypeKeyOption[A : TypeKey, B <: A with HasEmblem]: Option[TypeKey[B]] =
    if (typeKey[A].tpe <:< typeOf[HasEmblem])
      Some(typeKey[A].asInstanceOf[TypeKey[B]])
    else
      None

  private def introduceHasEmblemTraverseEmblemOption[A, B <: A with HasEmblem : TypeKey](input: TraverseInput[A])
  : Option[TraverseResult[A]] = {
    traverseEmblemOption[B](input.asInstanceOf[TraverseInput[B]]).asInstanceOf[Option[TraverseResult[A]]]
  }

  private def traverseEmblemOption[A <: HasEmblem : TypeKey](input: TraverseInput[A])
  : Option[TraverseResult[A]] = {
    emblemPool.get(typeKey[A]) map { emblem => traverseFromEmblem(emblem, input) }
  }

  private def traverseFromEmblem[A <: HasEmblem](emblem: Emblem[A], hasEmblemInput: TraverseInput[A])
  : TraverseResult[A] = {
    val emblemPropInputIterator: Iterator[TraverseEmblemPropInput[A, _]] =
      stageEmblemProps(emblem, hasEmblemInput)
    val emblemPropResultIterator: Iterator[TraverseEmblemPropResult[A, _]] =
        emblemPropInputIterator.map { case (prop, input) =>
          (prop, traverseEmblemProp(emblem, prop, input))
        }
    unstageEmblemProps(emblem, hasEmblemInput, emblemPropResultIterator)
  }

  private def traverseEmblemProp[A <: HasEmblem, B](
    emblem: Emblem[A],
    prop: EmblemProp[A, B],
    input: TraverseInput[B])
  : TraverseResult[B] = {
    traverse(input)(prop.typeKey)
  }

  private def traverseShorthandOption[Actual : TypeKey](input: TraverseInput[Actual])
  : Option[TraverseResult[Actual]] =
    shorthandPool.get[Actual] map { s => traverseFromShorthand[Actual](s, input) }

  private def traverseFromShorthand[Actual](
    shorthand: Shorthand[Actual, _],
    input: TraverseInput[Actual])
  : TraverseResult[Actual] =
    traverseFromFullyTypedShorthand(shorthand, input)

  private def traverseFromFullyTypedShorthand[Actual, Abbreviated](
    shorthand: Shorthand[Actual, Abbreviated],
    input: TraverseInput[Actual])
  : TraverseResult[Actual] = {
    val abbreviatedInput = stageShorthand(shorthand, input)
    val abbreviatedResult = traverse(abbreviatedInput)(shorthand.abbreviatedTypeKey)
    unstageShorthand(shorthand, abbreviatedResult)
  }

  // TODO: remove code duplication below with option/set/list, generalize to other kinds of "collections"

  private def traverseOptionOption[OptionA : TypeKey](input: TraverseInput[OptionA])
  : Option[TraverseResult[OptionA]] = {
    val keyOption = optionElementTypeKeyOption(typeKey[OptionA])
    def doTraverse[A : TypeKey] = traverseOption(input.asInstanceOf[TraverseInput[Option[A]]])
    keyOption map { key => doTraverse(key).asInstanceOf[TraverseResult[OptionA]] }
  }

  // returns a `Some` containing the enclosing type of the option whenever the supplied type argument `A`
  // is an Option. otherwise returns `None`.
  private def optionElementTypeKeyOption[A : TypeKey]: Option[TypeKey[_]] =
    if (typeKey[A].tpe <:< typeOf[Option[_]]) Some(typeKey[A].typeArgs.head) else None

  private def traverseOption[A : TypeKey](optionInput: TraverseInput[Option[A]]): TraverseResult[Option[A]] = {
    val optionValueInputOption: Option[TraverseInput[A]] = stageOptionValue[A](optionInput)
    val optionValueResultOption: Option[TraverseResult[A]] = optionValueInputOption map { optionValueInput =>
      traverse[A](optionValueInput)
    }
    unstageOptionValue[A](optionInput, optionValueResultOption)
  }

  private def traverseSetOption[SetA : TypeKey](input: TraverseInput[SetA]): Option[TraverseResult[SetA]] = {
    val keyOption = setElementTypeKeyOption(typeKey[SetA])
    def doTraverse[A : TypeKey] = traverseSet(input.asInstanceOf[TraverseInput[Set[A]]])
    keyOption map { k => doTraverse(k).asInstanceOf[TraverseResult[SetA]] }
  }

  // returns a `Some` containing the enclosing type of the set whenever the supplied type argument `A`
  // is a Set. otherwise returns `None`.
  private def setElementTypeKeyOption[A : TypeKey]: Option[TypeKey[_]] =
    if (typeKey[A].tpe <:< typeOf[Set[_]]) Some(typeKey[A].typeArgs.head) else None

  private def traverseSet[A : TypeKey](aSetInput: TraverseInput[Set[A]]): TraverseResult[Set[A]] = {
    val aInputIterator: Iterator[TraverseInput[A]] = stageSetElements[A](aSetInput)
    val aResultIterator: Iterator[TraverseResult[A]] = aInputIterator map { aInput => traverse[A](aInput) }
    unstageSetElements[A](aSetInput, aResultIterator)
  }

  private def traverseListOption[ListA : TypeKey](input: TraverseInput[ListA]): Option[TraverseResult[ListA]] = {
    val keyOption = listElementTypeKeyOption(typeKey[ListA])
    def doTraverse[A : TypeKey] = traverseList(input.asInstanceOf[TraverseInput[List[A]]])
    keyOption map { k => doTraverse(k).asInstanceOf[TraverseResult[ListA]] }
  }

  private def traverseList[A : TypeKey](aListInput: TraverseInput[List[A]]): TraverseResult[List[A]] = {
    val aInputIterator: Iterator[TraverseInput[A]] = stageListElements[A](aListInput)
    val aResultIterator: Iterator[TraverseResult[A]] = aInputIterator map { aInput => traverse[A](aInput) }
    unstageListElements[A](aListInput, aResultIterator)
  }

  // returns a `Some` containing the enclosing type of the list whenever the supplied type argument `A`
  // is a List. otherwise returns `None`.
  private def listElementTypeKeyOption[A : TypeKey]: Option[TypeKey[_]] =
    if (typeKey[A].tpe <:< typeOf[List[_]]) Some(typeKey[A].typeArgs.head) else None

  private def traverseBasicOption[Basic : TypeKey](input: TraverseInput[Basic]): Option[TraverseResult[Basic]] =
    basicTraversors.get[Basic] map { traversor => traversor(input) }

}
