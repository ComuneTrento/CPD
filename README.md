# Collaborative Procedure Designer (CPD)

Intro

## Description



## Content

 * 1
 * 2
 * ...

## Building

To build the code:

    mvn clean install

To build the documentation:

    cd docs
    docker run -it -v `pwd`:/documents/ asciidoctor/docker-asciidoctor "./build.sh" "html"
    # or for fish
    docker run -it -v (pwd):/documents/ asciidoctor/docker-asciidoctor "./build.sh" "html"
