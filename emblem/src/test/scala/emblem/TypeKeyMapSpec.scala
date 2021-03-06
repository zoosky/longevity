package emblem

import org.scalatest._

/** [[TypeKeyMap]] specifications */
class TypeKeyMapSpec extends FlatSpec with GivenWhenThen with Matchers {

  behavior of "the example code in the scaladocs for TypeKeyMap"
  it should "compile and produce the expected values" in {

    import emblem.testData.computerParts._

    var partLists = TypeKeyMap[ComputerPart, List]()
    partLists += Memory(2) :: Memory(4) :: Memory(8) :: Nil
    partLists += CPU(2.2) :: CPU(2.4) :: CPU(2.6) :: Nil
    partLists += Display(720) :: Display(1080) :: Nil

    val memories: List[Memory] = partLists[Memory]
    memories.size should be (3)
    val cpus: List[CPU] = partLists[CPU]
    cpus.size should be (3)
    val displays: List[Display] = partLists[Display]
    displays.size should be (2)

    val cpu: CPU = partLists[CPU].head
    cpu should equal (CPU(2.2))
    val display: Display = partLists[Display].tail.head
    display should equal (Display(1080))
  }

  // TODO pt-86951076: more specs:
  // - identity example
  // - double-TP value type (like in ExtractorPool)

  behavior of "a TypeKeyMap where the value type has a single type parameter"

  import emblem.testData.blogs._
  val userRepo = new CrmUserRepo
  val blogRepo = new CrmBlogRepo

  it should "only allow key/value pairs with matching type param" in {
    var entityTypeToRepoMap = TypeKeyMap[CrmEntity, CrmRepo]()
    "entityTypeToRepoMap += typeKey[CrmUser] -> userRepo" should compile
    "entityTypeToRepoMap += typeKey[CrmUser] -> blogRepo" shouldNot compile
    "entityTypeToRepoMap += typeKey[CrmBlog] -> userRepo" shouldNot compile
  }

  it should "store multiple key/value pairs for a given type param" in {
    val typeKeySet = Set(typeKey[CrmUser], typeKey[CrmBlog])

    var localEntityStore = TypeKeyMap[CrmEntity, Seq]()
    localEntityStore += (typeKey[CrmUser] -> Seq(CrmUser("user1"), CrmUser("user2"), CrmUser("user3")))
    localEntityStore += (typeKey[CrmBlog] -> Seq(CrmBlog("blog1"), CrmBlog("blog2")))

    var entityTypeToRepoMap = TypeKeyMap[CrmEntity, CrmRepo]()
    entityTypeToRepoMap += userRepo
    entityTypeToRepoMap += blogRepo

    // http://scabl.blogspot.com/2015/01/introduce-type-param-pattern.html
    def saveEntities[E <: CrmEntity : TypeKey]: Unit = {
      val entitySeq = localEntityStore(typeKey)
      val repo = entityTypeToRepoMap(typeKey)
      entitySeq.foreach { entity => repo.save(entity) }
    }

    typeKeySet.foreach { implicit typeKey => saveEntities }

    userRepo.saveCount should equal (3)
    blogRepo.saveCount should equal (2)
  }

