package emblem.traversors

import emblem.TypeBoundFunction
import emblem.exceptions.CouldNotGenerateException
import emblem.exceptions.CouldNotTraverseException
import emblem.exceptions.ExtractorInverseException
import emblem.imports._
import emblem.traversors.Generator._

/** recursively generates a data structure by type.
 *
 * you can generate arbritrary data to your liking by implementing the protected vals and defs in this
 * interface. as of yet, i haven't been able to generate the scaladoc for those protected methods.
 * sorry about that.
 *
 * @see [[TestDataGenerator]] for an example usage
 */
trait Generator {

  /** generates data for the specified type `A`
   * @tparam A the type of data to generate
   * @return the generated data
   * @throws emblem.exceptions.CouldNotGenerateException when we encounter a type in the recursive traversal
   * that we don't know how to generate for
   */
  def generate[A : TypeKey]: A = try {
    traversor.traverse[A](())
  } catch {
    case e: CouldNotTraverseException => throw new CouldNotGenerateException(e.typeKey, e)
  }

  /** the emblems to use in the recursive generation */
  protected val emblemPool: EmblemPool = EmblemPool.empty

  /** the extractors to use in the recursive generation */
  protected val extractorPool: ExtractorPool = ExtractorPool.empty

  /** the custom generators to use in the recursive generation */
  protected val customGeneratorPool: CustomGeneratorPool = CustomGeneratorPool.empty

  /** generates an option */
  protected def option[A](a: => A): Option[A]

  /** generates a set */
  protected def set[A](a: => A): Set[A]

  /** generates a list */
  protected def list[A](a: => A): List[A]

  /** generates a boolean */
  protected def boolean: Boolean

  /** generates a char */
  protected def char: Char

  /** generates a double */
  protected def double: Double

  /** generates a float */
  protected def float: Float

  /** generates an int */
  protected def int: Int

  /** generates a long */
  protected def long: Long
  
  /** generates a string */
  protected def string: String

  private val traversor = new Traversor {

    type TraverseInput[A] = Unit
    type TraverseResult[A] = A

    def traverseBoolean(input: Unit): Boolean = boolean

    def traverseChar(input: Unit): Char = char

    def traverseDouble(input: Unit): Double = double

    def traverseFloat(input: Unit): Float = float

    def traverseInt(input: Unit): Int = int

    def traverseLong(input: Unit): Long = long

    def traverseString(input: Unit): String = string

    override protected val extractorPool = Generator.this.extractorPool
    override protected val emblemPool = Generator.this.emblemPool

    override protected val customTraversors = {
      class GenCustomTraversor[A](val customGenerator: CustomGenerator[A]) extends CustomTraversor[A] {
        def apply[B <: A : TypeKey](input: Unit): B = customGenerator.apply[B](Generator.this)
      }
      val generatorToTraversor = new TypeBoundFunction[Any, CustomGenerator, CustomTraversor] {
        def apply[A](generator: CustomGenerator[A]): CustomTraversor[A] = new GenCustomTraversor(generator)
      }
      customGeneratorPool.mapValues(generatorToTraversor)
    }

    protected def stageEmblemProps[A <: HasEmblem](emblem: Emblem[A], input: Unit)
    : Iterator[PropInput[A, _]] =
      emblem.props.map((_, ())).iterator

    protected def unstageEmblemProps[A <: HasEmblem](
      emblem: Emblem[A],
      input: Unit,
      result: Iterator[PropResult[A, _]])
    : A = {
      val builder = emblem.builder()
      result.foreach { case (prop, propResult) => builder.setProp(prop, propResult) }
      builder.build()
    }

    protected def stageExtractor[Domain : TypeKey, Range](
      extractor: Extractor[Domain, Range],
      input: Unit)
    : Unit =
      ()

    protected def unstageExtractor[Domain : TypeKey, Range](
      extractor: Extractor[Domain, Range],
      range: Range)
    : Domain =
      try {
        extractor.inverse(range)
      } catch {
        case e: Exception => throw new ExtractorInverseException(range, typeKey[Domain], e)
      }

    protected def stageOptionValue[A : TypeKey](input: Unit): Option[Unit] = option(())

    protected def unstageOptionValue[A : TypeKey](input: Unit, result: Option[A]): Option[A] = result

    protected def stageSetElements[A : TypeKey](input: Unit): Iterator[Unit] = list(()).iterator

    protected def unstageSetElements[A : TypeKey](input: Unit, result: Iterator[A]): Set[A] =
      result.toSet

    protected def stageListElements[A : TypeKey](input: Unit): Iterator[Unit] = list(()).iterator

    protected def unstageListElements[A : TypeKey](input: Unit, result: Iterator[A]): List[A] =
      result.toList

  }

}

/** holds types and zero values used by the [[Generator generators]] */
object Generator {

  /** a [[TypeKeyMap]] for [[CustomGenerator generator functions]] */
  type CustomGeneratorPool = TypeKeyMap[Any, CustomGenerator]

  object CustomGeneratorPool {

    /** an empty map of [[CustomGenerator generator functions]] */
    val empty: CustomGeneratorPool = TypeKeyMap[Any, CustomGenerator]()
  }

}
