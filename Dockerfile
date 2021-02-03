FROM hseeberger/scala-sbt:11.0.6_1.3.10_2.13.1

WORKDIR /app

COPY ./ ./

RUN sbt clean assembly

ENTRYPOINT bash


# enable wsl-2 docker
# docker build -t can-chord .
# docker run -it can-chord
# sbt clean compile run

# DCLI: docker images
#
# docker tag [Image-Name]   D-Hub-User/Repository:Tag
# docker tag 9dac0c9131b6 mttdavid/can-chord:course-project
#
# docker login docker.io
#
# docker push dockhubusername/[Repository]:mytag