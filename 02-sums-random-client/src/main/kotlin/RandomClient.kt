import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.random.Random
import kotlin.system.exitProcess

/*
Напишите клиента, который генерирует случайное количество случайных целых чисел (Random.nextInt),
формирует запрос с числами на сервер, отправляет его, получает от сервера и печатает результат.
Всего клиент должен посылать 100 запросов.
 */

fun sumsClient(hostname: String, port: Int) = runBlocking {
    val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress(hostname, port))
    val input = socket.openReadChannel()
    val output = socket.openWriteChannel(autoFlush = true)

    var request = "0"

    repeat(Random.nextInt(20)) {
        request += " " + Random.nextInt(200)
    }

    output.writeStringUtf8("$request\r\n")
    println("Server said: '${input.readUTF8Line()}'")

    output.writeStringUtf8("c0000")
    socket.close()
}

fun main(args: Array<String>) {
    sumsClient("172.25.15.126", 8082)
}