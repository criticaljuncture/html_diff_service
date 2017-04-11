FROM openjdk:8

RUN adduser app -uid 1000 --system
RUN mkdir -p /var/log/html_diff_service && chown app /var/log/html_diff_service

USER app
COPY . /home/app
WORKDIR /home/app
RUN javac -cp servlet.jar:jetty.jar:daisydiff.jar HtmlDiffService.java -Xlint

RUN touch /var/log/html_diff_service/html_diff_service.log

EXPOSE 5001
CMD java -cp .:servlet.jar:jetty.jar:daisydiff.jar HtmlDiffService > /var/log/html_diff_service/html_diff_service.log 2>&1
