import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

fun sumsServer(hostname: String, port: Int) = runBlocking {
    val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress(hostname, port))
    println("Started summation server at ${server.localAddress}")

    while (true) {
        val socket = server.accept()

        launch {
            println("Socket accepted: ${socket.remoteAddress}")

            val input = socket.openReadChannel()
            val output = socket.openWriteChannel(autoFlush = true)

            try {
                while (true) {
                    val line = input.readUTF8Line()

                    println("${socket.remoteAddress}: $line")

                    val answer = line?.let {
                        line.split(" ").map { it.toInt() }.sum()
                    }

                    output.writeStringUtf8("$answer\r\n")
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                socket.close()
            }
        }
    }
}

fun sumsClient(hostname: String, port: Int) = runBlocking {
    val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress(hostname, port))
    val input = socket.openReadChannel()
    val output = socket.openWriteChannel(autoFlush = true)

    while(true) {
        val request = readLine()
        output.writeStringUtf8("$request\n")
        val response = input.readUTF8Line()
        println("Server said: '$response'")
    }
}

fun usage() {
    println("""
        Usage: 
            sums server
            sums client
    """.trimIndent())
}

fun main(args: Array<String>) {
    if(args.size == 1) {
        when(args[0]) {
            "client" -> sumsClient("127.0.0.1", 2323)
            "server" -> sumsServer("127.0.0.1", 2323)
            else -> usage()
        }
    } else {
        usage()
    }
}