package emblem.traversors

import emblem._
import emblem.exceptions.CouldNotTraverseException
import scala.reflect.runtime.universe.typeOf

// TODO: extract traits TraversorOfCustoms, EmblemTraversor, ShorthandTraversor, BasicTraversor

/** TODO scaladoc */
trait Traversor {

  // pubic stuff:

  type TraverseInput[A]
  type TraverseEmblemInput[A <: HasEmblem]
  type TraverseResult[A]
  type TraversorFunction[A] = (TraverseInput[A]) => TraverseResult[A]

  trait CustomTraversor[A] {
    def apply[B <: A : TypeKey](input: TraverseInput[B]): TraverseResult[B]
  }

  /** A [[TypeKeyMap]] for [[CustomTraversors custom traversors]] */
  type CustomTraversors = TypeKeyMap[Any, CustomTraversor]

  /** An empty map of [[CustomTraversor custom traversors]] */
  val emptyCustomTraversor: CustomTraversors = TypeKeyMap[Any, CustomTraversor]()

  /** traverses an object of any supported type */
  def traverseAny[A : TypeKey](input: TraverseInput[A]): TraverseResult[A] =
    traverseAnyOption[A](input) getOrElse {
      throw new CouldNotTraverseException(typeKey[A])
    }

  /** traverses an object with a [[CustomTraversor custom traversor]] */
  def traverseCustom[A : TypeKey](input: TraverseInput[A]): TraverseResult[A] =
    traverseCustomOption[A](input) getOrElse {
      throw new CouldNotTraverseException(typeKey[A])
    }

  /** traverses an object with an emblem */
  def traverseEmblem[A <: HasEmblem : TypeKey](input: TraverseInput[A]): TraverseResult[A] =
    introduceHasEmblemTraverseEmblemOption[A, A](input) getOrElse {
      throw new CouldNotTraverseException(typeKey[A])
    }

  /** traverses an object with a shorthand */
  def traverseShorthand[Actual : TypeKey](input: TraverseInput[Actual]): TraverseResult[Actual] =
    traverseShorthandOption[Actual](input) getOrElse {
      throw new CouldNotTraverseException(typeKey[Actual])
    }

  /** traverses an option */
  def traverseOption[A : TypeKey](input: TraverseInput[Option[A]]): TraverseResult[Option[A]] = {
    val aInputOption: Option[TraverseInput[A]] = stageTraverseOption[A](input)
    val aResultOption: Option[TraverseResult[A]] = aInputOption map { aInput => traverseAny[A](aInput) }
    unstageTraverseOption[A](aResultOption)
  }

  /** traverses an set */
  def traverseSet[A : TypeKey](input: TraverseInput[Set[A]]): TraverseResult[Set[A]] = {
    val inputASet = stageTraverseSet[A](input)
    val resultASet = inputASet map { inputA => traverseAny[A](inputA) }
    unstageTraverseSet[A](resultASet)
  }

  /** traverses an list */
  def traverseList[A : TypeKey](input: TraverseInput[List[A]]): TraverseResult[List[A]] = {
    val inputAList = stageTraverseList[A](input)
    val resultAList = inputAList map { inputA => traverseAny[A](inputA) }
    unstageTraverseList[A](resultAList)
  }

  /** traverses a boolean */
  def traverseBoolean(input: TraverseInput[Boolean]): TraverseResult[Boolean]

  /** traverses a char */
  def traverseChar(input: TraverseInput[Char]): TraverseResult[Char]

  /** traverses a double */
  def traverseDouble(input: TraverseInput[Double]): TraverseResult[Double]

  /** traverses a float */
  def traverseFloat(input: TraverseInput[Float]): TraverseResult[Float]

  /** traverses a int */
  def traverseInt(input: TraverseInput[Int]): TraverseResult[Int]

  /** traverses a long */
  def traverseLong(input: TraverseInput[Long]): TraverseResult[Long]

  /** traverses a string */
  def traverseString(input: TraverseInput[String]): TraverseResult[String]

  // protected stuff:

  protected val shorthandPool: ShorthandPool = ShorthandPool()
  protected val emblemPool: EmblemPool = EmblemPool()
  protected val customTraversors: CustomTraversors = emptyCustomTraversor

  protected def stageTraverseEmblem[A <: HasEmblem](
    emblem: Emblem[A],
    input: TraverseInput[A])
  : TraverseEmblemInput[A]

  protected def stageTraverseEmblemProp[A <: HasEmblem, B](
    emblem: Emblem[A],
    prop: EmblemProp[A, B],
    input: TraverseEmblemInput[A])
  : TraverseInput[B]

  protected def unstageTraverseEmblemProp[A <: HasEmblem, B](
    emblem: Emblem[A],
    prop: EmblemProp[A, B],
    emblemInput: TraverseEmblemInput[A],
    propResult: TraverseResult[B])
  : TraverseEmblemInput[A]

  protected def unstageTraverseEmblem[A <: HasEmblem](
    emblem: Emblem[A],
    input: TraverseEmblemInput[A])
  : TraverseResult[A]

  protected def stageTraverseShorthand[Actual, Abbreviated](
    shorthand: Shorthand[Actual, Abbreviated],
    input: TraverseInput[Actual])
  : TraverseInput[Abbreviated]