  behavior of "TypeKeyMaps in the face of someone trying to break through my bulletpoof type signatures"
  it should "thwart the attack with a compile time error of unspecified opacity" in {
    import testData.pets._
    val dog = Dog("dog")
    val hound = Hound("hound")

    // standard usage is to map dogs to dogs and hounds to hounds
    var petMap = TypeKeyMap[Pet, PetIdentity]()
    petMap += dog // type key is inferred
    petMap[Dog] should equal (dog)

    petMap = TypeKeyMap[Pet, PetIdentity]()
    petMap += typeKey[Dog] -> dog // type key is specified
    petMap[Dog] should equal (dog)

    // you are not allowed to map a hound to a dog.
    // otherwise you could then call "petToPetMap(hound)" and get a ClassCastException.
    "petMap += typeKey[Hound] -> dog" shouldNot compile

    // you are allowed to map a dog to a hound.
    // in this case petMap[Dog] returns something of type PetIdentity[Dog] (which =:= Dog), and the
    // something returned is hound. nothing weird, no ClassCastException.
    "petMap += typeKey[Dog] -> hound" should compile

    petMap = TypeKeyMap[Pet, PetIdentity]()
    petMap += typeKey[Dog] -> hound
    petMap[Dog] should equal (hound)

    // standard usage is to map dogs to dogs and hounds to hounds
    val dogInvar = new PetBoxInvar(dog)
    val houndInvar = new PetBoxInvar(hound)
    var petBoxInvarMap = TypeKeyMap[Pet, PetBoxInvar]()
    petBoxInvarMap += dogInvar
    petBoxInvarMap[Dog] should be theSameInstanceAs dogInvar

    petBoxInvarMap = TypeKeyMap[Pet, PetBoxInvar]()
    petBoxInvarMap += typeKey[Dog] -> dogInvar
    petBoxInvarMap[Dog] should be theSameInstanceAs dogInvar

    // you are not allowed to map a hound to a PetBoxInvar[Dog].
    // otherwise you could then call "petBoxInvarMap[Hound]" and get a ClassCastException.
    "petBoxInvarMap += typeKey[Hound] -> dogInvar" shouldNot compile

    // you are not allowed to map a dog to a PetBoxInvar[Hound].
    // otherwise you could then call "petToPetBoxInvarMap(dog)" and get a ClassCastException.
    "petBoxInvarMap += typeKey[Dog] -> houndInvar" shouldNot compile

    // standard usage is to map dogs to dogs and hounds to hounds
    val dogCovar = new PetBoxCovar(dog)
    val houndCovar = new PetBoxCovar(hound)
    var petBoxCovarMap = TypeKeyMap[Pet, PetBoxCovar]()
    petBoxCovarMap += dogCovar
    petBoxCovarMap[Dog] should be theSameInstanceAs dogCovar

    petBoxCovarMap = TypeKeyMap[Pet, PetBoxCovar]()
    petBoxCovarMap += typeKey[Dog] -> dogCovar
    petBoxCovarMap[Dog] should be theSameInstanceAs dogCovar

    // you are not allowed to map a hound to a PetBoxCovar[Dog].
    // otherwise you could then call "petBoxCovarMap[Hound]" and get a ClassCastException.
    "petBoxCovarMap += typeKey[Hound] -> dogCovar" shouldNot compile

    // you are allowed to map a dog to a PetBoxCovar[Hound].
    // in this case petToPetMap(dog) returns something of type PetBoxCovar[Dog], which is >:> PetBoxCovar[Hound]
    petBoxCovarMap = TypeKeyMap[Pet, PetBoxCovar]()
    petBoxCovarMap += typeKey[Dog] -> houndCovar
    petBoxCovarMap[Dog] should be theSameInstanceAs houndCovar

    // standard usage is to map dogs to dogs and hounds to hounds
    val dogContravar = new PetBoxContravar
    val houndContravar = new PetBoxContravar
    var petBoxContravarMap = TypeKeyMap[Pet, PetBoxContravar]()

    // this next line of code DOES NOT do what you might expect!
    // when the compiler infers type parameter [TypeParam <: TypeBound] from an argument of type
    // Contra[TypeParam], where type Contra is defined e.g. trait Contra[+T], it's always going to pick
    // TypeParam as TypeBound. there seems to be nothing i can do within TypeKeyMap to circumvent this.
    petBoxContravarMap += dogContravar
    // the inferred TypeKey was not Dog
    intercept[NoSuchElementException] { petBoxContravarMap[Dog] }
    // the inferred TypeKey was Pet
    petBoxContravarMap[Pet] should be theSameInstanceAs dogContravar

    // with contravariance you will want to explicitly specify the type key like so, and not try to infer
    // it as above
    petBoxContravarMap = TypeKeyMap[Pet, PetBoxContravar]()
    petBoxContravarMap += typeKey[Dog] -> dogContravar
    petBoxContravarMap[Dog] should be theSameInstanceAs dogContravar

    // you are allowed to map a hound to a PetBoxContravar[Dog].
    // in this case petBoxContravarMap[Hound] returns something of type PetBoxContravar[Hound],
    // which is >:> PetBoxContravar[Dog]
    petBoxContravarMap = TypeKeyMap[Pet, PetBoxContravar]()
    petBoxContravarMap += typeKey[Hound] -> dogContravar
    petBoxContravarMap[Hound] should be theSameInstanceAs dogContravar    

    // you are not allowed to map a dog to a PetBoxContravar[Hound].
    // otherwise you could then call "petToPetBoxContravarMap(dog)" and get a ClassCastException.
    "petToPetBoxCovarMap += dog -> new PetBoxContravar[Hound]" shouldNot compile    
  }

