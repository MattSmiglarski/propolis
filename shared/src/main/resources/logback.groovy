import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.status.OnConsoleStatusListener
import propolis.shared.ColourThreadConverter

statusListener(OnConsoleStatusListener)

def appenderList = ["CONSOLE"]

appender("CONSOLE", ConsoleAppender) {

    conversionRule("threadColour", ColourThreadConverter)

    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %threadColour([%thread]) %highlight(%-5level) %logger{36} - %msg%n"
    }
}

root(INFO, appenderList)