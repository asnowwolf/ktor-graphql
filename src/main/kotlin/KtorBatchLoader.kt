import com.expediagroup.graphql.server.extensions.getValueFromDataLoader
import com.expediagroup.graphql.server.extensions.getValuesFromDataLoader
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.dataloader.BatchLoader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import kotlin.reflect.KClass

class KtorBatchLoaderAdaptor<K, V>(val loader: KtorBatchLoader<K, V>) : BatchLoader<K, V?> {
    override fun load(keys: List<K>): CompletionStage<List<V?>> {
        return CompletableFuture.supplyAsync { runBlocking { loader.load(keys) } }
    }
}

interface KtorBatchLoader<K, V> {
    suspend fun load(keys: List<K>): List<V?>
}

suspend inline fun <reified K, reified V, reified LOADER : KtorBatchLoader<K, V>> DataFetchingEnvironment.getValuesFromBatchLoader(
    loader: KClass<LOADER>,
    keys: List<K>,
): List<V> {
    return getValuesFromDataLoader<K, V>(loader.qualifiedName!!, keys).await()
}

suspend inline fun <reified K, reified V, reified LOADER : KtorBatchLoader<K, V>> DataFetchingEnvironment.getValueFromBatchLoader(
    loader: KClass<LOADER>,
    key: K,
): V {
    return getValueFromDataLoader<K, V>(loader.qualifiedName!!, key).await()
}
