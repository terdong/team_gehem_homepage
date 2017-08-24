def nextError = util.Try{ 1/ util.Random.nextInt(2)}

val x = nextError

val y = nextError

val z = nextError flatMap { _ => nextError }


