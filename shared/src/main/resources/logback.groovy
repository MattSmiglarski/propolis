import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.status.OnConsoleStatusListener

statusListener(OnConsoleStatusListener)

def appenderList = ["CONSOLE"]

appender("CONSOLE", ConsoleAppender) {

    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %highlight(%-5level) [%thread] %logger{36} - %msg%n"
    }
}

root(INFO, appenderList)