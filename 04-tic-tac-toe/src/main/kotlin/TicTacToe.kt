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
    for (i in Desc.indices) {
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

fun checkUp(x: Int, y: Int, value: Int): Boolean {
    val op = if (value == 1) {2} else {1}
    var tmpY = y + 1

    while (tmpY < 8 && Desc[tmpY][x] == op) { tmpY += 1 }

    if (tmpY >= 8 || Desc[tmpY][x] != value)
        return false

    tmpY = y + 1
    while (Desc[tmpY][x] == op) {
        Desc[tmpY][x] = value
        tmpY += 1
    }
    return true
}

fun checkDown(x: Int, y: Int, value: Int): Boolean {
    val op = if (value == 1) {2} else {1}
    var tmpY = y - 1

    while (tmpY >= 0 && Desc[tmpY][x] == op) { tmpY -= 1 }

    if (tmpY < 0 || Desc[tmpY][x] != value)
        return false

    tmpY = y - 1
    while (Desc[tmpY][x] == op) {
        Desc[tmpY][x] = value
        tmpY -= 1
    }
    return true
}

fun checkLeft(x: Int, y: Int, value: Int): Boolean {
    val op = if (value == 1) {2} else {1}
    var tmpX = x - 1

    while (tmpX >= 0 && Desc[y][tmpX] == op) { tmpX -= 1 }

    if (tmpX < 0 || Desc[y][tmpX] != value)
        return false

    tmpX = x - 1
    while (Desc[y][tmpX] == op) {
        Desc[y][tmpX] = value
        tmpX -= 1
    }
    return true
}

fun checkRight(x: Int, y: Int, value: Int): Boolean {
    val op = if (value == 1) {2} else {1}
    var tmpX = x + 1

    while (tmpX > 8 && Desc[y][tmpX] == op) { tmpX += 1 }

    if (tmpX >= 8 || Desc[y][tmpX] != value)
        return false

    tmpX = x + 1
    while (Desc[y][tmpX] == op) {
        Desc[y][tmpX] = value
        tmpX += 1
    }
    return true
}

fun setDesc(code: Int): Boolean {
    val x = (code / 10) % 10
    val y = code % 10
    val value = code / 100

    if (Desc[y][x] != 0)
        return false

    Desc[y][x] = value

    checkUp(x, y, value)
    checkDown(x, y, value)
    checkLeft(x, y, value)
    checkRight(x, y, value)

    return true
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
        val status = input.readUTF8Line()
        print(status)

        clearConsole()

        when (status) {
            "exit" -> break
            "wait" -> {
                clearConsole()
                println("Status: Waiting for opponent ...\n")
                printDesc()
            }
            "set" -> {
                clearConsole()
                println("Status: Getting data ...\n")
                setDesc(input.readInt())
                printDesc()
            }
            "sync" -> {
                clearConsole()
                println("Status: Synchronisation data ...\n")
                repeat (64) {
                    setDesc(input.readInt())
                }
                printDesc()
            }
            "turn" -> {
                clearConsole()
                println("Status: Your turn!\n")
                printDesc()
                println("Your choose (x, y): ")
                val xy = readLine().toString().split(" ")
                Desc[xy[1].toInt()][xy[0].toInt()] = id
                output.writeInt(getDesc(xy[2].toInt(), xy[1].toInt()))
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
    output1.writeStringUtf8("wait")
    println("Socket1 accepted: ${socket1.remoteAddress}")

    val socket2 = server.accept()
    val input2 = socket2.openReadChannel()
    val output2 = socket2.openWriteChannel(autoFlush = true)
    output2.writeInt(2)
    output2.writeStringUtf8("wait")
    println("Socket2 accepted: ${socket2.remoteAddress}")

    var counter = 0

    try {
        while (true) {
            counter++

            if (counter > 8) {
                output1.writeStringUtf8("sync")
                output2.writeStringUtf8("sync")
                for (i in Desc.indices) {
                    for (j in Desc.indices) {
                        output1.writeInt(getDesc(j, i))
                        output2.writeInt(getDesc(j, i))
                    }
                }
            }

            output1.writeStringUtf8("turn")
            var value = input1.readInt()
            output1.writeStringUtf8("wait")

            setDesc(value)

            output2.writeStringUtf8("set")
            output2.writeInt(value)

            output2.writeStringUtf8("turn")
            value = input2.readInt()
            output2.writeStringUtf8("wait")

            setDesc(value)

            output1.writeStringUtf8("set")
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
        "client" -> gameClient("172.25.15.126", 8082)
        "server" -> gameServer("172.25.15.126", 8082)
    }
}
