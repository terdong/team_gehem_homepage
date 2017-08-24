def inc(i: Int):Stream[Int] = Stream.cons(i, inc(i+1))
def inc2(head: Int):Stream[Int] = head #:: inc2(head + 1)

val s = inc(1)
s
s(1)
val l = s.take(5).toList
s

val s2 = inc2(2)

inc2(10) take(10) toList

inc2(10).take(5).toList


def to(head:Char, end:Char):Stream[Char] = (head > end) match {
  case true =>  { println(s"head = $head, end = $end"); Stream.empty;}
  case flase => head #:: to((head + 1).toChar, end)
}

to('A', 'F') take(20) toList