  protected def unstageTraverseShorthand[Actual, Abbreviated](
    shorthand: Shorthand[Actual, Abbreviated],
    abbreviatedResult: TraverseResult[Abbreviated])
  : TraverseResult[Actual]

  protected def stageTraverseOption[A : TypeKey](
    input: TraverseInput[Option[A]])
  : Option[TraverseInput[A]]

  protected def unstageTraverseOption[A : TypeKey](
    result: Option[TraverseResult[A]])
  : TraverseResult[Option[A]]

  protected def stageTraverseSet[A : TypeKey](
    input: TraverseInput[Set[A]])
  : Iterator[TraverseInput[A]]

  protected def unstageTraverseSet[A : TypeKey](
    result: Iterator[TraverseResult[A]])
  : TraverseResult[Set[A]]

  protected def stageTraverseList[A : TypeKey](
    input: TraverseInput[List[A]])
  : List[TraverseInput[A]]

  protected def unstageTraverseList[A : TypeKey](
    result: List[TraverseResult[A]])
  : TraverseResult[List[A]]


  // private stuff:

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

  private def traverseFromEmblem[A <: HasEmblem](
    emblem: Emblem[A],
    input: TraverseInput[A])
  : TraverseResult[A] = {
    val emblemInput: TraverseEmblemInput[A] = stageTraverseEmblem(emblem, input)
    val emblemInput2: TraverseEmblemInput[A] = emblem.props.foldLeft(emblemInput) {
      case (emblemInput, prop) => traverseEmblemProp(emblem, prop, emblemInput)
    }
    unstageTraverseEmblem(emblem, emblemInput2)
  }

  private def traverseEmblemProp[A <: HasEmblem, B](
    emblem: Emblem[A],
    prop: EmblemProp[A, B],
    emblemInput: TraverseEmblemInput[A])
  : TraverseEmblemInput[A] = {
    val propInput: TraverseInput[B] = stageTraverseEmblemProp(emblem, prop, emblemInput)
    val propResult: TraverseResult[B] = traverseAny(propInput)(prop.typeKey)
    unstageTraverseEmblemProp(emblem, prop, emblemInput, propResult)
  }

  private def traverseShorthandOption[Actual : TypeKey](
    input: TraverseInput[Actual])
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
    val abbreviatedInput = stageTraverseShorthand(shorthand, input)
    val abbreviatedResult = traverseAny(abbreviatedInput)(shorthand.abbreviatedTypeKey)
    unstageTraverseShorthand(shorthand, abbreviatedResult)
  }

  // TODO: remove code duplication below with option/set/list, generalize to other kinds of "collections"

  private def traverseOptionOption[OptionA : TypeKey](
    input: TraverseInput[OptionA])
  : Option[TraverseResult[OptionA]] = {

    val keyOption = optionElementTypeKeyOption(typeKey[OptionA])
    def doTraverse[A : TypeKey] = traverseOption(input.asInstanceOf[TraverseInput[Option[A]]])
    keyOption map { key => doTraverse(key).asInstanceOf[TraverseResult[OptionA]] }

  }

  private def traverseSetOption[SetA : TypeKey](
    input: TraverseInput[SetA])
  : Option[TraverseResult[SetA]] = {
    val keyOption = setElementTypeKeyOption(typeKey[SetA])
    def doTraverse[A : TypeKey] = traverseSet(input.asInstanceOf[TraverseInput[Set[A]]])
    keyOption map { k => doTraverse(k).asInstanceOf[TraverseResult[SetA]] }
  }

  private def traverseListOption[ListA : TypeKey](
    input: TraverseInput[ListA])
  : Option[TraverseResult[ListA]] = {
    val keyOption = listElementTypeKeyOption(typeKey[ListA])
    def doTraverse[A : TypeKey] = traverseList(input.asInstanceOf[TraverseInput[List[A]]])
    keyOption map { k => doTraverse(k).asInstanceOf[TraverseResult[ListA]] }
  }

  private def traverseBasicOption[Basic : TypeKey](input: TraverseInput[Basic]): Option[TraverseResult[Basic]] =
    basicTraversors.get[Basic] map { traversor => traversor(input) }



  // TODO: remove code duplication below with option/set/list, generalize to other kinds of "collections"
  // TODO: the defs below should probably be private

  /** returns a `Some` containing the enclosing type of the option whenever the supplied type argument `A`
   * is an Option. otherwise returns `None`.
   */
  protected def optionElementTypeKeyOption[A : TypeKey]: Option[TypeKey[_]] =
    if (typeKey[A].tpe <:< typeOf[Option[_]]) Some(typeKey[A].typeArgs.head) else None

  /** returns a `Some` containing the enclosing type of the set whenever the supplied type argument `A`
   * is a Set. otherwise returns `None`.
   */
  protected def setElementTypeKeyOption[A : TypeKey]: Option[TypeKey[_]] =
    if (typeKey[A].tpe <:< typeOf[Set[_]]) Some(typeKey[A].typeArgs.head) else None

  /** returns a `Some` containing the enclosing type of the list whenever the supplied type argument `A`
   * is a List. otherwise returns `None`.
   */
  protected def listElementTypeKeyOption[A : TypeKey]: Option[TypeKey[_]] =
    if (typeKey[A].tpe <:< typeOf[List[_]]) Some(typeKey[A].typeArgs.head) else None

}