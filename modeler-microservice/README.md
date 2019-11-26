The Collaborative Procedure Designer (CPD) is a web server&application
that provides services to draw administrative procedures in a
collaborative way.

Getting started
===============

These instructions will get you a copy of the project on your machine
for testing or development purposes.

-   The “[Building the application](#build)” section describes how to
    produce an executable copy of the application.

-   The “[Running the application](#run)” section describes how to set
    up the runtime environment and execute the application.

-   The “[Deploying the application as a docker container](#dockerization)”
    section describes how to build and set-up a docker container running the CPD application.

<a name="build"></a> Building the application
=============================================

This is a Java Maven project.

<a name="pom"></a> pom.xml
--------------------------

A Project Object Model (POM) is the fundamental unit of work in Maven.
There are two **build profiles** of the POM that deserve attention:
**production** and **develop**, depending on whether the project is
going to be built for production or for development purposes. Each
profile uses one of the two Java properties files created following the
configuration instructions in the “[example.properties](#properties)”
section:

-   `production.properties`: represents the end configuration file to
    use for building the test server and will usually only be configured
    once, based on your target server deploy configuration;

-   `develop.properties`: this (optional) file has to be used when you
    want to try the application locally in your development environment
    (e.g. eclipse) and try some different config parameters without
    touching the `production.properties`.

Before you compile from a new release, make sure to do a diff of the new
pulled `example.properties` with your own versions of
`production.properties` and/or `development.properties` in order to find
out any possible new or renamed property.

<a name="build-pre"></a> Build prerequisites
--------------------------------------------

In order to produce an executable copy of the application you’ll need
the following:

-   Java Development Kit 8.x

-   Maven 3

Configuration
-------------

The CPD’s root folder contains **four** files to support the
configuration, build and deploy of the server:

1.  `example.properties`

2.  `self-signed-keystore.sh`

3.  `prepare-bundle.sh`

4.  `deploy.sh`

Of these, just the **first** one needs to be used for the actual
configurations. The last three are helper scripts that can be used in
order to simplify the build and deployment phases.

There are other two scripts to help database maintenance:

-   `db-restore.sh`: this script requires the `dump` directory in the
    same path of execution. It should be executed before the first run.
    It creates the `cpd` database and indexes the collections. It also
    populates the db with some diagrams.

-   `db-dump.sh`: this script creates a backup of the database
    overwriting any previous data in the `dump` directory.

Both scripts require a parameter that represents the pilot ID
(trento|galicia|sheffield) in order to restore/dump the db from/in a
separate directory for each pilot.

**Example:**

    ./db-restore.sh trento

### <a name="properties"></a> example.properties

This is an example Java properties file that contains all the
configuration parameters for the application.

The POM handles two Java properties files called `production.properties`
and `develop.properties`. Create the `production.properties` by **making
a copy of this file** and replacing the property values accordingly. If
you’re going to test different configurations in your IDE, create the
`develop.properties` in the same way and refer to the “[Running from
your favourite development environment](#ide)” section for further
configurations.

-   `cpd.ssl.enabled`: `true` or `false` depending on whether you want
    to use https or http (this can impact on `cpd.server.scheme` and
    `cpd.server.allowedOriginPattern` properties, check and revise them
    accordingly);

-   `cpd.ssl.keystore.filename` (only if `cpd.ssl.enabled=true`): the
    path to the jks keystore file. Can be absolute or relative to the
    CPD home (e.g. `/home/citizenpedia/cpd-server/keystore.jks` or just
    `keystore.jks`);

If you plan to use your own signed certificate, make sure it is given to
the application in jsk format. If you don’t own a signed certificate,
you can create a jks self signed one by using the
[`self-signed-keystore.sh`](#self-signed) script.

-   `cpd.ssl.keystore.password` (only if `cpd.ssl.enabled=true`): the
    password of the keystore;

-   `cpd.server.scheme`: `https` or `http` depending on whether you want
    to use ssl on unencrypted connections (see `cpd.ssl.enabled`);

-   `cpd.server.host`: the hostname or the ip address of the machine
    executing the application (this can be `localhost` if you’re
    planning to access the application locally);

-   `cpd.server.port`: the port to use;

-   `cpd.server.baseHref`: the **base href** of the server, must start
    and end with a `/` character (e.g. `/` or `/cpd/`);

-   `cpd.server.allowedOriginPattern`: this is a **regex pattern** for
    [CORS](http://www.w3.org/TR/cors) allowed origins (use `***` to
    allow calls from the entire planet, but for security reasons it is
    always best to put in only the origins that will **actually** use
    the API, e.g. `http:\\\\/\\\\/localhost:(8080|8901)`;

Remember that in Java properties files the double backslash `\\` must be
escaped two times: `\\\\`).

-   `cpd.server.adminId`: The server admin’s ID (use the one generated
    by the aythorization system)

        cpd.server.adminId=example_admin_id

-   `cpd.server.pub.scheme`, `cpd.server.pub.host`,
    `cpd.server.pub.port`: these properties are similar to
    `cpd.server.scheme`, `cpd.server.host` and `cpd.server.port`
    respectively. Change them when the application is running behind a
    proxy: set these properties to the proxy scheme, domain and port
    values. An example excerpt from a `production.properties` of an
    instance running behind an Apache2 reverse proxy follows:

        cpd.server.scheme=http
        cpd.server.host=localhost
        cpd.server.port=8901
        ! server.public
        cpd.server.pub.scheme=https
        cpd.server.pub.host=simpatico.example.com
        cpd.server.pub.port=443

-   `cpd.app.useLocalAuth`: `true` or `false` in order to enable or
    disable the local database-based login;

-   `cpd.mongodb.host`: the mongodb hostname;

-   `cpd.mongodb.port`: the mongodb port;

-   `cpd.mongodb.username`: the mongodb username (leave blank in case of
    none);

-   `cpd.mongodb.password`: the mongodb password for user (leave blank
    in case of none);

-   `cpd.oauth2.origin`: the oauth2 origin to send to the authority
    (e.g. `http://localhost:8901`);

-   `cpd.oauth2.providers`: this property **must** be a list of comma
    separated json objects. Each json object must contain the following
    fields:

        {
          "provider":"AAC",                           // the id of the oauth2 provider
          "logoUrl":"assets/img/oauth2_aac_logo.png", // the url to the logo to show in the login form
          "site":"http://my.aac:8080",                // the site of the authorization server
          "authPath":"/aac/eauth/authorize",          // the path to the authorization endpoint
          "tokenPath":"/aac/oauth/token",             // the path to the token endpoint
          "clientId":"my aac app client id",          // the application client id
          "clientSecret":"my aac app cient secret",   // the application client secret
          "flows":[
            {
              "flowType":"IMPLICIT",                  // the oauth2 flow (see the following note)
              "scope":"profile.basicprofile.me",      // the comma or space delimited scopes
              "getUserProfile": "http://my.aac:8080/aac/basicprofile/me"
              // the endpoint at which to retrieve the user profile (absolute path)
            },
            {
              "flowType":"CLIENT"
            }
          ]
        }, {
          ...
        }

the CPD accepts three oauth2 flows: "AUTH\_CODE", "IMPLICIT" or
"CLIENT".

Remember that in Java properties files, in order to continue writing the
same string in a new line, a `\` must be placed at the end of the
previous line (see the `example.properties` file for an example).

In case you want to test google OAuth2 but don’t have an API account,
create a project in your [Google API Mangement
Console](https://console.developers.google.com/apis/credentials) and
then create the OAuth client ID for the web application.

In order to use google OAuth2 service, you have to add a redirect
callback URI for every different `cpd.oauth2.origin` and/or
`cpd.server.baseHref` the user can utilize to access the application in
the *authorized redirect URI list*.

The URI to put in your console must be written in the following form:

    <cpd.oauth2.origin><cpd.server.baseHref>oauth2/server/callback

e.g. using `cpd.oauth2.origin=http://localhost:8901` and
`cpd.server.baseHref=/cpd/`:

    http://localhost:8901/cpd/oauth2/server/callback

use the following property in the properties file:

    cpd.oauth2.providers=\
    {\
      "provider":"Google",\
      "logoUrl":"assets/img/oauth2_google_logo.png",\
      "site":"https://accounts.google.com",\
      "authPath":"/o/oauth2/auth",\
      "tokenPath":"https://www.googleapis.com/oauth2/v3/token",\
      "introspectionPath":"https://www.googleapis.com/oauth2/v3/tokeninfo",\
      "clientId":"the client id of your application",\
      "clientSecret":"the client secter of your application",\
      "flows":[\
        {\
          "flowType":"AUTH_CODE",\
          "scope":"email",\
          "getUserProfile": "https://www.googleapis.com/plus/v1/people/{userId}"\
        }\
      ]\
    }

### <a name="self-signed"></a> self-signed-keystore.sh

If you need to test the server in ssl (https) mode but don’t own a
signed certificate, this utility script will generate a new Java
keystore storing a self-signed certificate by using the JRE keytool
utility. It has pre-set values to produce a keystore named
`keystore.jks` with alias `simpatico` and password `simpatico`.
`<filename>`, `<alias>` and `<password>` can be passed as input
arguments. Type `./self-signed-keystore.sh --help` for details.

After the script is launched, the Java keytool will ask you to fill in
the prompts for your organization information. **When it asks for your
first and last name, enter the domain name of the server that users will
be entering to connect to the CPD application** (e.g.
`www.my-public-domain.com`).

### <a name="bundle"></a> prepare-bundle.sh

This script creates a bundle ready for deployment. It expects an input
parameter between one of these two possible values: `production` or
`develop`. In the case no parameter is given, it will be assumed
`production` by default. You can inspect the file to understand how the
`deploy-bundle` is set up.

The final bundle will be found under the `target/deploy-bundle`
directory. That directory can be copied to the target machine and
renamed to your liking. The application can then be started and stopped
with the provided `start.sh` and `stop.sh` scripts respectively.

Before launching the deployed bundle with `start.sh`, make sure the
machine you’re going to run the server satisfies the [Runtime prerequisites](#run-pre).

If the application is configured for ssl and you used a relative path in
the `cpd.keystore.filename`, make sure the path is relative to the
deployed bundle directory root (i.e. where the `start.sh` file is).

### <a name="deploy"></a> deploy.sh

This script has been added to simplify the deployment of the production
bundle by

1.  invoking the [`prepare-bundle.sh production`](#bundle) command;

2.  copying via ssh the produced `deploy-bundle` as `cpd-server` under
    the home of the given user (i.e. `/home/<user>/cpd-server`).

The script will eventually stop the running instance of the application
before the ssh copy and always start the newly deployed application
after the ssh copy.

Before launching the `deploy.sh` script, make sure the ssh target
machine you’re deploying the application satisfies the [Runtime
prerequisites](#run-pre).

The `deploy.sh` script requires **two** mandatory input parameters:

-   the `USERNAME` of the user account to be used on the remote machine.
    The application will run with that user’s privileges;

Never launch the application as `root` user!

-   the `SERVER` hostname or ip address of the remote machine where the
    application will be deployed (this should be equal to the
    `cpd.server.host` property value of the `production.properties`
    file).

<a name="run"></a> Running the application
==========================================

The following sections describe how to run the application from the
[deploy bundle](#bundle) or from your Integrated Development Environment
(IDE).

<a name="run-pre"></a> Runtime prerequisites
--------------------------------------------

The CPD runs on \*nix equipped machines. Before trying to launch the
server, make sure the following softwares/runtimes/libraries are
available at the target machine:

-   Java Runtime Environment 8.x

-   MongoDB 3.4

Before the first run of the application, execute the
[`db-restore.sh`](#db-restore) script in order to create the `cpd`
database and populate it with some data.

Running from the produced deploy bundle
---------------------------------------

If built with [`prepare-bundle.sh`](#bundle), the application can be
started with the `start.sh` script that can be found inside the bundled
package.

If built and deployed with [`deploy.sh`](#deploy), the application
should have been started automatically.

In both cases, the application can be stopped using the `stop.sh`
script.

Running from behind a reverse proxy server
------------------------------------------

Apart from REST, the CPD makes use of WebSocket. In order to enable the
WebSocket when behind a reverse proxy, some configurations need to be
addressed. Steps for Apache and NGINX are described below.

### 1. WebSocket with Apache

Enable `proxy_wstunnel` module; then

    ProxyPass "/cpd/eventbus"  "ws://[ip:port of CPD server]/cpd/eventbus"

or

    <Location /cpd/eventbus>
      ProxyPass ws://[ip:port of CPD server]/cpd/eventbus
    </Location>

### 2. WebSocket with NGINX

    # Socket.IO Support
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";

thanks to [smendez-hi](https://github.com/smendez-hi).

<a name="ide"></a> Running from your favourite development environment
----------------------------------------------------------------------

Make sure your development environment satisfies both the “[Build prerequisites](#build-pre)”
and the “[Running prerequisites](#run-pre)”.

### IDE configuration

There are extra configuration steps that must be taken for development
purpose. The application expects the following two directories:

1.  `./conf/`: directory containing the generated `config.json`
    configuration file;

2.  `./web/`: directory containing the static resources to be served.

So, create them as symbolic links in the directory you will launch the
application.

**Assuming you’ll run the launch command from the project root**:

1.  `ln -s target/deploy-bundle/conf conf`;

2.  `ln -s target/deploy-bundle/web web`.

Make sure the active POM profile is `develop`.

The configuration parameters can be changed in the `develop.properties`
file (see the “[example.properties](#properties)” section).

### Compilation

    mvn clean package [-P develop]

will generate a `cpd-server-[version]-fat.jar` Java **fat jar**, which
is a standalone *all-in-one* executable jar.  
Maven will automatically filter the `config.json` file based on the
`develop.properties` file and put it in the `target/deploy-bundle/conf`
directory for you.

If no profile is passed to the `mvn` command, maven will default to
`develop`.

### Execution

    java -jar target/cpd-server-[version]-fat.jar

Alternatively, you can configure your IDE to launch the application by
setting these run/debug configuration:

-   main class: `it.beng.microservice.common.Launcher`

-   arguments: `run it.beng.modeler.microservice.ModelerConfigVerticle`

Test the application
--------------------

After running the application, you can check that everything is working
by opening in your browser the url you defined in the relative
[`.properties`](#properties) file (e.g. `http://localhost:8901/cpd/`).

### User roles

User roles can be set by the application’s admin through the "Settings"
page. The current CPD version handles three types of roles for each
account:

1.  **system role** can be one of “user”, “admin”. It defines the logged
    in status and the main security role.

2.  **action role** can be one of “citizen”, “civil-servant”. It
    identifies the main features associated to the user.

3.  **diagram role** can be one of “owner”, “reviewer”, “editor”,
    “observer”. It identifies the collaboration role inside a diagram
    designing cycle.

<a name="dockerization"></a> Deploy the application as a docker container
=========================================================================

You can locally install a ready-to-run instance of the CPD application
by means of the docker framework. The following instructions assume that
the 18.03.1-ce version of the docker framework is going to be used.

<a name="docker-pull"></a> Install and run the docker container
---------------------------------------------------------------

### <a name="docker-run"></a> docker-run.sh

This script pulls the latest (up to date) CPD docker image from a remote
docker-hub repository and runs a container out of it. The script must be
run with superuser privileges (e.g., "sudo ./docker-run.sh"). If the
script succeeds, the user is prompted in a new bash shell within the
newly created container, where the following scripts will be available.

<a name="cpd-application"></a> Configure and run the CPD application
--------------------------------------------------------------------

### <a name="db-restore"></a> db-restore.sh

This script popultates the CPD databases with sample procedure diagrams.
It must be launched only once, before the very first run of the CPD
application.

### <a name="cpd-run"></a> start.sh

The script bundles the CPD configuration and run commands. First, the
run script invokes the configuration script
([configure.sh](#CPD-configure)). Upon a successful configuration, the
CPD application gets automatically run. Check with the log/cpd.log file
for any errors that may occur during the application boot. As for the
configuration script, when it is launched for the first time the user
will be asked to configure some parameters to correctly set-up the CPD
application before running it. Those values get persistently stored on
local folders. Subsequent run commands will cause the fecth of those
values from the local folders (no need to re-configure). Explicit
re-configurations of such parameters must be invoked through the
([configure.sh](#CPD-configure)) script.

### <a name="cpd-authentication"></a> oauth2providers.json

This file must be manually created and edited (use the "vim" editor
packaged with the docker image) to specify which oauth2 providers will
be called upon by the CPD in order to enforce the user authentication. A
template (docker.oauth2providers.json) can be used to figure out how to
correctly edit this file.

### <a name="cpd-configure"></a> configure.sh

The script allows the user to configure some important application
parameters. For each parameter, the default value is pre-loaded from a
template. Most of those values can just be accepted by the user as they
are. Some require the user to specify values according to the production
environment that the CPD application will be run into
(cpd.server.scheme, cpd.server.host, cpd.server.port, …..,
cpd.server.pub.scheme, cpd.server.pub.host, cpd.server.pub.port). See
section [example.properties](#properties) for hints on how to set up
each value.

<a name="cpd-update"></a> Update the CPD application
----------------------------------------------------

After installing a new version of the CPD application, the following
steps must be taken:

1.  remove the config-persistent directory from the host’s file system
    ("sudo rm -fr config-persistent")

2.  remove the config-persistent directory from the host’s file system
    ("sudo rm -fr mongo-persistent")