  behavior of "TypeKeyMap.++"
  it should "produce the union of the two type key maps" in {
    import emblem.testData.computerParts._

    val memoryList = Memory(2) :: Memory(4) :: Memory(8) :: Nil
    val cpuList = CPU(2.2) :: CPU(2.4) :: CPU(2.6) :: Nil
    val displayList = Display(720) :: Display(1080) :: Nil

    val emptyPartLists = TypeKeyMap[ComputerPart, List]()
    val partListsM = emptyPartLists + memoryList
    val partListsC = emptyPartLists + cpuList
    val partListsD = emptyPartLists + displayList
    val partListsMC = partListsM + cpuList
    val partListsMD = partListsM + displayList
    val partListsCD = partListsC + displayList
    val partListsMCD = partListsMC + displayList

    (emptyPartLists ++ emptyPartLists) should equal (emptyPartLists)
    (emptyPartLists ++ partListsM) should equal (partListsM)
    (emptyPartLists ++ partListsC) should equal (partListsC)
    (emptyPartLists ++ partListsD) should equal (partListsD)
    (emptyPartLists ++ partListsMC) should equal (partListsMC)
    (emptyPartLists ++ partListsMD) should equal (partListsMD)
    (emptyPartLists ++ partListsCD) should equal (partListsCD)
    (emptyPartLists ++ partListsMCD) should equal (partListsMCD)

    (partListsM ++ emptyPartLists) should equal (partListsM)
    (partListsM ++ partListsM) should equal (partListsM)
    (partListsM ++ partListsC) should equal (partListsMC)
    (partListsM ++ partListsD) should equal (partListsMD)
    (partListsM ++ partListsMC) should equal (partListsMC)
    (partListsM ++ partListsMD) should equal (partListsMD)
    (partListsM ++ partListsCD) should equal (partListsMCD)
    (partListsM ++ partListsMCD) should equal (partListsMCD)

    (partListsMC ++ emptyPartLists) should equal (partListsMC)
    (partListsMC ++ partListsM) should equal (partListsMC)
    (partListsMC ++ partListsC) should equal (partListsMC)
    (partListsMC ++ partListsD) should equal (partListsMCD)
    (partListsMC ++ partListsMC) should equal (partListsMC)
    (partListsMC ++ partListsMD) should equal (partListsMCD)
    (partListsMC ++ partListsCD) should equal (partListsMCD)
    (partListsMC ++ partListsMCD) should equal (partListsMCD)

    (partListsMCD ++ emptyPartLists) should equal (partListsMCD)
    (partListsMCD ++ partListsM) should equal (partListsMCD)
    (partListsMCD ++ partListsC) should equal (partListsMCD)
    (partListsMCD ++ partListsD) should equal (partListsMCD)
    (partListsMCD ++ partListsMC) should equal (partListsMCD)
    (partListsMCD ++ partListsMD) should equal (partListsMCD)
    (partListsMCD ++ partListsCD) should equal (partListsMCD)
    (partListsMCD ++ partListsMCD) should equal (partListsMCD)
  }

}
