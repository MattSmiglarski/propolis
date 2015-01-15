// always a good idea to add an on console status listener
statusListener(OnConsoleStatusListener)

def appenderList = ["CONSOLE"]

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    }
}

root(INFO, appenderList)