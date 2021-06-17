import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.random.Random

/*
Напишите программу, в которой создаётся и запускается 5 клиентов из предыдущего упражнения.
Для запуска следует использовать конструкцию launch внутри тела цикла. Организуйте консольный вывод так,
чтобы было понятно, какой из клиентов получает ответ (например, добавляя номер клиента).
 */

fun sumsClient(hostname: String, port: Int) = runBlocking {
    repeat(5) {
        launch {
            val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress(hostname, port))
            val input = socket.openReadChannel()
            val output = socket.openWriteChannel(autoFlush = true)

            var request = "0"

            repeat(Random.nextInt(20)) {
                request += " " + Random.nextInt(200)
            }

            output.writeStringUtf8("$request\r\n")
            println("Server said to $it: '${input.readUTF8Line()}'")

            output.writeStringUtf8("c0000")
            socket.close()
        }
    }
}

fun main(args: Array<String>) {
    sumsClient("172.25.15.126", 8082)
}