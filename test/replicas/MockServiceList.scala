package replicas

/**
  * Created by toidiu on 11/28/16.
  */
object MockService1 extends MockService
object MockService2 extends MockService
object MockService3 extends MockService
object MockService4 extends MockService

object MockDisabledService extends MockAbstractService{
  override val isEnable: Boolean = false
  override val isWhiteList: Boolean = false
  override val mimeList: List[String] = Nil
  override val maxLength: Long = 1000000000
}

object Mock10MaxLengthService extends MockAbstractService{
  override val isEnable: Boolean = true
  override val isWhiteList: Boolean = false
  override val mimeList: List[String] = Nil
  override val maxLength: Long = 10
}

object MockWhiteTxtService extends MockAbstractService{
  override val isEnable: Boolean = true
  override val isWhiteList: Boolean = true
  override val mimeList: List[String] = List("text/plain")
  override val maxLength: Long = 1000000000
}

object MockBlackTxtService extends MockAbstractService{
  override val isEnable: Boolean = true
  override val isWhiteList: Boolean = false
  override val mimeList: List[String] = List("text/plain")
  override val maxLength: Long = 1000000000
}