package dev.nautchkafe.hub.v2.communication.api.packet

import com.fasterxml.jackson.annotation.JsonTypeInfo
import jodd.exception.ExceptionUtil.message
import java.io.Serializable
import java.util.concurrent.ThreadLocalRandom


/**
 * @author: noyzys on 17:42, 12.06.2021
 **/

typealias HubRedisPacketSender = IHubRedisPacketSender
typealias HubPacketFailureMessage = String

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
sealed class HubPacket : Serializable

interface IHubPacketHandler<in T : HubPacket> {
    fun handle(packet: T)
}

interface IHubPacketCallback {
    fun successfully(packet: HubPacket)
    fun failed(packetFailureMessage: HubPacketFailureMessage)
}

interface IHubRedisPacketSender {

    fun sendPacket(topic: String, packet: HubPacket)
    fun sendPacket(topic: String, packet: HubPacketCallback, callback: HubPacketCallback)
}

sealed class HubPacketCallback : HubPacket() {

    val callbackId
        get() = ThreadLocalRandom.current().nextLong(Long.MIN_VALUE, Long.MAX_VALUE)

    abstract val callbackMessage: String
    fun isSucess() = callbackMessage == null
}

data class UserDataPacket(
    private val name: String,
    private val age: Int
) : HubPacket()