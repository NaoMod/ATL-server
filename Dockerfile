FROM gradle

COPY . .
RUN gradle build

EXPOSE 8080
ENTRYPOINT [ "gradle", "run" ]