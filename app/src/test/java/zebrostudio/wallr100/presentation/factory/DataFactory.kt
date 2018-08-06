package zebrostudio.wallr100.presentation.factory

class DataFactory {

  companion object Factory {

    fun randomString(): String {
      return java.util.UUID.randomUUID().toString()
    }

    fun randomInteger(): Int {
      return Math.random().toInt()
    }
  }
}