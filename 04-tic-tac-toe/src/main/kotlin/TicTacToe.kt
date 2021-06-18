import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.random.Random

/*
Напишите консольную сетевую версию игры "Крестики-нолики": один игрок является сервером, другой — клиентом.
Определите массив с символами (как object) на обеих сторонах и меняйте его элементы в зависимости от
ввода пользователя и данных, полученных от клиента (сервера). Выводить массив можно как поле игры на обеих сторонах
после каждого хода. Для простоты можно считать, что сервер всегда начинает и играет крестиками.
В первой версии можно не проверять корректность ходов и обнаруживать конец игры.
Клетки можно нумеровать числами от 0 до 8 как индексы элементов массива.
 */

var Desc = arrayOf(
    arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
    arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
    arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
    arrayOf(0, 0, 0, 2, 1, 0, 0, 0),
    arrayOf(0, 0, 0, 1, 2, 0, 0, 0),
    arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
    arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
    arrayOf(0, 0, 0, 0, 0, 0, 0, 0)
)

fun printDesc() {
    print("  ")
    for (j in Desc[0].indices) {
        print("$j")
    }
    println()
    for (i in Desc.indices) {
        print("$i ")
        for (j in Desc[i].indices) {
            when (Desc[i][j]) {
                0 -> print("_")
                1 -> print("X")
                2 -> print("O")
            }
        }
        println()
    }
}

fun checkDir(x: Int, y: Int, value: Int, dX: Int, dY: Int): Int {
    val op = if (value == 1) {2} else {1}
    var tmpX = x + dX
    var tmpY = y + dY

    while (tmpX in 0..7 && tmpY in 0..7 && Desc[tmpY][tmpX] == op) {
        tmpX += dX
        tmpY += dY
    }

    if (tmpX !in 0..7 || tmpY !in 0..7 || Desc[tmpY][tmpX] != value)
        return 0

    tmpX = x + dX
    tmpY = y + dY
    while (Desc[tmpY][tmpX] == op) {
        Desc[tmpY][tmpX] = value
        tmpX += dX
        tmpY += dY
    }
    return 1
}

fun setDesc(code: Int): Boolean {
    val x = (code / 10) % 10
    val y = code % 10
    val value = code / 100
    var res = 0

    if (Desc[y][x] != 0)
        return false

    Desc[y][x] = value

    for (i in -1..1)
        for (j in -1..1)
            if (i != 0 || j != 0)
                res += checkDir(x, y, value, i, j)

    return res > 0
}

fun getDesc(x: Int, y: Int): Int {
    return Desc[y][x] * 100 + x * 10 + y
}

fun gameClient(hostname: String, port: Int) = runBlocking {
    val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress(hostname, port))
    val input = socket.openReadChannel()
    val output = socket.openWriteChannel(autoFlush = true)

    val id = input.readInt()

    print(id)

    while (true) {
        val status = input.readInt()
        print(status)

        clearConsole()

        when (status) {
            0 /*"exit"*/ -> break
            1 /*"wait"*/ -> {
                clearConsole()
                println("Status: Waiting for opponent ...\n")
                printDesc()
            }
            2 /*"set"*/ -> {
                clearConsole()
                println("Status: Getting data ...\n")
                setDesc(input.readInt())
                printDesc()
            }
            3 /*"sync"*/ -> {
                clearConsole()
                println("Status: Synchronisation data ...\n")
                repeat (64) {
                    setDesc(input.readInt())
                }
                printDesc()
            }
            4 /*"turn"*/ -> {
                clearConsole()
                println("Status: Your turn!\n")
                printDesc()
                println("Your choose (x, y): ")
                val xy = readLine().toString().split(" ")
                setDesc(xy[1].toInt() + xy[0].toInt() * 10 + id * 100)
                output.writeInt(getDesc(xy[0].toInt(), xy[1].toInt()))
            }
        }
    }

    clearConsole()
    println("Status: Connection closed!\n")
    printDesc()

    output.writeStringUtf8("c0000")
    socket.close()
}

fun gameServer(hostname: String, port: Int) = runBlocking {
    val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress(hostname, port))
    println("Started summation server at ${server.localAddress}")

    val socket1 = server.accept()
    val input1 = socket1.openReadChannel()
    val output1 = socket1.openWriteChannel(autoFlush = true)
    output1.writeInt(1)
    output1.writeInt(1) //"wait")
    println("Socket1 accepted: ${socket1.remoteAddress}")

    val socket2 = server.accept()
    val input2 = socket2.openReadChannel()
    val output2 = socket2.openWriteChannel(autoFlush = true)
    output2.writeInt(2)
    output2.writeInt(1) //"wait")
    println("Socket2 accepted: ${socket2.remoteAddress}")

    var counter = 0

    try {
        while (true) {
            counter++

            if (counter > 8) {
                output1.writeInt(3) //"sync")
                output2.writeInt(3) //"sync")
                for (i in Desc.indices) {
                    for (j in Desc.indices) {
                        output1.writeInt(getDesc(j, i))
                        output2.writeInt(getDesc(j, i))
                    }
                }
            }

            output1.writeInt(4) //"turn")
            var value = input1.readInt()
            output1.writeInt(1) //"wait")

            setDesc(value)

            output2.writeInt(2) //"set")
            output2.writeInt(value)

            output2.writeInt(4) //"turn")
            value = input2.readInt()
            output2.writeInt(1) //"wait")

            setDesc(value)

            output1.writeInt(2) ///"set")
            output1.writeInt(value)
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        socket1.close()
        socket2.close()
    }
}

fun clearConsole() {
    println("\n\n\n\n\n\n\n\n\n\n\n\n\n")
}

fun main(args: Array<String>) {
    when(args[0]) {
        "client" -> gameClient("127.0.0.1", 8082)
        "server" -> gameServer("127.0.0.1", 8082)
    }
}
