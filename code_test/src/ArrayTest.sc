val colors = Array("red", "green", "blue")

colors(0) = "purple"

colors
println("very purple: " + colors)

val files = new java.io.File("c:/").listFiles

val sys = files map (_.getName) filter(_ endsWith "sys")

