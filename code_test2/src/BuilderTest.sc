val b = Set.newBuilder[Int]

b += 1

println("hello")

b ++= List(2,3,4,5)

val rb = b result

b.toString
rb toString

val list = b mapResult(_.toList)


val rl1 = list result()
rl1 toString

list += 6
list += 7

val rl = list result
val rl2 = rl.sorted
rl toString()
rl2 toString()






