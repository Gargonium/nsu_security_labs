FROM gradle:9.4.1-jdk25

WORKDIR /app

COPY build.gradle settings.gradle gradle.properties ./

RUN gradle dependencies --no-daemon || true

COPY src src

EXPOSE 8080

CMD ["gradle", "bootRun", "--no-daemon"]