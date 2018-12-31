package mazerunner

import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.function.Function
import java.util.regex.Pattern


/**
 * Translates web socket messages into a corresponding movement in the maze.
 *
 * Expects inbound message to be in the form of:
 * ```
 * (x,y)
 * ```
 * ...positive coordinates. This handler then replies messages in the form of:
 * ```
 * (x,y)[]
 * (x,y)[(xn1,yn1),(xn2,yn2),(xn3,yn3),(xn4,yn4)]
 * ```
 * ...where (x,y) is the current position after the move request, and the values
 * (xn_,yn_) are the neighboring coordinates.
 *
 * @param factory the service containing the active maze and runners
 */
class MazeMovementWebSocketHandler(private val factory: MazeRunnerFactory,
                                   private val tagFunction: Function<WebSocketSession, Tag?>) : WebSocketHandler {

    private val dataBufferFactory = DefaultDataBufferFactory()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val tag = tagFunction.apply(session) ?: return session.close(CloseStatus.POLICY_VIOLATION)
        val runner = factory.create(tag) ?: return session.close(CloseStatus.POLICY_VIOLATION)

        return doSend(session, session.receive()
                .map { received -> received.payloadAsText }
                .map { payload -> extractPoint(payload) }
                .map { point -> runner.move(point) }
                .map { position ->
                    val buffer = dataBufferFactory.allocateBuffer()
                    writePosition(position, buffer)
                    WebSocketMessage(WebSocketMessage.Type.TEXT, buffer)
                }
        )
    }

    // TODO: workaround for suspected RxNetty WebSocket client issue
    // https://github.com/ReactiveX/RxNetty/issues/560
    private fun doSend(session: WebSocketSession, output: Publisher<WebSocketMessage>): Mono<Void> {
        return session.send(Mono.delay(Duration.ofMillis(100)).thenMany(output))
    }

    companion object {

        private val PATTERN = Pattern.compile("\\((-?\\d+),(-?\\d+)\\)")

        fun extractPoint(text: String): Point {
            val matcher = PATTERN.matcher(text)
            require(matcher.matches()) {
                "Expecting message to be in the form of '(x,y)', where x and y are " +
                        "positive integers, or (-1,-1) to get the current position. Got $text"
            }
            val x = Integer.parseInt(matcher.group(1))
            val y = Integer.parseInt(matcher.group(2))
            return Point(x, y)
        }

        fun writePosition(position: Position, buffer: DataBuffer) {
            writePoint(position.current, buffer)
            if (position.neighbors.isEmpty()) {
                buffer.write("[]".toByteArray())
                return
            }

            buffer.write('['.toByte())
            for (p in position.neighbors) {
                writePoint(p, buffer)
                buffer.write(','.toByte())
            }
            buffer.writePosition(buffer.writePosition() - 1) // move one backwards
            buffer.write(']'.toByte())
        }

        private fun writePoint(point: Point, buffer: DataBuffer) {
            buffer.write("(${point.x},${point.y})".toByteArray())
        }
    }

}