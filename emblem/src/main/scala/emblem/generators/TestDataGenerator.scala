package emblem.generators

import TestDataGenerator._
import emblem._
import emblem.exceptions.CouldNotGenerateException
import emblem.reflectionUtil.makeTypeTag
import scala.reflect.runtime.universe.typeOf

/** holds types and zero values used by the [[TestDataGenerator]] */
object TestDataGenerator {

  /** A [[TypeKeyMap]] for [[CustomGenerator generator functions]] */
  type CustomGenerators = TypeKeyMap[Any, CustomGenerator]

  /** An empty map of [[CustomGenerator generator functions]] */
  def emptyCustomGenerators: CustomGenerators = TypeKeyMap[Any, CustomGenerator]()

}

/** Generates test data for a pool of shorthands, a pool of emblems, and some custom generators. You can
 * generate any kind of data you like by providing the appropriate [[TypeKey]] to [[TestDataGenerator.any]].
 * Or you can use the provided methods for generating specific kinds of data. If the generator does not know
 * how to generate for the type you requested, it will throw a [[emblem.exceptions.CouldNotGenerateException]].
 *
 * Out of the box, a TestDataGenerator knows how to generate the following basic and collection types:
 *
 *   - boolean
 *   - char
 *   - double
 *   - float
 *   - int
 *   - long
 *   - string
 *   - list
 *   - option
 *   - set
 *
 * You can extend this behavior by supplying the generator with [[Shorthand shorthands]], [[Emblem emblems]],
 * and [[CustomGenerator custom generators]].
 *
 * @param shorthandPool the shorthands to generate test data for. defaults to empty
 * @param emblemPool the emblems to generate test data for. defaults to empty
 * @param customGenerators custom generation functions. defaults to empty. custom generators take precedence
 * over all other generators
 */
