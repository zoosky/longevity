package emblem.traversors

import emblem.imports._
import emblem.TypeBoundFunction
import emblem.exceptions.CouldNotVisitException
import emblem.exceptions.CouldNotTraverseException
import emblem.traversors.Visitor._

// TODO pt-92300784 VisitorSpec

/** recursively visits a data structure by type.
 *
 * you can visit arbritrary data to your liking by implementing the protected vals and defs in this
 * interface. as yet, i haven't been able to generate the scaladoc for those protected methods.
 * sorry about that.
 *
 * WARNING: as of yet, this code is completely untested, and there is no example usage for you to follow.
 */
trait Visitor {

  /** visits an element of type `A`
   * @throws emblem.exceptions.CouldNotVisitException when it encounters a type it doesn't know how to
   * visit
   */
  def visit[A : TypeKey](input: A): Unit = try {
    traversor.traverse[A](input)
  } catch {
    case e: CouldNotTraverseException => throw new CouldNotVisitException(e.typeKey, e)
  }

  /** the emblems to use in the recursive visit */
  protected val emblemPool: EmblemPool = EmblemPool.empty

  /** the extractors to use in the recursive visit */
  protected val extractorPool: ExtractorPool = ExtractorPool.empty

  /** the custom visitors to use in the recursive visit */
  protected val customVisitors: CustomVisitorPool = CustomVisitorPool.empty

  /** visits a boolean */
  protected def visitBoolean(input: Boolean): Unit = {}

  /** visits a char */
  protected def visitChar(input: Char): Unit = {}

  /** visits a double */
  protected def visitDouble(input: Double): Unit = {}

  /** visits a float */
  protected def visitFloat(input: Float): Unit = {}

  /** visits an int */
  protected def visitInt(input: Int): Unit = {}

  /** visits a long */
  protected def visitLong(input: Long): Unit = {}

  /** visits a string */
  protected def visitString(input: String): Unit = {}

  private lazy val traversor = new Traversor {

    type TraverseInput[A] = A
    type TraverseResult[A] = Unit

    def traverseBoolean(input: Boolean): Unit = visitBoolean(input)

    def traverseChar(input: Char): Unit = visitChar(input)

    def traverseDouble(input: Double): Unit = visitDouble(input)

    def traverseFloat(input: Float): Unit = visitFloat(input)

    def traverseInt(input: Int): Unit = visitInt(input)

    def traverseLong(input: Long): Unit = visitLong(input)

    def traverseString(input: String): Unit = visitString(input)

    override protected val extractorPool = Visitor.this.extractorPool
    override protected val emblemPool = Visitor.this.emblemPool

    override protected val customTraversors = {
      class VisCustomTraversor[A](val customVisitor: CustomVisitor[A]) extends CustomTraversor[A] {
        def apply[B <: A : TypeKey](input: B): Unit =
          customVisitor.apply[B](Visitor.this, input)
      }
      val visitorToTraversor = new TypeBoundFunction[Any, CustomVisitor, CustomTraversor] {
        def apply[A](visitor: CustomVisitor[A]): CustomTraversor[A] = new VisCustomTraversor(visitor)
      }
      customVisitors.mapValues(visitorToTraversor)
    }

    protected def stageEmblemProps[A <: HasEmblem](emblem: Emblem[A], input: A)
    : Iterator[PropInput[A, _]] = {
      def propInput[B](prop: EmblemProp[A, B]) = (prop, prop.get(input))
      emblem.props.map(propInput(_)).iterator
    }

    protected def unstageEmblemProps[A <: HasEmblem](
      emblem: Emblem[A],
      input: A,
      result: Iterator[PropResult[A, _]])
    : Unit =
      ()

    protected def stageExtractor[Domain : TypeKey, Range](
      extractor: Extractor[Domain, Range],
      input: Domain)
    : Range =
      extractor.apply(input)

    protected def unstageExtractor[Domain : TypeKey, Range](
      extractor: Extractor[Domain, Range],
      domainResult: Unit)
    : Unit =
      ()

    protected def stageOptionValue[A : TypeKey](input: Option[A]): Option[A] = input

    protected def unstageOptionValue[A : TypeKey](input: Option[A], result: Option[Unit]): Unit = ()

    protected def stageSetElements[A : TypeKey](input: Set[A]): Iterator[A] = input.iterator

    protected def unstageSetElements[A : TypeKey](input: Set[A], result: Iterator[Unit]): Unit = ()

    protected def stageListElements[A : TypeKey](input: List[A]): Iterator[A] = input.iterator

    protected def unstageListElements[A : TypeKey](input: List[A], result: Iterator[Unit]): Unit = ()

  }

}

/** holds types and zero values used by the [[Visitor visitors]] */
object Visitor {

  /** a [[TypeKeyMap]] for [[CustomVisitor visitor functions]] */
  type CustomVisitorPool = TypeKeyMap[Any, CustomVisitor]

  object CustomVisitorPool {

    /** an empty map of [[CustomVisitor visitor functions]] */
    def empty: CustomVisitorPool = TypeKeyMap[Any, CustomVisitor]
  }

}
