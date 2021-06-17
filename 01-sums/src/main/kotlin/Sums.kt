import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.system.exitProcess

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
                while (!socket.isClosed) {
                    val line = input.readUTF8Line()

                    if (line == "c0000") {
                        socket.close()
                    } else {
                        println("${socket.remoteAddress}: $line")

                        var answer = ""

                        answer = try {
                            line?.let {
                                line.split(" ").map { it.toInt() }.sum()
                            }.toString()
                        } catch (e: Throwable) {
                            e.toString()
                        }

                        output.writeStringUtf8("$answer\r\n")
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                socket.close()
            }
        }
    }
}

fun sumsClient(hostname: String, port: Int) = runBlocking {
    while(true) {
        launch {
            val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress(hostname, port))
            val input = socket.openReadChannel()
            val output = socket.openWriteChannel(autoFlush = true)

            while (!socket.isClosed) {
                val request = readLine()

                if (request == "exit") {
                    socket.close()
                    exitProcess(0)
                }

                output.writeStringUtf8("$request\r\n")
                val response = input.readUTF8Line()
                println("Server said: '$response'")
            }
        }
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
            "client" -> sumsClient("127.25.15.126", 2323)
            "server" -> sumsServer("172.25.15.126", 8082)
            else -> usage()
        }
    } else {
        usage()
    }
}