class TestDataGenerator (
  private val shorthandPool: ShorthandPool = ShorthandPool(),
  private val emblemPool: EmblemPool = EmblemPool(),
  private val customGenerators: CustomGenerators = emptyCustomGenerators
) {

  private val random = new util.Random

  private val basicGenerators =
    TypeKeyMap[Any, Function0] + boolean _ + char _ + double _ + float _ + int _ + long _ + string _

  /** Generates test data for the specified type `A`.
   * @throws emblem.exceptions.CouldNotGenerateException when we cannot find a way to generate something of
   * type A
   */
  def any[A : TypeKey]: A = anyOption[A] getOrElse {
    throw new CouldNotGenerateException(typeKey[A])
  }

  /** Generates test data for the specified type `A` according to a custom generator
   * @throws emblem.exceptions.CouldNotGenerateException when there is no custom generator for type A
   */
  def custom[A : TypeKey]: A = customOption[A] getOrElse {
    throw new CouldNotGenerateException(typeKey[A])
  }

  /** Generates test data for the specified type `A` via an emblem in the pool
   * @throws emblem.exceptions.CouldNotGenerateException when there is no emblem in the pool for type A
   */
  def emblem[A <: HasEmblem : TypeKey]: A = emblemOption[A] getOrElse {
    throw new CouldNotGenerateException(typeKey[A])
  }

  /** Generates test data for the specified type `A` via a shorthand in the pool
   * @throws emblem.exceptions.CouldNotGenerateException when there is no shorthand in the pool for type A
   */
  def shorthand[Actual : TypeKey]: Actual = shorthandOption[Actual] getOrElse {
    throw new CouldNotGenerateException(typeKey[Actual])
  }

  /** Generates an option containing (or not) an element of type A. Generates `Some` and `None` values
   * at about a 50-50 ratio.
   * @throws emblem.exceptions.CouldNotGenerateException when we cannot generate the contained type A
   */
  def option[A : TypeKey]: Option[A] = if (boolean) Some(any[A]) else None

  /** Generates a set containing (or not) elements of type A. Generates sets of size 0, 1, 2 and 3
   * at about a 25-25-25-25 ratio.
   * @throws emblem.exceptions.CouldNotGenerateException when we cannot generate the contained type A
   */
  def set[A : TypeKey]: Set[A] = math.abs(int % 4) match {
    case 0 => Set[A]()
    case 1 => Set[A](any[A])
    case 2 => Set[A](any[A], any[A])
    case 3 => Set[A](any[A], any[A], any[A])
  }

  /** Generates a list containing (or not) elements of type A. Generates lists of size 0, 1, 2 and 3
   * at about a 25-25-25-25 ratio.
   * @throws emblem.exceptions.CouldNotGenerateException when we cannot generate the contained type A
   */
  def list[A : TypeKey]: List[A] = math.abs(int % 4) match {
    case 0 => List[A]()
    case 1 => List[A](any[A])
    case 2 => List[A](any[A], any[A])
    case 3 => List[A](any[A], any[A], any[A])
  }

  /** Generates a boolean that is true around half the time */
  def boolean: Boolean = random.nextBoolean()

  /** Generates a char that is either a decimal digit or a letter (upper or lowercase) from the Roman
   * alphabet */
  def char: Char = math.abs(random.nextInt % 62) match {
    case i if i < 26 => (i + 'A').toChar
    case i if i < 52 => (i - 26 + 'a').toChar
    case i => (i - 52 + '0').toChar
  }

  /** Generates a double */
  def double: Double = random.nextDouble() 

  /** Generates a float */
  def float: Float = random.nextFloat() 

  /** Generates an int */
  def int: Int = random.nextInt()

  /** Generates a long */
  def long: Long = random.nextLong() 
  
  /** Generates a string of length 8 */
  def string: String = string(8)

  /** Generates a string of the specified length */
  def string(length: Int): String = new String((1 to length).map(i => char).toArray)

  // custom generators have to come first. after that order is immaterial
  private def anyOption[A : TypeKey]: Option[A] =
    customOption orElse
    emblemOptionFromAny orElse
    shorthandOption orElse
    optionOption orElse
    setOption orElse
    listOption orElse
    basicOption

  private def customOption[A : TypeKey]: Option[A] = {
    val keyOpt: Option[TypeKey[_ >: A]] = customGenerators.keys.map(_.castToLowerBound[A]).flatten.headOption
    def getGenerator[B >: A : TypeKey]: CustomGenerator[B] = customGenerators(typeKey[B])
    keyOpt map { key => getGenerator(key).apply[A](this) }
  }

  private def emblemOptionFromAny[A : TypeKey]: Option[A] = {
    val keyOption = hasEmblemTypeKeyOption(typeKey[A])
    keyOption flatMap { k => emblemOption(k) }
  }

  private def hasEmblemTypeKeyOption[A : TypeKey, B <: A with HasEmblem]: Option[TypeKey[B]] =
    if (typeKey[A].tpe <:< typeOf[HasEmblem])
      Some(typeKey[A].asInstanceOf[TypeKey[B]])
    else
      None

  private def emblemOption[A <: HasEmblem : TypeKey]: Option[A] =
    emblemPool.get(typeKey[A]) map { e => genFromEmblem(e) }

  private def genFromEmblem[A <: HasEmblem](emblem: Emblem[A]): A = {
    val builder = emblem.builder()
    emblem.props.foreach { prop => setEmblemProp(builder, prop) }
    builder.build()
  }

  private def shorthandOption[Actual : TypeKey]: Option[Actual] =
    shorthandPool.get[Actual] map { s => genFromShorthand[Actual](s) }

  private def genFromShorthand[Actual](shorthand: Shorthand[Actual, _]): Actual =
    genFromFullyTypedShorthand(shorthand)

  // http://scabl.blogspot.com/2015/01/introduce-type-param-pattern.html
  private def genFromFullyTypedShorthand[Actual, Abbreviated](
    shorthand: Shorthand[Actual, Abbreviated]): Actual =
    basicGenerators.get(shorthand.abbreviatedTypeKey) match {
      case Some(gen) => shorthand.unabbreviate(gen())
      case None => throw new CouldNotGenerateException(shorthand.abbreviatedTypeKey)
    }

  // TODO: try to remove code duplication below with optionOption / setOption / listOption
  // generalize to other kinds of "collections"

  private def optionOption[OptionA : TypeKey]: Option[OptionA] = {
    val keyOption = optionTypeKeyOption(typeKey[OptionA])
    keyOption map { k => option(k).asInstanceOf[OptionA] }
  }

  /** returns a `Some` containing the enclosing type of the option whenever the supplied type argument `A`
   * is an Option. otherwise returns `None`. */
  private def optionTypeKeyOption[A : TypeKey]: Option[TypeKey[_]] =
    if (typeKey[A].tpe <:< typeOf[Option[_]]) Some(typeKey[A].typeArgs.head) else None

  private def setOption[SetA : TypeKey]: Option[SetA] = {
    val keyOption = setTypeKeyOption(typeKey[SetA])
    keyOption map { k => set(k).asInstanceOf[SetA] }
  }

  /** returns a `Some` containing the enclosing type of the set whenever the supplied type argument `A`
   * is an Set. otherwise returns `None`. */
  private def setTypeKeyOption[A : TypeKey]: Option[TypeKey[_]] =
    if (typeKey[A].tpe <:< typeOf[Set[_]]) Some(typeKey[A].typeArgs.head) else None

  private def listOption[ListA : TypeKey]: Option[ListA] = {
    val keyOption = listTypeKeyOption(typeKey[ListA])
    keyOption map { k => list(k).asInstanceOf[ListA] }
  }

  /** returns a `Some` containing the enclosing type of the list whenever the supplied type argument `A`
   * is an List. otherwise returns `None`. */
  private def listTypeKeyOption[A : TypeKey]: Option[TypeKey[_]] =
    if (typeKey[A].tpe <:< typeOf[List[_]]) Some(typeKey[A].typeArgs.head) else None

  private def basicOption[Basic : TypeKey]: Option[Basic] = basicGenerators.get[Basic] map { gen => gen() }

  private def setEmblemProp[A <: HasEmblem, B](builder: HasEmblemBuilder[A], prop: EmblemProp[A, B]): Unit =
    builder.setProp(prop, any(prop.typeKey))

  private def isAlphaNumeric(c: Char) =
    (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')

}
