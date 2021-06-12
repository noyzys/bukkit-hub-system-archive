package dev.nautchkafe.hub.v2.communication.api

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.nautchkafe.hub.v2.communication.api.packet.*
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime


/**
 * @author: noyzys on 17:40, 12.06.2021
 **/
@ExperimentalTime
class HubRedisClient(
    val redissonClient: RedissonClient
) : HubRedisPacketSender {

    val callbackCache: Cache<Long, HubPacketCallback> =
        Caffeine.newBuilder()
            .expireAfterWrite(2L, DurationUnit.MINUTES)
            .maximumSize(10000L)
            .build()

    inline fun <reified T : HubPacket> subscribe(
        topic: String,
        type: Class<out T>,
        handler: IHubPacketHandler<T>
    ) {
        redissonClient.getTopic(topic).addListener(type) { _, callbackMessage ->
            val packetCallback = callbackMessage as? HubPacketCallback
            packetCallback?.callbackMessage.let {
            val callback = (callbackCache.getIfPresent(packetCallback?.callbackId) ?: return@let) as IHubPacketCallback
                callbackCache.invalidate(packetCallback?.callbackId)

                packetCallback?.isSucess() ?: packetCallback?.let{ callback.successfully(it) } ?: callback.failed(
                    packetCallback?.callbackMessage ?: return@let
                )

                handler.handle(callbackMessage)
            }
        }
    }

    override fun sendPacket(topic: String, packet: HubPacket) {
        redissonClient.getTopic(topic).publish(packet)
    }

    override fun sendPacket(topic: String, packet: HubPacketCallback, callback: HubPacketCallback) {
        sendPacket(topic, packet)
        callbackCache.put(packet.callbackId, callback)
    }
}

@ExperimentalTime
fun main() {
    val config = Config().apply {
        useSingleServer().address = "redis://127.0.0.1:6379"
        codec = JsonJacksonCodec.INSTANCE
    }

    val redis = HubRedisClient(Redisson.create(config))
    redis.subscribe("userpacket", UserDataPacket::class.java) { packet -> print("doszlo") }
}

