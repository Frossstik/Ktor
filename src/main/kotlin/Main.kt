import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.*
import java.io.File

suspend fun downloadImage(client: HttpClient, url: String): ByteArray {
    return client.get(url).body()
}

suspend fun saveImage(data: ByteArray, path: String) {
    withContext(Dispatchers.IO) {
        File(path).writeBytes(data)
    }
}

fun getSavePathProvider(useOriginalName: Boolean): (Int, String) -> String {
    return if (useOriginalName) {
        { _, url -> "images/${url.substringAfterLast('/')}.jpg" }
    } else {
        { index, _ -> "images/image${index + 1}.jpg" }
    }
}

fun downloadAndSaveImages(urls: List<String>, savePathProvider: (index: Int, url: String) -> String) {
    runBlocking {
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
            }
        }

        val images = urls.mapIndexed { index, url ->
            async {
                try {
                    val data = downloadImage(client, url)
                    val path = savePathProvider(index, url)
                    saveImage(data, path)
                    println("${path} загружено!")
                }
                catch (e: HttpRequestTimeoutException){
                    println("Таймаут $url")
                }
            }
        }

        images.awaitAll()
        client.close()
    }
}

fun main() {
    val urls = List(100) { "https://picsum.photos/${(200..900).random()}" } //Работает только с впн
    print("true/false?: ")
    val isOriginal = readln() //bool
    downloadAndSaveImages(urls, getSavePathProvider(isOriginal.toBooleanStrict()))
}