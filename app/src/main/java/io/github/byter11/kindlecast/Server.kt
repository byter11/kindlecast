package io.github.byter11.kindlecast

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

class AZW3Server(private val fileToServe: File, port: Int = 8080) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        return try {
            val inputStream = fileToServe.inputStream()
            // Using octet-stream to force download on the Kindle browser
            newFixedLengthResponse(
                Response.Status.OK,
                "application/octet-stream",
                inputStream,
                fileToServe.length()
            ).apply {
                addHeader("Content-Disposition", "attachment; filename=\"${fileToServe.name}\"")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
        }
    }

    fun getIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address) return addr.hostAddress
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }
}